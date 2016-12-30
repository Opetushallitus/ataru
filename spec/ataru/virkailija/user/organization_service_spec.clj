(ns ataru.virkailija.user.organization-service-spec
  (:require [ataru.virkailija.user.organization-service :as org-service]
            [ataru.virkailija.user.ldap-client-spec :refer [test-user1 test-user1-organization-oid]]
            [ataru.virkailija.user.organization-client-spec :refer [expected-flat-organizations]]
            [speclj.core :refer [describe it should= tags around]]
            [clj-ldap.client :as ldap]
            [ataru.virkailija.user.ldap-client :as ataru-ldap]
            [oph.soresu.common.config :refer [config]]
            [ataru.cas.client :as cas-client]
            [clojure.java.io :as io]))

(def test-user-with-group {:employeeNumber "1.2.246.562.24.23424"
                 :description "[\"USER_jorma\", \"VIRKAILIJA\", \"LANG_fi\", \"APP_HAKULOMAKKEENHALLINTA_CRUD_1.2.246.562.6.214933\", \"APP_HAKULOMAKKEENHALLINTA_CRUD_1.2.246.562.28.1.2\"]"})

(defn fake-ldap-search-only-orgs [connection path props] [test-user1])
(defn fake-ldap-search-orgs-and-groups [connection path props] [test-user-with-group])
(defn fake-create-connection [] :fake-conn)
(defn fake-cas-auth-organization-hierarchy [call-count cas-client url]
  (swap! call-count inc)
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-hierarchy1.json"))})
(defn fake-cas-auth-organization [cas-client url]
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response1.json"))})
(defn fake-cas-auth-org-and-group [cas-client url]
  (if (.contains url "hae/nimi")
    {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response1.json"))}
    {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response-groups.json"))}))
(def fake-config {:organization-service {:base-address "dummy"} :cas {}})
(defn create-org-service-instance [] (.start (org-service/->IntegratedOrganizationService)))

(describe "OrganizationService"
          (tags :unit :organization)

          (around [spec]
                  (with-redefs [ldap/search                       fake-ldap-search-only-orgs
                                ataru-ldap/create-ldap-connection fake-create-connection
                                config                            fake-config
                                cas-client/new-client             {}]
                    (spec)))

          (it "should use ldap module to fetch organization oids"
              (let [org-service-instance (create-org-service-instance)]
                (should= [test-user1-organization-oid] (.get-direct-organization-oids org-service-instance "testi2editori"))))
          (it "Should get all organizations from organization client and cache the result"
              (let [cas-get-call-count (atom 0)]
                (with-redefs [cas-client/cas-authenticated-get (partial fake-cas-auth-organization-hierarchy cas-get-call-count)]
                  (let [org-service-instance (create-org-service-instance)]
                    (should= expected-flat-organizations
                             (.get-all-organizations org-service-instance
                                                     [{:oid test-user1-organization-oid :name {:fi "org1"}}]))
                    (should= {test-user1-organization-oid expected-flat-organizations}
                             (into {} (for [[k v] @(:all-orgs-cache org-service-instance)] [k v])))
                    (should= 1 @cas-get-call-count)))))
          
          (it "Should get direct organizatons from organization client"
              (with-redefs [cas-client/cas-authenticated-get fake-cas-auth-organization]
                (let [org-service-instance (create-org-service-instance)]
                  (should= [{:name {:fi "Telajärven seudun koulutuskuntayhtymä"}
                             :oid  "1.2.246.562.10.3242342"
                             :type :organization}]
                           (.get-direct-organizations org-service-instance "testi2editori")))))
          (it "Should get organizations from org client, groups from org client and group dump should be cached"
              (with-redefs [cas-client/cas-authenticated-get fake-cas-auth-org-and-group
                            ldap/search                      fake-ldap-search-orgs-and-groups]
                (let [org-service-instance (create-org-service-instance)]
                  (should=
                   [{:name {:fi "Telajärven seudun koulutuskuntayhtymä"}, :oid "1.2.246.562.10.3242342", :type :organization}
                    {:name {:fi "Yhteiskäyttöryhmä"}, :oid "1.2.246.562.28.1.2", :type :group}]
                   (.get-direct-organizations org-service-instance "user-name"))))))

