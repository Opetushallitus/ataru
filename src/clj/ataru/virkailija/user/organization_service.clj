(ns ataru.virkailija.user.organization-service
  (:require
   [com.stuartsierra.component :as component]
   [ataru.virkailija.user.ldap-client :as ldap-client]
   [ataru.virkailija.user.organization-client :as org-client]))

(defprotocol OrganizationService
  "Facade for ldap and organization clients. Is responsible
  for passing stateful services to the stateless ldap and
  organization clients. Can also be switched to a test-double
  when needed."
  (get-direct-organization-oids [this user-name]
    "Gets this user's direct organization ids (oids) which are connected to the
     required user-right (see ldap-client/user-right-name)")
  (get-all-organizations [this user-name]
    "Gets a flattened organization hierarhy: all organizations this user has
     the right (ldap-client/user-right-name) for. Includes sub-organizations,
     but no parents"))

; The real implementation for Organization service
(defrecord IntegratedOrganizationService []
  component/Lifecycle
  OrganizationService

  (get-direct-organization-oids [this user-name]
    (ldap-client/get-organization-oids (:ldap-connection this) user-name))

  (get-all-organizations [this user-name]
    (let [direct-oids (get-direct-organizations-oids this user-name)]
      (flatten (map #(org-client/get-organizations (:cas-client this) %) direct-oids))))

  (start [this]
    (assoc this :ldap-connection (ldap-client/create-ldap-connection)))

  (stop [this]
    (.close (:ldap-connection this))))

(defn new-organization-service []
  (->IntegratedOrganizationService))
