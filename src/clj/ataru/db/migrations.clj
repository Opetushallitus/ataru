(ns ataru.db.migrations
  (:require
    [ataru.forms.form-store :as store]
    [ataru.virkailija.component-data.person-info-module :as person-info-module]
    [oph.soresu.common.db.migrations :as migrations]
    [clojure.core.match :refer [match]]
    [taoensso.timbre :refer [spy debug]])
  (:use [oph.soresu.common.config :only [config]]))

(defn- update-person-info-module
  [new-person-info-module form]
  (clojure.walk/postwalk
    (fn [expr]
      (match expr
        {:module (:or :person-info "person-info")}
        new-person-info-module
        :else expr))
    form))

(defn refresh-person-info-modules []
  (let [new-person-module (person-info-module/person-info-module)
        existing-forms    (try
                            (map #(store/fetch-by-id (:id %)) (store/get-all-forms))
                            (catch Exception _ []))]
    (doseq [form existing-forms]
      (let [changed-form (update-person-info-module new-person-module form)]
        (store/create-form-or-increment-version! changed-form (:organization-oid form))))))

(migrations/defmigration
  migrate-person-info-module "1.13"
  "Update person info module structure in existing forms"
  (refresh-person-info-modules))

(migrations/defmigration
  migrate-person-info-module "1.21"
  "Update person info module structure in existing forms"
  (refresh-person-info-modules))

(defn migrate
  []
 (migrations/migrate :db "db.migration" "ataru.db.migrations"))
