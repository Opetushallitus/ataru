(ns ataru.virkailija.user.organization-service-spec
  (:require [ataru.virkailija.user.organization-service :as org-service]
            [ataru.virkailija.user.ldap-client-spec :refer [test-user1 test-user1-organization-oid]]
            [ataru.virkailija.user.organization-client-spec :refer [expected-flat-organizations]]
            [speclj.core :refer [describe it should= tags]]
            [clj-ldap.client :as ldap]
            [ataru.virkailija.user.ldap-client :as ataru-ldap]
            [ataru.cas.client :as cas-client]
            [clojure.java.io :as io]))

(defn fake-ldap-search [connection path props] [test-user1])
(defn fake-create-connection [] :fake-conn)
(defn fake-cas-authenticated-get [call-count cas-client url]
  (swap! call-count inc)
  {:status 200 :body (io/resource "organisaatio_service/organization-hierarchy1.json")})
(defn create-org-service-instance [] (.start (org-service/->IntegratedOrganizationService)))

(describe "OrganizationService"
  (tags :unit)
  (it "should use ldap module to fetch organization oids"
      (with-redefs [ldap/search                       fake-ldap-search
                    ataru-ldap/create-ldap-connection fake-create-connection]
        (let [org-service-instance (create-org-service-instance)]
          (should= [test-user1-organization-oid] (.get-direct-organization-oids org-service-instance "testi2editori")))))
  (it "Should get organizations from organization client and cache the result"
      (let [cas-get-call-count (atom 0)]
        (with-redefs [ldap/search                       fake-ldap-search
                      ataru-ldap/create-ldap-connection fake-create-connection
                      cas-client/cas-authenticated-get  (partial fake-cas-authenticated-get cas-get-call-count)]
          (let [org-service-instance (create-org-service-instance)]
            (should= expected-flat-organizations
                     (.get-all-organizations org-service-instance test-user1-organization-oid))
            (should= {test-user1-organization-oid  expected-flat-organizations}
                     (into {} @(:all-orgs-cache org-service-instance)))
            (should= 1 @cas-get-call-count))))))
