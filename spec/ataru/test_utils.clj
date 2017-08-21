(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.virkailija.user.organization-service :as org-service]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [ataru.db.db :as db]
            [ataru.db.migrations :as migrations]
            [ataru.fixtures.db.browser-test-db :refer [insert-test-form insert-test-application reset-test-db]]
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

(defn prepare-ui-tests []
  (reset-test-db true)
  (ataru.koodisto.koodisto-db-cache/get-cached-koodi-options :db "posti" 1)) ;; Warm up koodisto cache or getting city by postal code will fail)

(defn get-latest-form
  [form-name]
  (if-let [form (->> (form-store/get-all-forms)
                     (filter #(= (:name %) form-name))
                     (first))]
    form
    (insert-test-form form-name)))

(defn get-latest-application-for-form [form-name]
  (-> (get-latest-form form-name)
      :key
      application-store/get-application-list-by-form
      first))

(defn get-test-vars-params
  []
  (let [test-form (get-latest-form "Testilomake")]
    {:test-form-key                (:key test-form)
     :ssn-form-key                 (:key (get-latest-form "SSN_testilomake"))
     :test-form-application-secret (-> (insert-test-application (:id test-form))
                                       (application-store/get-application)
                                       :secret)}))
