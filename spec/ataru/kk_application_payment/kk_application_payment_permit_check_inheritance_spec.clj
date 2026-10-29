(ns ataru.kk-application-payment.kk-application-payment-permit-check-inheritance-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.attachment-deadline.attachment-deadline-service :as attachment-deadline]
            [ataru.background-job.job :as job]
            [ataru.cache.cache-service :as cache-service]
            [ataru.component-data.kk-application-payment-module :as payment-module]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.kk-application-payment.fixtures :as fixtures]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.kk-application-payment.kk-application-payment-permit-check :as permit-check]
            [ataru.kk-application-payment.kk-application-payment-permit-check-inheritance :as inheritance]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as updater-job]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.log.audit-log :as audit-log]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.schema.form-schema :as ataru-schema]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.test-utils :refer [set-fixed-time reset-fixed-time!]]
            [ataru.time :as time]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [schema.core :as s]
            [speclj.core :refer [after around before describe it should= should-be-nil tags]])
  (:import (java.time Instant)))

(def hakukohde "payment-info-test-kk-hakukohde")
(def permit-attachment-key "continuous-residence-permit-front")
(def permit-back-attachment-key "continuous-residence-permit-back")
(def passport-attachment-key "continuous-residence-passport-attachment")
(def koulutustoimija-oid "1.2.246.562.10.2000000001")
(def oppilaitos-oid "1.2.246.562.10.2000000002")
(def nested-toimipiste-oid "1.2.246.562.10.2000000004")

(def fake-organization-service (organization-service/->FakeOrganizationService))
(def fake-ohjausparametrit-service (ohjausparametrit-service/new-ohjausparametrit-service))
(def audit-logger (audit-log/new-dummy-audit-logger))

(defrecord MockMaksutService []
  MaksutServiceProtocol
  (create-kk-application-payment-lasku [_ lasku] {:order_id  (payment/maksut-reference->maksut-order-id (:reference lasku))
                                                  :amount    (:amount lasku)
                                                  :status    :active
                                                  :due_date  ""
                                                  :origin    (:origin lasku)
                                                  :reference (:reference lasku)
                                                  :secret    "1234ABCD5678EFGH"})
  (create-kasittely-lasku [_ _] {})
  (create-paatos-lasku [_ _] {})
  (list-lasku-statuses [_ _] {})
  (list-laskut-by-application-key [_ _] [])
  (download-receipt [_ _] nil)
  (invalidate-laskut [_ _] nil)
  (force-invalidate-laskut [_ _] nil)
  (delete-laskut [_ _] nil)
  (update-laskut-due-date [_ _ _] nil))

(defn- cache-returning [value]
  (reify cache-service/Cache
    (get-from [_ _] value)
    (get-many-from [_ _])
    (remove-from [_ _])
    (clear-all [_])))

(defrecord FakeJobRunner []
  component/Lifecycle
  job/JobRunner
  (start-job [_ _ _ _]))

(def runner
  (map->FakeJobRunner {:attachment-deadline-service (attachment-deadline/->AttachmentDeadlineService
                                                      fake-ohjausparametrit-service)
                       :tarjonta-service            (tarjonta-service/->MockTarjontaKoutaService)
                       :form-by-id-cache            (cache-returning nil)
                       :organization-service        fake-organization-service
                       :ohjausparametrit-service    fake-ohjausparametrit-service
                       :person-service              (person-service/->FakePersonService)
                       :get-haut-cache              (cache-returning [{:haku "payment-info-test-kk-haku"}])
                       :koodisto-cache              (cache-returning nil)
                       :audit-logger                audit-logger
                       :maksut-service              (->MockMaksutService)}))

(defn- application-fixture
  [option delivered-attachment-keys]
  (-> application-fixtures/application-with-hakemusmaksu-exemption
      (application-fixtures/with-kk-application-payment-option option)
      (update :answers into (map (fn [attachment-key]
                                   {:key       attachment-key
                                    :value     [(str attachment-key "-file")]
                                    :fieldType "attachment"})
                                 delivered-attachment-keys))))

(def continuous-residence-option
  (:continuous-residence-option-value payment-module/kk-application-payment-document-options))

(def delivered-application
  (application-fixture continuous-residence-option [permit-attachment-key passport-attachment-key]))

(def missing-application
  (application-fixture continuous-residence-option [passport-attachment-key]))

