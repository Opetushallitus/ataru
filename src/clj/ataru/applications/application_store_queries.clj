(ns ataru.applications.application-store-queries
  (:require [yesql.core :refer [defqueries]]
            [hugsql.core :as hugsql]))

(defqueries "sql/application-queries.sql")

(hugsql/def-sqlvec-fns "sql/application-list-query.sql")
