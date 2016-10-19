(ns ataru.background-job.job
  "Public API of the Background Job system"
  (:require
   [oph.soresu.common.config :refer [config]]
   [taoensso.timbre :as log]
   [com.stuartsierra.component :as component]
   [ataru.background-job.job-execution :as execution]
   [ataru.background-job.job-store :as job-store]))

(defn start-job
  "Start a new background job of type <job-type>.
   initial-state is the initial data map needed to start the job (can be anything)"
  [system-job-definitions job-type initial-state]
  (if-let [job-definition (get system-job-definitions job-type)]
    (job-store/store-new job-type initial-state)
    (log/error (str "No job definition found for job " job-type))))

(defrecord JobRunner [job-definitions]
  component/Lifecycle
  (start [this]
    (let [this-with-jobs (assoc this :job-definitions job-definitions)]
      (log/info "Starting background job runner")
      (if-not job-definitions (throw (Exception. "No job definintions given for JobRunner")))
      (assoc this-with-jobs :executor (execution/start this-with-jobs))))
  (stop [this]
    (log/info "Stopping background job runner")
    (-> this :executor (.shutdown))
    this))

(defrecord FakeJobRunner []
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defn new-job-runner [job-definitions]
  (if (-> config :dev :fake-dependencies) ;; Ui automated test mode
    (->FakeJobRunner)
    (->JobRunner job-definitions)))
