(ns lomake-editori.authentication.auth
  (:require [oph.soresu.common.config :refer [config]]
            [taoensso.timbre :refer [info error]]
            [lomake-editori.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(def cas-client-url (-> config :authentication :cas-client-url))

(defn login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client cas-client-url)
        username (.run (.validateServiceTicket cas-client virkailija-login-url ticket))]
    (cas-store/login ticket)
    username))

(defn logout [session]
  (let [ticket (-> session :identity :ticket)]
    (cas-store/logout ticket)))

(defn cas-initiated-logout [logout-request]
  (info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (error "Could not parse ticket from CAS request" logout-request)
      (cas-store/logout (.get ticket)))))

(defn logged-in? [request]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket)))
