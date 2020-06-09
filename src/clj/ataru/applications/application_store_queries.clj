(ns ataru.applications.application-store-queries
  (:require [yesql.core :refer [defqueries]]))

(defqueries "sql/application-queries.sql")