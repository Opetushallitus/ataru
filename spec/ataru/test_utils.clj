(ns ataru.test-utils
  (:require [ataru.virkailija.virkailija-routes :as v]
            [ataru.virkailija.user.organization-service :as org-service]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]))

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
