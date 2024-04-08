(ns ataru.background-job.job
  "Public API of the Background Job system"
  (:require
    [ataru.aws.cloudwatch :as cloudwatch]
    [ataru.config.core :refer [config]]
    [taoensso.timbre :as log]
    [com.stuartsierra.component :as component]
    [clojure.java.jdbc :as jdbc]
    [proletarian.worker :as worker]
    [proletarian.job :as job]
    [chime.core :as chime]
    [yesql.core :refer [defqueries]]
    [cronstar.core :as cron]
    [clojure.string :refer [includes? join]])
  (:import java.sql.BatchUpdateException
           java.time.Instant
           java.time.Duration))

(defprotocol JobRunner
  (start-job [this connection job-type initial-state]
    "Start a new background job of type <job-type>.
     initial-state is the initial data map needed to start the job
     (can be anything)")

  (get-job-types [this]
    "List currently configured job-types and whether they are enabled")
  (update-job-types [this job-type-statuses]
    "Update enabled status for currently configured job types")

  (get-queue-lengths [this]
    "Returns queue lengths for jobs, used for metrics")

  (cleanup-archived-jobs [this seconds]
    "Cleans up archived jobs older than seconds parameter"))

(def proletarian-enqueue-options
  "Default options for enqueueing work"
  {:proletarian/job-table "proletarian_jobs"})
(def proletarian-worker-options
  "Default options for creating Proletarian workers"
  {:proletarian/log (fn [event payload] (if (includes? event "polling") (log/debug event payload) (log/info event payload)))
   :proletarian/job-table "proletarian_jobs"
   :proletarian/archived-job-table "proletarian_archived_jobs"
   :proletarian/polling-interval-ms 2000
   :proletarian.retry/failed-job-fn (fn [attrs e] (log/error e "Failed job" attrs))})

(declare yesql-add-scheduled-job-instance!)
(declare yesql-add-job-type!)
(declare yesql-get-job-types)
(declare yesql-update-job-type!)
(declare yesql-get-queue-lengths)
(declare yesql-remove-archived-jobs-older-than!)


(defqueries "sql/scheduled-job-queries.sql")

(defn- cron-schedule
  "Returns a lazy seq of Instants based on a cron schedule"
  [exp]
  (map (fn [t] (Instant/ofEpochMilli (.getMillis t))) (cron/times exp)))

(defn- create-worker
  "Creates and starts a new Proletarian worker based on job definition where:
   - queue name is the job type
   - other options are based on defaults (see: [[proletarian-worker-options]])
     overridden with options supplied in :queue attribute of the job definition
   - jobs run are provided with the payload as the first and JobRunner as the
     second parameter (this contains different services needed by jobs)"
  [ds job-definition job-runner]
  (let [worker (worker/create-queue-worker
    ds
    (fn [_ payload]
      (log/info "Running" (:type job-definition))
      (try
        ((:handler job-definition) payload job-runner)
        (catch Throwable t
          (log/error t "Failed job" (:type job-definition))
          (throw t))))
    (merge proletarian-worker-options
           (:queue job-definition)
           {:proletarian/queue (:type job-definition)}))]
    (worker/start! worker)
    worker))

(defn- create-scheduler
  "Creates a new scheduler for running a recurring job"
  [ds job-definition job-runner]
  (let [schedule (:schedule job-definition)] (when schedule
    (log/info "Scheduling recurring job" (:type job-definition))
    (chime/chime-at
      (if (or (vector? schedule) (seq? schedule))
        schedule
        (cron-schedule schedule))
      (fn [scheduled-at]
        (jdbc/with-db-transaction
          [connection {:datasource ds}]
          (try
            (log/info "Running recurring job" (:type job-definition) (str scheduled-at))
            (yesql-add-scheduled-job-instance! {:job_type (:type job-definition)
                                                :scheduled_at (str scheduled-at)}
                                               {:connection connection})
            (start-job job-runner connection (:type job-definition) nil)
            (catch BatchUpdateException _
              (log/info "Job" (:type job-definition) "already scheduled at" (str scheduled-at)))
            (catch Throwable t
              (log/error t "Error scheduling job" (:type job-definition))))))))))

(defn- shutdown-job
  "Shuts down the worker and scheduler for a job"
  [job-type job-status]
  (log/info "Shutting down job:" job-type)
  (try
    (when-let [worker (:worker job-status)] (worker/stop! @worker))
    (catch Throwable t
      (log/error t "Error shutting down worker" job-type)))
  (try
    (when-let [scheduler (:scheduler job-status)] (.close scheduler))
    (catch Throwable t
      (log/error t "Error shutting down scheduler" job-type))))

(defn- store-job-definitions
  "Stores added job definitions to database. This is done at every start-up so that default configuration for new
   jobs get added without a migration"
  [ds job-definitions]
  (jdbc/with-db-transaction [connection {:datasource ds}]
                            (doseq [[job-type _] job-definitions]
                              (yesql-add-job-type! {:job_type (str job-type)
                                                    :enabled true}
                                                   {:connection connection}))))

(defn- reconcile-configuration [ds job-definitions job-runner]
  (locking job-runner                                       ; only one configuration update at a time, otherwise workers
                                                            ; and/or schedulers might escape
    (jdbc/with-db-transaction
      [connection {:datasource ds}]
      (let [job-type-statuses (:job-type-statuses job-runner)
            stored-configuration (into {} (for [job-type (yesql-get-job-types {} {:connection connection})]
                                            [(:job_type job-type) {:enabled (:enabled job-type)}]))
            merged-statuses (merge-with (fn [& args] (apply merge args)) job-definitions @job-type-statuses stored-configuration)
            new-statuses (into {} (for [[job-type job-definition] merged-statuses]
                                    [job-type
                                     (case (str (true? (and (:type job-definition) (:enabled job-definition))) ":" (true? (:running job-definition)))
                                       "true:false" (do (log/info "Starting job:" job-type)
                                                        (merge job-definition {:running true
                                                                               ; start worker in separate thread to minimize restart time in local dev
                                                                               :worker (future (create-worker ds job-definition job-runner))
                                                                               :scheduler (create-scheduler ds job-definition job-runner)}))
                                       "false:true" (do (shutdown-job job-type job-definition)
                                                        (merge job-definition {:running false
                                                                               :worker nil
                                                                               :scheduler nil}))
                                       job-definition)]))]
        (reset! job-type-statuses new-statuses)

        ; wait for all workers to start (for tests)
        (into [] (for [[_ v] @job-type-statuses] (when-let [worker (:worker v)] @worker)))))))

(defrecord PersistentJobRunner [job-definitions ds enable-running]
  component/Lifecycle
  (start [this]
    (log/info "Starting Job-Runner")
    (store-job-definitions ds job-definitions)
    (let [job-type-statuses (atom nil)  ; Current status for jobs. The configuration-monitor process will
          ; periodically compares this with current configuration and
          ; starts/stops workers/schedulers to match it
          job-runner (promise)
          monitor (when enable-running
                    (chime/chime-at
                      (chime/periodic-seq (Instant/now) (Duration/ofSeconds 15))
                      (fn [_]
                        (reconcile-configuration ds job-definitions @job-runner))))]
      (deliver job-runner (assoc this :monitor monitor :job-type-statuses job-type-statuses))
      @job-runner))

  (stop [this]
    (try
      (log/info "Shutting down Job-Runner")
      (doseq [[job-type job-status] @(:job-type-statuses this)] (shutdown-job job-type job-status))
      (when-let [monitor (:monitor this)] (.close monitor))
      (catch Exception e
        (log/error e "Error shutting down job JobRunner")))
    (assoc this :monitor nil :job-type-statuses nil))

  JobRunner
  (start-job [_ connection job-type payload]
    (log/info "Registering job" job-type)
    (job/enqueue!
      (:connection connection)
      job-type
      payload
      (merge proletarian-enqueue-options
             {:proletarian/queue job-type})))

  (get-job-types [_]
    (jdbc/with-db-transaction [connection {:datasource ds}]
                              (yesql-get-job-types {} {:connection connection})))

  (update-job-types [this job-types]
    (jdbc/with-db-transaction [connection {:datasource ds}]
                              (doseq [job-type job-types]
                                (yesql-update-job-type! job-type {:connection connection})))
    ; update configuration instantly (for tests)
    (reconcile-configuration ds job-definitions this)
    (get-job-types this))

  (get-queue-lengths [_]
    (jdbc/with-db-transaction [connection {:datasource ds}]
                              (yesql-get-queue-lengths {} {:connection connection})))

  (cleanup-archived-jobs [_ seconds]
    (let [timestamp (Instant/now)
          groups (group-by (fn [job-definition]
                             (or (:remove-older-than job-definition) seconds))
                           (for [[_ job-definition] job-definitions] job-definition))]
      (jdbc/with-db-transaction
        [connection {:datasource ds}]
        (doall (for [[seconds job-definitions] groups]
                 (let [job-types (map (fn [job-definition] (:type job-definition)) job-definitions)]
                   (log/info "Removing jobs older than" seconds " seconds, groups: " (join " " job-types))
                   (yesql-remove-archived-jobs-older-than!
                     {:timestamp (str (.minusSeconds timestamp seconds))
                      :queues job-types}
                     {:connection connection}))))))))

(defrecord FakeJobRunner []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)
  (get-job-types [_])
  (update-job-types [_ _])
  (get-queue-lengths [_])
  (cleanup-archived-jobs [_ _])

  JobRunner
  (start-job [_ _ _ _]))

