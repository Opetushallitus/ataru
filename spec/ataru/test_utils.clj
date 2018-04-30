(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.db.migrations :as migrations]
            [ataru.fixtures.db.browser-test-db :refer [insert-test-form]]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [yesql.core :as sql]))

(sql/defqueries "sql/virkailija-queries.sql")

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

(defn- create-fake-virkailija-update-secret
  [application-key]
  (db/exec :db yesql-upsert-virkailija<! {:oid        "1213"
                                          :first_name "Hemuli"
                                          :last_name  "Hemuli?"})
  (virkailija-edit/create-virkailija-update-secret
   {:identity {:oid        "1213"
               :username   "tsers"
               :first-name "Hemuli"
               :last-name  "Hemuli?"}}
   application-key))

(defn- create-fake-virkailija-create-secret
  []
  (db/exec :db yesql-upsert-virkailija<! {:oid        "1214"
                                          :first_name "Mymmeli"
                                          :last_name  "Mymmeli?"})
  (virkailija-edit/create-virkailija-create-secret
   {:identity {:oid        "1214"
               :username   "ksers"
               :first-name "Mymmeli"
               :last-name  "Mymmeli?"}}))

(defn get-latest-form
  [form-name]
  (if-let [form (->> (form-store/get-all-forms)
                     (filter #(= (-> % :name :fi) form-name))
                     (first))]
    form
    (insert-test-form form-name)))

(defn get-latest-application-id-for-form [form-name]
  (->> {:query_key   "form"
        :query_value (:key (get-latest-form form-name))}
       application-store/get-application-heading-list
       first
       :id))

(defn get-latest-application-secret []
      (application-store/get-latest-application-secret))

(defn alter-application-to-hakuaikaloppu-for-secret [secret]
      (let [app (application-store/get-latest-application-by-secret secret)
            hakukohteet (:hakukohde app)
            switched-hakukohteet (concat ["1.2.246.562.20.49028100001"] (rest hakukohteet))]
           (application-store/alter-application-hakukohteet-with-secret secret switched-hakukohteet)
           (str switched-hakukohteet)))

(defn get-latest-application-by-form-name [form-name]
  (if-let [latest-application-id (get-latest-application-id-for-form form-name)]
    (application-store/get-application latest-application-id)
    (println "No test application found. Run hakija form test first!")))

(defn get-test-vars-params
  "Used in hakija routes to get required test params instead of writing them to test url."
  []
  (let [test-form   (get-latest-form "Testilomake")
        application (get-latest-application-by-form-name "Testilomake")]
    (println (str "using application " (:key application)))
    (cond->
      {:test-form-key                (:key test-form)
       :ssn-form-key                 (:key (get-latest-form "SSN_testilomake"))
       :test-question-group-form-key (:key (get-latest-form "Kysymysryhmä: testilomake"))
       :test-form-application-secret (:secret application)
       :virkailija-create-secret     (create-fake-virkailija-create-secret)}

      (some? application)
      (assoc :virkailija-secret (create-fake-virkailija-update-secret (:key application))))))
