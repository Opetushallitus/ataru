(ns ataru.background-job.job-execution
  (:require
   [taoensso.timbre :as log]
   [clojure.core.match :refer [match]]
   [ataru.background-job.job-store :as job-store])
  (:import
   (java.util.concurrent Executors TimeUnit)))

(def job-exec-interval-seconds 10)

(defn determine-status [transition])

(defn determine-next-step [transition current-step]
  (match transition
    {:id :to-next :step next-step}
    next-step

    {:id :retry}
    current-step

    {:id (:or :final :fail)}
    nil))

(defn determine-status [transition]
  (match transition
    {:id (:or :final :fail)}
    :stopped

    :else
    :running))

(defn store-job [job job-definition step-result]
  (job-store/store (-> job
                       (assoc :state (or (:update-state step-result)
                                         (:state job)))
                       (assoc :next-step (determine-next-step (:transition step-result) (:next-step job)))
                       (assoc :status (determine-status (:transition step-result))))))

(defn exec-step [step state runner]
  (step state runner))

(defn exec-steps
  "For now, only execs the next step.
   Will execute nonfinal steps until some final condition at once."
  [runner job job-definition]
  (let [step-to-exec (get (:steps job-definition) (:next-step job))]
    (if-not step-to-exec
      (throw (Exception. (str "Could not find step " (:next-step job) " from job definition for " (:job-type job)))))
    (exec-step step-to-exec (:state job) runner)))

(defn exec-job [runner job]
  (let [job-definitions (:job-definitions runner)
        job-definition (get job-definitions (:job-type job))]
    (if job-definition
      (let [step-result (exec-steps runner job job-definition)]
        (store-job job job-definition step-result)
        ;;store job with the transition we coulnd't execute immediately
        )
      (log/error "Could not find job definition for " (:job-type job)))))

(defn get-jobs-and-exec [runner]
  (let [due-jobs        (job-store/get-due-jobs)]
    (mapv (partial exec-job runner) due-jobs)))

(defn execute-due-jobs [runner]
  (try
    (log/debug "Executing due jobs")
    (get-jobs-and-exec runner)
    (catch Exception e ;; We need to catch everything, executor will stop if we let this escalate
      (log/error "Error while executing background jobs:")
      (log/error e))))

(defn start [runner]
  (let [scheduled-executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleWithFixedDelay scheduled-executor #(execute-due-jobs runner) 0 job-exec-interval-seconds TimeUnit/SECONDS)
    scheduled-executor))
