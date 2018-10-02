(ns ataru.organization-service.organization-service
  (:require
   [clojure.string :refer [join]]
   [com.stuartsierra.component :as component]
   [ataru.config.core :refer [config]]
   [clojure.core.cache :as cache]
   [medley.core :refer [map-kv]]
   [ataru.organization-service.organization-client :as org-client]))

(def all-orgs-cache-time-to-live (* 2 60 1000))
(def org-parents-cache-time-to-live (* 5 60 1000))
(def group-cache-time-to-live (* 5 60 1000))
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
    (cache/lookup (swap! cache-instance
                         #(if (cache/has? % cache-key)
                            (cache/hit % cache-key)
                            (cache/miss % cache-key @item)))
                  cache-key)))

(defn get-orgs-from-cache-or-client [all-orgs-cache direct-oids]
  (let [cache-key (join "-" direct-oids)]
    (get-from-cache-or-real-source
     all-orgs-cache
     cache-key
     #(mapcat org-client/get-organizations direct-oids))))

(defn get-groups-from-cache-or-client [group-cache]
  (get-from-cache-or-real-source
   group-cache
   :groups
   (fn [] (reduce #(assoc %1 (:oid %2) %2) {} (org-client/get-groups)))))

(defn group-oid? [oid] (clojure.string/starts-with? oid group-oid-prefix))

(defn get-group [group-cache group-oid]
  {:pre [(group-oid? group-oid)]}
  (let [groups (get-groups-from-cache-or-client group-cache)]
    (get groups group-oid (unknown-group group-oid))))

(defn- hakukohderyhmat-from-groups [groups]
  (let [hakukohde-groups (filter :hakukohderyhma? groups)]
    (map #(select-keys % [:oid :name]) hakukohde-groups)))

;; The real implementation for Organization service
(defrecord IntegratedOrganizationService []
  component/Lifecycle
  OrganizationService

  (get-hakukohde-groups [this]
    (let [groups (vals (get-groups-from-cache-or-client (:group-cache this)))]
      (hakukohderyhmat-from-groups groups)))

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
          groups      (map (partial get-group (:group-cache this))
                           group-oids)]
      (concat normal-orgs groups)))

  (start [this]
    (-> this
        (assoc :all-orgs-cache (atom (cache/ttl-cache-factory {} :ttl all-orgs-cache-time-to-live)))
        (assoc :group-cache (atom (cache/ttl-cache-factory {} :ttl group-cache-time-to-live)))
        (assoc :org-parents-cache (atom (cache/ttl-cache-factory {} :ttl org-parents-cache-time-to-live)))))

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
    (->IntegratedOrganizationService)))
