(ns lomake-editori.authentication.auth
  (:require [oph.soresu.common.config :refer [config]]
            [lomake-editori.authentication.cas-ticketstore :as cas-store]
            [clj-util.cas :as cas]))

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
  (cas-store/logout logout-request))

(defn logged-in? [request]
  (let [ticket (-> request :session :identity :ticket)]
    (cas-store/logged-in? ticket)))
