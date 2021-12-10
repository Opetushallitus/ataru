(ns ataru.background-job.maksut-poller
  (:require
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]
   [ataru.config.core :refer [config]]
   [ataru.background-job.maksut-poller-job :as maksut-poller-job]
   [ataru.db.db :as db]
   [yesql.core :refer [defqueries]])
  (:import [java.util.concurrent Executors TimeUnit]))

(declare yesql-get-status-poll-applications)

(defqueries "sql/maksut-queries.sql")

(defn- start-maksut-poller-job [application-service maksut-service _ apps]
  ;TODO maybe no need for full bg-job style execution, as this service will be anyways ran hourly?

   (maksut-poller-job/poll-maksut application-service maksut-service apps)

;  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
;    (job/start-job job-runner
;      connection
;      "maksut-poller-job"
;      {:keys keys}))

  )

;(defn muu-person-info-module? [application]
;  (person-info-module/muu-person-info-module?
;   (form-store/fetch-by-id (:form application))))
;
;(defn- upsert-and-log-person [job-runner person-service application-id application]
;  (log/info "Adding applicant from application"
;            application-id
;            "to oppijanumerorekisteri")
;  (try
;    (let [{:keys [status oid]} (person-service/create-or-find-person
;                                 person-service
;                                 application)]
;      (match status
;             :created
;             (log/info "Added person" oid "to oppijanumerorekisteri")
;             :exists
;             (log/info "Person" oid "already existed in oppijanumerorekisteri"))
;      (application-store/add-person-oid application-id oid)
;      (log/info "Added person" oid "to application" application-id)
;      (start-jobs-for-person job-runner oid)
;      (log/info "Started person info update job for application" application-id))
;    (catch IllegalArgumentException e
;      (log/error e "Failed to create-or-find person for application"
;        application-id))))
;
;(defn upsert-person
;  [{:keys [application-id]}
;   {:keys [person-service] :as job-runner}]
;  {:pre [(not (nil? application-id))
;         (not (nil? person-service))]}
;  (let [application (application-store/get-application application-id)]
;    (if (muu-person-info-module? application)
;      (log/info "Not adding applicant from application"
;                application-id
;                "to oppijanumerorekisteri")
;      (upsert-and-log-person job-runner person-service application-id application))
;    {:transition {:id :final}}))
;
;(defn- update-person-info-as-in-person
;  [person-oid person]
;  (pos? (db/exec :db yesql-update-person-info-as-in-person!
;                 {:preferred_name (:kutsumanimi person)
;                  :last_name      (:sukunimi person)
;                  :ssn            (:hetu person)
;                  :dob            (:syntymaaika person)
;                  :person_oid     person-oid})))
;
;(defn- update-person-info-as-in-application
;  [person-oid]
;  (pos? (db/exec :db yesql-update-person-info-as-in-application!
;                 {:person_oid person-oid})))
;
;(defn- update-person-info
;  [henkilo-cache person-service person-oid]
;  (log/info "Checking person info of" person-oid)
;  (cache/remove-from henkilo-cache person-oid)
;  (let [person (person-service/get-person person-service person-oid)]
;    (if (or (:yksiloity person)
;            (:yksiloityVTJ person))
;      (when (update-person-info-as-in-person person-oid person)
;        (log/info "Updated person info of" person-oid
;                  "to that on oppijanumerorekisteri"))
;      (when (update-person-info-as-in-application person-oid)
;        (log/info "Updated person info of" person-oid
;                  "to that on application")))))
;
;(defn update-person-info-job-step
;  [{:keys [person-oid]}
;   {:keys [henkilo-cache person-service]}]
;  (update-person-info henkilo-cache person-service person-oid)
;  {:transition {:id :final}})

;(def job-type (str (ns-name *ns*)))
;
;(def job-definition {:steps {:initial upsert-person}
;                     :type  job-type})
;
;(defn- parse-henkilo-modified-message
;  [s]
;  (if-let [oid (:oidHenkilo (json/parse-string s true))]
;    oid
;    (throw (new RuntimeException
;                (str "Could not find key oidHenkilo from message '" s "'")))))

(defn- find-applications
  [application-service maksut-service job-runner]
  (try
    (if-let [apps (seq (db/exec :db yesql-get-status-poll-applications {:form_key (-> config :tutkintojen-tunnustaminen :maksut :form-key)}))]
      (do
        (log/info "Found " (count apps) " applications in states waiting for Maksut -actions, checking their statuses")
        (start-maksut-poller-job application-service maksut-service job-runner apps))
      (log/info "No applications in need of Maksut-polling found"))
    (catch Exception e
      (log/error e "Maksut polling failed"))))

(defrecord MaksutPollWorker [job-runner
                             application-service
                             maksut-service
                             enabled?
                             executor]
  component/Lifecycle
  (start [this]
    (when (not enabled?)
      (log/warn "MaksutPollWorker disabled"))
    (if (and enabled? (nil? executor))
      (let [executor (Executors/newSingleThreadScheduledExecutor)
            ;interval (-> config :tutkintojen-tunnustaminen :maksut :poll-interval-minutes) ;TODO use this config
            ]
        (.scheduleAtFixedRate
         executor
         (partial find-applications
                  application-service
                  maksut-service
                  job-runner)
         0 5 TimeUnit/MINUTES)
        (assoc this :executor executor))
      this))
  (stop [this]
    (when (some? executor)
      (.shutdown executor))
    (assoc this :executor nil)))
