(ns ataru.background-job.job-store)

(def in-memory-store (atom {}))
(def id-seq (atom 0))

(defn store-new [job-type state]
  (swap! in-memory-store assoc (swap! id-seq inc) {job-type state}))
