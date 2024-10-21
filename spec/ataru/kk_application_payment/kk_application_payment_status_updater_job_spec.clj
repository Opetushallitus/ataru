(ns ataru.kk-application-payment.kk-application-payment-status-updater-job-spec
  (:require [ataru.db.db :as db]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [it describe should-not-throw stub should-have-invoked
                                 tags with-stubs should= around before]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as updater-job]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]))

(def test-person-oid
  (:person-oid application-fixtures/application-without-hakemusmaksu-exemption))
(def test-term "kausi_s")
(def test-year 2025)

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [_ _ _ _]))

(def fake-person-service (person-service/->FakePersonService))
(def fake-tarjonta-service (tarjonta-service/->MockTarjontaKoutaService))

(defrecord MockMaksutService []
  MaksutServiceProtocol

  (create-kk-application-payment-lasku [_ _] {})
  (create-kasittely-lasku [_ _] {})
  (create-paatos-lasku [_ _] {})
  (list-lasku-statuses [_ _] {})
  (list-laskut-by-application-key [_ _] []))

(def mock-maksut-service (->MockMaksutService))

(def fake-haku-cache (reify cache-service/Cache
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

(def runner
  (map->FakeJobRunner {:tarjonta-service fake-tarjonta-service
                       :person-service   fake-person-service
                       :haku-cache       fake-haku-cache
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

          (before (clear!))

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response))]
                    (spec)))

          (it "should not fail when nothing to update"
              (should-not-throw (updater-job/update-kk-payment-status-scheduler-handler {} runner)))

          (it "should queue update for relevant haku"
              (with-redefs [updater-job/update-statuses-for-haku (stub :update-statuses-for-haku)]
                (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                              application-fixtures/application-without-hakemusmaksu-exemption
                                              nil)
                (updater-job/update-kk-payment-status-scheduler-handler {} runner)
                (should-have-invoked :update-statuses-for-haku
                                     {:times 1
                                      :with [#(= (:oid %) "payment-info-test-kk-haku") :*]})))

          (it "should update payment status for oid"
              (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                            application-fixtures/application-without-hakemusmaksu-exemption
                                            nil)
              (updater-job/update-kk-payment-status-handler
                {:person_oid test-person-oid :term test-term :year test-year}
                runner)
              (let [state-data (first (payment/get-raw-payments [test-person-oid] test-term test-year))]
                (should=
                  {:person-oid "1.2.3.4.5.303" :start-term "kausi_s" :start-year 2025
                   :state (:awaiting payment/all-states)}
                  (dissoc state-data :id :created-time :modified-time)))))