(defn new-job-runner
  "Creates a new JobRunner with supplied job-definitions. If enable-running is false (ataru-hakija) jobs can be
   registered, but they are not run."
  [job-definitions & [ds enable-running]]
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeJobRunner)
    (->PersistentJobRunner job-definitions ds enable-running)))

(defn- report-handler [_ job-runner]
  (log/info "Reporting queue length metrics")
  (cloudwatch/store-metrics (:amazon-cloudwatch job-runner)
                            (map (fn [queue]
                                   {:name "queue-length"
                                    :value (:length queue)
                                    :dimensions [{:name "job-type"
                                                  :value (:job_type queue)}]}) (get-queue-lengths job-runner))))

(defn- shift-schedule
  "Shifts a cron schedule a given amount of seconds forwards or backwards"
  [schedule seconds]
  (map (fn [instant] (.plusSeconds instant seconds)) (cron-schedule schedule)))

(def report-job {:type "report-metrics"
                 :handler report-handler
                 :remove-older-than 120
                 :schedule (interleave (shift-schedule "*/1 * * * *" -30)
                                       (shift-schedule "*/1 * * * *" 0))})

(def cleanup-job {:type "cleanup-archived-jobs"
                 :handler (fn [_ job-runner] (cleanup-archived-jobs job-runner (* 60 60 24 30 2)))
                 :schedule "0 */1 * * *"})