(ns ataru.kk-application-payment.kk-application-payment-spec
  (:require [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.time :as time]
            [speclj.core :refer [describe tags it should-throw should= should-be-nil should-not-be-nil
                                 before around]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.cache.cache-service :as cache-service]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as mock-tarjonta-service]
            [ataru.kk-application-payment.utils :as payment-utils]
            [ataru.test-utils :refer [set-fixed-time reset-fixed-time!]]
            [ataru.applications.application-store :as application-store]
            [ataru.ohjausparametrit.mock-ohjausparametrit-service :refer [->MockOhjausparametritService]]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline-service]))

(defn- store-field-deadline [deadline]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/insert! conn "field_deadlines" deadline)))

(def fake-person-service (person-service/->FakePersonService))
(def fake-tarjonta-service (mock-tarjonta-service/->MockTarjontaKoutaService))

(def fake-ohjausparametrit-service
  (->MockOhjausparametritService))

(def fake-hakukohderyhma-settings-cache (reify cache-service/Cache
                                          (get-from [_ _])
                                          (get-many-from [_ _])
                                          (remove-from [_ _])
                                          (clear-all [_])))

(def fake-attachment-deadline-service (attachment-deadline-service/->AttachmentDeadlineService fake-ohjausparametrit-service))

(def fake-form-by-id-cache (reify cache-service/Cache
                             (get-from [_ key]
                               (when (not= "" key)
                                 (form-store/fetch-by-id (Integer/valueOf key))))
                             (get-many-from [_ _])
                             (remove-from [_ _])
                             (clear-all [_])))

(def fake-koodisto-cache (reify cache-service/Cache
                           (get-from [_ _])
                           (get-many-from [_ _])
                           (remove-from [_ _])
                           (clear-all [_])))

(def fake-organization-service (org-service/->FakeOrganizationService))

(def fake-haku-cache (reify cache-service/Cache
                       (get-from [_ _]
                         [{:haku "payment-info-test-kk-haku"}
                          {:haku "payment-info-test-kk-haku-2030"}
                          {:haku "payment-info-test-kk-haku-daylight-savings"}
                          {:haku "payment-info-test-kk-haku-past"}
                          {:haku "payment-info-test-kk-haku-custom-grace"}
                          {:haku "payment-info-test-kk-haku-form-field-dl"}])
                       (get-many-from [_ _])
                       (remove-from [_ _])
                       (clear-all [_])))

(def fake-job-runner
  {:attachment-deadline-service fake-attachment-deadline-service
   :person-service fake-person-service
   :tarjonta-service fake-tarjonta-service
   :form-by-id-cache fake-form-by-id-cache
   :koodisto-cache fake-koodisto-cache
   :organization-service fake-organization-service
   :ohjausparametrit-service fake-ohjausparametrit-service
   :hakukohderyhma-settings-cache fake-hakukohderyhma-settings-cache
   :get-haut-cache fake-haku-cache})

(declare conn)
(defn- delete-states-and-events! []
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payments [])
                            (jdbc/delete! conn :kk_application_payments_history [])
                            (jdbc/delete! conn :application_hakukohde_reviews [])
                            (jdbc/delete! conn :application_hakukohde_attachment_reviews [])
                            (jdbc/delete! conn :field_deadlines [])))

(def term-fall "kausi_s")
(def year-ok 2025)

(def state-awaiting (:awaiting payment/all-states))
(def state-not-required (:not-required payment/all-states))
(def state-paid (:paid payment/all-states))
(def state-overdue (:overdue payment/all-states))
(def state-ok-by-proxy (:ok-by-proxy payment/all-states))

(def reason-eu-citizen (:eu-citizen payment/all-reasons))
(def reason-exemption  (:exemption-field payment/all-reasons))

(defn- should-be-matching-state
  [example state]
  (should= example (select-keys state [:application-key :state :reason])))

(def exempt-test-oid "1.2.3.4.5.303") ; FakePersonService returns non-EU nationality for this one

