(ns ataru.background-job.job-execution
  (:require [taoensso.timbre :as log])
  (:import (java.util.concurrent Executors TimeUnit)))

(def job-exec-interval-seconds 3)

(defn execute-due-jobs [runner]
  (try
    (println "Executing due jobs")
    (catch Exception e ;; We need to catch everything, executor will stop if we let this escalate
      (log/error "Error while executing background job:")
      (log/error e))))

(defn start [runner]
  (let [scheduled-executor (Executors/newSingleThreadScheduledExecutor)]
    (.scheduleWithFixedDelay scheduled-executor #(execute-due-jobs runner) 0 job-exec-interval-seconds TimeUnit/SECONDS)
    scheduled-executor))
