(ns ataru.form-store-spec
  (:require [speclj.core :refer [describe it should should= should-not= should-throw tags]]
            [ataru.time :as t]
            [ataru.fixtures.person-info-form :refer [form]]
            [ataru.log.audit-log :as audit-log]
            [ataru.forms.form-store :as store])
  (:import
   (clojure.lang ExceptionInfo)))

(defonce spec-session {:user-agent "spec"})

(def org-id  "1.2.246.562.10.2.45")
(def id-less (-> form (dissoc :id) (assoc :organization_oid org-id)))

(def audit-logger (audit-log/new-dummy-audit-logger))

(describe "form versioning"
  (tags :unit :form-versioning)

  (it "should be saved as new form"
      (let [{:keys [id key]} (store/create-new-form! id-less)]
        (should id)
        (should key)))

  (it "should version subsequent forms"
      (let [{:keys [id key created-time] :as version-one} (store/create-form-or-increment-version! (assoc id-less :organization-oid org-id) spec-session audit-logger)
            version-two (store/create-form-or-increment-version! (assoc version-one :organization-oid org-id) spec-session audit-logger)]
        (should= key (:key version-two))
        (should-not= id (:id version-two))
        (should (t/after? (:created-time version-two) created-time))))

  (it "should retrieve latest version with old version"
      (let [version-one (store/create-form-or-increment-version! (assoc id-less :organization-oid org-id) spec-session audit-logger)
            version-two (store/create-form-or-increment-version! (assoc version-one :organization-oid org-id) spec-session audit-logger)]
        (should= (:id version-two) (:id (store/fetch-latest-version (:id version-one))))
        (should= (:id version-two) (:id (store/fetch-latest-version (:id version-two))))))

  (it "should throw when later version already exists"
      (let [version-one (store/create-form-or-increment-version! (assoc id-less :organization-oid org-id) spec-session audit-logger)
            _           (store/create-form-or-increment-version! (assoc version-one :organization-oid org-id) spec-session audit-logger)]
        (should-throw ExceptionInfo "Lomakkeen sisältö on muuttunut. Lataa sivu uudelleen."
                      (keys (store/create-form-or-increment-version! (assoc version-one :organization-oid org-id) spec-session audit-logger))))))
