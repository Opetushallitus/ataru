(ns ataru.organization-service.organization-service
  (:require
   [clojure.string :refer [join]]
   [com.stuartsierra.component :as component]
   [ataru.cache.cache-service :as cache]
   [ataru.config.core :refer [config]]
   [clojure.core.cache :as ccache]
   [medley.core :refer [map-kv]]
   [ataru.organization-service.organization-client :as org-client]))

(def all-orgs-cache-time-to-live (* 2 60 1000))
(def group-oid-prefix "1.2.246.562.28")

(defn unknown-group [oid] {:oid oid :name {:fi "Tuntematon ryhmä"} :type :group})

(defprotocol OrganizationService
  (get-all-organizations [this direct-organizations-for-user]
    "Gets a flattened organization hierarhy based on direct organizations")
  (get-hakukohde-groups [this]
    "Gets all hakukohde groups")
  (get-organizations-for-oids [this organization-oids]))

(defn get-from-cache-or-real-source [cache-instance cache-key get-from-source-fn]
  (let [item (delay (get-from-source-fn))]
    (ccache/lookup (swap! cache-instance
                          #(if (ccache/has? % cache-key)
                             (ccache/hit % cache-key)
                             (ccache/miss % cache-key @item)))
                   cache-key)))

(defn get-orgs-from-cache-or-client [all-orgs-cache direct-oids]
  (let [oids-key  (join "-" direct-oids)
        cache-key (if (clojure.string/blank? oids-key) "all-orgs" oids-key)]
    (get-from-cache-or-real-source
      all-orgs-cache
      cache-key
      #(mapcat org-client/get-organizations direct-oids))))

(defn group-oid? [oid] (clojure.string/starts-with? oid group-oid-prefix))

(defn- hakukohderyhmat-from-groups [groups]
  (let [hakukohde-groups (filter :hakukohderyhma? groups)]
    (map #(select-keys % [:oid :name :hakukohderyhma? :active?]) hakukohde-groups)))

;; The real implementation for Organization service
(defrecord IntegratedOrganizationService [all-organization-groups-cache]
  component/Lifecycle
  OrganizationService

  (get-hakukohde-groups [this]
    (->> (cache/get-from all-organization-groups-cache :dummy-key)
         vals
         hakukohderyhmat-from-groups))

  (get-all-organizations [this direct-organizations]
    (let [[groups orgs]       ((juxt filter remove) #(group-oid? (:oid %)) direct-organizations)
          ;; Only fetch hierarchy for actual orgs, not groups:
          flattened-hierarchy (get-orgs-from-cache-or-client
                               (:all-orgs-cache this)
                               (map :oid orgs))]
      ;; Include groups as-is in the result:
      (concat groups flattened-hierarchy)))

  (get-organizations-for-oids [this organization-oids]
    (let [[group-oids normal-org-oids] ((juxt filter remove) group-oid? organization-oids)
          normal-orgs (map org-client/get-organization normal-org-oids)
          all-groups  (cache/get-from all-organization-groups-cache :dummy-key)
          groups      (map #(get all-groups % (unknown-group %)) group-oids)]
      (concat normal-orgs groups)))

  (start [this]
    (-> this
        (assoc :all-orgs-cache (atom (ccache/ttl-cache-factory {} :ttl all-orgs-cache-time-to-live)))))

  (stop [this]
    (assoc this :all-orgs-cache nil)))

(def fake-org-by-oid
  {"1.2.246.562.10.11"         {:name {:fi "Lasikoulu"}, :oid "1.2.246.562.10.11", :type :organization}
   "1.2.246.562.10.22"         {:name {:fi "Omnia"}, :oid "1.2.246.562.10.22", :type :organization}
   "1.2.246.562.10.1234334543" {:name {:fi "Telajärven aikuislukio"}, :oid "1.2.246.562.10.1234334543", :type :organization}
   "1.2.246.562.10.0439845"    {:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845" :type :organization}
   "1.2.246.562.28.1"          {:name {:fi "Test group"}, :oid "1.2.246.562.28.1" :type :group}
   "1.2.246.562.10.0439846"    {:name {:fi "Test org 2"}, :oid "1.2.246.562.10.0439846" :type :organization}
   "1.2.246.562.28.2"          {:name {:fi "Test group 2"}, :oid "1.2.246.562.28.2" :type :group}})

(defn fake-orgs-by-root-orgs [root-orgs]
  (some->> root-orgs
           (map :oid)
           (map name)
           (map #(get fake-org-by-oid %))))

;; Test double for UI tests
(defrecord FakeOrganizationService []
  OrganizationService

  (get-hakukohde-groups [this]
    (hakukohderyhmat-from-groups
      [(org-client/fake-hakukohderyhma 1)
       (org-client/fake-hakukohderyhma 2)
       (org-client/fake-hakukohderyhma 3)
       (org-client/fake-hakukohderyhma 4)]))

  (get-all-organizations [this root-orgs]
    (fake-orgs-by-root-orgs root-orgs))

  (get-organizations-for-oids [this organization-oids]
    (map fake-org-by-oid organization-oids)))

(defn new-organization-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeOrganizationService)
    (->IntegratedOrganizationService nil)))
