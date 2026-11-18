(ns ataru.kk-application-payment.kk-application-payment-permit-check-spec
  (:require [ataru.applications.application-access-control :as aac]
            [ataru.applications.application-store :as application-store]
            [ataru.db.db :as db]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.kk-application-payment.kk-application-payment-permit-check :as permit-check]
            [ataru.kk-application-payment.kk-application-payment-permit-check-store :as permit-check-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-client :as org-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.time :as time]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [describe it should= should-be-nil should-not-be-nil tags before]]
            [yesql.core :as sql]))

(sql/defqueries "sql/virkailija-queries.sql")
(declare yesql-upsert-virkailija<!)

(def fake-organization-service (organization-service/->FakeOrganizationService))
(def audit-logger (audit-log/new-dummy-audit-logger))

(def tarjoaja-oid "1.2.246.562.10.10826252479")
(def koulutustoimija-oid "1.2.246.562.10.2000000001")
(def oppilaitos-oid "1.2.246.562.10.2000000002")
(def toimipiste-oid "1.2.246.562.10.2000000003")
(def nested-toimipiste-oid "1.2.246.562.10.2000000004")
(def other-tarjoaja-oid "1.2.246.562.10.10826252480")

(def tarjoaja-by-hakukohde
  {"payment-info-test-kk-hakukohde"   other-tarjoaja-oid
   "payment-info-test-kk-hakukohde-2" tarjoaja-oid})

; Hakukohteet of the mock tarjonta services have no tarjoajat
(defrecord TarjoajaTarjontaService []
  tarjonta/TarjontaService
  (get-hakukohteet [_ hakukohde-oids]
    (map (fn [oid] {:oid oid :tarjoaja-oids [(tarjoaja-by-hakukohde oid)]}) hakukohde-oids)))

(def fake-tarjonta-service (->TarjoajaTarjontaService))
(def virkailija-oid unit-test-db/permit-check-virkailija-oid)

(defn- session-with-edit-rights [& organization-oids]
  {:identity {:oid                      virkailija-oid
              :user-right-organizations {:edit-applications (mapv (fn [oid] {:oid oid}) organization-oids)}}})

(def superuser-session
  {:identity {:oid       virkailija-oid
              :superuser true}})

(def permit-valid-until (str (.plusYears (time/today) 1)))

(def check-body
  {:found-in-migri                  true
   :permit-valid-until              permit-valid-until
   :payment-obligation-basis        "not-obligated"
   :payment-obligation-other-reason nil})

(defn- init-application []
  (let [application-id (unit-test-db/init-db-fixture
                         form-fixtures/payment-exemption-test-form
                         (assoc application-fixtures/application-with-hakemusmaksu-exemption
                                :hakukohde ["payment-info-test-kk-hakukohde" "payment-info-test-kk-hakukohde-2"])
                         nil)]
    (:key (application-store/get-application application-id))))

(defn- save [session application-key attachment-key body]
  (permit-check/save-permit-check fake-organization-service fake-tarjonta-service audit-logger session
                                  application-key attachment-key body))

(def ^:private unavailable-organization-service
  (reify organization-service/OrganizationService
    (get-organization-with-parents [_ _]
      (throw (Exception. "Organization service not available")))))

(defn- stored-check [application-key attachment-key]
  (permit-check-store/get-permit-check application-key attachment-key))

