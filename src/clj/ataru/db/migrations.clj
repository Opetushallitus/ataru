(ns ataru.db.migrations
  (:require
    [ataru.forms.form-store :as store]
    [ataru.db.migrations.application-migration-store :as migration-app-store]
    [ataru.virkailija.component-data.person-info-module :as person-info-module]
    [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
    [clojure.java.jdbc :as jdbc :refer [with-db-transaction]]
    [crypto.random :as c]
    [oph.soresu.common.db :refer [get-datasource]]
    [oph.soresu.common.db.migrations :as migrations]
    [clojure.core.match :refer [match]]
    [taoensso.timbre :refer [spy debug info error]]
    [oph.soresu.common.config :refer [config]]))

(def default-fetch-size 50)

(defn- with-query-results-cursor [conn [sql & params :as sql-params] func]
  (with-open [stmt (.prepareStatement (jdbc/get-connection conn) sql)]
    (doseq [[index value] (map vector (iterate inc 1) params)]
      (.setObject stmt index value))
    (.setFetchSize stmt default-fetch-size)
    (with-open [rset (.executeQuery stmt)]
      (func (jdbc/result-set-seq rset)))))

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

(defn- wrap-followups [form]
  (let [fw           (atom nil)
        wrapped-form (clojure.walk/prewalk
                       (fn [expr]
                         (match expr
                           {:followup followup}
                           (do
                             (reset! fw followup)
                             (-> (dissoc expr :followup)
                                 (assoc :followups [followup])))

                           :else expr))
                       form)]
    (when @fw
      wrapped-form)))

(defn followups-to-vectored-followups
  []
  (let [existing-forms (try
                         (map #(store/fetch-by-id (:id %)) (store/get-all-forms))
                         (catch Exception _ []))]
    (doseq [form existing-forms]
      (some-> form
              wrap-followups
              (store/create-form-or-increment-version! (:organization-oid form))))))

(defn followups-to-vectored-followups-like-all-of-them
  []
  (let [update (fn [form conn]
                 (info "Updating followups of form-id:" (:id form))
                 (jdbc/execute! conn ["update forms set content = ? where id = ?" (:content form) (:id form)]))]
    (with-db-transaction [conn {:datasource (get-datasource :db)}]
      (with-query-results-cursor conn ["select id, content from forms"]
        (fn [forms]
          (doseq [form forms]
            (some->
                form
                wrap-followups
              (update conn))))))))

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
  migrate-application-haku-ids "1.36"
  "Add haku oids to applications (from tarjonta-service) with hakukohde data"
  (add-haku-details-for-applications))

(migrations/defmigration
  migrate-followups-to-vectored-followups "1.38"
  "Wrap all existing followups with vector"
  (followups-to-vectored-followups))

(migrations/defmigration
  migrate-followups-to-vectored-followups "1.39"
  "Wrap all existing followups with vector, like really all of them ever."
  (followups-to-vectored-followups-like-all-of-them))

(defn migrate
  []
 (migrations/migrate :db "db.migration" "ataru.db.migrations"))
