(ns ataru.virkailija.user.ldap-client
  (:require
   [clojure.string :as str]
   [clj-ldap.client :as ldap]
   [oph.soresu.common.config :refer [config]]
   [cheshire.core :as json]
   [ataru.virkailija.user.user-rights :as user-rights])
  (:import (java.net InetAddress)))

(def people-path-base "ou=People,dc=opintopolku,dc=fi")
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

(defn get-organization-oids-from-description-seq [description-seq]
  (let [split-descriptions (map #(str/split % #"_") description-seq)
        last-items         (map #(last %) split-descriptions)]
    (distinct (filter #(.contains % oid-prefix) last-items))))

(defn get-user [connection user-name]
  (first (ldap/search connection people-path-base {:filter (str "(uid=" user-name ")")})))

(defn get-organization-oids-for-right [right description-seq]
  (let [relevant-descriptions (filter #(.contains % (user-rights/ldap-right right)) description-seq)
        oids                  (get-organization-oids-from-description-seq relevant-descriptions)]
    (when (< 0 (count oids))
      [right oids])))

(defn user->right-organization-oids
  [user rights]
  {:pre [(< 0 (count rights))]}
  (let [description-seq (get-description-seq user)]
    (into {} (map #(get-organization-oids-for-right % description-seq) rights))))

(defn get-right-organization-oids [connection user-name rights]
  (user->right-organization-oids (get-user connection user-name) rights))


