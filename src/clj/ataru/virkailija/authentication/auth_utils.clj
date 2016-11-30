(ns ataru.virkailija.authentication.auth-utils
  (:require [oph.soresu.common.config :refer [config]]))

(def ^:private opintopolku-login-url (get-in config [:authentication :opintopolku-login-url]))
(def ^:private ataru-login-success-url (get-in config [:authentication :ataru-login-success-url]))

(defn cas-auth-url []
  (str opintopolku-login-url ataru-login-success-url))
