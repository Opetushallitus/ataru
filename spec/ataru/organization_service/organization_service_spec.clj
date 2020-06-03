(ns ataru.organization-service.organization-service-spec
  (:require [ataru.cache.cache-service :as cache]
            [ataru.cache.in-memory-cache :as in-memory]
            [ataru.config.core :refer [config]]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.organization-service.organization-client-spec :refer [expected-flat-organizations]]
            [ataru.organization-service.organization-service :as org-service]
            [clojure.java.io :as io]
            [clj-http.client :as http]
            [speclj.core :refer [describe it should= tags around]])
  (:import java.util.concurrent.TimeUnit))

(def test-user1-organization-oid "1.2.246.562.6.214933")

(defn fake-organization-hierarchy [call-count _]
  (swap! call-count inc)
  {:status 200 :body (slurp (io/resource "organisaatio_service/organization-hierarchy1.json"))})

(def fake-config {:organization-service {:base-address "dummy"} :cas {}})

(defn create-org-service-instance
  []
  (.start (org-service/->IntegratedOrganizationService
           ; hierarchies cache
           (.start
             (in-memory/map->InMemoryCache
              {:loader        (cache/->FunctionCacheLoader
                               (fn [key] (organization-client/get-organizations key)))
               :expires-after [3 TimeUnit/DAYS]
               :refresh-after [5 TimeUnit/MINUTES]}))
           ; groups cache
           (.start
             (in-memory/map->InMemoryCache
              {:loader        (cache/->FunctionCacheLoader
                               (fn [_] (organization-client/get-groups)))
               :expires-after [3 TimeUnit/DAYS]
               :refresh-after [5 TimeUnit/MINUTES]})))))

(describe "OrganizationService"
          (tags :unit :organization)

          (around [spec]
                  (with-redefs [config fake-config]
                    (spec)))

          (it "Should get all organizations from organization client and cache the result"
              (let [cas-get-call-count (atom 0)]
                (with-redefs [http/request (partial fake-organization-hierarchy cas-get-call-count)]
                  (let [org-service-instance (create-org-service-instance)]
                    (should= expected-flat-organizations
                             (org-service/get-all-organizations org-service-instance
                                                                [{:oid test-user1-organization-oid :name {:fi "org1"}}]))
                    (should= 1 @cas-get-call-count)))))

          (it "Should get all organizations from organization client and return passed in groups as-is"
              (with-redefs [http/request (partial fake-organization-hierarchy (atom 0))]
                (let [org-service-instance (create-org-service-instance)
                      group                {:name {:fi "Ryhmä-x"} :oid "1.2.246.562.28.1.29" :type :group}]
                  (should= (into [group] expected-flat-organizations)
                           (org-service/get-all-organizations org-service-instance
                                                              [{:oid test-user1-organization-oid :name {:fi "org1"}}
                                                               group]))))))

