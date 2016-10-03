(ns ataru.background-job.job
  "Public API of the Background Job system"
  (:require
   [taoensso.timbre :as log]
   [com.stuartsierra.component :as component]
   [ataru.background-job.job-execution :as execution]
   [ataru.background-job.job-store :as job-store]))

;; These "step-transitions" are common to all jobs.
;; The go to final steps or remain in current
(def common-transitions [:initial :final :fail :retry :to-next])

(defn start-job
  "Start a new background job of type <job-type>.
   initial-state is the initial data map needed to start the job (can be anything)"
  [system-job-definitions job-type initial-state]
  (let [job-definition (get system-job-definitions job-type)]
    (if job-definition
      (job-store/store-new job-type initial-state)
      (log/error (str "No job definition found for job " job-type)))))

(defrecord JobRunner []
  component/Lifecycle
  (start [this]
    (assoc this :executor (execution/start this)))
  (stop [this]
    (-> this :executor (.shutdown))
    this))
