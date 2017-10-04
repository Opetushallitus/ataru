(ns ataru.fixtures.db.unit-test-db
  (:require [yesql.core :refer [defqueries]]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as ataru-db]))

(defqueries "sql/dev-form-queries.sql")

(defn- nuke-old-fixture-data [form-id]
  (ataru-db/exec :db yesql-delete-fixture-application-review! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application-events! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-application! {:form_id form-id})
  (ataru-db/exec :db yesql-delete-fixture-form! {:id form-id}))

(defn init-db-fixture
  [fixture]
  (nuke-old-fixture-data (:id fixture))
  (let [{:keys [id] :as form} (form-store/create-form-or-increment-version! fixture)]
    (ataru-db/exec :db yesql-set-form-id! {:old_id id :new_id (:id fixture)})
    form))

(defn clear-database []
                     (ataru-db/clear-db! :db (get-in config [:db :schema])))
