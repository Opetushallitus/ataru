(ns ataru.organization-service.user-rights
  (:require [ataru.config.core :refer [config]]
            [cheshire.core :as json]
            [schema.core :as s]))

(def ^:private
  rights
  {:form-edit         "APP_ATARU_EDITORI_CRUD"
   :view-applications "APP_ATARU_HAKEMUS_READ"
   :edit-applications "APP_ATARU_HAKEMUS_CRUD"})

(def right-names (keys rights))

(def oid-prefix "1.2.246.562")

(s/defschema Right (apply s/enum right-names))

(s/defn ^:always-validate ldap-right [right :- Right]
  (let [name-from-config (-> config :ldap :user-right-names right)]
    (if (not (clojure.string/blank? name-from-config))
      name-from-config
      (right rights))))

(defn- get-description-seq [user]
  (json/parse-string (:description user)))

(defn- get-organization-oids-from-description-seq [description-seq]
  (let [split-descriptions (map #(clojure.string/split % #"_") description-seq)
        last-items         (map #(last %) split-descriptions)]
    (distinct (filter #(.contains % oid-prefix) last-items))))

(defn- get-organization-oids-for-right [right description-seq]
  (let [relevant-descriptions (filter #(.contains % (ldap-right right)) description-seq)
        oids                  (get-organization-oids-from-description-seq relevant-descriptions)]
    (when (< 0 (count oids))
      [right oids])))

(defn user->right-organization-oids
  [user rights]
  {:pre [(< 0 (count rights))]}
  (let [description-seq (get-description-seq user)]
    (into {} (map #(get-organization-oids-for-right % description-seq) rights))))
