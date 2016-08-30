(ns ataru.virkailija.ldap-service
  (:require
   [clj-ldap.client :as ldap]
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json])
  (:import (java.net InetAddress)))

(def people-path-base "ou=People,dc=opintopolku,dc=fi")

(defn create-ldap-connection []
  {:pre [(some? (:ldap config))]}
  (let [ldap-config (:ldap config)
        host        (InetAddress/getByName (:server ldap-config))
        host-name   (.getHostName host)]
    (ldap/connect {:host [{:address host-name
                           :port (:port ldap-config)}]
                   :bind-dn (:userdn ldap-config)
                   :password (:password ldap-config)
                   :ssl? (:ssl ldap-config)
                   :num-connections 4})))

(defn get-description-seq [user]
  (json/parse-string (:description user)))

(defn get-user [connection user-name]
  (first (ldap/search connection people-path-base {:filter (str "(uid=" user-name ")")})))
