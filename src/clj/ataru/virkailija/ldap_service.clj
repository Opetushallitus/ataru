(ns ataru.virkailija.ldap-service
  (:require
   [clj-ldap.client :as ldap]
   [oph.soresu.common.config :refer [config]])
  (:import (java.net InetAddress)))

(def people-path-base "ou=People,dc=opintopolku,dc=fi")

(defn- ldap-pool [{:keys [hostname port user password]}]
  (ldap/connect {:host [{:address (.getHostName hostname) :port port}]
                 :bind-dn user
                 :password password
                 :ssl? false
                 :num-connections 1}))

(defn create-ldap-connection []
  (println "create connection")
  (let [ldap-config (:ldap config)
        hostname (InetAddress/getByName (:server ldap-config))]
    (ldap-pool {:hostname hostname
                :port (:port ldap-config)
                :user (:userdn ldap-config)
                :password (:password ldap-config)})))

(defn get-user [connection user-name]
  (ldap/search connection people-path-base {:filter (str "(uid=" user-name ")")}))
