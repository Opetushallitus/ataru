(ns lomake-editori.authentication.login
  (:require [oph.soresu.common.config :refer [config]]
            [clj-util.cas :as cas]))

(def cas-client-url (-> config :authentication :cas-client-url))

(defn login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client cas-client-url)
        username (.run (.validateServiceTicket cas-client virkailija-login-url ticket))]
    username))
