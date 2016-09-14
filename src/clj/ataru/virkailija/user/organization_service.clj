(ns ataru.virkailija.user.organization-service
  (:require
   [clojure.string :refer [join]]
   [com.stuartsierra.component :as component]
   [oph.soresu.common.config :refer [config]]
   [ataru.virkailija.user.ldap-client :as ldap-client]
   [ataru.cas.client :as cas-client]
   [clojure.core.cache :as cache]
   [ataru.virkailija.user.organization-client :as org-client]))

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
  (get-all-organizations [this user-name]
    "Gets a flattened organization hierarhy: all organizations this user has
     the right (ldap-client/user-right-name) for. Includes sub-organizations,
     but no parents"))

(defn get-orgs-from-client [cas-client direct-oids]
  (flatten (map #(org-client/get-organizations cas-client %) direct-oids)))

(defn get-orgs-from-cache-or-service [all-orgs-cache cas-client direct-oids]
  (let [cache-key (join "-" direct-oids)]
    ;; According to this:
    ;; https://github.com/clojure/core.cache/wiki/Using
    ;; has?/hit/miss pattern _must_ be used (although seems a bit redundant here)
    (if (cache/has? @all-orgs-cache cache-key)
      (let [orgs (cache/lookup @all-orgs-cache cache-key)]
        (swap! all-orgs-cache cache/hit cache-key)
        orgs)
      (let [orgs (get-orgs-from-client cas-client direct-oids)]
        (swap! all-orgs-cache cache/miss cache-key orgs)
        orgs))))

;; The real implementation for Organization service
(defrecord IntegratedOrganizationService []
  component/Lifecycle
  OrganizationService

  (get-direct-organization-oids [this user-name]
    (ldap-client/get-organization-oids (:ldap-connection this) user-name))

  (get-direct-organizations [this user-name]
    (let [direct-oids (get-direct-organization-oids this user-name)]
      (map #(org-client/get-organization (:cas-client this) %) direct-oids)))

  (get-all-organizations [this user-name]
    (let [direct-oids (get-direct-organization-oids this user-name)]
      (get-orgs-from-cache-or-service
       (:all-orgs-cache this)
       (:cas-client this)
       direct-oids)))

  (start [this]
    (-> this
        (assoc :cas-client (cas-client/new-client "/organisaatio-service"))
        (assoc :ldap-connection (ldap-client/create-ldap-connection))
        (assoc :all-orgs-cache (atom (cache/ttl-cache-factory {} :ttl 60000)))))

  (stop [this]
    (.close (:ldap-connection this))
    (assoc this :all-orgs-cache nil)))

;; Test double for UI tests
(defrecord FakeOrganizationService []
  OrganizationService

  (get-direct-organization-oids [this user-name] ["1.2.246.562.10.0439845"])
  (get-direct-organizations [this user-name]
    [{:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845"}])
  (get-all-organizations [this user-name]
    [{:name {:fi "Test org"}, :oid "1.2.246.562.10.0439845"}]))


(defn new-organization-service []
  (cond (not (-> config :organization-service :enabled)) {}
        ;; Ui automated test mode
        (-> config :dev :fake-dependencies)              (->FakeOrganizationService)
        ;; Normal mode
        :else                                            (->IntegratedOrganizationService)))
