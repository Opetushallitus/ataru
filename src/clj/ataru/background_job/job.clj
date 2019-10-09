(ns ataru.background-job.job
  "Public API of the Background Job system"
  (:require
   [ataru.config.core :refer [config]]
   [taoensso.timbre :as log]
   [com.stuartsierra.component :as component]
   [ataru.background-job.job-execution :as execution]
   [ataru.background-job.job-store :as job-store]))

(defn status [period]
  (let [status (job-store/get-status period)]
    (if (and
         (= 1 (get-in status ["start-automatic-eligibility-if-ylioppilas-job-job"
                              :total]))
         (zero? (get-in status ["start-automatic-eligibility-if-ylioppilas-job-job"
                                :fail]))
         (every? (fn [[job status]]
                   (if (= job "automatic-eligibility-if-ylioppilas-job")
                     (> 10 (:error status))
                     (= 0 (:error status))))
                 status))
      (assoc status :ok true)
      (assoc status :ok false))))

(defprotocol JobRunner
  (start-job [this connection job-type initial-state]
    "Start a new background job of type <job-type>.
     initial-state is the initial data map needed to start the job
     (can be anything)"))

(defrecord PersistentJobRunner [job-definitions]
  component/Lifecycle
  (start [this]
    (let [this-with-jobs (assoc this :job-definitions job-definitions)]
      (log/info "Starting background job runner")
      (if-not job-definitions (throw (Exception. "No job definintions given for JobRunner")))
      (assoc this-with-jobs :executor (execution/start this-with-jobs))))
  (stop [this]
    (log/info "Stopping background job runner")
    (-> this :executor (.shutdown))
    this)

  JobRunner
  (start-job [_ connection job-type initial-state]
    (if-let [job-definition (get job-definitions job-type)]
      (job-store/store-new connection job-type initial-state)
      (throw (new RuntimeException (str "No job definition found for job "
                                        job-type))))))

(defrecord FakeJobRunner []
  component/Lifecycle
  (start [this] this)
  (stop [this] this)

  JobRunner
  (start-job [_ _ _ _]))

(defn new-job-runner [job-definitions]
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeJobRunner)
    (->PersistentJobRunner job-definitions)))
