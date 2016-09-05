(ns ataru.virkailija.user.organization-service-spec
  (:require [ataru.virkailija.user.organization-service :as org-service]
            [ataru.virkailija.user.ldap-client-spec :refer [test-user1 test-user1-organization-oid]]
            [speclj.core :refer [describe it should= tags]]
            [clj-ldap.client :as ldap]
            [ataru.virkailija.user.ldap-client :as ataru-ldap]))

(defn fake-ldap-search [connection path props] [test-user1])
(defn fake-create-connection [] :fake-conn)

(defn create-org-service-instance [] (.start (org-service/->IntegratedOrganizationService)))

(describe "OrganizationService"
  (tags :unit)
  (it "should use ldap module to fetch organization oids"
      (with-redefs [ldap/search fake-ldap-search
                    ataru-ldap/create-ldap-connection fake-create-connection]
        (let [org-service-instance (create-org-service-instance)]
          (should= [test-user1-organization-oid] (.get-direct-organization-oids org-service-instance "testi2editori")))))
  (it "Should get organizations from organization client and cache the result"
      (with-redefs [ldap/search fake-ldap-search
                    ataru-ldap/create-ldap-connection fake-create-connection]
        (let [org-service-instance (create-org-service-instance)]
          (println "all orgs cache " @(:all-orgs-cache org-service-instance))))))
