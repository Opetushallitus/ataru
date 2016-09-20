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
  migrate-person-info-module "1.13"
  "Update person info module structure in existing forms"
  (let [new-person-module (person-info-module/person-info-module)
        existing-forms    (try (store/get-all-forms) (catch Exception _ []))]
    (doseq [form existing-forms]
      (let [changed-form (update-person-info-module form new-person-module)]
        ; Form versioning deprecates this migration which made it into production
        ; before form versioning. No harm done for empty databases, for existing development databases
        ; this may or may not work. comment below expression if it doesn't :)
        (store/create-form-or-increment-version! changed-form)
        ))))

(defn migrate
  []
  (migrations/migrate :db "db.migration" "ataru.db.migrations"))
