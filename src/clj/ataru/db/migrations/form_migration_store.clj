(ns ataru.db.migrations.form-migration-store
  (:require [ataru.db.db :as db]
            [yesql.core :as sql]))

(declare yesql-migration-get-forms-query)
(sql/defqueries "sql/migration-form-queries.sql")

; Legacy migration form queries
(defn get-all-forms []
  (db/exec :db yesql-migration-get-forms-query {:authorized_organization_oids [""]
                                                :query_type                   "ALL"}))

; End legacy queries
