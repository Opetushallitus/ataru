(ns lomake-editori.db.form-store
  (:require [yesql.core :refer [defqueries]]
            [oph.soresu.common.db :refer [exec]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defqueries "sql/form-queries.sql")

(defn get-forms []
  (transform-keys ->kebab-case-keyword (exec :db get-forms-query {})))

(defn upsert-form [form]
  (let [existing-form-id-result (exec :db form-exists-query {:id (:id form)})
        exists (not-empty existing-form-id-result)]
    (if exists
      (exec :db update-form-query! form)
      (exec :db add-form-query<! form))))
