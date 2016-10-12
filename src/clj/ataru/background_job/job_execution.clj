(ns ataru.background-job.job-execution
  (:require
   [taoensso.timbre :as log]
   [schema.core :as s]
   [clj-time.core :as time]
   [clojure.core.match :refer [match]]
   [ataru.background-job.job-store :as job-store]
   [oph.soresu.common.config :refer [config]])
  (:import
   (java.util.concurrent Executors TimeUnit)))

(def max-retries 100)

;; Iterations resulting in running or attempting to run steps
;; are so central, that we've specified them with schema
;; and verify them in exec-job-step
(s/defschema ResultIteration
  {:step                         s/Keyword
   :state                        {s/Any s/Any}
   :final                        s/Bool
   :retry-count                  s/Int
   :next-activation              s/Any
   :transition                   s/Keyword
   :error                        (s/maybe s/Str)})

;; Less stuff here, but the ID is needed because this has to be updated
(s/defschema StoredIteration
  {:step s/Keyword
   :state {s/Any s/Any}
   :retry-count s/Int
   s/Any s/Any})

(s/defschema Runner {:job-definitions {s/Str {:steps {s/Keyword s/Any}
                                              :type  s/Str}}
                     s/Any s/Any})

(s/defschema JobWithStoredIteration {:job-type s/Str :iteration StoredIteration :job-id s/Int})

(defn- determine-next-step [transition current-step]
  (match transition
    {:id :to-next :step next-step}
    next-step

    {:id (:or :retry :error-retry)}
    current-step

    {:id (:or :final :fail)}
    nil))

(defn- next-activation-for-retry [retry-count]
  (time/plus (time/now) (time/minutes retry-count)))

(defn- next-activation [next-is-retry next-is-final retry-count]
  (cond
    next-is-final
    nil

    next-is-retry
    (next-activation-for-retry (inc retry-count))

    :else
    (time/now)))

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

(defn- handle-error [iteration throwable]
  (let [msg (str "Error occurred while executing step " (:step iteration) ": ")]
    (log/error msg)
    (log/error throwable)
    (if (instance? Exception throwable) ;; Exceptions are retried, Errors cause job stop
      (retry-error-iteration (:step iteration) (:state iteration) (inc (:retry-count iteration)) (str msg throwable))
      (final-error-iteration (:step iteration) (:state iteration) (:retry-count iteration) (str msg throwable)))))

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
       :next-activation (next-activation next-is-retry next-is-final retry-count)
       :state           (or (:updated-state step-result) state)
       :error           nil})
    (catch Throwable t
      (handle-error iteration t))))

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
      (>= (:retry-count iteration) max-retries)
      (final-error-iteration (:step iteration)
                             (:state iteration)
                             (inc (:retry-count iteration))
                             (str "Retry limit exceeded for step "
                                  (:step iteration)
                                  " in job "
                                  (:type job-definition)))

      :else
      (exec-step iteration step-fn runner))))

(s/defn ^:always-validate exec-job-step :- ResultIteration
  [runner :- Runner
   job :- JobWithStoredIteration]
  (let [job-definitions (:job-definitions runner)
        job-definition (get job-definitions (:job-type job))]
    (log/info "Executing job" (:job-id job) (:job-type job))
    (if job-definition
      (maybe-exec-step runner (:iteration job) job-definition)
      (let [msg (str "Could not find job definition for " (:job-type job))]
        (log/error msg)
        (final-error-iteration (-> job :iteration :step) {} 0 msg)))))

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
      (log/error
       ;; ExceptionInfo instances did not behave well (nothing was logged)
       ;; with (log/error t)
       ;; with this (log/error t "message") a new Throwable was thrown causing
       ;; Executor to stop :(
       ;; So we had to resort to (str t) which at least prints lots of info
       (str "Error while executing background job: " t)))))

(defn job-exec-interval-seconds
  "Function instead of def so we can override this in tests"
  [] (or (-> config :background-job :exec-interval-seconds) 15))

(defn start [runner]
  (let [scheduled-executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleWithFixedDelay scheduled-executor #(execute-due-job-steps runner) 0 (job-exec-interval-seconds) TimeUnit/SECONDS)
    scheduled-executor))
