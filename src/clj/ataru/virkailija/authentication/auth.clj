(ns ataru.virkailija.authentication.auth
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.user-rights :as rights]
            [ataru.person-service.person-service :as person-service]
            [clj-ring-db-session.authentication.login :as crdsa-login]
            [clj-ring-db-session.session.session-store :as crdsa-session-store]
            [medley.core :refer [map-kv]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :as log]
            [yesql.core :as sql])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(declare yesql-upsert-virkailija<!)

(defn- redirect-to-login-failed-page []
  (log/info "login failed")
  (resp/redirect (resolve-url :cas.failure)))

(sql/defqueries "sql/virkailija-queries.sql")

(defn cas-login [cas-client ticket]
  (fn []
    (log/info "cas login, ticket" ticket)
    (when ticket
      (let [userdetails-future (.validateServiceTicketWithVirkailijaUserDetails cas-client (resolve-url :ataru.login-success) ticket)
            userdetails        (.get userdetails-future) ;; Use .get to retrieve the result of CompletableFuture
            userdetails-map    (bean userdetails)]
        (log/debug "cas login, userdetails" userdetails)
        (log/debug (.getUser userdetails))
        (log/debug (.getRoles userdetails))
        (log/debug userdetails-map)
        [userdetails-map ticket]))))

(defn- user-right-organizations->organization-rights
  "Takes map keyed by right with list of organizations as values, outputs map keyed by organization oid with list of rights as values"
  [user-right-organizations]
  (reduce
    (fn [acc [right organizations]]
      (reduce
        (fn [acc organization]
          (let [existing-rights (get-in acc [(:oid organization) :rights] #{})]
            (assoc acc (:oid organization) (assoc organization :rights (conj existing-rights right)))))
        acc
        organizations))
    {}
    user-right-organizations))


(defn- login-failed
  ([e]
   (log/error e "Error in login ticket handling")
   (redirect-to-login-failed-page))
  ([]
   (redirect-to-login-failed-page)))

(defn- login-succeeded [organization-service audit-logger session response roles henkilo username ticket]
  (log/debug "login succeeded")
  (log/debug "roles:" (rights/convert-to-organisaatiot (rights/strip-role-app (rights/only-ataru (rights/with-oid roles)))))
  ;TODO refaktoroi siistimmäksi, toistaiseksi välimuuttujat debuggausta varten
  (let [roles-with-oids           (rights/with-oid roles)
        ataru-roles               (rights/only-ataru roles-with-oids)
        stripped-roles            (rights/strip-role-app ataru-roles)
        converted-roles           (rights/convert-to-organisaatiot stripped-roles)
        right-organization-oids   (rights/virkailija->right-organization-oids converted-roles rights/right-names)
        organization-oids         (-> (vals right-organization-oids) (flatten) (set))
        oph-organization-member?  (contains? organization-oids organization-client/oph-organization)
        user-right-organizations  (map-kv
                                    (fn [right org-oids]
                                      [right (organization-service/get-organizations-for-oids organization-service org-oids)])
                                    right-organization-oids)
        organizations-with-rights (->> user-right-organizations
                                       (map-kv (fn [right organizations]
                                                 [right (organization-service/get-all-organizations organization-service organizations)]))
                                       (user-right-organizations->organization-rights))]
    (log/info "user" username "logged in")
    (log/info "henkilo" henkilo)
    (log/info "right-organization-oids" right-organization-oids)
    (log/info "organization-oids" organization-oids)
    (log/info "oph-organization-member?" oph-organization-member?)
    (log/info "user-right-organizations" user-right-organizations)
    (log/info "organizations-with-rights" organizations-with-rights)
    (db/exec :db yesql-upsert-virkailija<! {:oid        (:oidHenkilo henkilo)
                                            :first_name (:kutsumanimi henkilo)
                                            :last_name  (:sukunimi henkilo)})
    (audit-log/log audit-logger
                   {:new       {:ticket ticket}
                    :id        {:henkiloOid (:oidHenkilo henkilo)}
                    :session   session
                    :operation audit-log/operation-login})

    (update-in
      response
      [:session :identity]
      assoc
      :user-right-organizations user-right-organizations
      :superuser oph-organization-member?
      :organizations organizations-with-rights)))

(defn login [login-provider
             person-service
             organization-service
             audit-logger
             redirect-url
             session]
  (log/debug "login attempt")
  (try
    (if-let [[userdetails ticket] (login-provider)]
      (do
        (log/debug "login with username" (:user userdetails) "and ticket" ticket)
        (let [username    (:user userdetails)
              henkilo-oid (:henkiloOid userdetails)
              henkilo     (person-service/get-person person-service henkilo-oid)
              roles       (:roles userdetails)
              response    (crdsa-login/login
                          {:username             username
                           :henkilo              henkilo
                           :ticket               ticket
                           :success-redirect-url redirect-url})]
          (log/debug "fetched username" username "and henkilo-oid" henkilo-oid)
          (log/debug "fetched henkilo" henkilo)
          (log/debug "fetched roles" roles)
        (login-succeeded organization-service audit-logger session response roles henkilo username ticket)))
      (login-failed))
    (catch Exception e
      (log/error e "Exception in login")
      (login-failed e))))

(defn cas-initiated-logout [logout-request session-store]
  (log/info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (log/info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (log/error "Could not parse ticket from CAS request" logout-request)
      (crdsa-session-store/logout-by-ticket! session-store (.get ticket)))
    (ok)))
