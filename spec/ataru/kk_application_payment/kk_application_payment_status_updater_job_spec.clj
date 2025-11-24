(ns ataru.kk-application-payment.kk-application-payment-status-updater-job-spec
  (:require [ataru.db.db :as db]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [speclj.core :refer [it describe should-throw should-not-throw stub
                                 should-have-invoked should-not-have-invoked
                                 should-be-nil tags with-stubs should= around before after]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.kk-application-payment.utils :as utils]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as updater-job]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.applications.application-store :as application-store]
            [ataru.test-utils :as test-utils :refer [set-fixed-time reset-fixed-time!]]
            [ataru.log.audit-log :as audit-log]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline]
            [ataru.kk-application-payment.kk-application-payment-store :as payment-store]))

(def test-person-oid
  (:person-oid application-fixtures/application-without-hakemusmaksu-exemption))
(def test-term "kausi_s")
(def test-year 2025)

(def fake-person-service (person-service/->FakePersonService))
(def fake-tarjonta-service (tarjonta-service/->MockTarjontaKoutaService))
(def fake-organization-service (organization-service/->FakeOrganizationService))
(def fake-ohjausparametrit-service (ohjausparametrit-service/new-ohjausparametrit-service))

(def audit-logger (audit-log/new-dummy-audit-logger))
(def test-maksut-secret "1234ABCD5678EFGH")

(defn invalidate-laskut-fn [_ _])

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
  (list-laskut-by-application-key [_ _] [])
  (invalidate-laskut [this keys] (invalidate-laskut-fn this keys)))

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
(def empty-get-haut-cache (reify cache-service/Cache
                           (get-from [_ _]
                             [])
                           (get-many-from [_ _])
                           (remove-from [_ _])
                           (clear-all [_])))

(def fake-attachment-deadline-service (attachment-deadline/->AttachmentDeadlineService fake-ohjausparametrit-service))

(def fake-form-by-id-cache (reify cache-service/Cache
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
  (map->FakeJobRunner {:attachment-deadline-service fake-attachment-deadline-service
                       :tarjonta-service            fake-tarjonta-service
                       :form-by-id-cache            fake-form-by-id-cache
                       :organization-service        fake-organization-service
                       :ohjausparametrit-service    fake-ohjausparametrit-service
                       :person-service              fake-person-service
                       :get-haut-cache              fake-get-haut-cache
                       :koodisto-cache              fake-koodisto-cache
                       :audit-logger                audit-logger
                       :maksut-service              mock-maksut-service}))

(def runner-with-empty-haku-cache
  (map->FakeJobRunner {:attachment-deadline-service fake-attachment-deadline-service
                       :tarjonta-service            fake-tarjonta-service
                       :organization-service        fake-organization-service
                       :ohjausparametrit-service    fake-ohjausparametrit-service
                       :person-service              fake-person-service
                       :get-haut-cache              empty-get-haut-cache
                       :koodisto-cache              fake-koodisto-cache
                       :audit-logger                audit-logger
                       :maksut-service              mock-maksut-service}))

(declare conn)
(declare spec)

(defn- get-tuition-payment-obligation-review [application-key hakukohde]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query
                                   conn
                                   ["select * FROM application_hakukohde_reviews WHERE application_key = ? AND requirement = ? AND hakukohde = ?"
                                    application-key "payment-obligation" hakukohde])
                                 first)))

(defn- store-tuition-fee-not-required-review [application-key hakukohde]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/insert! conn "application_hakukohde_reviews"
                                          {:application_key application-key
                                           :requirement "payment-obligation"
                                           :hakukohde hakukohde
                                           :state "not-obligated"})))


(defn- clear! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :applications [])
                            (jdbc/delete! conn :kk_application_payments [])
                            (jdbc/delete! conn :kk_application_payments_history [])))

