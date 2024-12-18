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
                                 should-be-nil tags with-stubs should= around before]]
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

(defn- get-payment-obligation-review [application-key hakukohde]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query
                                   conn
                                   ["select * FROM application_hakukohde_reviews WHERE application_key = ? AND requirement = ? AND hakukohde = ?"
                                    application-key "payment-obligation" hakukohde])
                                 first)))

(defn- store-not-obligated-review [application-key hakukohde]
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

(defn- check-mail-fn [mail-content]
  (and
    (= (count (:recipients mail-content)) 1)
    (= "aku@ankkalinna.com" (first (:recipients mail-content)))
    (str/includes? (:subject mail-content) "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään")
    (str/includes? (:body mail-content) "Voit maksaa hakemusmaksun allaolevan linkin kautta.")
    (str/includes? (:body mail-content) (str "maksut-ui/fi?secret=" test-maksut-secret))
    (str/includes? (:body mail-content) "Olet hakenut haussa: testing2")
    (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/fi/sivu/hakemusmaksu")))

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
                                        (str/includes? (:subject mail-content) "Opintopolku: Hakemusmaksu, maksathan maksun viimeistään")
                                        (str/includes? (:body mail-content) "Olet hakenut haussa: testing2")
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
                                               check-mail-fn]})
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
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (str/includes? (:subject mail-content) "Studyinfo: application fee, reminder to pay the fee by")
                                        (str/includes? (:body mail-content) "You have applied in: testing4")
                                        (str/includes? (:body mail-content) "You can find more information about application fees on our website")
                                        (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/en/sivu/application-fee")
                                        (str/includes? (:body mail-content) (str "maksut-ui/en?secret=" test-maksut-secret))))
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
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "should create a payment and e-mail sending job for swedish application"
              (with-redefs [start-runner-job (stub :start-job)]
                (let [application-id (unit-test-db/init-db-fixture
                                       form-fixtures/payment-exemption-test-form
                                       (merge application-fixtures/application-without-hakemusmaksu-exemption
                                              {:lang "sv"})
                                       nil)
                      check-mail-fn (fn [mail-content]
                                      (and
                                        (str/includes? (:subject mail-content) "Studieinfo: Ansökningsavgift, betala avgiften senast")
                                        (str/includes? (:body mail-content) "Du har ansökt i följande ansökan: testing3")
                                        (str/includes? (:body mail-content) "Mera information om ansökningsavgiften finns på vår webbsida")
                                        (str/includes? (:body mail-content) "https://opintopolku.fi/konfo/sv/sivu/ansoekningsavgift")
                                        (str/includes? (:body mail-content) (str "maksut-ui/sv?secret=" test-maksut-secret))))
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
                    (select-keys payment [:application-key :state :maksut-secret])))))

          (it "creates payment email sending job"
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
                      _ (updater-job/resend-payment-email runner application-key)]
                  (should-have-invoked :start-job
                                       {:with [:* :*
                                               "ataru.kk-application-payment.kk-application-payment-email-job"
                                               check-mail-fn]}))))

          (it "should set tuition fee obligation for non fi/sv hakukohde when payment state changes to awaiting"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should= {:application_key application-key, :requirement "payment-obligation",
                          :state "obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde]))))

          (it "should set tuition fee obligation for non fi/sv hakukohde when payment state changes to ok-by-proxy"
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
                    obligation (get-payment-obligation-review second-key "payment-info-test-kk-hakukohde")]
                (should= (:ok-by-proxy payment/all-states) (:state payment))
                (should= {:application_key second-key, :requirement "payment-obligation",
                          :state "obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde]))))

          (it "should not set tuition fee obligation for non fi/sv hakukohde when payment state changes to not-required"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-with-hakemusmaksu-exemption
                                     nil)
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    application-key (:key (application-store/get-application application-id))
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:not-required payment/all-states) (:state payment))
                (should-be-nil obligation)))

          (it "should not set tuition fee obligation for fi/sv hakukohde"
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
                    obligation (get-payment-obligation-review application-key "payment-info-test-kk-fisv-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should-be-nil obligation)))

          (it "should not override a non-automatic obligation"
              (let [application-id (unit-test-db/init-db-fixture
                                     form-fixtures/payment-exemption-test-form
                                     application-fixtures/application-without-hakemusmaksu-exemption
                                     nil)
                    application-key (:key (application-store/get-application application-id))
                    _ (store-not-obligated-review application-key "payment-info-test-kk-hakukohde")
                    _ (updater-job/update-kk-payment-status-for-person-handler
                        {:person_oid test-person-oid :term test-term :year test-year} runner)
                    payment (first (payment/get-raw-payments [application-key]))
                    obligation (get-payment-obligation-review application-key "payment-info-test-kk-hakukohde")]
                (should= (:awaiting payment/all-states) (:state payment))
                (should= {:application_key application-key, :requirement "payment-obligation",
                          :state "not-obligated", :hakukohde "payment-info-test-kk-hakukohde"}
                         (select-keys obligation [:application_key :requirement :state :hakukohde])))))
