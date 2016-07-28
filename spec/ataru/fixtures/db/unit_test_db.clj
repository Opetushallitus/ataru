(ns ataru.fixtures.db.unit-test-db
  (:require [yesql.core :refer [defqueries]]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.forms.form-store :as form-store]
            [oph.soresu.common.config :refer [config]]
            [oph.soresu.common.db :as soresu-db]))

(defqueries "sql/form-queries.sql")
(defqueries "sql/dev-form-queries.sql")

(defn init-db-fixture []
  (let [id (:id (form-store/upsert-form form-fixtures/person-info-form))]
    (soresu-db/exec :db yesql-set-form-id! {:old_id id :new_id (:id form-fixtures/person-info-form)})))

(defn clear-database []
  (soresu-db/clear-db! :db (get-in config [:db :schema])))
