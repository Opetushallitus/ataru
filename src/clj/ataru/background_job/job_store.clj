(ns ataru.background-job.job-store
  (:require
   [yesql.core :refer [defqueries]]
   [taoensso.timbre :as log]
   [camel-snake-kebab.extras :refer [transform-keys]]
   [camel-snake-kebab.core :refer [->snake_case ->kebab-case-keyword]]
   [clj-time.core :as time]
   [clojure.java.jdbc :as jdbc]
   [oph.soresu.common.db :as db]))

(defqueries "sql/background-job-queries.sql")

(defn store-new [job-type state]
  (jdbc/with-db-transaction [data-source {:datasource (db/get-datasource :db)}]
    (let [connection {:connection data-source}
          new-job-id (:id (yesql-add-background-job<! {:job_type job-type} connection))]
      (yesql-add-job-iteration<! {:job_id new-job-id
                                  :step "initial"
                                  :final false
                                  :transition "start"
                                  :state state
                                  :next_activation (time/now)
                                  :retry_count 0
                                  :caused_by_error nil}
                                  connection)
      new-job-id)))

(defn job-iteration->db-format [job-iteration job-id]
  (assoc (transform-keys ->snake_case job-iteration)
         :step (-> job-iteration :step name)
         :transition (-> job-iteration :transition name)
         :job_id job-id))

(defn store-job-result [connection job result-iteration]
  (yesql-update-previous-iteration! {:id (-> job :iteration :iteration-id)} connection)
  (let [result-iteration-db-format (job-iteration->db-format result-iteration (:job-id job))]
    (yesql-add-job-iteration<! result-iteration-db-format connection)
    (log/debug "Stored result iteration for job"
               (:job-id job)
               (:job-type job)
               "transition:"
               (:transition result-iteration)
               "result step:"
               (:step result-iteration))))

(defn- job->job-with-iteration [job]
  {:job-id (:job-id job)
   :job-type (:job-type job)
   :iteration {:state (:state job)
               :step (keyword (:step job))
               :iteration-id (:iteration-id job)
               :retry-count (:retry-count job)}})

(defn- raw-job->job [raw-job]
  (->> raw-job
       (transform-keys ->kebab-case-keyword)
       job->job-with-iteration))

(defn with-due-job
  "Execute due job in transaction. Returns a boolean if there was a job to execute, false if none was
   found at this time."
  [exec-job-fn job-types]
  (jdbc/with-db-transaction [data-source {:datasource (db/get-datasource :db)}]
    (let [connection {:connection data-source}
          raw-job    (first (yesql-select-job-for-execution {:job_types job-types} connection))]
      (when raw-job
        (let [job               (raw-job->job raw-job)
              result-iteration  (exec-job-fn job)]
          (store-job-result connection job result-iteration)))
      ;; When there are no more jobs right now to execute, the caller can decide to
      ;; stop execution for a short period
      (boolean raw-job))))
