(ns ataru.virkailija.form-store
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [oph.soresu.common.db :refer [exec get-datasource]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defqueries "sql/form-queries.sql")

(defn get-forms []
  (transform-keys ->kebab-case-keyword (exec :db get-forms-query {})))

(defn upsert-form [{:keys [id] :as form}]
  (->> (if (some? (when id
                    (first (exec :db form-exists-query form))))
         (do
           (jdbc/with-db-transaction [conn {:datasource (get-datasource :db)}]
             (update-form-query! form conn)
             (first (get-by-id {:id (:id form)} conn))))
         (exec :db add-form-query<! form))
       (transform-keys ->kebab-case-keyword)))

(defn fetch-form [id]
  (->>
    (exec :db get-by-id {:id id})
    first
    (transform-keys ->kebab-case-keyword)))

