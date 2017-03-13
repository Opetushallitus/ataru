(ns ataru.virkailija.authentication.auth
  (:require [ataru.virkailija.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas]
            [environ.core :refer [env]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :refer [info spy error]]
            [oph.soresu.common.config :refer [config]]
            [ataru.log.audit-log :as audit-log]
            [ataru.virkailija.user.user-rights :as rights])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(def cas-client-url (-> config :authentication :cas-client-url))
(def opintopolku-login-url (-> config :authentication :opintopolku-login-url))
(def opintopolku-logout-url (-> config :authentication :opintopolku-logout-url))
(def ataru-login-success-url (-> config :authentication :ataru-login-success-url))

(defn- redirect-to-logged-out-page []
  {:pre [(and (not-empty opintopolku-login-url) (not-empty ataru-login-success-url))]}
  (resp/redirect (str opintopolku-login-url ataru-login-success-url)))

(defn- cas-login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client cas-client-url)
        username (if (-> config :dev :fake-dependencies)
                   "DEVELOPER"
                   (.run (.validateServiceTicket cas-client virkailija-login-url ticket)))]
    (cas-store/login ticket)
    username))

(defn login [ticket organization-service redirect-url]
  (try
    (if ticket
      (if-let [username (cas-login ticket ataru-login-success-url)]
        (let [user-right-organizations (.get-direct-organizations-for-rights organization-service username rights/right-names)]
          (info "username" username "logged in, redirect to" redirect-url)
          (audit-log/log {:new       ticket
                          :id        username
                          :operation audit-log/operation-login})
          (-> (resp/redirect redirect-url)
              (assoc :session {:identity {:username username
                                          :ticket ticket
                                          :user-right-organizations user-right-organizations}})))
        (redirect-to-logged-out-page))
      (redirect-to-logged-out-page))
    (catch Exception e
      (error e "Error in login ticket handling")
      (redirect-to-logged-out-page))))

(defn logout [session]
  (info "username" (-> session :identity :username) "logged out")
  (cas-store/logout (-> session :identity :ticket))
  (-> (resp/redirect (str opintopolku-logout-url ataru-login-success-url))
      (assoc :session {:identity nil})))

(defn cas-initiated-logout [logout-request]
  (info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (error "Could not parse ticket from CAS request" logout-request)
      (cas-store/logout (.get ticket)))
    (ok)))

(defn logged-in? [request]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket)))