(defn- events [application-key event-type]
  (filter #(= event-type (:event-type %)) (application-store/get-application-events application-key)))

(describe "kk-application-payment-permit-check"
  (tags :unit)

  (before
    (db/exec :db yesql-upsert-virkailija<! {:oid        virkailija-oid
                                            :first_name "Hemuli"
                                            :last_name  "Hemuli?"})
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (jdbc/delete! conn :kk_application_payment_permit_checks [])))

  (it "stores the check with the oppilaitos of the first hakukohde virkailija has edit rights to"
    (let [application-key (init-application)
          saved           (save (session-with-edit-rights tarjoaja-oid) application-key
                                "continuous-residence-permit-front" check-body)]
      (should= {:attachment-key                  "continuous-residence-permit-front"
                :found-in-migri                  true
                :permit-valid-until              permit-valid-until
                :payment-obligation-basis        "not-obligated"
                :payment-obligation-other-reason nil
                :inherited-from-application-key  nil
                :inherited-time                  nil}
               (dissoc saved :checked-time))
      (should-not-be-nil (:checked-time saved))
      (should= [saved] (permit-check/get-permit-checks fake-organization-service application-key))
      (should= {:virkailija-oid   virkailija-oid
                :organization-oid tarjoaja-oid}
               (select-keys (stored-check application-key "continuous-residence-permit-front")
                            [:virkailija-oid :organization-oid]))
      (should= 1 (count (events application-key "kk-payment-permit-check-updated")))))

  (it "stores the oppilaitos of the first hakukohde in hakutoive order when virkailija has rights to several"
    (let [application-key (init-application)
          _               (save (session-with-edit-rights tarjoaja-oid other-tarjoaja-oid) application-key
                                "continuous-residence-permit-front" check-body)]
      (should= other-tarjoaja-oid
               (:organization-oid (stored-check application-key "continuous-residence-permit-front")))))

  (it "stores OPH as the organization of a superuser"
    (let [application-key (init-application)
          _               (save superuser-session application-key "eu-blue-card-attachment" check-body)]
      (should= "1.2.246.562.10.00000000001"
               (:organization-oid (stored-check application-key "eu-blue-card-attachment")))))

  (it "stores the oppilaitos of the hakukohde under the koulutustoimija virkailija is attached to"
    (with-redefs [tarjoaja-by-hakukohde {"payment-info-test-kk-hakukohde"   other-tarjoaja-oid
                                         "payment-info-test-kk-hakukohde-2" toimipiste-oid}]
      (let [application-key (init-application)
            _               (save (session-with-edit-rights koulutustoimija-oid) application-key
                                  "eu-blue-card-attachment" check-body)]
        (should= oppilaitos-oid
                 (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "stores the oppilaitos of the toimipiste virkailija is attached to"
    (with-redefs [tarjoaja-by-hakukohde {"payment-info-test-kk-hakukohde"   nested-toimipiste-oid
                                         "payment-info-test-kk-hakukohde-2" nested-toimipiste-oid}]
      (let [application-key (init-application)
            _               (save (session-with-edit-rights toimipiste-oid) application-key
                                  "eu-blue-card-attachment" check-body)]
        (should= oppilaitos-oid
                 (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "does not store an organization when the hakukohde has no oppilaitos"
    (with-redefs [tarjoaja-by-hakukohde {"payment-info-test-kk-hakukohde"   koulutustoimija-oid
                                         "payment-info-test-kk-hakukohde-2" koulutustoimija-oid}]
      (let [application-key (init-application)
            _               (save (session-with-edit-rights koulutustoimija-oid) application-key
                                  "eu-blue-card-attachment" check-body)]
        (should-be-nil (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "stores the oppilaitos under the organization a superuser has selected"
    (with-redefs [tarjoaja-by-hakukohde {"payment-info-test-kk-hakukohde"   toimipiste-oid
                                         "payment-info-test-kk-hakukohde-2" toimipiste-oid}]
      (let [application-key (init-application)
            _               (save (assoc superuser-session :selected-organization {:oid    koulutustoimija-oid
                                                                                   :rights ["edit-applications"]})
                                  application-key "eu-blue-card-attachment" check-body)]
        (should= oppilaitos-oid
                 (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "stores OPH for a superuser who has selected OPH"
    (with-redefs [aac/applications-access-authorized? (constantly true)]
      (let [application-key (init-application)
            _               (save (assoc superuser-session :selected-organization {:oid    org-client/oph-organization
                                                                                   :rights ["edit-applications"]})
                                  application-key "eu-blue-card-attachment" check-body)]
        (should= org-client/oph-organization
                 (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "stores the check without an organization when the organizations cannot be fetched"
    (with-redefs [aac/applications-access-authorized? (constantly true)]
      (let [application-key (init-application)
            _               (permit-check/save-permit-check unavailable-organization-service fake-tarjonta-service
                                                            audit-logger (session-with-edit-rights tarjoaja-oid)
                                                            application-key "eu-blue-card-attachment" check-body)
            check           (stored-check application-key "eu-blue-card-attachment")]
        (should= "not-obligated" (:payment-obligation-basis check))
        (should-be-nil (:organization-oid check)))))

  (it "stores the check without an organization when the hakukohteet cannot be fetched"
    (with-redefs [aac/applications-access-authorized? (constantly true)]
      (let [application-key (init-application)
            _               (permit-check/save-permit-check fake-organization-service
                                                            (reify tarjonta/TarjontaService
                                                              (get-hakukohteet [_ _]
                                                                (throw (Exception. "Tarjonta not available"))))
                                                            audit-logger (session-with-edit-rights tarjoaja-oid)
                                                            application-key "eu-blue-card-attachment" check-body)
            check           (stored-check application-key "eu-blue-card-attachment")]
        (should= "not-obligated" (:payment-obligation-basis check))
        (should-be-nil (:organization-oid check)))))

  (it "does not store an organization when virkailija is not attached to the tarjoaja or its parents"
    (with-redefs [aac/applications-access-authorized? (constantly true)]
      (let [application-key (init-application)
            _               (save (session-with-edit-rights "1.2.246.562.10.11") application-key
                                  "eu-blue-card-attachment" check-body)]
        (should-be-nil (:organization-oid (stored-check application-key "eu-blue-card-attachment"))))))

  (it "overwrites the earlier check of the same attachment"
    (let [application-key (init-application)
          _               (save superuser-session application-key "eu-blue-card-attachment" check-body)
          saved           (save superuser-session application-key "eu-blue-card-attachment"
                                {:found-in-migri                  false
                                 :permit-valid-until              nil
                                 :payment-obligation-basis        "obligated-other"
                                 :payment-obligation-other-reason "  Lupa vanhentunut  "})]
      (should= {:found-in-migri                  false
                :permit-valid-until              nil
                :payment-obligation-basis        "obligated-other"
                :payment-obligation-other-reason "Lupa vanhentunut"}
               (select-keys saved [:found-in-migri :permit-valid-until :payment-obligation-basis
                                   :payment-obligation-other-reason]))
      (should= 1 (count (permit-check/get-permit-checks fake-organization-service application-key)))))

  (it "drops the other reason unless the payment obligation basis is other"
    (let [application-key (init-application)
          saved           (save superuser-session application-key "eu-blue-card-attachment"
                                (assoc check-body :payment-obligation-other-reason "Ei kuulu tähän"))]
      (should-be-nil (:payment-obligation-other-reason saved))))

  (it "refuses checks for attachments other than residence permits"
    (let [application-key (init-application)]
      (should= :bad-request (save superuser-session application-key "passport-attachment" check-body))
      (should= :bad-request (save superuser-session application-key "continuous-residence-permit-back" check-body))
      (should= [] (permit-check/get-permit-checks fake-organization-service application-key))))

  (it "refuses permit card validity dates outside the sanity limits"
    (let [application-key (init-application)
          save-valid-until (fn [date]
                             (save superuser-session application-key "eu-blue-card-attachment"
                                   (assoc check-body :permit-valid-until (str date))))]
      (should= :invalid-permit-valid-until (save-valid-until (.minusDays (.minusYears (time/today) 5) 2)))
      (should= :invalid-permit-valid-until (save-valid-until (.plusDays (.plusYears (time/today) 6) 2)))
      (should= [] (permit-check/get-permit-checks fake-organization-service application-key))
      (should= (str (.minusYears (time/today) 5))
               (:permit-valid-until (save-valid-until (.minusYears (time/today) 5))))
      (should= (str (.plusYears (time/today) 6))
               (:permit-valid-until (save-valid-until (.plusYears (time/today) 6))))))

  (it "accepts a stored permit card validity date that is no longer within the sanity limits"
    (let [application-key (init-application)
          old-date        (str (.minusYears (time/today) 7))]
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (jdbc/insert! conn :kk_application_payment_permit_checks
                      {:application_key    application-key
                       :attachment_key     "eu-blue-card-attachment"
                       :permit_valid_until (java.sql.Date/valueOf old-date)
                       :virkailija_oid     virkailija-oid
                       :checked_time       (java.sql.Timestamp. (System/currentTimeMillis))}))
      (should= "obligated-own-studies"
               (:payment-obligation-basis (save superuser-session application-key "eu-blue-card-attachment"
                                                (assoc check-body
                                                       :permit-valid-until old-date
                                                       :payment-obligation-basis "obligated-own-studies"))))
      (should= :invalid-permit-valid-until
               (save superuser-session application-key "eu-blue-card-attachment"
                     (assoc check-body :permit-valid-until (str (.minusYears (time/today) 8)))))))

  (it "refuses checks for a nonexistent application"
    (should= :not-found (save superuser-session "1.2.246.562.11.00000000000000999999"
                              "eu-blue-card-attachment" check-body)))

  (it "refuses checks from virkailija without edit rights to the application"
    (let [application-key (init-application)]
      (should= :unauthorized (save (session-with-edit-rights "1.2.246.562.10.11") application-key
                                   "eu-blue-card-attachment" check-body))
      (should= [] (permit-check/get-permit-checks fake-organization-service application-key))))

  (it "resets the checks of the application saved at the latest at the given time"
    (let [application-key (init-application)
          _               (save superuser-session application-key "eu-blue-card-attachment" check-body)
          _               (save superuser-session application-key "continuous-residence-permit-front" check-body)
          ; A new check is saved at the same moment as it is checked
          modified-until  (:checked-time (stored-check application-key "continuous-residence-permit-front"))
          _               (Thread/sleep 10)
          _               (save superuser-session application-key "brexit-permit-attachment" check-body)
          removed         (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                            (permit-check/reset-permit-checks connection application-key modified-until))]
      (should= #{"eu-blue-card-attachment" "continuous-residence-permit-front"} (set (map :attachment-key removed)))
      (should= ["brexit-permit-attachment"] (map :attachment-key (permit-check/get-permit-checks fake-organization-service application-key)))
      (should= 2 (count (events application-key "kk-payment-permit-check-reset")))))

  (it "does not reset checks saved after the given time"
    (let [application-key (init-application)
          _               (save superuser-session application-key "eu-blue-card-attachment" check-body)
          modified-until  (.minusSeconds (:checked-time (stored-check application-key "eu-blue-card-attachment")) 1)]
      (should= [] (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                    (permit-check/reset-permit-checks connection application-key modified-until)))
      (should= 1 (count (permit-check/get-permit-checks fake-organization-service application-key)))
      (should= 0 (count (events application-key "kk-payment-permit-check-reset")))))

  (it "removes the checks when the application is deleted"
    (let [application-key (init-application)
          _               (save superuser-session application-key "eu-blue-card-attachment" check-body)]
      ; Only inactivated or processed applications are deleted
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
        (jdbc/update! conn :application_reviews {:state "inactivated"} ["application_key = ?" application-key]))
      (application-store/mass-delete-application-data superuser-session [application-key] "Hemuli" "Testi" audit-logger)
      (should-be-nil (stored-check application-key "eu-blue-card-attachment")))))
