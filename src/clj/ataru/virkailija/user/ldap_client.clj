(ns ataru.virkailija.user.ldap-client
  (:require
   [clojure.string :as str]
   [clj-ldap.client :as ldap]
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json])
  (:import (java.net InetAddress)))

(def people-path-base "ou=People,dc=opintopolku,dc=fi")
(def user-right-name "APP_HAKULOMAKKEENHALLINTA_CRUD")
(def oid-prefix "1.2.246.562")

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

(defn filter-with-user-right [description-seq]
  (filter #(.contains % user-right-name) description-seq))

(defn get-organization-oids-from-description-seq [description-seq]
  (let [split-descriptions (map #(str/split % #"_") description-seq)
        last-items         (map #(last %) split-descriptions)]
    (filter #(.contains % oid-prefix) last-items)))

(defn get-user [connection user-name]
  (first (ldap/search connection people-path-base {:filter (str "(uid=" user-name ")")})))

(defn get-organization-oids
  ([user]
   (-> user
       get-description-seq
       filter-with-user-right
       get-organization-oids-from-description-seq))
  ([connection user-name]
   (get-organization-oids (get-user connection user-name))))
