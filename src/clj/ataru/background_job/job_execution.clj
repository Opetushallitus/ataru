(ns ataru.background-job.job-execution
  (:require
   [taoensso.timbre :as log]
   [clj-time.core :as time]
   [clojure.core.match :refer [match]]
   [ataru.background-job.job-store :as job-store])
  (:import
   (java.util.concurrent Executors TimeUnit)))

(def job-exec-interval-seconds 10)

(defn determine-next-step [transition current-step]
  (match transition
    {:id :to-next :step next-step}
    next-step

    {:id (:or :retry :error-retry)}
    current-step

    {:id (:or :final :fail)}
    nil))

(defn next-activation-for-retry [retry-count]
  (time/plus (time/now) (time/minutes retry-count)))

(def max-retries 100)

(defn continue-running-steps? [transition-id]
  (match transition-id
    (:or :final :fail :retry :error-retry)
    false

    :to-next
    true))

(defn- final-error-iteration [step state retry-count msg]
  {:step step
   :state state
   :final true
   :retry-count retry-count
   :next-activation nil
   :transition :fail
   :error msg})

(defn- retry-error-iteration [step state retry-count msg]
  {:step step
   :state state
   :final false
   :retry-count retry-count
   :next-activation (next-activation-for-retry retry-count)
   :transition :error-retry
   :error msg})

(defn exec-step [iteration step-fn runner]
  (log/debug "Executing step:" (:step iteration))
  (try
    (let [state                (:state iteration)
          step                 (:step iteration)
          retry-count          (:retry-count iteration)
          step-result          (step-fn state runner)
          result-transition-id (-> step-result :transition :id)
          next-step            (determine-next-step (:transition step-result) step)
          next-is-retry        (= :retry result-transition-id)
          next-is-final        (contains? #{:final :fail} result-transition-id)]
      (log/debug "result:" step-result)
      {:step            (or next-step step) ;; current step if final iteration
       :transition      result-transition-id
       :final           next-is-final
       :retry-count     (if next-is-retry (inc retry-count) 0)
       :next-activation (cond
                          next-is-final
                          nil

                          next-is-retry
                          (next-activation-for-retry (inc retry-count))

                          :else
                          (time/now))
       :state           (or (:updated-state step-result) state)
       :error           nil})
    (catch Throwable t
      (let [msg (str "Error occurred while executing step " (:step iteration) ": ")]
        (log/error msg)
        (log/error t)
        (if (instance? Exception t) ;; Exceptions are retried, Errors cause job stop
          (retry-error-iteration (:step iteration) (:state iteration) (inc (:retry-count iteration)) (str msg t))
          (final-error-iteration (:step iteration) (:state iteration) (:retry-count iteration) (str msg t)))))))

(defn maybe-exec-step
  "Attempt to exec the next iteration's step if the function exists in job definition and
   if we haven't exceeded retry-limit"
  [runner iteration job-definition]
  (let [step-fn (get (:steps job-definition) (:step iteration))]
    (cond
      (nil? step-fn)
      (final-error-iteration (:step iteration)
                             (:state iteration)
                             0
                             (str "Could not find step "
                                  (:step iteration)
                                  " from job definition for "
                                  (:job-type job-definition)))
      (> (:retry-count iteration) max-retries)
      (final-error-iteration (:step iteration)
                             (:state iteration)
                             (inc (:retry-count iteration))
                             (str "Retry limit exceeded for step "
                                  (:step iteration)
                                  " in job "
                                  (:job-type job-definition)))

      :else
      (exec-step iteration step-fn runner))))

(defn exec-job-step [runner job]
  (let [job-definitions (:job-definitions runner)
        job-definition (get job-definitions (:job-type job))]
    (log/debug "Executing job" (:job-id job) (:job-type job))
    (if job-definition
      (maybe-exec-step runner (:iteration job) job-definition)
      (let [msg (str "Could not find job definition for " (:job-type job))]
        (log/error msg)
        [(final-error-iteration (-> job :iteration :step) msg)]))))

(defn get-job-step-and-exec [runner]
  (job-store/with-due-job
    (fn [due-job]
      (exec-job-step runner due-job))
    (keys (:job-definitions runner))))

(defn exec-job-steps-while-due
  "Exec job step while there are due jobs which should be run immediately.
   When there are no more due jobs, we can take a short break and continue
   when we poll the jobs again."
  [runner]
  (if (get-job-step-and-exec runner) (recur runner)))

(defn execute-due-job-steps [runner]
  (try
    (exec-job-steps-while-due runner)
    ;; We need to catch everything, executor will stop SILENTLY if we let this escalate
    (catch Throwable t
      (log/error "Error while executing background job:")
      (log/error t))))

(defn start [runner]
  (let [scheduled-executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleWithFixedDelay scheduled-executor #(execute-due-job-steps runner) 0 job-exec-interval-seconds TimeUnit/SECONDS)
    scheduled-executor))
