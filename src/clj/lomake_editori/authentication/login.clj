(ns lomake-editori.authentication.login
  (:require [clj-util.cas :as cas]))

(defn login [ticket virkailija-login-url]
  (let [cas-client (cas/cas-client "https://testi.virkailija.opintopolku.fi")
        username (.run (.validateServiceTicket cas-client virkailija-login-url ticket))]
    username))