(defn create-payment-exempt-by-application
  ([merge-map form]
   (let [application-id (unit-test-db/init-db-fixture form
                                                      (merge application-fixtures/application-with-hakemusmaksu-exemption
                                                             {:person-oid exempt-test-oid} merge-map)
                                                      nil)
         application-key (:key (application-store/get-application application-id))]
     application-key))
  ([merge-map]
   (create-payment-exempt-by-application merge-map form-fixtures/payment-exemption-test-form)))

(defn- create-2030-payment-exempt-by-application []
  (create-payment-exempt-by-application {:haku "payment-info-test-kk-haku-2030"}))

(defn- create-daylight-savings-payment-exempt-by-application []
  (create-payment-exempt-by-application {:haku "payment-info-test-kk-haku-daylight-savings"}))

(defn- create-past-payment-exempt-by-application-with-custom-grace-days []
  (create-payment-exempt-by-application {:haku "payment-info-test-kk-haku-custom-grace"}))

(defn create-past-payment-exempt-by-application-with-custom-form-field-deadline []
  (create-payment-exempt-by-application {:person-oid exempt-test-oid
                                         :haku "payment-info-test-kk-haku-form-field-dl"
                                         :form 909910}
                                        form-fixtures/payment-exemption-test-form-with-deadline))

