(ns ataru.db.migrations
  (:require
    [ataru.forms.form-store :as store]
    [ataru.db.migrations.application-migration-store :as migration-app-store]
    [ataru.virkailija.component-data.person-info-module :as person-info-module]
    [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
    [crypto.random :as c]
    [oph.soresu.common.db.migrations :as migrations]
    [clojure.core.match :refer [match]]
    [taoensso.timbre :refer [spy debug info error]])
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

(defn- secrets->keys
  [secrets {:keys [key]}]
  (if-not (contains? secrets key)
    (let [secret (c/url-part 34)]
      (assoc secrets key secret))
    secrets))

(defn- secret->application [application]
  (let [secret (migration-app-store/get-application-secret application)]
    (assoc application :secret secret)))

(defn populate-application-secrets
  []
  (let [applications (->> (migration-app-store/get-all-applications)
                          (map secret->application))
        secrets      (reduce secrets->keys {} applications)]
    (doseq [{:keys [key] :as application} applications
            :let [secret (get secrets key)]]
      (migration-app-store/set-application-secret application secret))))

(defn- get-haku-for-hakukohde
  [hakukohde-oid]
  (info "Loading hakukohde" hakukohde-oid)
  (when-let [haku-oid (:hakuOid (tarjonta-client/get-hakukohde hakukohde-oid))]
    (tarjonta-client/get-haku haku-oid)))

(def memo-get-haku-for-hakukohde (memoize get-haku-for-hakukohde))

(defn- add-haku-details-for-applications
  []
  (doseq [{:keys [id hakukohde]} (migration-app-store/get-applications-without-haku)]
    (if-let [haku (memo-get-haku-for-hakukohde hakukohde)]
      (do
        (migration-app-store/update-application-add-haku id haku)
        (info "Updated haku details for application" id))
      (error "Could not update haku for application" id "with hakukohde" hakukohde))))

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

(migrations/defmigration
  migrate-application-secrets "1.28"
  "Add a secret key to each application in database"
  (populate-application-secrets))

(migrations/defmigration
  migrate-application-haku-ids "1.35"
  "Add haku oids to applications (from tarjonta-service) with hakukohde data"
  (add-haku-details-for-applications))

(defn migrate
  []
 (migrations/migrate :db "db.migration" "ataru.db.migrations"))
