(ns ataru.db.migrations
  (:require
    [ataru.forms.form-store :as store]
    [ataru.db.migrations.application-migration-store :as migration-app-store]
    [ataru.virkailija.component-data.person-info-module :as person-info-module]
    [oph.soresu.common.db.migrations :as migrations]
    [clojure.core.match :refer [match]]
    [taoensso.timbre :refer [spy debug]])
  (:use [oph.soresu.common.config :only [config]]))

(defn- update-person-info-module
  [new-person-info-module form]
  (clojure.walk/prewalk
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

(defn application-id->application-key
  "Make application_events to refer to applications using
   applications.key instead of applications.id"
  []
  (let [applications (migration-app-store/get-all-applications)]
    (doseq [application applications
            :let [application-id  (:id application)
                  application-key (:key application)]]
      (doseq [application-event (migration-app-store/get-application-events-by-application-id application-id)
              :when (nil? (:application-key application-event))
              :let [event-id (:id application-event)]]
        (migration-app-store/set-application-key-to-application-event event-id application-key))
      (doseq [confirmation-email (migration-app-store/get-application-confirmation-emails application-id)
              :when (nil? (:application-key confirmation-email))
              :let [confirmation-id (:id confirmation-email)]]
        (migration-app-store/set-application-key-to-application-confirmation-email confirmation-id application-key))
      (let [application-review (migration-app-store/get-application-review-by-application-id application-id)]
        (when (nil? (:application-key application-review))
          (let [review-id (:id application-review)]
            (migration-app-store/set-application-key-to-application-review review-id application-key)))))))

(migrations/defmigration
  migrate-person-info-module "1.13"
  "Update person info module structure in existing forms"
  (refresh-person-info-modules))

(migrations/defmigration
  migrate-person-info-module "1.22"
  "Update person info module structure in existing forms"
  (refresh-person-info-modules))

(migrations/defmigration
  migrate-application-versioning "1.25"
  "Change references to applications.id to be references to applications.key"
  (application-id->application-key))

(defn migrate
  []
 (migrations/migrate :db "db.migration" "ataru.db.migrations"))
