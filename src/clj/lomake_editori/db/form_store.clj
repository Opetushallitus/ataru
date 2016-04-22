(ns lomake-editori.db.form-store
  (:require [yesql.core :refer [defquery]]
            [oph.soresu.common.db :refer [exec]]))

(defquery get-forms-query "sql/get-forms.sql")

(defn get-forms []
  (exec :db get-forms-query {}))
