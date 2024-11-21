(ns ataru.forms.form-access-control-spec
  (:require
    [speclj.core :refer [describe it should= should-not-be-nil tags]]
    [ataru.organization-service.organization-service :as os]
    [ataru.forms.form-access-control :as fac]
    [ataru.forms.form-store :as form-store]
    [ataru.fixtures.form :as form-fixtures]
    [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-wrapper-key]]
    [ataru.tarjonta-service.mock-tarjonta-service :as mts]))

(defonce session
  {:identity
   {:user-right-organizations {:form-edit [{:oid "1.2.246.562.10.1234334543"}
                                           {:oid "form-access-control-test-oppilaitos"}]}}})

(def basic-form
  {:key              "form-access-control-test-basic-form"
   :organization-oid "1.2.246.562.10.1234334543"})

(def form-with-hakukohde
  {:key              "form-access-control-test-hakukohde-form"
   :organization-oid "no-access-to-this-org"})

(def yhteishaku-form
  {:key              "form-access-control-test-yhteishaku-form"
   :organization-oid "no-access-to-this-org"})

(def field-id-test-form
  {:key              "test-field-id-change-form"
   :organization-oid "1.2.246.562.10.12345"
   :created_by       "Test-user"
   :content          [{:fieldClass "formField"
                       :id         "vanha-id"}
                      {:fieldClass "formField"
                       :id         "jo-olemassaoleva-id"}]
   :name             {:fi "Testilomake"}
   :locked           nil
   :locked-by        nil})

(def locked-form
  {:key              "form-access-control-test-basic-form"
   :organization-oid "1.2.246.562.10.1234334543"
   :locked           nil
   :locked-by        nil})

(def locked-synthetic-form
  {:key              "form-access-control-test-basic-form"
   :organization-oid "1.2.246.562.10.1234334543"
   :locked           nil
   :locked-by        nil
   :locked-by-oid    "synteettinen_hakemus"})

(describe "update-form-lock"
   (tags :unit)

   (it "changes form lock"
     (with-redefs [form-store/fetch-form (fn [_] locked-form)
                   fac/post-form (fn [form _ _ _ _] form)]
       (let [tarjonta-service     (mts/->MockTarjontaService)
             organization-service (os/->FakeOrganizationService)
             result               (fac/update-form-lock 0 "close" session tarjonta-service organization-service nil)]
         (should-not-be-nil (:locked result)))))

   (it "doesn't allow changing form lock with synthetic forms"
     (with-redefs [form-store/fetch-form (fn [_] locked-synthetic-form)
                   fac/post-form (fn [form _ _ _ _] form)]
       (let [tarjonta-service     (mts/->MockTarjontaService)
             organization-service (os/->FakeOrganizationService)
             result               (try (fac/update-form-lock 0 "close" session tarjonta-service organization-service nil)
                                       (catch Throwable e
                                         (.getMessage e)))]
         (should= "Synteettistä hakemuslomaketta ei voi avata muokattavaksi" result)))))

(describe
  "update-form-payment-info"
  (tags :unit)

  (it "Should not be able to update payment info as a non-superuser"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              non-superuser-session (update session :identity assoc :superuser false)
              result (try (fac/update-form-payment-info "test-field-id-change-form" "payment-type-astu" nil 100
                                                       non-superuser-session tarjonta-service organization-service nil)
                          (catch Throwable e
                            (.getMessage e)))]
          (should= "Vain rekisterinpitäjä voi muokata lomakkeen maksutietoja." result))))

  (it "Should fail to update payment info if form already has applications"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)
                    form-store/form-has-applications (fn [form-key]
                                                       (= "test-field-id-change-form" form-key))]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session (update session :identity assoc :superuser true)
              failure-reason (try (fac/update-form-payment-info "test-field-id-change-form" "payment-type-astu" nil 100
                                                                superuser-session tarjonta-service organization-service nil)
                                  (catch Throwable e
                                    (.getMessage e)))]
          (should= "Lomakkeella test-field-id-change-form on hakemuksia." failure-reason)))))

(describe
  "upsert-kk-application-payment-module"
  (tags :unit)

  (it "Is not be able to upsert application payment module as a non-superuser"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)]
        (let [non-superuser-session (update session :identity assoc :superuser false)
              result (try (fac/upsert-kk-application-payment-module "test-field-id-change-form" non-superuser-session nil)

                          (catch Throwable e
                            (.getMessage e)))]
          (should= "Ei oikeuksia muokata lomaketta" result))))

  (it "Fails to upsert application payment module when form already has applications"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)
                    form-store/form-has-applications (fn [form-key]
                                                       (= "test-field-id-change-form" form-key))]
        (let [superuser-session (update session :identity assoc :superuser true)
              failure-reason (try (fac/upsert-kk-application-payment-module "test-field-id-change-form" superuser-session nil)
                                  (catch Throwable e
                                    (.getMessage e)))]
          (should= "Lomakkeella test-field-id-change-form on hakemuksia." failure-reason))))

  (it "Updates application payment module"
      (with-redefs [form-store/fetch-by-key (fn [_] form-fixtures/person-info-form)
                    form-store/create-form-or-increment-version! (fn [_ _ _])
                    form-store/form-has-applications (fn [_] false)]
        (let [superuser-session (update session :identity assoc :superuser true)
              result (fac/upsert-kk-application-payment-module "test-field-id-change-form" superuser-session nil)]
          (should= "Lomakkeen maksumoduuli päivitetty" result))))

  (it "Adds application payment module"
      (let [content (filter #(not= (:id %) kk-application-payment-wrapper-key) (:content form-fixtures/person-info-form))
            form (assoc form-fixtures/person-info-form :content content)]
        (with-redefs [form-store/fetch-by-key (fn [_] form)
                      form-store/create-form-or-increment-version! (fn [_ _ _])
                      form-store/form-has-applications (fn [_] false)]
          (let [superuser-session (update session :identity assoc :superuser true)
                result (fac/upsert-kk-application-payment-module "test-field-id-change-form" superuser-session nil)]
            (should= "Lisätty maksumoduuli lomakkeelle" result))))))

