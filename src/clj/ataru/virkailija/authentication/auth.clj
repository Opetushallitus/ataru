(ns ataru.virkailija.authentication.auth
  (:require [ataru.virkailija.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas]
            [environ.core :refer [env]]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [taoensso.timbre :refer [info spy error]]
            [oph.soresu.common.config :refer [config]]
            [ataru.log.audit-log :as audit-log]
            [ataru.virkailija.user.user-rights :as rights]
            [ataru.config.url-helper :refer [resolve-url]])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(defn- redirect-to-logged-out-page []
  (resp/redirect (resolve-url :opintopolku.login)))

(defn- cas-login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client (resolve-url :cas-client))
        username (if (-> config :dev :fake-dependencies)
                   "DEVELOPER"
                   (.run (.validateServiceTicket cas-client virkailija-login-url ticket)))]
    (cas-store/login ticket)
    username))

(defn login [ticket organization-service redirect-url]
  (try
    (if ticket
      (if-let [username (cas-login ticket (resolve-url :ataru.login-success))]
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
  (-> (resp/redirect (resolve-url :opintopolku.logout))
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