(defn- filter-by-application-keys [application-keys coll]
  (filter #(contains? application-keys (:application-key %)) coll))

(defn update-payment
  ([application-key]
   (update-payment application-key exempt-test-oid))
  ([application-key person-oid]
   (let [changed (->> (:modified-payments
                        (payment/update-payments-for-person-term-and-year
                          fake-job-runner person-oid term-fall year-ok))
                      (filter-by-application-keys #{application-key}))
         payment (first (payment/get-raw-payments [application-key]))]
     [changed payment])))

(defn update-and-check-changed-payments [application-key person-oid expected-count expected-payment]
  (let [[changed payment] (update-payment application-key person-oid)]
    (should= expected-count (count changed))
    (should-be-matching-state expected-payment payment)))

(defn- save-and-check-single-state
  [application-key state-func desired-state desired-reason]
  (let [state-data (state-func application-key nil)]
    (should= (:state state-data) desired-state)
    (should= (:reason state-data) desired-reason)))

(describe "creating valid invoicing data"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (it "should generate valid order id from application key"
              (let [key "1.2.246.562.11.00000000000002353349"
                    order-id (payment/maksut-reference->maksut-order-id key)]
                (should= "KKHA2353349" order-id)))

          (it "should generate valid invoicing data from a payment and an application"
              (let [application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                 application-fixtures/application-without-hakemusmaksu-exemption
                                                                 nil)
                    application (application-store/get-application application-id)
                    payment (payment/set-application-fee-paid (:key application) nil)
                    invoice (payment/generate-invoicing-data fake-tarjonta-service payment application)
                    expected-invoice {:reference (:key application)
                                      :origin payment/kk-application-payment-origin
                                      :amount "100.00"
                                      :due-days 7
                                      :first-name "Aku Petteri"
                                      :last-name "Ankka"
                                      :email "aku@ankkalinna.com"
                                      :extend-deadline true
                                      :metadata {:haku-name {:fi "testing2", :sv "testing3", :en "testing4"}
                                                 :alkamiskausi "kausi_s"
                                                 :alkamisvuosi 2025}}]
                (should= invoice expected-invoice))))

(describe "get-haut-for-update"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (it "should return haku ending in the future regardless of start date"
              ; FWIW first-application-payment-hakuaika-start redef will not be needed in tests after 1.1.2025.
              ; Meanwhile, we could also modify the config, but then other tests testing the actual official date would fail.
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2023 1 1)
                            payment/get-haut-with-tarjonta-data
                            (constantly [(fixtures/haku-with-hakuajat
                                           (-> payment-utils/haku-update-grace-days time/days time/from-now)
                                           (-> (* payment-utils/haku-update-grace-days 2) time/days time/from-now))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should return haku ending today at the end of day"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2023 1 1)
                            payment/get-haut-with-tarjonta-data (constantly [(fixtures/haku-with-hakuajat
                                                                               (time/now)
                                                                               (time/today-at 23 59))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should return haku that ended in grace days"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2023 1 1)
                            payment/get-haut-with-tarjonta-data
                            (constantly [(fixtures/haku-with-hakuajat
                                           (-> (* payment-utils/haku-update-grace-days 2) time/days time/ago)
                                           (time/with-time-at-start-of-day
                                             (-> payment-utils/haku-update-grace-days time/days time/ago)))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 1 (count haut)))))

          (it "should not return haku that ended before grace days"
              (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2023 1 1)
                            payment/get-haut-with-tarjonta-data
                            (constantly [(fixtures/haku-with-hakuajat
                                           (time/minus
                                            (-> (* payment-utils/haku-update-grace-days 2) time/days time/ago)
                                            (time/hours 1))
                                           (time/minus
                                            (-> (+ payment-utils/haku-update-grace-days 1) time/days time/ago)
                                            (time/hours 1)))])]
                (let [haut (payment/get-haut-for-update fake-haku-cache fake-tarjonta-service)]
                  (should= 0 (count haut))))))

(describe "mark-reminder-sent"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (it "should mark reminder sent for a payment"
              (let [application-key "1.2.3.4.5.6"
                    initial-data (payment/set-application-fee-required application-key nil)
                    _ (payment/mark-reminder-sent application-key)
                    updated-data (first (payment/get-raw-payments [application-key]))]
                (should-be-nil (:reminder-sent-at initial-data))
                (should-not-be-nil (:reminder-sent-at updated-data))))

          (it "should throw an exception when trying to mark reminder sent for nonexisting payment"
              (should-throw (payment/mark-reminder-sent "1.2.3.4.5.1234"))))

(describe "set-maksut-secret"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (it "should set maksut secret for a payment"
              (let [application-key "1.2.3.4.5.6"
                    maksut-secret "1234ABCD5678EFGH"
                    initial-data (payment/set-application-fee-required application-key nil)
                    _ (payment/set-maksut-secret application-key maksut-secret)
                    updated-data (first (payment/get-raw-payments [application-key]))]
                (should-be-nil (:maksut-secret initial-data))
                (should= maksut-secret (:maksut-secret updated-data))))

          (it "should throw an exception when trying to set maksut secret for nonexisting payment"
              (should-throw (payment/set-maksut-secret "1.2.3.4.5.1234" "1234ABCD5678EFGH"))))

(describe "update-payment-status"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (around [spec]
                  (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                                (case uri
                                                                  "valtioryhmat"
                                                                  fixtures/koodisto-valtioryhmat-response
                                                                  []))]
                    (spec)))

          (describe "without exemption"
                    (it "should return nil without any updates when the person has no applications"
                        (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                      application-fixtures/application-without-hakemusmaksu-exemption
                                                      nil)
                        (let [oid "1.2.3.4.5.1234"                       ; Should have no applications
                              states (payment/update-payments-for-person-term-and-year
                                       fake-job-runner oid term-fall year-ok)]
                          (should= 0 (count states))))

                    (it "should return existing paid (terminal) state"
                        (let [oid "1.2.3.4.5.6"
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                            application-fixtures/application-without-hakemusmaksu-exemption
                                                                            {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              initial-payment (payment/set-application-fee-paid application-key nil)
                              [changed payment] (update-payment application-key oid)]
                          (should= 0 (count changed))
                          (should= initial-payment payment)
                          (should-be-matching-state {:application-key application-key, :state state-paid :reason nil} payment)))

                    (it "should set ok via proxy for application when another application by linked oid has been paid for"
                        (let [oid "1.2.3.4.5.303"                     ; FakePersonService returns non-EU nationality for this one
                              linked-oid (str oid "2")                ; See FakePersonService
                              application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                            [(merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid oid})
                                                                             (merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid linked-oid})])
                              primary-application-key (:key (application-store/get-application (first application-ids)))
                              linked-application-key (:key (application-store/get-application (second application-ids)))
                              _ (payment/set-application-fee-paid linked-application-key nil)
                              changed (->> (:modified-payments
                                             (payment/update-payments-for-person-term-and-year
                                               fake-job-runner oid term-fall year-ok))
                                           (filter-by-application-keys #{primary-application-key linked-application-key}))
                              primary-payment (first (payment/get-raw-payments [primary-application-key]))
                              linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                          (should= 1 (count changed))
                          (should= primary-payment (first changed))
                          (should-be-matching-state {:application-key primary-application-key, :state state-ok-by-proxy
                                                     :reason nil}
                                                    primary-payment)
                          (should-be-matching-state {:application-key linked-application-key, :state state-paid
                                                     :reason nil}
                                                    linked-payment)))

                    (it "should reset paid via linked oid status to person's normal status when the linking has been removed"
                        (let [oid "1.2.3.4.5.303"                     ; FakePersonService returns non-EU nationality for this one
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-without-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              initial-payment (payment/set-application-fee-ok-by-proxy application-key nil)
                              [changed payment] (update-payment application-key oid)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-ok-by-proxy
                                                     :reason nil} initial-payment)
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    (it "should set payment status for Finnish citizen as not required"
                        (let [oid "1.2.3.4.5.7"                       ; FakePersonService returns Finnish nationality by default
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                             application-fixtures/application-without-hakemusmaksu-exemption
                                                                             {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              [changed payment] (update-payment application-key oid)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-eu-citizen} payment)))

                    (it "should set payment status for VTJ-yksilöity EU citizen as not required"
                        (let [oid "1.2.3.4.5.808"                       ; FakePersonService returns French nationality for this one
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                            application-fixtures/application-without-hakemusmaksu-exemption
                                                                            {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              [changed payment] (update-payment application-key oid)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-eu-citizen} payment)))

                    (it "should set payment status for non VTJ-yksilöity EU citizen as not required"
                        (let [oid "1.2.3.4.5.909"                       ; FakePersonService returns French nationality but no yksiloityVTJ
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                            application-fixtures/application-without-hakemusmaksu-exemption
                                                                            {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              [changed payment] (update-payment application-key oid)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    (it "should set payment status for non EU citizen without exemption as required"
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                             (merge
                                                                               application-fixtures/application-without-hakemusmaksu-exemption
                                                                               {:person-oid oid}) nil)
                                application-key (:key (application-store/get-application application-id))
                                [changed payment] (update-payment application-key oid)]
                            (should= 1 (count changed))
                            (should= payment (first changed))
                            (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                       :reason nil} payment))))

                    (it "should use kk application payment obligation reviewed state to bypass attachment deadline"
                        ; before attachment deadline
                        (let [_ (set-fixed-time "2025-01-15T12:00:00")
                              person-oid      "1.2.3.4.5.303" ; FakePersonService returns non-EU nationality for this one
                              application-id  (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                            application-fixtures/application-with-hakemusmaksu-exemption
                                                                            nil)
                              application-key (:key (application-store/get-application application-id))]
                          (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                              :attachment_key "brexit-permit-attachment"
                                                              :hakukohde "payment-info-test-kk-hakukohde"
                                                              :state "attachment-missing"}
                                                             {:application_key application-key
                                                              :attachment_key "brexit-passport-attachment"
                                                              :hakukohde "payment-info-test-kk-hakukohde"
                                                              :state "not-checked"}])
                          (update-and-check-changed-payments application-key person-oid 1
                                                             {:application-key application-key
                                                              :state           state-not-required
                                                              :reason          reason-exemption})

                          ; add kk application payment obligation reviewed state
                          (unit-test-db/init-db-application-hakukohde-review-fixture
                            {:hakukohde "payment-info-test-kk-hakukohde"
                             :review-requirement "kk-application-payment-obligation"
                             :review-state "reviewed"} application-key)
                          (update-and-check-changed-payments application-key person-oid 1
                                                             {:application-key application-key
                                                              :state           state-awaiting
                                                              :reason          nil})

                          ; reset kk application payment obligation review to unreviewd state
                          (unit-test-db/init-db-application-hakukohde-review-fixture
                            {:hakukohde "payment-info-test-kk-hakukohde"
                             :review-requirement "kk-application-payment-obligation"
                             :review-state "unreviewed"} application-key)
                          ; after attachment deadline
                          (set-fixed-time "2025-02-16T12:00:00")
                          (update-and-check-changed-payments application-key person-oid 0
                                                             {:application-key application-key
                                                              :state           state-awaiting
                                                              :reason          nil})))

                    (it "should set payment status for non eu citizen with existing linked overdue payment as required"
                        (with-redefs [payment/exemption-in-application? (constantly false)]
                          (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                                linked-oid (str oid "2")                  ; See FakePersonService
                                application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                              [(merge
                                                                                 application-fixtures/application-without-hakemusmaksu-exemption
                                                                                 {:person-oid oid})
                                                                               (merge
                                                                                 application-fixtures/application-without-hakemusmaksu-exemption
                                                                                 {:person-oid linked-oid})])
                                primary-application-key (:key (application-store/get-application (first application-ids)))
                                linked-application-key (:key (application-store/get-application (second application-ids)))
                                _ (payment/set-application-fee-overdue linked-application-key nil)
                                changed (->> (:modified-payments
                                               (payment/update-payments-for-person-term-and-year
                                                 fake-job-runner oid term-fall year-ok))
                                             (filter-by-application-keys #{primary-application-key linked-application-key}))
                                primary-payment (first (payment/get-raw-payments [primary-application-key]))
                                linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                            (should= 1 (count changed))
                            (should= primary-payment (first changed))
                            (should-be-matching-state {:application-key primary-application-key, :state state-awaiting
                                                       :reason nil} primary-payment)
                            (should-be-matching-state {:application-key linked-application-key, :state state-overdue
                                                       :reason nil} linked-payment)))))

          (describe "with exemption"
                    (around [spec]
                            (with-redefs [payment-utils/first-application-payment-hakuaika-start (time/date-time 2024 1 1)]
                              (spec))
                            (reset-fixed-time!))

                    (it "should set payment status for non eu citizen with exemption as not required"
                        (let [application-key   (create-payment-exempt-by-application {})
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should set payment status as not required if an exemption attachment is missing before deadline"
                        (let [fixed-date-str-in-finland "2030-01-15T14:59:59"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should set payment status as not required if an exemption attachment is incomplete before deadline"
                        (let [fixed-date-str-in-finland "2030-01-15T14:59:59"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "incomplete-attachment"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should set payment status as not required if an exemption attachments are ok after deadline"
                        (let [fixed-date-str-in-finland "2030-06-15T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should set payment status as not required if only unrelated / non-triggering attachments are missing or incomplete after deadline"
                        (let [fixed-date-str-in-finland "2030-06-15T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "none-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "foobar-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "incomplete-attachment"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should set payment status as required if an exemption attachment is incomplete after deadline"
                        (let [fixed-date-str-in-finland "2030-06-15T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "incomplete-attachment"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    (it "should set payment status as required if an exemption attachment is missing after deadline"
                        (let [fixed-date-str-in-finland "2030-06-15T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    (it "should not set payment status as required if a passport attachment is missing after deadline"
                        (let [fixed-date-str-in-finland "2030-06-15T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should use custom application field deadline date"
                        (let [fixed-date-str-in-finland "2030-06-01T01:22:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              application-key   (create-2030-payment-exempt-by-application) ; Hakuaika ends 2030-06-01
                              _                 (store-field-deadline {:application_key application-key
                                                                       :field_id "brexit-permit-attachment"
                                                                       :deadline (time/plus (time/now) (time/days 1))})
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    ; Hakuaika ends at 15:00, so the attachment deadline should be at 15:00, regardless of daylight savings.
                    (it "should work correctly with daylight savings: deadline passed"
                        (let [fixed-date-str-in-finland "2030-04-08T15:00:01"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              ; Hakuaika ends 2030-03-25 so the deadline is going to be after daylight savings adjustment
                              application-key   (create-daylight-savings-payment-exempt-by-application)
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-awaiting
                                                     :reason nil} payment)))

                    ; Hakuaika ends at 15:00, so the attachment deadline should be at 15:00, regardless of daylight savings.
                    (it "should work correctly with daylight savings: deadline not passed"
                        (let [fixed-date-str-in-finland "2030-04-08T14:59:59"
                              _ (set-fixed-time fixed-date-str-in-finland)
                                                  ; Hakuaika ends 2030-03-25 so the deadline is going to be after daylight savings adjustment
                              application-key   (create-daylight-savings-payment-exempt-by-application)
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should use custom ohjausparametrit deadline days"
                        ; Mock ohjausparametrit returns grace days 10000 for the attached haku...
                        (let [application-key   (create-past-payment-exempt-by-application-with-custom-grace-days)
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-permit-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "attachment-missing"}
                                                                                   {:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should use custom form field deadline"
                        (let [fixed-date-str-in-finland "2030-04-10T12:00:00"
                              _ (set-fixed-time fixed-date-str-in-finland)
                              ; Mock form-cache returns field deadline in the future
                              application-key   (create-past-payment-exempt-by-application-with-custom-form-field-deadline)
                              _                 (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                                                                    :attachment_key "brexit-passport-attachment"
                                                                                    :hakukohde "payment-info-test-kk-hakukohde"
                                                                                    :state "not-checked"}])
                              [changed payment] (update-payment application-key)]
                          (should= 1 (count changed))
                          (should= payment (first changed))
                          (should-be-matching-state {:application-key application-key, :state state-not-required
                                                     :reason reason-exemption} payment)))

                    (it "should not set payment status of non eu citizens another application based on a previous exemption"
                        ; This test is to ensure that the exemption does not apply to other applications of the same person.
                        ; We create two applications, one with exemption and one without, run the payment update and check that the
                        ; application without exemption is not set to not required.
                        (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                              linked-oid (str oid "2")                  ; See FakePersonService
                              application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                            [(merge
                                                                              application-fixtures/application-with-hakemusmaksu-exemption
                                                                              {:person-oid oid})
                                                                             (merge
                                                                              application-fixtures/application-without-hakemusmaksu-exemption
                                                                              {:person-oid linked-oid})])
                              primary-application-key (:key (application-store/get-application (first application-ids)))
                              linked-application-key (:key (application-store/get-application (second application-ids)))
                              changed (->> (:modified-payments
                                             (payment/update-payments-for-person-term-and-year
                                               fake-job-runner oid term-fall year-ok))
                                           (filter-by-application-keys #{primary-application-key linked-application-key}))
                              primary-changed (first (filter #(= primary-application-key (:application-key %)) changed))
                              linked-changed (first (filter #(= linked-application-key (:application-key %)) changed))
                              primary-payment (first (payment/get-raw-payments [primary-application-key]))
                              linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                          (should= 2 (count changed))
                          (should= primary-payment primary-changed)
                          (should= linked-payment linked-changed)
                          (should-be-matching-state {:application-key primary-application-key, :state state-not-required
                                                     :reason reason-exemption} primary-payment)
                          (should-be-matching-state {:application-key linked-application-key, :state state-awaiting
                                                     :reason nil} linked-payment)))

                    (it "should not set override exempt application status to ok by proxy when another application has been paid"
                        ; Sanity check that the exemption doesn't get overridden by an "inherited" state.
                        (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                              linked-oid (str oid "2")                  ; See FakePersonService
                              application-ids (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                            [(merge
                                                                              application-fixtures/application-with-hakemusmaksu-exemption
                                                                              {:person-oid oid})
                                                                             (merge
                                                                              application-fixtures/application-without-hakemusmaksu-exemption
                                                                              {:person-oid linked-oid})])
                              primary-application-key (:key (application-store/get-application (first application-ids)))
                              linked-application-key (:key (application-store/get-application (second application-ids)))
                              _ (payment/set-application-fee-paid linked-application-key nil)
                              changed (->> (:modified-payments
                                             (payment/update-payments-for-person-term-and-year
                                               fake-job-runner oid term-fall year-ok))
                                           (filter-by-application-keys #{primary-application-key linked-application-key}))
                              primary-payment (first (payment/get-raw-payments [primary-application-key]))
                              linked-payment (first (payment/get-raw-payments [linked-application-key]))]
                          (should= 1 (count changed))
                          (should= primary-payment (first changed))
                          (should-be-matching-state {:application-key primary-application-key, :state state-not-required
                                                     :reason reason-exemption} primary-payment)
                          (should-be-matching-state {:application-key linked-application-key, :state state-paid
                                                     :reason nil} linked-payment)))

                    (it "should not set exempt application status when application has already been marked as overdue"
                        (let [oid "1.2.3.4.5.303"                       ; FakePersonService returns non-EU nationality for this one
                              application-id (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form
                                                                           (merge
                                                                            application-fixtures/application-with-hakemusmaksu-exemption
                                                                            {:person-oid oid}) nil)
                              application-key (:key (application-store/get-application application-id))
                              _ (payment/set-application-fee-overdue application-key nil)
                              [changed payment] (update-payment application-key oid)]
                          (should= 0 (count changed))
                          (should-be-matching-state {:application-key application-key, :state state-overdue
                                                     :reason nil} payment)))))

(describe "application payment states"
          (tags :unit :kk-application-payment)

          (before (delete-states-and-events!))

          (describe "payment state validation"
                    (it "should not allow setting fee with no application key"
                        (should-throw (payment/set-application-fee-required "" nil))))

          (describe "payment state setting"
                    (it "should set and get application fee required"
                        (save-and-check-single-state
                          "1.2.3.4.5.6" payment/set-application-fee-required state-awaiting nil))

                    (it "should set and get application fee not required for eu citizen"
                        (save-and-check-single-state
                          "1.2.3.4.5.7" payment/set-application-fee-not-required-for-eu-citizen
                          state-not-required reason-eu-citizen))

                    (it "should set and get application fee not required due to exemption"
                        (save-and-check-single-state
                          "1.2.3.4.5.8" payment/set-application-fee-not-required-for-exemption
                          state-not-required reason-exemption))

                    (it "should set and get application fee paid"
                        (save-and-check-single-state
                          "1.2.3.4.5.9" payment/set-application-fee-paid state-paid nil))

                    (it "should set and get application fee overdue"
                        (save-and-check-single-state
                          "1.2.3.4.5.10" payment/set-application-fee-overdue state-overdue nil)))

          (describe "due date"
                    (around [spec]
                            (spec)
                            (reset-fixed-time!))

                    (it "should store and retrieve due date correctly when Europe/Helsinki-time is already on the next day compared to UTC-time"
                        (let [fixed-date-str-in-finland "2020-01-02T01:22:01"];This time is selected so that in UTC time it's still 2020-01-01, but in Finland timezone the date is already 2020-01-02.
                          (set-fixed-time fixed-date-str-in-finland)
                          (let [data            (payment/set-application-fee-required "1.2.3.4.5.12" nil)
                                due-date-stored (payment/parse-due-date (:due-date data))
                                comparison-date (time/plus (-> fixed-date-str-in-finland
                                                                java.time.LocalDateTime/parse
                                                                (.atZone (time/default-zone)))
                                                           (time/days payment/kk-application-payment-due-days))]
                            (should= (time/year due-date-stored) (time/year comparison-date))
                            (should= (time/month due-date-stored) (time/month comparison-date))
                            (should= (time/day due-date-stored) (time/day comparison-date)))))
                    (it "should store and retrieve due date correctly when Europe/Helsinki-time is on the same day as UTC-time"
                        (let [fixed-date-str-in-finland "2020-01-02T23:01:01"];This time is selected so that the UTC vs Finland timezones make no difference to yyyy-mm-dd.
                          (set-fixed-time fixed-date-str-in-finland)
                          (let [data            (payment/set-application-fee-required "1.2.3.4.5.12" nil)
                                due-date-stored (payment/parse-due-date (:due-date data))
                                comparison-date (time/plus (-> fixed-date-str-in-finland
                                                                java.time.LocalDateTime/parse
                                                                (.atZone (time/time-zone-for-id "Europe/Helsinki")))
                                                           (time/days payment/kk-application-payment-due-days))]
                            (should= (time/year due-date-stored) (time/year comparison-date))
                            (should= (time/month due-date-stored) (time/month comparison-date))
                            (should= (time/day due-date-stored) (time/day comparison-date))))))

          (describe "preserving and overwriting previous state data"
                    (it "should reset approved state data when fee is required"
                        (let [initial-data (payment/set-application-fee-not-required-for-exemption "1.2.3.4.5.11" nil)
                              updated-data (payment/set-application-fee-required "1.2.3.4.5.11" initial-data)]
                          (should-not-be-nil (:approved-at initial-data))
                          (should-be-nil     (:required-at initial-data))
                          (should-be-nil     (:due-date    initial-data))
                          (should-be-nil     (:total-sum   initial-data))

                          (should-be-nil     (:approved-at updated-data))
                          (should-not-be-nil (:required-at updated-data))
                          (should-not-be-nil (:due-date    updated-data))
                          (should-not-be-nil (:total-sum   updated-data))))

                    (it "should reset payment data when setting payment as not required for eu citizen"
                        (let [initial-data (payment/set-application-fee-required "1.2.3.4.5.12" nil)
                              updated-data (payment/set-application-fee-not-required-for-eu-citizen "1.2.3.4.5.12" initial-data)]
                          (should-be-nil     (:approved-at initial-data))
                          (should-not-be-nil (:required-at initial-data))
                          (should-not-be-nil (:due-date    initial-data))
                          (should-not-be-nil (:total-sum   initial-data))

                          (should-not-be-nil (:approved-at updated-data))
                          (should-not-be-nil (:required-at updated-data))
                          (should-be-nil     (:due-date    updated-data))
                          (should-be-nil     (:total-sum   updated-data))))

                    (it "should reset payment data when setting payment as not required due to exemption"
                        (let [initial-data (payment/set-application-fee-required "1.2.3.4.5.13" nil)
                              updated-data (payment/set-application-fee-not-required-for-exemption "1.2.3.4.5.13" initial-data)]
                          (should-be-nil     (:approved-at initial-data))
                          (should-not-be-nil (:required-at initial-data))
                          (should-not-be-nil (:due-date    initial-data))
                          (should-not-be-nil (:total-sum   initial-data))

                          (should-not-be-nil (:approved-at updated-data))
                          (should-not-be-nil (:required-at updated-data))
                          (should-be-nil     (:due-date    updated-data))
                          (should-be-nil     (:total-sum   updated-data))))

                    (it "should preserve payment data and mark approval when setting payment as paid"
                        (let [initial-data (payment/set-application-fee-required "1.2.3.4.5.14" nil)
                              updated-data (payment/set-application-fee-paid "1.2.3.4.5.14" initial-data)]
                          (should-be-nil     (:approved-at initial-data))
                          (should-not-be-nil (:required-at initial-data))
                          (should-not-be-nil (:due-date    initial-data))
                          (should-not-be-nil (:total-sum   initial-data))

                          (should-not-be-nil (:approved-at updated-data))
                          (should-not-be-nil (:required-at updated-data))
                          (should-not-be-nil (:due-date    updated-data))
                          (should-not-be-nil (:total-sum   updated-data))))

                    (it "should preserve payment data without approval when setting payment as overdue"
                        (let [initial-data (payment/set-application-fee-required "1.2.3.4.5.14" nil)
                              updated-data (payment/set-application-fee-overdue "1.2.3.4.5.14" initial-data)]
                          (should-be-nil     (:approved-at initial-data))
                          (should-not-be-nil (:required-at initial-data))
                          (should-not-be-nil (:due-date    initial-data))
                          (should-not-be-nil (:total-sum   initial-data))

                          (should-be-nil     (:approved-at updated-data))
                          (should-not-be-nil (:required-at updated-data))
                          (should-not-be-nil (:due-date    updated-data))
                          (should-not-be-nil (:total-sum   updated-data))))))
