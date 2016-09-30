(ns ataru.background-job.job
  (:require
   [ataru.background-job.job-store :as job-store]
   [taoensso.timbre :as log]))

;; These "step-transitions" are common to all jobs.
;; The go to final steps or remain in current
(def common-transitions [:initial :final :fail :retry :to-next])

(defn start-job
  "Start a new background job of type <job-type>.
   initial-state is the initial data map needed to start the job (can be anything)"
  [system-job-definitions job-type initial-state]
  (let [job-definition (get system-job-definitions job-type)]
    (if-not job-definition
      (log/error (str "No job definition found for job " job-type))
      (job-store/store-new job-type initial-state))))
