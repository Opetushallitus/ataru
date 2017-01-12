(ns ataru.virkailija.user.organization-service
  (:require
   [clojure.string :refer [join]]
   [com.stuartsierra.component :as component]
   [oph.soresu.common.config :refer [config]]
   [ataru.virkailija.user.ldap-client :as ldap-client]
   [ataru.cas.client :as cas-client]
   [clojure.core.cache :as cache]
   [ataru.virkailija.user.organization-client :as org-client]))

(def all-orgs-cache-time-to-live (* 2 60 1000))
(def group-cache-time-to-live (* 5 60 1000))
(def group-oid-prefix "1.2.246.562.28")

(defn unknown-group [oid] {:oid oid :name {:fi "Tuntematon ryhmä"} :type :group})

(defprotocol OrganizationService
  "Facade for ldap and organization clients. Is responsible
  for passing stateful services to the stateless ldap and
  organization clients. Can also be switched to a test-double
  when needed."
  (get-direct-organization-oids [this user-name]
    "Gets this user's direct organization ids (oids) which are connected to the
     required user-right (see ldap-client/user-right-name)")
  (get-direct-organizations [this user-name]
    "Gets this user's direct organizations (as in get-direct-organization-oids
     but gets organization name as well)")
  (get-all-organizations [this direct-organizations-for-user]
    "Gets a flattened organization hierarhy based on direct organizations"))

(defn get-orgs-from-client [cas-client direct-oids]
  (flatten (map #(org-client/get-organizations cas-client %) direct-oids)))

(defn get-groups-from-client [cas-client]
  (let [groups-as-seq (org-client/get-groups cas-client)]
    (into {} (map (fn [group] [(:oid group) group]) groups-as-seq))))

(defn get-from-cache-or-real-source [cache-instance cache-key get-from-source-fn]
  ;; According to this:
  ;; https://github.com/clojure/core.cache/wiki/Using
  ;; has?/hit/miss pattern _must_ be used (although seems a bit redundant here)
  (if (cache/has? @cache-instance cache-key)
    (let [item (cache/lookup @cache-instance cache-key)]
      (swap! cache-instance cache/hit cache-key)
      item)
    (let [item (get-from-source-fn)]
      (swap! cache-instance cache/miss cache-key item)
      item)))

(defn get-orgs-from-cache-or-client [all-orgs-cache cas-client direct-oids]
  (let [cache-key (join "-" direct-oids)]
    (get-from-cache-or-real-source
     all-orgs-cache
     cache-key
     #(get-orgs-from-client cas-client direct-oids))))

(defn get-groups-from-cache-or-client [group-cache cas-client]
  (get-from-cache-or-real-source
   group-cache
   :groups
   #(get-groups-from-client cas-client)))

(defn group-oid? [oid] (clojure.string/starts-with? oid group-oid-prefix))

(defn get-group [group-cache cas-client group-oid]
  {:pre [(group-oid? group-oid)]}
  (let [groups (get-groups-from-cache-or-client group-cache cas-client)]
    (get groups group-oid (unknown-group group-oid))))

;; The real implementation for Organization service
(defrecord IntegratedOrganizationService []
  component/Lifecycle
  OrganizationService

  (get-direct-organization-oids [this user-name]
    (ldap-client/get-organization-oids (:ldap-connection this) user-name))

  (get-direct-organizations [this user-name]
    (let [direct-oids                  (get-direct-organization-oids this user-name)
          [group-oids normal-org-oids] ((juxt filter remove) group-oid? direct-oids)
          normal-orgs                  (remove nil? (map #(org-client/get-organization (:cas-client this) %) normal-org-oids))
          groups                       (map (partial
                                             get-group
                                             (:group-cache this)
                                             (:cas-client this))
                                            group-oids)]
                                        ; OPH org doesn't exist in organization service, hence we'll have to filter out nil values
      (concat normal-orgs groups)))

  (get-all-organizations [this direct-organizations-for-user]
    (let [[groups orgs]       ((juxt filter remove) #(group-oid? (:oid %)) direct-organizations-for-user)
          ;; Only fetch hierarchy for actual orgs, not groups:
          flattened-hierarchy (get-orgs-from-cache-or-client
                               (:all-orgs-cache this)
                               (:cas-client this)
                               (map :oid orgs))]
      ;; Include groups as-is in the result:
      (concat groups flattened-hierarchy)))

  (start [this]
    (-> this
        (assoc :cas-client (cas-client/new-client "/organisaatio-service"))
        (assoc :ldap-connection (ldap-client/create-ldap-connection))
        (assoc :all-orgs-cache (atom (cache/ttl-cache-factory {} :ttl all-orgs-cache-time-to-live)))
        (assoc :group-cache (atom (cache/ttl-cache-factory {} :ttl group-cache-time-to-live)))))

  (stop [this]
    (.close (:ldap-connection this))
    (assoc this :all-orgs-cache nil)))

(def fake-orgs [{:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845" :type :organization}
                {:name {:fi "Test group"}, :oid "1.2.246.562.28.1" :type :group}])

;; Test double for UI tests
(defrecord FakeOrganizationService []
  OrganizationService

  (get-direct-organization-oids [this user-name] (:oid (first fake-orgs)))
  (get-direct-organizations [this user-name] fake-orgs)
  (get-all-organizations [this root-orgs] fake-orgs))

(defn new-organization-service []
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeOrganizationService)
    (->IntegratedOrganizationService)))
