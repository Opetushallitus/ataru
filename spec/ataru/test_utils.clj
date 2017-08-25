(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.virkailija.user.organization-service :as org-service]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.db.migrations :as migrations]
            [ataru.fixtures.db.browser-test-db :refer [insert-test-form]]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]))

(def virkailija-routes (->
                        (v/new-handler)
                        (assoc :organization-service (org-service/->FakeOrganizationService))
                        .start
                        :routes))

(defn login
  "Generate ring-session=abcdefgh cookie"
  []
  (-> (mock/request :get "/lomake-editori/auth/cas")
      virkailija-routes
      :headers
      (get "Set-Cookie")
      first
      (clojure.string/split #";")
      first))

(defn should-have-header
  [header expected-val resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-contain header headers)
    (should= expected-val (get headers header))))

(defn should-not-have-header
  [header resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-not-contain header headers)))

(defn create-fake-virkailija-credentials [application-key]
  (with-redefs [ataru.virkailija.user.ldap-client/get-virkailija-by-username (fn [username] {:employeeNumber "1213" :givenName "testi" :sn "tunkki"})]
    (ataru.virkailija.authentication.virkailija-edit/create-virkailija-credentials {:identity {:username "tsers"}} application-key)))

(defn get-latest-form
  [form-name]
  (if-let [form (->> (form-store/get-all-forms)
                     (filter #(= (:name %) form-name))
                     (first))]
    form
    (insert-test-form form-name)))

(defn get-latest-application-id-for-form [form-name]
  (-> (get-latest-form form-name)
      :key
      application-store/get-application-list-by-form
      first
      :id))

(defn get-latest-application-by-form-name [form-name]
  (if-let [latest-application-id (get-latest-application-id-for-form form-name)]
    (application-store/get-application latest-application-id)
    (println "No test application found. Run hakija form test first!")))

(defn get-test-vars-params
  "Used in hakija routes to get required test params instead of writing them to test url."
  []
  (let [test-form   (get-latest-form "Testilomake")
        application (get-latest-application-by-form-name "Testilomake")]
    (println (str "application: " application))
    (cond->
      {:test-form-key                (:key test-form)
       :ssn-form-key                 (:key (get-latest-form "SSN_testilomake"))
       :test-form-application-secret (:secret application)}

      (some? application)
      (assoc :virkailija-secret (:secret (create-fake-virkailija-credentials (:key application)))))))
