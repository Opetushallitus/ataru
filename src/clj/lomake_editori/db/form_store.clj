(ns lomake-editori.db.form-store
  (:require [yesql.core :refer [defqueries]]
            [oph.soresu.common.db :refer [exec]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defqueries "sql/form-queries.sql")

(defn get-forms []
  (transform-keys ->kebab-case-keyword (exec :db get-forms-query {})))

(defn upsert-form [{:keys [id] :as form}]
  (->> (if (some? (when id
                    (first (exec :db form-exists-query {:id id}))))
         (do (exec :db update-form-query! form) ; transaction required
             (first (exec :db get-by-id {:id (:id form)})))
         (exec :db add-form-query<! form))
       (transform-keys ->kebab-case-keyword)))

