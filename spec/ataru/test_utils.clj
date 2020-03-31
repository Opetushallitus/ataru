(ns ataru.test-utils
  (:require [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.virkailija.virkailija-routes :as v]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.fixtures.db.browser-test-db :refer [insert-test-form]]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.applications.application-service :as application-service]
            [ataru.log.audit-log :as audit-log]
            [yesql.core :as sql]))

(sql/defqueries "sql/virkailija-queries.sql")

(def virkailija-routes (->
                        (v/new-handler)
                        (assoc :organization-service (org-service/->FakeOrganizationService))
                        (assoc :tarjonta-service (tarjonta-service/->MockTarjontaService))
                        (assoc :person-service (person-service/->FakePersonService))
                        (assoc :kayttooikeus-service (kayttooikeus-service/->FakeKayttooikeusService))
                        (assoc :application-service (application-service/new-application-service))
                        (assoc :audit-logger (audit-log/new-dummy-audit-logger))
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
  (->> (application-store/get-application-heading-list
        {:form (:key (get-latest-form form-name))}
        {:order-by "created-time"
         :order    "desc"})
       first
       :id))

(defn get-latest-application-secret []
      (application-store/get-latest-application-secret))

(defn alter-application-to-hakuaikaloppu-for-secret [secret]
  (let [application (application-store/get-latest-version-of-application-for-edit false {:secret secret})
        hakukohde   (vec (cons "1.2.246.562.20.49028100001" (rest (:hakukohde application))))
        answers     (mapv (fn [answer]
                            (if (= "hakukohteet" (:key answer))
                              (assoc answer :value hakukohde)
                              answer))
                          (:answers application))]
    (application-store/alter-application-hakukohteet-with-secret secret hakukohde answers)))

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
       :test-selection-limit-form-key (:key (get-latest-form "Selection Limit"))
       :test-form-application-secret (:secret application)
       :virkailija-create-secret     (create-fake-virkailija-create-secret)}

      (some? application)
      (assoc :virkailija-secret (create-fake-virkailija-update-secret (:key application))))))
