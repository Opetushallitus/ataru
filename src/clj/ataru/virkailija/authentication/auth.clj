(ns ataru.virkailija.authentication.auth
  (:require [oph.soresu.common.config :refer [config]]
            [taoensso.timbre :refer [info error]]
            [ataru.virkailija.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(def cas-client-url (-> config :authentication :cas-client-url))
(def opintopolku-login-url (-> config :authentication :opintopolku-login-url))
(def opintopolku-logout-url (-> config :authentication :opintopolku-logout-url))
(def ataru-login-success-url (-> config :authentication :ataru-login-success-url))

(defn- redirect-to-logged-out-page []
  (resp/redirect (str opintopolku-login-url ataru-login-success-url)))

(defn- cas-login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client cas-client-url)
        username (.run (.validateServiceTicket cas-client virkailija-login-url ticket))]
    (cas-store/login ticket)
    username))

(defn login [ticket]
  (try
    (if ticket
      (if-let [username (cas-login ticket ataru-login-success-url)]
        (do
          (info "username" username "logged in")
          (-> (resp/redirect "/lomake-editori")
              (assoc :session {:identity {:username username :ticket ticket}})))
        (redirect-to-logged-out-page))
      (redirect-to-logged-out-page))
    (catch Exception e
      (error "Error in login ticket handling" e)
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
