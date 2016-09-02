(ns ataru.db.migrations
  (:require
    [ataru.forms.form-store :as store]
    [ataru.virkailija.component-data.person-info-module :as person-info-module]
    [oph.soresu.common.db.migrations :as migrations])
  (:use [oph.soresu.common.config :only [config]]))

(defn- update-person-info-module
  [form new-person-info-module]
  (let [content-without-person-info-module (remove #(= (:module %) "person-info") (:content form))
        content-with-new-person-info-module (into [new-person-info-module] content-without-person-info-module)]
    (assoc-in form [:content] content-with-new-person-info-module)))

(migrations/defmigration
  migrate-person-info-module "1.12"
  "Update person info module structure in existing forms"
  (let [new-person-module (person-info-module/person-info-module)
        existing-forms    (store/get-forms)]
    (doseq [form existing-forms]
      (let [changed-form (update-person-info-module form new-person-module)]
        (store/upsert-form changed-form)))))

(defn migrate
  []
  (migrations/migrate :db "db.migration" "ataru.db.migrations"))
