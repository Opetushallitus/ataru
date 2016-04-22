(ns lomake-editori.db.queries
  (:require [yesql.core :refer [defquery]]))

(defquery get-forms "sql/get-forms.sql")
