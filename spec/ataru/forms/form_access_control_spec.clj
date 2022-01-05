(ns ataru.forms.form-access-control-spec
  (:require
    [speclj.core :refer [describe it should= tags]]
    [ataru.organization-service.organization-service :as os]
    [ataru.forms.form-access-control :as fac]
    [ataru.forms.form-store :as form-store]
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

(describe
  "get-forms-for-editor"
  (tags :unit)

  (it "returns form with organization that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [basic-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {[form] :forms} (fac/get-forms-for-editor session tarjonta-service organization-service nil)]
        (should= "form-access-control-test-basic-form" (:key form)))))

  (it "returns regular form with hakukohde that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [form-with-hakukohde])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {[form] :forms}      (fac/get-forms-for-editor session tarjonta-service organization-service nil)]
        (should= "form-access-control-test-hakukohde-form" (:key form)))))

  (it "does not return toisen asteen yhteishaku form with hakukohde that user has edit rights to"
    (with-redefs [form-store/get-all-forms (fn [_] [yhteishaku-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            {forms :forms}       (fac/get-forms-for-editor session tarjonta-service organization-service nil)]
        (should= 0 (count forms)))))

  (it "returns toisen asteen yhteishaku form to superuser"
    (with-redefs [form-store/get-all-forms (fn [_] [yhteishaku-form])]
      (let [tarjonta-service     (mts/->MockTarjontaService)
            organization-service (os/->FakeOrganizationService)
            superuser-session    (update session :identity assoc :superuser true)
            {forms :forms}       (fac/get-forms-for-editor superuser-session tarjonta-service organization-service nil)]
        (println superuser-session)
        (should= 1 (count forms))))))