(describe
  "get-forms-for-editor"
  (tags :unit)

  (it "returns form with organization that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [basic-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {[form] :forms} (fac/get-forms-for-editor session tarjonta-service organization-service nil false)]
        (should= "form-access-control-test-basic-form" (:key form)))))

  (it "returns regular form with hakukohde that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [form-with-hakukohde])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {[form] :forms}      (fac/get-forms-for-editor session tarjonta-service organization-service nil false)]
        (should= "form-access-control-test-hakukohde-form" (:key form)))))

  (it "does not return toisen asteen yhteishaku form with hakukohde that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [yhteishaku-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {forms :forms}       (fac/get-forms-for-editor session tarjonta-service organization-service nil false)]
        (should= 0 (count forms)))))

  (it "returns toisen asteen yhteishaku form to superuser"
    (with-redefs [form-store/get-all-forms (fn [_] [yhteishaku-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            superuser-session    (update session :identity assoc :superuser true)
            {forms :forms}       (fac/get-forms-for-editor superuser-session
                                                           tarjonta-service organization-service nil false)]
        (should= 1 (count forms)))))

  (it "does not return closed form"
      (with-redefs [form-store/get-all-forms (fn [_] [(assoc-in yhteishaku-form [:properties :closed] true)])]
        (let [tarjonta-service     (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session    (update session :identity assoc :superuser true)
              {forms :forms}       (fac/get-forms-for-editor superuser-session
                                                             tarjonta-service organization-service nil false)]
          (should= true (empty? forms)))))

  (it "returns also closed form"
      (with-redefs [form-store/get-all-forms (fn [_] [(assoc-in yhteishaku-form [:properties :closed] true)])]
        (let [tarjonta-service     (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session    (update session :identity assoc :superuser true)
              {forms :forms}       (fac/get-forms-for-editor superuser-session
                                                             tarjonta-service organization-service nil true)]
          (should= 1 (count forms)))))

  (it "Should not be able to update existing form field id as a non-superuser"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              non-superuser-session (update session :identity assoc :superuser false)
              result (try (fac/update-field-id-in-form "test-field-id-change-form" "vanha-id" "uusi-id"
                                                       non-superuser-session tarjonta-service organization-service nil)
                          (catch Throwable e
                            (.getMessage e)))]
          (should= "Ei oikeuksia muokata lomakkeen kentän id:tä." result))))

  (it "Should fail to update existing form field if such field does not exist"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session (update session :identity assoc :superuser true)
              failure-reason (try (fac/update-field-id-in-form "test-field-id-change-form" "tuntematon-id" "uusi-id"
                                                               superuser-session tarjonta-service organization-service nil)
                                  (catch Throwable e
                                    (.getMessage e)))]
          (should= "Lomakkeelta ei löytynyt kenttää vanhalla id:llä tuntematon-id" failure-reason))))

  (it "Should fail to update existing form field if field with target id already exists"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session (update session :identity assoc :superuser true)
              failure-reason (try (fac/update-field-id-in-form "test-field-id-change-form" "vanha-id" "jo-olemassaoleva-id"
                                                               superuser-session tarjonta-service organization-service nil)
                                  (catch Throwable e
                                    (.getMessage e)))]
          (should= "Lomakkeelta löytyi jo kenttä uudella id:llä jo-olemassaoleva-id" failure-reason))))


  (it "Should fail to update existing form field if form already has applications"
      (with-redefs [form-store/fetch-by-key (fn [_] field-id-test-form)
                    form-store/form-has-applications (fn [form-key]
                                                       (= "test-field-id-change-form" form-key))]
        (let [tarjonta-service (mts/->MockTarjontaService)
              organization-service (os/->FakeOrganizationService)
              superuser-session (update session :identity assoc :superuser true)
              failure-reason (try (fac/update-field-id-in-form "test-field-id-change-form" "vanha" "uusi-id"
                                                               superuser-session tarjonta-service organization-service nil)
                                  (catch Throwable e
                                    (.getMessage e)))]
          (should= "Lomakkeella test-field-id-change-form on hakemuksia." failure-reason)))))
