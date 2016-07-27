(ns ataru.fixtures.db.unit-test-db
  (:require [yesql.core :refer [defqueries]]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.forms.form-store :as form-store]
            [oph.soresu.common.db :as soresu-db]))

(defqueries "sql/form-queries.sql")
(defqueries "sql/dev-form-queries.sql")

(defn init-db-fixture []
  (soresu-db/exec :db yesql-add-form-with-id-query<! form-fixtures/person-info-form))

(defn clear-database []
  (soresu-db/exec :db yesql-delete-all-application_events! {})
  (soresu-db/exec :db yesql-delete-all-applications! {})
  (soresu-db/exec :db yesql-delete-all-forms! {}))