(defn- init-applications
  "Stores the applications of the applicant, submitted at the given times."
  [& fixtures-and-submitted]
  (let [fixtures (take-nth 2 fixtures-and-submitted)
        keys     (->> (unit-test-db/init-db-fixture form-fixtures/payment-exemption-test-form fixtures)
                      (map #(:key (application-store/get-application %))))]
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (doseq [[application-key submitted] (map vector keys (take-nth 2 (rest fixtures-and-submitted)))]
        (jdbc/execute! conn ["UPDATE applications SET submitted = ?::timestamptz WHERE key = ?"
                             submitted application-key])))
    keys))

(defn- check!
  [application-key {:keys [permit-valid-until checked-time organization-oid payment-obligation-basis]
                    :or   {permit-valid-until       "2026-01-01"
                           checked-time             "2025-01-12T10:00:00+02:00"
                           organization-oid         oppilaitos-oid
                           payment-obligation-basis "not-obligated"}}]
  (unit-test-db/save-permit-check-to-db! {:application-key application-key :attachment-key permit-attachment-key})
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (jdbc/execute! conn [(str "UPDATE kk_application_payment_permit_checks SET permit_valid_until = ?::date, "
                              "checked_time = ?::timestamptz, organization_oid = ?, payment_obligation_basis = ? "
                              "WHERE application_key = ?")
                         permit-valid-until checked-time organization-oid payment-obligation-basis
                         application-key])))

(defn- review-obligation! [application-key state]
  (unit-test-db/init-db-application-hakukohde-review-fixture {:hakukohde          hakukohde
                                                              :review-requirement "kk-application-payment-obligation"
                                                              :review-state       state}
                                                             application-key))

(defn- review-attachments! [application-key state]
  (unit-test-db/save-reviews-to-db! (for [attachment-key [permit-attachment-key passport-attachment-key]]
                                      {:application_key application-key
                                       :attachment_key  attachment-key
                                       :hakukohde       hakukohde
                                       :state           state
                                       :modified_time   (time/now)
                                       :updated?        true})))

(defn- review-missing-attachments! [application-key]
  (unit-test-db/save-reviews-to-db! (for [[attachment-key state] [[permit-attachment-key "attachment-missing"]
                                                                  [permit-back-attachment-key "attachment-missing"]
                                                                  [passport-attachment-key "not-checked"]]]
                                      {:application_key application-key
                                       :attachment_key  attachment-key
                                       :hakukohde       hakukohde
                                       :state           state
                                       :modified_time   (time/now)
                                       :updated?        true})))

(defn- review-back-side-missing! [application-key]
  (unit-test-db/save-reviews-to-db! [{:application_key application-key
                                      :attachment_key  permit-back-attachment-key
                                      :hakukohde       hakukohde
                                      :state           "attachment-missing"
                                      :modified_time   (time/now)
                                      :updated?        true}]))

(defn- checked-source! [application-key & [check]]
  (check! application-key check)
  (review-attachments! application-key "checked")
  (review-obligation! application-key "reviewed"))

(defn- run-job! [application-key]
  (updater-job/update-kk-payment-status-for-person-handler {:application_key application-key} runner))

(defn- checks [application-key]
  (permit-check/get-permit-checks fake-organization-service application-key))

(defn- attachment-states [application-key]
  (->> (application-store/get-application-attachment-reviews application-key)
       (filter #(= hakukohde (:hakukohde %)))
       (map (juxt :attachment-key :state))
       (into {})))

(defn- obligation-state [application-key]
  (:state (first (payment/get-kk-application-payment-obligation-reviews application-key))))

(defn- payment-state [application-key]
  (:state (first (payment/get-raw-payments [application-key]))))

(defn- inherited-events [application-key]
  (filter #(= inheritance/inherited-event-type (:event-type %))
          (application-store/get-application-events application-key)))

(describe "kk application payment permit check inheritance"
  (tags :unit)

  (before
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (jdbc/delete! conn :kk_application_payment_permit_checks [])
      (jdbc/delete! conn :applications [])
      (jdbc/delete! conn :kk_application_payments [])
      (jdbc/delete! conn :kk_application_payments_history []))
    (set-fixed-time "2025-01-15T14:50:00"))

  (after
    (reset-fixed-time!))

  (around [spec]
    (with-redefs [koodisto/get-koodisto-options (fn [_ uri _ _]
                                                  (if (= "valtioryhmat" uri)
                                                    fixtures/koodisto-valtioryhmat-response
                                                    []))]
      (spec)))

  (it "inherits the check, the attachment reviews and the reviewed payment obligation to a later application"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      ; the test form has no attachment fields to create the reviews when the application is submitted
      (review-attachments! target-key "not-checked")
      (run-job! target-key)
      (should= [{:attachment-key                  permit-attachment-key
                 :found-in-migri                  true
                 :permit-valid-until              "2026-01-01"
                 :payment-obligation-basis        "not-obligated"
                 :payment-obligation-other-reason nil
                 :inherited-from-application-key  source-key
                 :original-check                  {:application-key source-key
                                                   :virkailija-name "Hemuli Hemuli?"
                                                   :organization    {:oid  oppilaitos-oid
                                                                     :name {:fi "Testikorkeakoulu"}}}}]
               (map #(-> %
                         (dissoc :checked-time :inherited-time)
                         (update :original-check dissoc :checked-time))
                    (checks target-key)))
      ; the checks are returned to the virkailija as such
      (should= (checks target-key) (s/validate [ataru-schema/KkPaymentPermitCheck] (checks target-key)))
      (should= (time/to-instant (:checked-time (first (checks source-key))))
               (time/to-instant (get-in (first (checks target-key)) [:original-check :checked-time])))
      (should= {permit-attachment-key "checked" passport-attachment-key "checked"} (attachment-states target-key))
      (should= "reviewed" (obligation-state target-key))
      (should= 1 (count (inherited-events target-key)))
      (should= (:not-required payment/all-states) (payment-state target-key))))

  (it "inherits when the later application has other files as the attachments"
    ; the applicant may take new pictures of the permit card for each application
    (let [other-files-application (update delivered-application :answers
                                          (partial map #(if (= "attachment" (:fieldType %))
                                                          (assoc % :value [(str (:key %) "-other-file")])
                                                          %)))
          [source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     other-files-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (review-attachments! target-key "not-checked")
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= {permit-attachment-key "checked" passport-attachment-key "checked"} (attachment-states target-key))
      (should= "reviewed" (obligation-state target-key))))

  (it "shows the oppilaitos of a check stored with a toimipiste"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:organization-oid nested-toimipiste-oid})
      (run-job! target-key)
      (should= oppilaitos-oid (get-in (first (checks target-key)) [:original-check :organization :oid]))))

  (it "does not show an organization when the oppilaitos of the check is not known"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:organization-oid koulutustoimija-oid})
      (run-job! target-key)
      (should-be-nil (get-in (first (checks target-key)) [:original-check :organization]))
      (should= (checks target-key) (s/validate [ataru-schema/KkPaymentPermitCheck] (checks target-key)))))

  (it "shows OPH for a check made by a superuser"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:organization-oid "1.2.246.562.10.00000000001"})
      (run-job! target-key)
      (should= {:oid "1.2.246.562.10.00000000001" :name {:fi "Opetushallitus"
                                                         :sv "Utbildningsstyrelsen"
                                                         :en "Finnish National Agency for Education"}}
               (get-in (first (checks target-key)) [:original-check :organization]))))

  (it "does not show the original check of an own check"
    (let [[source-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00")]
      (checked-source! source-key)
      (should-be-nil (:original-check (first (checks source-key))))))

  (it "inherits a check by which the applicant was found not obligated to an application without the residence permit attachment"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      ; the missing attachments have nothing to check
      (should= {permit-attachment-key        "attachment-missing"
                permit-back-attachment-key   "attachment-missing"
                passport-attachment-key      "checked"}
               (attachment-states target-key))
      (should= "reviewed" (obligation-state target-key))
      (should= 1 (count (inherited-events target-key)))
      (should= (:not-required payment/all-states) (payment-state target-key))))

  (it "does not make the applicant obligated by the attachments missing after the deadline when the check by which the applicant was found not obligated has been inherited"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis nil})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= {:state (:not-required payment/all-states) :reason (:exemption-field payment/all-reasons)}
               (select-keys (first (payment/get-raw-payments [target-key])) [:state :reason]))))

  (it "makes the applicant obligated by the attachments missing after the deadline when virkailija changes the inherited check"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis nil})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (permit-check/save-permit-check fake-organization-service (tarjonta-service/->MockTarjontaKoutaService)
                                      audit-logger
                                      {:identity {:oid       unit-test-db/permit-check-virkailija-oid
                                                  :superuser true}}
                                      target-key permit-attachment-key
                                      {:found-in-migri false :permit-valid-until "2026-01-01"})
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= (:awaiting payment/all-states) (payment-state target-key))))

  (it "makes the applicant obligated by the attachments missing after the deadline when the payment obligation is no longer reviewed"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (review-obligation! target-key "unreviewed")
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= (:awaiting payment/all-states) (payment-state target-key))))

  (it "makes the applicant obligated after the deadline when virkailija marks a delivered attachment missing in spite of the inherited check"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis nil})
      (review-attachments! target-key "not-checked")
      (run-job! target-key)
      (review-attachments! target-key "attachment-missing")
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= "reviewed" (obligation-state target-key))
      (should= (:awaiting payment/all-states) (payment-state target-key))))

  (it "does not make the applicant obligated by the attachments missing after the deadline when the payment of the original application becomes required"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis nil})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (review-attachments! source-key "attachment-missing")
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= (:awaiting payment/all-states) (payment-state source-key))
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= (:not-required payment/all-states) (payment-state target-key))))

  (it "inherits a check inherited further to an application without the residence permit attachment when the payment of the original application is required"
    (let [[source-key middle-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                                missing-application "2025-01-12T10:00:00+02:00"
                                                                missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-required source-key nil)
      ; the middle application has inherited the check while the payment of the original application was not required
      (check! middle-key {})
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (jdbc/execute! conn ["UPDATE kk_application_payment_permit_checks SET inherited_from_application_key = ? WHERE application_key = ?"
                             source-key middle-key]))
      (review-obligation! middle-key "reviewed")
      (payment/set-application-fee-not-required-for-exemption middle-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= (:not-required payment/all-states) (payment-state target-key))))

  (it "does not inherit to an application without the residence permit attachment when the earlier application has missing attachments"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis nil})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      ; the payment of the earlier application is required later in the same run
      (review-attachments! source-key "attachment-missing")
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [] (checks target-key))
      (should= (:awaiting payment/all-states) (payment-state source-key))))

  (it "inherits to an application without the residence permit attachment when the earlier application has missing attachments but the permit has been confirmed in Migri"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-attachments! source-key "attachment-missing")
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= (:not-required payment/all-states) (payment-state source-key))))

  (it "makes the applicant obligated after the deadline by a missing attachment the delivered inherited attachment does not cover"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      ; the back side of the permit card is missing in the later application
      (checked-source! source-key {:payment-obligation-basis nil})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-attachments! target-key "not-checked")
      (review-back-side-missing! target-key)
      (run-job! target-key)
      (set-fixed-time "2025-06-15T12:00:00") ; Hakuaika ends 2025-02-08
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= (:awaiting payment/all-states) (payment-state target-key))))

  (it "does not inherit to an application without the residence permit attachment when the payment of the earlier application is required"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-required source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [] (checks target-key))
      (should-be-nil (obligation-state target-key))))

  (it "does not inherit a check stating a payment obligation to an application without the residence permit attachment"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:payment-obligation-basis "obligated-own-studies"})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit to an application without the residence permit attachment when the permit card has expired before the application was submitted"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     missing-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:permit-valid-until "2025-01-13"})
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (review-missing-attachments! target-key)
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit to an application without the residence permit attachment of another document option"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     (application-fixture
                                                       (:eu-blue-option-value
                                                         payment-module/kk-application-payment-document-options)
                                                       ["eu-passport-attachment"])
                                                     "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-not-required-for-exemption source-key nil)
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit to an application with another document option"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     (application-fixture
                                                       (:eu-blue-option-value
                                                         payment-module/kk-application-payment-document-options)
                                                       ["eu-blue-card-attachment" "eu-passport-attachment"])
                                                     "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit when the permit card has expired before the later application was submitted"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:permit-valid-until "2025-01-13"})
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "inherits when the permit card is valid on the day the later application was submitted"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key {:permit-valid-until "2025-01-14"})
      (run-job! target-key)
      (should= 1 (count (checks target-key)))))

  (it "does not inherit to an earlier application"
    (let [[target-key source-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit a check whose payment obligation has not been reviewed"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (check! source-key {})
      (review-obligation! source-key "unreviewed")
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit when the payment obligation of the later application has been reviewed after the check"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (review-obligation! target-key "reviewed")
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "does not inherit when the payment obligation of the later application has been sent to Migri review after the check"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (review-obligation! target-key "in-migri-review")
      (run-job! target-key)
      (should= [] (checks target-key))
      (should= "in-migri-review" (obligation-state target-key))))

  (it "does not inherit to an application whose payment has already been paid"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (review-attachments! target-key "not-checked")
      (payment/set-application-fee-paid target-key nil)
      (run-job! target-key)
      (should= [] (checks target-key))
      (should= {permit-attachment-key "not-checked" passport-attachment-key "not-checked"}
               (attachment-states target-key))
      (should-be-nil (obligation-state target-key))
      (should= (:paid payment/all-states) (payment-state target-key))))

  (it "inherits the check of an application whose payment has already been paid"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (payment/set-application-fee-paid source-key nil)
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))))

  (it "keeps the inherited check when the later application has a new attachment after its earlier reviewed payment obligation"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (set-fixed-time "2025-01-11T10:00:00")
      (review-obligation! target-key "reviewed")
      (set-fixed-time "2025-01-11T12:00:00")
      (review-attachments! target-key "not-checked")
      (set-fixed-time "2025-01-15T14:50:00")
      (check! source-key {:payment-obligation-basis "obligated-own-studies"})
      (review-obligation! source-key "reviewed")
      (run-job! target-key)
      (should= [source-key] (map :inherited-from-application-key (checks target-key)))
      (should= "reviewed" (obligation-state target-key))
      (should= [] (filter #(= "kk-payment-permit-check-reset" (:event-type %))
                          (application-store/get-application-events target-key)))))

  (it "does not inherit when the own check of the later application has been reset because of a new attachment"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (check! target-key {})
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (permit-check/reset-permit-checks conn target-key (Instant/now)))
      (run-job! target-key)
      (should= [] (checks target-key))))

  (it "keeps the attachment reviews the virkailija has made in the later application after the check"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (review-attachments! target-key "incomplete-attachment")
      (run-job! target-key)
      (should= source-key (:inherited-from-application-key (first (checks target-key))))
      (should= {permit-attachment-key "incomplete-attachment" passport-attachment-key "incomplete-attachment"}
               (attachment-states target-key))))

  (it "does not copy the attachment reviews that have not been reviewed in the earlier application"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (check! source-key {})
      (review-attachments! source-key "not-checked")
      (review-obligation! source-key "reviewed")
      (set-fixed-time "2025-01-11T14:50:00")
      (review-attachments! target-key "checked")
      (set-fixed-time "2025-01-15T14:50:00")
      (run-job! target-key)
      (should= source-key (:inherited-from-application-key (first (checks target-key))))
      (should= {permit-attachment-key "checked" passport-attachment-key "checked"} (attachment-states target-key))))

  (it "inherits the latest of the checks"
    (let [[first-key second-key target-key] (init-applications
                                              delivered-application "2025-01-08T10:00:00+02:00"
                                              delivered-application "2025-01-10T10:00:00+02:00"
                                              delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! first-key {:checked-time             "2025-01-13T10:00:00+02:00"
                                  :payment-obligation-basis "obligated-own-studies"})
      (checked-source! second-key {:checked-time "2025-01-12T10:00:00+02:00"})
      (run-job! target-key)
      (should= [first-key "obligated-own-studies"]
               ((juxt :inherited-from-application-key :payment-obligation-basis) (first (checks target-key))))))

  (it "refers to the original application when a check is inherited further"
    (let [[first-key second-key third-key] (init-applications
                                             delivered-application "2025-01-08T10:00:00+02:00"
                                             delivered-application "2025-01-10T10:00:00+02:00"
                                             delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! first-key)
      (run-job! third-key)
      (should= [first-key first-key]
               (map (comp :inherited-from-application-key first checks) [second-key third-key]))))

  (it "inherits only once"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (run-job! target-key)
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (jdbc/delete! conn :kk_application_payment_permit_checks ["application_key = ?" target-key]))
      (run-job! target-key)
      (should= [] (checks target-key))
      (should= 1 (count (inherited-events target-key)))))

  (it "makes the inherited check virkailija's own when virkailija changes it"
    (let [[source-key target-key] (init-applications delivered-application "2025-01-10T10:00:00+02:00"
                                                     delivered-application "2025-01-14T10:00:00+02:00")]
      (checked-source! source-key)
      (run-job! target-key)
      (permit-check/save-permit-check fake-organization-service (tarjonta-service/->MockTarjontaKoutaService)
                                      audit-logger
                                      {:identity {:oid       unit-test-db/permit-check-virkailija-oid
                                                  :superuser true}}
                                      target-key permit-attachment-key
                                      {:found-in-migri           true
                                       :permit-valid-until       "2026-01-01"
                                       :payment-obligation-basis "obligated-own-studies"})
      (let [check (first (checks target-key))]
        (should-be-nil (:inherited-from-application-key check))
        (should-be-nil (:original-check check))))))
