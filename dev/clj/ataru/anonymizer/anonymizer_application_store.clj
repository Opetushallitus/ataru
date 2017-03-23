(ns ataru.anonymizer.anonymizer-application-store
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]))

(sql/defqueries "sql/anonymizer-application-queries.sql")

(defn get-all-applications []
  (db/exec :db sql-get-all-applications {}))

(defn update-application [application]
  (db/exec :db sql-update-application! application))
