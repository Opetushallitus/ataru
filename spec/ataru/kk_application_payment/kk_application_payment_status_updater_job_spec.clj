(ns ataru.kk-application-payment.kk-application-payment-status-updater-job-spec
  (:require [ataru.db.db :as db]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [speclj.core :refer [it describe should-not-throw stub should-have-invoked should-not-have-invoked
                                 tags with-stubs should= around before]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as updater-job]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.applications.application-store :as application-store]
            [ataru.kk-application-payment.kk-application-payment-store :as payment-store]))

(def test-person-oid
  (:person-oid application-fixtures/application-without-hakemusmaksu-exemption))
(def test-term "kausi_s")
(def test-year 2025)

(def fake-person-service (person-service/->FakePersonService))
(def fake-tarjonta-service (tarjonta-service/->MockTarjontaKoutaService))

(def test-maksut-secret "1234ABCD5678EFGH")

(defrecord MockMaksutService []
  MaksutServiceProtocol

  (create-kk-application-payment-lasku [_ lasku] {:order_id (payment/maksut-reference->maksut-order-id (:reference lasku))
                                                  :first_name "Test"
                                                  :last_name "Person"
                                                  :amount (:amount lasku)
                                                  :status :active
                                                  :due_date ""
                                                  :origin (:origin lasku)
                                                  :reference (:reference lasku)
                                                  :secret test-maksut-secret})
  (create-kasittely-lasku [_ _] {})
  (create-paatos-lasku [_ _] {})
  (list-lasku-statuses [_ _] {})
  (list-laskut-by-application-key [_ _] []))

(def mock-maksut-service (->MockMaksutService))

(def fake-get-haut-cache (reify cache-service/Cache
                           (get-from [_ _]
                             [{:haku "payment-info-test-kk-haku"}])
                           (get-many-from [_ _])
                           (remove-from [_ _])
                           (clear-all [_])))
(def fake-koodisto-cache (reify cache-service/Cache
                           (get-from [_ _])
                           (get-many-from [_ _])
                           (remove-from [_ _])
                           (clear-all [_])))

(defn start-runner-job [_ _ _ _])

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [this connection job-type initial-state]
    (start-runner-job this connection job-type initial-state)))

(def runner
  (map->FakeJobRunner {:tarjonta-service fake-tarjonta-service
                       :person-service   fake-person-service
                       :get-haut-cache   fake-get-haut-cache
                       :koodisto-cache   fake-koodisto-cache
                       :maksut-service   mock-maksut-service}))

(declare conn)
(declare spec)

(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :applications [])
                            (jdbc/delete! conn :kk_application_payments [])
                            (jdbc/delete! conn :kk_application_payments_history [])))

(describe "kk-application-payment-status-updater-job"
          (tags :unit)
          (with-stubs)

          (before
            (clear!))

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response))]
                    (spec)))

          (it "should not fail when nothing to update"
              (should-not-throw (updater-job/update-kk-payment-status-for-all-handler {} runner)))

          (it "should queue update for relevant haku"
              (with-redefs [updater-job/update-statuses-for-haku (stub :update-statuses-for-haku)]
                (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                              application-fixtures/application-without-hakemusmaksu-exemption
                                              nil)
                (updater-job/update-kk-payment-status-for-all-handler {} runner)
                (should-have-invoked :update-statuses-for-haku
                                     {:times 1
                                      :with [#(= (:oid %) "payment-info-test-kk-haku") :*]})))

          (it "should update payment status fetching person and term with application id"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:application_id application-id} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))]
                (should=
                  {:application-key application-key :state (:awaiting payment/all-states)
                   :maksut-secret test-maksut-secret}
                  (select-keys payment [:application-key :state :maksut-secret]))))

          (it "should update payment status for oid"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))]
                (should=
                  {:application-key application-key :state (:awaiting payment/all-states)
                   :maksut-secret test-maksut-secret}
                  (select-keys payment [:application-key :state :maksut-secret]))))

          (it "should create a reminder e-mail and a sending job"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [reminder-maksut-secret "54215421ABCDABCD"
                      application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       nil)
                      application-key (:key (application-store/get-application application-id))
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (str/includes? (:body mail-content) "Hakemusmaksusi erääntyy pian (fi)")
                                        (str/includes? (:body mail-content) reminder-maksut-secret)))
                      _ (payment-store/create-or-update-kk-application-payment!
                          {:application-key      application-key
                           :state                (:awaiting payment/all-states)
                           :reason               nil
                           :due-date             (time-format/unparse payment/default-time-format
                                                                      (time/plus (time/today-at 12 0 0)
                                                                                 (time/days 2)))
                           :total-sum            payment/kk-application-payment-amount
                           :maksut-secret        reminder-maksut-secret
                           :required-at          "now()"
                           :notification-sent-at nil
                           :approved-at          nil})
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)

                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret reminder-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "should not create a reminder e-mail and a sending job too early"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [reminder-maksut-secret "54215421ABCDABCD"
                      application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       nil)
                      application-key (:key (application-store/get-application application-id))
                      _ (payment-store/create-or-update-kk-application-payment!
                          {:application-key      application-key
                           :state                (:awaiting payment/all-states)
                           :reason               nil
                           :due-date             (time-format/unparse payment/default-time-format
                                                                      (time/plus (time/today-at 12 0 0)
                                                                                 (time/days 3)))
                           :total-sum            payment/kk-application-payment-amount
                           :maksut-secret        reminder-maksut-secret
                           :required-at          "now()"
                           :notification-sent-at nil
                           :approved-at          nil})
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)

                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-not-have-invoked :start-job)
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret reminder-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "should create a payment e-mail and a sending job"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       nil)
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (= (count (:recipients mail-content)) 1)
                                        (= "aku@ankkalinna.com" (first (:recipients mail-content)))
                                        (not-empty (:subject mail-content))
                                        (str/includes? (:body mail-content) "Voit maksaa hakemusmaksun osoitteessa (fi)")
                                        (str/includes? (:body mail-content) test-maksut-secret)))
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      application-key (:key (application-store/get-application application-id))
                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret test-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret]))))))