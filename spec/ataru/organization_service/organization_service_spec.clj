(ns ataru.organization-service.organization-service-spec
  (:require [ataru.config.core :refer [config]]
            [ataru.organization-service.ldap-client :as ataru-ldap]
            [ataru.organization-service.ldap-client-spec :refer [test-user1 test-user1-organization-oid]]
            [ataru.organization-service.organization-client-spec :refer [expected-flat-organizations]]
            [ataru.organization-service.organization-service :as org-service]
            [clj-ldap.client :as ldap]
            [clojure.java.io :as io]
            [org.httpkit.client :as http]
            [speclj.core :refer [describe it should= tags around]]))

(def test-user-with-group {:employeeNumber "1.2.246.562.24.23424"
                           :description "[\"USER_jorma\", \"VIRKAILIJA\", \"LANG_fi\", \"APP_ATARU_EDITORI_CRUD_1.2.246.562.6.214933\", \"APP_ATARU_EDITORI_CRUD_1.2.246.562.28.1.2\"]"})

(def test-user1-organization
  {:name {:fi "Telajärven seudun koulutuskuntayhtymä"}, :oid "1.2.246.562.10.3242342", :type :organization})

(def telajarvi-org {:name {:fi "Telajärven seudun koulutuskuntayhtymä"}, :oid "1.2.246.562.10.3242342", :type :organization})

(defn fake-ldap-search-only-orgs [connection path props] [test-user1])

(defn fake-ldap-search-orgs-and-groups [connection path props] [test-user-with-group])

(defn fake-create-connection [] :fake-conn)

(defn fake-organization-hierarchy [call-count _]
  (swap! call-count inc)
  (atom {:status 200 :body (slurp (io/resource "organisaatio_service/organization-hierarchy1.json"))}))

(defn fake-organization [_]
  (atom {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response1.json"))}))

(defn fake-org-and-group [{:keys [url]}]
  (atom
   (if (.contains url "hae/nimi")
     {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response1.json"))}
     {:status 200 :body (slurp (io/resource "organisaatio_service/organization-response-groups.json"))})))

(def fake-config {:organization-service {:base-address "dummy"} :cas {}})

(defn create-org-service-instance [] (.start (org-service/->IntegratedOrganizationService)))

(describe "OrganizationService"
          (tags :unit :organization)

          (around [spec]
                  (with-redefs [ldap/search                       fake-ldap-search-only-orgs
                                ataru-ldap/create-ldap-connection fake-create-connection
                                config                            fake-config]
                    (spec)))

          (it "should use ldap module to fetch organization oids"
              (with-redefs [http/request fake-org-and-group]
                (let [org-service-instance (create-org-service-instance)]
                  (should= {:form-edit [test-user1-organization]}
                           (org-service/get-direct-organizations-for-rights
                            org-service-instance
                            "testi2editori"
                            [:form-edit])))))

          (it "Should get all organizations from organization client and cache the result"
              (let [cas-get-call-count (atom 0)]
                (with-redefs [http/request (partial fake-organization-hierarchy cas-get-call-count)]
                  (let [org-service-instance (create-org-service-instance)]
                    (should= expected-flat-organizations
                             (.get-all-organizations org-service-instance
                                                     [{:oid test-user1-organization-oid :name {:fi "org1"}}]))
                    (should= {test-user1-organization-oid expected-flat-organizations}
                             (into {} (for [[k v] @(:all-orgs-cache org-service-instance)] [k v])))
                    (should= 1 @cas-get-call-count)))))

          (it "Should get direct organizatons from organization client"
              (with-redefs [http/request fake-organization]
                (let [org-service-instance (create-org-service-instance)]
                  (should= {:form-edit [telajarvi-org]}
                           (org-service/get-direct-organizations-for-rights
                            org-service-instance
                            "testi2editori"
                            [:form-edit])))))

          (it "Should get organizations from org client, groups from org client and group dump should be cached"
              (with-redefs [http/request fake-org-and-group
                            ldap/search fake-ldap-search-orgs-and-groups]
                (let [org-service-instance (create-org-service-instance)
                      expected-group       {:name {:fi "Yhteiskäyttöryhmä"}, :oid "1.2.246.562.28.1.2", :type :group :hakukohderyhma? false}
                      result      (org-service/get-direct-organizations-for-rights org-service-instance "user-name" [:form-edit])]
                  (should=
                   {:form-edit [telajarvi-org expected-group]}
                   result)
                  (should= expected-group (get-in @(:group-cache org-service-instance) [:groups "1.2.246.562.28.1.2"])))))

          (it "Should get all organizations from organization client and return passed in groups as-is"
              (with-redefs [http/request (partial fake-organization-hierarchy (atom 0))]
                (let [org-service-instance (create-org-service-instance)
                      group                {:name {:fi "Ryhmä-x"} :oid "1.2.246.562.28.1.29" :type :group}]
                  (should= (into [group] expected-flat-organizations)
                           (.get-all-organizations org-service-instance
                                                   [{:oid test-user1-organization-oid :name {:fi "org1"}}
                                                    group]))))))

