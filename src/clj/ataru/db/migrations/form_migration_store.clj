(ns ataru.db.migrations.form-migration-store
  (:require [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :as t]
            [ataru.db.db :as db]
            [yesql.core :as sql]))

(sql/defqueries "sql/migration-form-queries.sql")

; Legacy migration form queries
(defn get-all-forms []
  (db/exec :db yesql-migration-get-forms-query {:authorized_organization_oids [""]
                                                :query_type                   "ALL"}))

; End legacy queries