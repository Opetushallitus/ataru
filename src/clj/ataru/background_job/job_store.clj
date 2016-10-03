(ns ataru.background-job.job-store)

(def in-memory-store (atom {}))
(def id-seq (atom 0))

(defn store-new [job-type state]
  (let [new-job-id (swap! id-seq inc)]
    (swap! in-memory-store assoc new-job-id {:id new-job-id :job-type job-type :state state :next-step :initial :status :running})))

(defn store [job]
  (swap! in-memory-store assoc (:id job) job))

(defn get-due-jobs []
  (filter #(= :running (:status %)) (vals @in-memory-store)))
