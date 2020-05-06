(ns ataru.cypress
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]))

(declare yesql-remove-form!)

(sql/defqueries "sql/cypress/cypress-queries.sql")

(defn delete-form [form-key]
  (db/exec :db yesql-remove-form! {:form_key form-key}))
