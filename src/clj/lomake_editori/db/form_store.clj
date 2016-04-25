(ns lomake-editori.db.form-store
  (:require [yesql.core :refer [defqueries]]
            [oph.soresu.common.db :refer [exec]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defqueries "sql/form-queries.sql")

(defn t-exec [& args]
  (transform-keys ->kebab-case-keyword (apply exec args)))

(defn get-forms []
  (t-exec :db get-forms-query {}))

(defn upsert-form [{:keys [id] :as form}]
  (if (some? (when id
               (exec :db form-exists-query {:id id})))
    (t-exec :db update-form-query! form)
    (t-exec :db add-form-query<! form)))