(defn- check-mail-fn [application-key mail-content]
  (and
    (= (count (:recipients mail-content)) 1)
    (= "aku@ankkalinna.com" (first (:recipients mail-content)))
    (str/includes? (:subject mail-content) "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään")
    (str/includes? (:subject mail-content) (str "(Hakemusnumero: " application-key ")"))
    (str/includes? (:body mail-content) "Voit maksaa hakemusmaksun alla olevan linkin kautta:")
    (str/includes? (:body mail-content) (str "Hakemusnumerosi on: " application-key))
    (str/includes? (:body mail-content) (str "maksut-ui/fi?secret=" test-maksut-secret))
    (str/includes? (:body mail-content) "Olet hakenut haussa: testing2")
    (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/fi/sivu/hakemusmaksu")))

(defn- edit-application-as-virkailija [application update-answers-fn]
  (application-store/update-application
    (-> application
        (update :answers update-answers-fn)
        (assoc :virkailija-secret (test-utils/create-fake-virkailija-rewrite-secret (:key application)))
        (dissoc :secret))
    nil form-fixtures/payment-exemption-test-form {:identity {:oid "1.2.246.562.24.00000001213"}} audit-logger nil))

(describe "kk-application-payment-status-updater-job"
          (tags :unit)
          (with-stubs)

          (before
            (clear!)
            (set-fixed-time "2025-01-15T14:50:00"))

          (after
            (reset-fixed-time!))

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response
                                                                  []))]
                    (spec)))

          (it "should not fail when nothing to update"
              (should-not-throw (updater-job/update-kk-payment-status-for-all-handler {} runner)))

          (it "should be able ton handle hakuaikas without any endtimes"
              (should= false (utils/time-is-before-some-hakuaika-grace-period? {:oid "test-haku-oid" :hakuajat [{:start (time/date-time 2025 10 1) :end nil}]} 180 (time/date-time 2025 10 15))))

          (it "should be able ton handle hakuaikas with some endtimes"
              (should= true (utils/time-is-before-some-hakuaika-grace-period? {:oid "test-haku2-oid" :hakuajat [{:start (time/date-time 2025 10 1) :end (time/date-time 2025 10 30)} {:start (time/date-time 2025 11 1) :end nil}]} 180 (time/date-time 2025 10 15))))

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

          (it "should throw an exception when person and term found but updater doesn't find eligible applications"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)]
                ; Here the handler first resolves the person from application correctly, but then the updater
                ; function itself queries for all applications with hakus from get-haku-cache (= all haku oids from applications)
                ; and finds none from an empty one. This may happen with the very first application(s) for haku until
                ; cache is completely refreshed, so the job runner needs to retry.
                (should-throw
                  (updater-job/update-kk-payment-status-for-person-handler
                    {:application_id application-id} runner-with-empty-haku-cache))))

          (it "should update payment status fetching person and term with application key"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    application-key (:key (application-store/get-application application-id))
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:application_key application-key} runner)
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

          (it "should create a reminder e-mail, a sending job and a sent event"
              ;Note! this testcase fails if executed within the timeframe of 1 or 2 days before the beginning of DST (kesäaika).
              ;To bypass this, change the due-date of kk-application-payment below so that it is 1 day after current date,
              ;instead of default 2 days. Or make some permanent correction to the test case.
              (with-redefs [start-runner-job (stub :start-job)]
                (let [reminder-maksut-secret "54215421ABCDABCD"
                      application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       nil)
                      application-key (:key (application-store/get-application application-id))
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (= "aku@ankkalinna.com" (first (:recipients mail-content)))
                                        (str/includes? (:subject mail-content) "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään")
                                        (str/includes? (:subject mail-content) (str "(Hakemusnumero: " application-key ")"))
                                        (str/includes? (:body mail-content) "Olet hakenut haussa: testing2")
                                        (str/includes? (:body mail-content) (str "Hakemusnumerosi on: " application-key))
                                        (str/includes? (:body mail-content) "Emme ole vielä saaneet maksusuoritustasi hakemusmaksua koskien")
                                        (str/includes? (:body mail-content) "23:59:00")
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
                      payment (first (payment/get-raw-payments [application-key]))
                      events (filter #(= (:event-type %) "kk-application-payment-reminder-email-sent")
                                     (application-store/get-application-events application-key))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret reminder-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret]))
                  (should= 1 (count events)))))

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

                      payment (first (payment/get-raw-payments [application-key]))
                      events (filter #(= (:event-type %) "kk-application-payment-reminder-email-sent")
                                     (application-store/get-application-events application-key))]
                  (should-not-have-invoked :start-job)
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret reminder-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret]))
                  (should= 0 (count events)))))

          (it "should call maksut payment invalidation when changing payments status to ok-by-proxy"
              (with-redefs [invalidate-laskut-fn (stub :invalidate-laskut)]
                (let [application-ids (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       [application-fixtures/application-without-hakemusmaksu-exemption
                                        application-fixtures/application-without-hakemusmaksu-exemption
                                        application-fixtures/application-without-hakemusmaksu-exemption
                                        application-fixtures/application-without-hakemusmaksu-exemption])
                      application-keys (map #(:key (application-store/get-application %)) application-ids)
                      _ (payment-store/create-or-update-kk-application-payment!
                          {:application-key      (first application-keys)
                           :state                (:paid payment/all-states)
                           :reason               nil
                           :due-date             (time-format/unparse payment/default-time-format
                                                                      (time/plus (time/today-at 12 0 0)
                                                                                 (time/days 3)))
                           :total-sum            payment/kk-application-payment-amount
                           :maksut-secret        test-maksut-secret
                           :required-at          "now()"
                           :notification-sent-at nil
                           :approved-at          "now()"})
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      payments (payment/get-raw-payments application-keys)
                      paid-payments (filter #(= (:paid payment/all-states) (:state %)) payments)
                      proxy-payments (filter #(= (:ok-by-proxy payment/all-states) (:state %)) payments)
                      check-keys-fn (fn [keys] (= (set keys) (set (map :application-key proxy-payments))))]
                  (should= 1 (count paid-payments))
                  (should= 3 (count proxy-payments))
                  (should-have-invoked :invalidate-laskut
                                       {:with [:*
                                               check-keys-fn]}))))

          (it "should not call maksut payment invalidation when changing payments status to other state than ok-by-proxy"
              (with-redefs [invalidate-laskut-fn (stub :invalidate-laskut)]
                (let [application-ids (unit-test-db/init-db-fixture
                                        form-fixtures/payment-exemption-test-form
                                        [application-fixtures/application-without-hakemusmaksu-exemption
                                         application-fixtures/application-without-hakemusmaksu-exemption])
                      application-keys (map #(:key (application-store/get-application %)) application-ids)
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      payments (payment/get-raw-payments application-keys)
                      awaiting-payments (filter #(= (:awaiting payment/all-states) (:state %)) payments)]
                  (should= 2 (count awaiting-payments))
                  (should-not-have-invoked :invalidate-laskut))))

          (it "should create a payment and e-mail sending job"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       nil)
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      application-key (:key (application-store/get-application application-id))
                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               (partial check-mail-fn application-key)]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret test-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "should create a payment and e-mail sending job for english application"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       (merge application-fixtures/application-without-hakemusmaksu-exemption
                                              {:lang "en"})
                                       nil)
                      application-key (:key (application-store/get-application application-id))
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (str/includes? (:subject mail-content) "Studyinfo: Application fee, reminder to pay the fee by")
                                        (str/includes? (:subject mail-content) (str "(Application number: " application-key ")"))
                                        (str/includes? (:body mail-content) "You have applied in: testing4")
                                        (str/includes? (:body mail-content) (str "Your application number is: " application-key))
                                        (str/includes? (:body mail-content) "You can find more information about the application fee and exemptions from the fee on our")
                                        (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/en/sivu/application-fee")
                                        (str/includes? (:body mail-content) (str "maksut-ui/en?secret=" test-maksut-secret))))
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret test-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "should create a payment and e-mail sending job for swedish application"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       (merge application-fixtures/application-without-hakemusmaksu-exemption
                                              {:lang "sv"})
                                       nil)
                      application-key (:key (application-store/get-application application-id))
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (str/includes? (:subject mail-content) "Studieinfo: Ansökningsavgift, betala avgiften senast")
                                        (str/includes? (:subject mail-content) (str "(Ansökningsnummer: " application-key ")"))
                                        (str/includes? (:body mail-content) "Du har ansökt i följande ansökan: testing3")
                                        (str/includes? (:body mail-content) (str "Ditt ansökningsnummer är: " application-key))
                                        (str/includes? (:body mail-content) "Du hittar mer information om ansökningsavgiften och grunderna för befrielse från avgiften på vår")
                                        (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/sv/sivu/ansoekningsavgift")
                                        (str/includes? (:body mail-content) (str "maksut-ui/sv?secret=" test-maksut-secret))))
                      _ (updater-job/update-kk-payment-status-for-person-handler
                          {:person_oid test-person-oid :term test-term :year test-year} runner)
                      payment (first (payment/get-raw-payments [application-key]))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]})
                  (should=
                    {:application-key application-key :state (:awaiting payment/all-states)
                     :maksut-secret test-maksut-secret}
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "creates payment email resending job and event"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
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
                           :maksut-secret        test-maksut-secret
                           :required-at          "now()"
                           :notification-sent-at nil
                           :approved-at          nil})
                      _ (updater-job/resend-payment-email runner application-key nil)
                      events (filter #(= (:event-type %) "kk-application-payment-email-resent")
                                     (application-store/get-application-events application-key))]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               (partial check-mail-fn application-key)]})
                  (should= 1 (count events)))))

          (it "should set tuition fee obligation for non fi/sv hakukohde when payment state changes to awaiting"
              ; Initial state: hakukohde in the application has only english as teaching language,
              ; and the application / person has no exemption, meaning the application should require both
              ; application fee AND tuition fee for the hakukohde.
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-tuition-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should= {:application_key application-key, :requirement "payment-obligation",
                          :state "obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde]))))

          (it "should set tuition fee obligation for non fi/sv hakukohde when payment state changes to ok-by-proxy"
              ; Initial state: hakukohde in both applications has only english as teaching language,
              ; and the application / person has no exemption, and we mark one of the applications paid manually.
              ; This means the other application should not require application payment BUT should still require
              ; tuition fee.
              (let [application-ids (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      [application-fixtures/application-without-hakemusmaksu-exemption
                                       application-fixtures/application-without-hakemusmaksu-exemption])
                    [first-key second-key] (map #(:key (application-store/get-application %)) application-ids)
                    _ (payment-store/create-or-update-kk-application-payment!
                        {:application-key      first-key
                         :state                (:paid payment/all-states)
                         :reason               nil
                         :due-date             (time-format/unparse payment/default-time-format
                                                                    (time/plus (time/today-at 12 0 0)
                                                                               (time/days 3)))
                         :total-sum            payment/kk-application-payment-amount
                         :maksut-secret        test-maksut-secret
                         :required-at          "now()"
                         :notification-sent-at nil
                         :approved-at          "now()"})
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    payment (first (payment/get-raw-payments [second-key]))
                    obligation (get-tuition-payment-obligation-review second-key "payment-info-test-kk-hakukohde")]
                (should= (:ok-by-proxy payment/all-states) (:state payment))
                (should= {:application_key second-key, :requirement "payment-obligation",
                          :state "obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde]))))

          (it "should not set tuition fee obligation for non fi/sv hakukohde when payment state changes to not-required"
              ; Initial state: hakukohde in the application has only english as teaching language,
              ; but the application / person has an exemption, meaning the application should require neither
              ; application fee nor tuition fee for the hakukohde.
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-with-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-tuition-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:not-required payment/all-states) (:state payment))
                (should-be-nil obligation)))

          (it "should be flagged to send 'virkailija edited' kk application payment email after virkailija edited nationality"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                      (unit-test-db/init-db-fixture
                                        form-fixtures/payment-exemption-test-form
                                        application-fixtures/application-without-hakemusmaksu-exemption
                                        nil))
                        application-key (:key application)
                        edited-application (edit-application-as-virkailija
                                             application #(map (fn [answer]
                                                                 (if (= "nationality" (:key answer))
                                                                   (assoc answer :value [["004"]])
                                                                   answer)) %))
                        _ (updater-job/update-kk-payment-status-for-person-handler
                            {:person_oid test-person-oid
                             :term test-term
                             :year test-year
                             :application_key application-key
                             :application_id (:id edited-application)}
                            runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= true @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should be flagged to send 'virkailija edited' kk application payment email after virkailija edited kk-application-payment-option"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                     (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      application-fixtures/application-with-hakemusmaksu-exemption
                                      nil))
                        application-key (:key application)
                        edited-application (edit-application-as-virkailija
                                            application #(map (fn [answer]
                                                                (if (= "kk-application-payment-option" (:key answer))
                                                                  (assoc answer :value "8")
                                                                  answer)) %))
                        _ (updater-job/update-kk-payment-status-for-person-handler
                           {:person_oid test-person-oid
                            :term test-term
                            :year test-year
                            :application_key application-key
                            :application_id (:id edited-application)}
                           runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= true @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should be flagged to send 'virkailija edited' kk application payment email after virkailija marked kk application payment attachment as incomplete"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                     (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      application-fixtures/application-without-hakemusmaksu-exemption
                                      nil))
                        application-key (:key application)
                        _ (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                              :attachment_key "brexit-permit-attachment"
                                                              :hakukohde "payment-info-test-kk-hakukohde"
                                                              :state "incomplete-attachment"}])
                        _ (updater-job/update-kk-payment-status-for-person-handler
                           {:person_oid test-person-oid
                            :term test-term
                            :year test-year
                            :application_key application-key
                            :application_id (:id application)}
                           runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= true @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should be flagged to send 'virkailija edited' kk application payment email after virkailija marked kk application payment attachment as missing"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                     (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      application-fixtures/application-without-hakemusmaksu-exemption
                                      nil))
                        application-key (:key application)
                        _ (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                              :attachment_key "brexit-permit-attachment"
                                                              :hakukohde "payment-info-test-kk-hakukohde"
                                                              :state "not-checked"}])
                        _ (application-store/save-attachment-hakukohde-review application-key
                                                                              "payment-info-test-kk-hakukohde"
                                                                              "brexit-permit-attachment"
                                                                              "attachment-missing"
                                                                              {:identity
                                                                               {:oid "1.2.246.562.24.00000001213"}}
                                                                              audit-logger)
                        _ (updater-job/update-kk-payment-status-for-person-handler
                           {:person_oid test-person-oid
                            :term test-term
                            :year test-year
                            :application_key application-key
                            :application_id (:id application)}
                           runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= true @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should be flagged to send 'normal' kk application payment email after virkailija edited fields not affecting payment"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                     (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      application-fixtures/application-without-hakemusmaksu-exemption
                                      nil))
                        application-key (:key application)
                        edited-application (edit-application-as-virkailija
                                            application #(map (fn [answer]
                                                                (if (= "home-town" (:key answer))
                                                                  (assoc answer :value "003")
                                                                  answer)) %))
                        _ (updater-job/update-kk-payment-status-for-person-handler
                           {:person_oid test-person-oid
                            :term test-term
                            :year test-year
                            :application_key application-key
                            :application_id (:id edited-application)}
                           runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= false @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should be flagged to send 'normal' kk application payment email after virkailija marked non kk application payment related review as invalid"
              (let [virkailija-edited (atom nil)]
                (with-redefs [updater-job/create-payment-and-send-email
                              (fn [_ _ _ virkailija-edited?]
                                (swap! virkailija-edited (constantly virkailija-edited?)))]
                  (let [application (application-store/get-application
                                     (unit-test-db/init-db-fixture
                                      form-fixtures/payment-exemption-test-form
                                      application-fixtures/application-without-hakemusmaksu-exemption
                                      nil))
                        application-key (:key application)
                        _ (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                              :attachment_key "none-passport-attachment"
                                                              :hakukohde "payment-info-test-kk-hakukohde"
                                                              :state "attachment-missing"}])
                        _ (updater-job/update-kk-payment-status-for-person-handler
                           {:person_oid test-person-oid
                            :term test-term
                            :year test-year
                            :application_key application-key
                            :application_id (:id application)}
                           runner)
                        payment (first (payment/get-raw-payments [application-key]))]
                    (should= false @virkailija-edited)
                    (should= (:awaiting payment/all-states) (:state payment))))))

          (it "should send 'virkailija edited' kk application payment email after virkailija edited nationality"
            (with-redefs [start-runner-job (stub :start-job)]
              (let [application (application-store/get-application
                                  (unit-test-db/init-db-fixture
                                    form-fixtures/payment-exemption-test-form
                                    application-fixtures/application-without-hakemusmaksu-exemption
                                    nil))
                    application-key (:key application)
                    edited-application (edit-application-as-virkailija
                                         application #(map (fn [answer]
                                                             (if (= "nationality" (:key answer))
                                                               (assoc answer :value [["004"]])
                                                               answer)) %))
                    check-mail-fn (fn [mail-content]
                                    (and
                                      (str/includes? (:subject mail-content) "Opintopolku: Hakemusmaksuvelvollisuutesi on tarkastettu - maksathan viimeistään")
                                      (str/includes? (:subject mail-content) (str "(Hakemusnumero: " application-key ")"))
                                      (str/includes? (:body mail-content) "Hakemusmaksuvelvollisuutesi on tarkastettu, eikä sinulla ole hakemuksellasi hakemusmaksusta vapauttavaa")
                                      (str/includes? (:body mail-content) "Koulutuksen alkamiskausi on: syksy 2025")))
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid
                         :term test-term
                         :year test-year
                         :application_key application-key
                         :application_id (:id edited-application)} runner)
                    payment (first (payment/get-raw-payments [application-key]))]
                (should= (:awaiting payment/all-states) (:state payment))
                (should-have-invoked :start-job
                                     {:with [:* :*
                                             "ataru.kk-application-payment.kk-application-payment-email-job"
                                             check-mail-fn]}))))

          (it "should not set tuition fee obligation for fi/sv hakukohde"
              ; Initial state: hakukohde in the application has swedish and/or finnish in its teaching languages,
              ; so even the application / person has no exemption, the application should require only an
              ; application fee, but no tuition fee for the hakukohde.
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     (merge
                                       application-fixtures/application-without-hakemusmaksu-exemption
                                       {:hakukohde ["payment-info-test-kk-fisv-hakukohde"]})
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-tuition-payment-obligation-review application-key "payment-info-test-kk-fisv-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should-be-nil obligation)))

          (it "should not override a non-automatic obligation"
              ; Initial state: hakukohde in the application has only english as teaching language,
              ; and application / person has no exemption, but there's a human review already for the tuition.
              ; Application fee should be required, but tuition fee state should not change automatically anymore.
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    application-key (:key (application-store/get-application application-id))
                    _ (store-tuition-fee-not-required-review application-key "payment-info-test-kk-hakukohde")
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-tuition-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should= {:application_key application-key, :requirement "payment-obligation",
                          :state "not-obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde]))))


          (it "should reset kk application payment obligation state when new attachments are added after review"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-with-hakemusmaksu-exemption
                                     nil)
                    application-key (:key (application-store/get-application application-id))]
                (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                    :attachment_key "passport-attachment"
                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                    :state "attachment-missing"
                                                    :modified_time (time/now)}])
                ; setting application payment obligation to reviewed -> bypasses attachment deadline
                (unit-test-db/init-db-application-hakukohde-review-fixture {:hakukohde "payment-info-test-kk-hakukohde"
                                                                            :review-requirement "kk-application-payment-obligation"
                                                                            :review-state "reviewed"} application-key)
                (updater-job/update-kk-payment-status-for-person-handler {:application_id application-id} runner)
                (should= {:application-key application-key
                          :state (:awaiting payment/all-states)
                          :maksut-secret test-maksut-secret}
                         (select-keys (first (payment/get-raw-payments [application-key])) [:application-key :state :maksut-secret]))

                (set-fixed-time "2025-01-15T14:55:00")
                ; adding new attachments (not checked) within deadline resets application payment obligation state
                (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                    :attachment_key "passport-attachment"
                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                    :state "not-checked"
                                                    :modified_time (time/now)
                                                    :updated? true}])
                (updater-job/update-kk-payment-status-for-person-handler {:application_id application-id} runner)
                (should= {:requirement "kk-application-payment-obligation"
                          :state "unreviewed"
                          :hakukohde "payment-info-test-kk-hakukohde"
                          :application-key application-key}
                         (select-keys (first (payment/get-kk-application-payment-obligation-reviews application-key)) [:requirement :state :hakukohde :application-key]))
                (should= {:application-key application-key
                          :state (:not-required payment/all-states)}
                         (select-keys (first (payment/get-raw-payments [application-key])) [:application-key :state]))))

          (it "should not reset kk application payment obligation state when new attachments are added after review and payment is not required"
              (let [application-id (unit-test-db/init-db-fixture
                                    form-fixtures/payment-exemption-test-form
                                    application-fixtures/application-with-hakemusmaksu-exemption
                                    nil)
                    application-key (:key (application-store/get-application application-id))]
                (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                    :attachment_key "passport-attachment"
                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                    :state "checked"
                                                    :modified_time (time/now)}])
                ; setting application payment obligation to reviewed -> bypasses attachment deadline
                (unit-test-db/init-db-application-hakukohde-review-fixture {:hakukohde "payment-info-test-kk-hakukohde"
                                                                            :review-requirement "kk-application-payment-obligation"
                                                                            :review-state "reviewed"} application-key)
                (updater-job/update-kk-payment-status-for-person-handler {:application_id application-id} runner)
                (should= {:application-key application-key
                          :state (:not-required payment/all-states)}
                         (select-keys (first (payment/get-raw-payments [application-key])) [:application-key :state]))

                (set-fixed-time "2025-01-15T14:55:00")
                (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                    :attachment_key "passport-attachment"
                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                    :state "not-checked"
                                                    :modified_time (time/now)
                                                    :updated? true}])
                (updater-job/update-kk-payment-status-for-person-handler {:application_id application-id} runner)
                (should= {:requirement "kk-application-payment-obligation"
                          :state "reviewed"
                          :hakukohde "payment-info-test-kk-hakukohde"
                          :application-key application-key}
                         (select-keys (first (payment/get-kk-application-payment-obligation-reviews application-key)) [:requirement :state :hakukohde :application-key]))
                (should= {:application-key application-key
                          :state (:not-required payment/all-states)}
                         (select-keys (first (payment/get-raw-payments [application-key])) [:application-key :state])))))
