(ns ataru.background-job.job-spec
  (:require [speclj.core :refer :all]
            [clj-test-containers.core :as tc]
            [hikari-cp.core :refer [make-datasource]]
            [clojure.java.jdbc :as jdbc]
            [ataru.background-job.job :as job]
            [taoensso.timbre :as log])
  (:import (java.time Instant)
           (org.joda.time DateTime)))

(defn- should-eventually [f timeout]
  (let [timed-out (promise)]
    (future (Thread/sleep timeout) (deliver timed-out true))
    (while (not (f))
      (when (deref timed-out 250 false)
        (should false)))))

(describe "background job"
          (tags :unit :validator)
          (before-all
            (log/merge-config! {:ns-filter {:allow #{"*"} :deny #{"org.testcontainers.*"
                                                                  "com.github.dockerjava.*"
                                                                  "com.zaxxer.hikari.*"}}})
            (def container (-> (tc/create {:image-name    "postgres:15.4"
                                           :exposed-ports [5432]
                                           :env-vars      {"POSTGRES_DB" "test"
                                                           "POSTGRES_PASSWORD" "postgres"}})
                               (tc/start!)))
            (def ds (make-datasource {:database-name "test"
                                      :pool-name     "test"
                                      :username      "postgres"
                                      :password      "postgres"
                                      :server-name   "localhost"
                                      :port-number   (get (:mapped-ports container) 5432)
                                      :adapter       "postgresql"})))

          (before
            (jdbc/with-db-transaction
              [connection {:datasource ds}]
              (jdbc/execute! connection [(slurp "resources/db/migration/V20240305000000__add_proletarian_schema.sql")])))

          (after
            (jdbc/with-db-transaction
              [connection {:datasource ds}]
              (jdbc/execute! connection ["DROP TABLE proletarian_jobs;
                                          DROP TABLE proletarian_archived_jobs;
                                          DROP TABLE job_types;"])))

          (it "should run (nearly) immediately when queued"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ _] (deliver ready true))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil))

                  (should (deref ready 300 false))       ; job marked ready
                  (finally (.stop job-runner)))))

          (it "should not block current thread"
              (let [ready (promise)
                    can-run (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ _] @can-run (deliver ready true))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil))

                  (deliver can-run true)                    ; job can now run
                  (should (deref ready 300 false))          ; job gets run
                  (finally (.stop job-runner)))))

          (it "gets supplied payload"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [payload _] (deliver ready payload))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" "test payload"))

                  (should (= "test payload" (deref ready 300 false))) ; payload equals supplied
                  (finally (.stop job-runner)))))

          (it "payload can contain joda DateTime"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [payload _] (deliver ready payload))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (let [payload {:time (DateTime.)}]
                    (jdbc/with-db-transaction
                      [connection {:datasource ds}]
                      (job/start-job job-runner connection "queued" payload))

                    (should (= payload (deref ready 300 false)))) ; payload equals supplied
                  (finally (.stop job-runner)))))

          (it "payload can contain java.time.Instant"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [payload _] (deliver ready payload))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (let [payload {:time (Instant/ofEpochMilli (.getMillis (DateTime.)))}]
                    (jdbc/with-db-transaction
                      [connection {:datasource ds}]
                      (job/start-job job-runner connection "queued" payload))

                    (should (= payload (deref ready 300 false)))) ; payload equals supplied
                  (finally (.stop job-runner)))))

          (it "gets JobRunner as second parameter"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ job-runner] (deliver ready job-runner))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil))

                  (should (= job-runner (deref ready 300 false))) ; second param is job-runner
                  (finally (.stop job-runner)))))

          (it "should run job as many times as queued"
              (let [counter (atom 0)
                    ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ _]
                                                         ; we are ready when job run twice
                                                         (swap! counter inc)
                                                         (when (= @counter 2) (deliver ready true)))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    ; schedule job twice
                    (job/start-job job-runner connection "queued" nil)
                    (job/start-job job-runner connection "queued" nil))

                  (should (deref ready 300 false))       ; job run twice
                  (finally (.stop job-runner)))))

          (it "should support retries"
              (let [counter (atom 0)
                    ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ _]
                                                         ; we are ready when job run three times, otherwise fail job
                                                         (swap! counter inc)
                                                         (if (= @counter 3) (deliver ready true)
                                                                            (throw (Exception. "not ready"))))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100
                                                      ; configure two retries
                                                      :proletarian/retry-strategy-fn
                                                      (fn [_ _] {:retries 2
                                                                 :delays [10]})}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    ; schedule job only once
                    (job/start-job job-runner connection "queued" nil))

                  (should (deref ready 300 false))       ; job run once plus two retries
                  (finally (.stop job-runner)))))

          (it "job type should not preempt other job types"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"one-off" {:handler (fn [_ _] (deliver ready true))
                                              :type "one-off"
                                              :queue {:proletarian/polling-interval-ms 100}}
                                    "mass-job" {:handler (fn [_ _] (Thread/sleep 10))
                                              :type "mass-job"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (dotimes [_ 500] (job/start-job job-runner connection "mass-job" nil))
                    (job/start-job job-runner connection "one-off" nil))

                  (should (deref ready 300 false))       ; job run despite mass-job
                  (finally (.stop job-runner)))))

          (it "should not run when disabled"
              (let [ready (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   {"queued" {:handler (fn [_ _] (deliver ready true))
                                              :type "queued"
                                              :queue {:proletarian/polling-interval-ms 100}}}
                                   ds
                                   true))]
                (try
                  (job/update-job-types job-runner [{:job_type "queued" :enabled false}])
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil))
                  (should (not (deref ready 300 false)))    ; not done as disabled

                  (job/update-job-types job-runner [{:job_type "queued" :enabled true}])
                  (should (deref ready 300 false))          ; gets done when job enabled
                  (finally (.stop job-runner)))))

          (it "should run when scheduled"
              (let [scheduled-at (.plusMillis (Instant/now) 400)
                    polling-interval 25
                    tolerance-ms (* 2 polling-interval)
                    lower-bound (.toEpochMilli (.minusMillis scheduled-at tolerance-ms))
                    upper-bound (.toEpochMilli (.plusMillis scheduled-at tolerance-ms))
                    done-at (promise)
                    job-runner (.start
                                 (job/->PersistentJobRunner
                                   ; schedule job 500ms from now
                                   {"scheduled" {:handler (fn [_ _] (deliver done-at (.toEpochMilli (Instant/now))))
                                                 :type "scheduled"
                                                 :schedule [scheduled-at]
                                                 :queue {:proletarian/polling-interval-ms polling-interval}}}
                                   ds
                                   true))]
                (try
                  ; done at approx. scheduled time
                  (should (< lower-bound (deref done-at 600 false) upper-bound))
                  (finally (.stop job-runner)))))

          (it "should only run once per scheduled time"
              (let [counter (atom 0)
                    ready [(promise) (promise)]
                    ; schedule the same job to be run by two runners at exactly the same time
                    time (.plusMillis (Instant/now) 300)
                    job-definitions {"scheduled" {:handler (fn [_ _] (swap! counter (fn [v]
                                                                              (deliver (nth ready v) true)
                                                                              (+ v 1))))
                                                  :type "scheduled"
                                                  :schedule [time]
                                                  :queue {:proletarian/polling-interval-ms 100}}}
                    job-runner1 (.start (job/->PersistentJobRunner job-definitions ds true))
                    job-runner2 (.start (job/->PersistentJobRunner job-definitions ds true))]

                (try (should (deref (nth ready 0) 500 false))            ; run once
                     (should (not (deref (nth ready 1) 500 false)))      ; but not twice
                     (finally (.stop job-runner1)
                              (.stop job-runner2)))))

          (it "should not run when scheduled but disabled"
              (let [ready (promise)
                    job-runner (.start (job/->PersistentJobRunner
                                 {"scheduled" {:handler (fn [_ _] (deliver ready true))
                                               :type "scheduled"
                                               :schedule [(.plusMillis (Instant/now) 500) (.plusMillis (Instant/now) 1000)]
                                               :queue {:proletarian/polling-interval-ms 100}}}
                                 ds
                                 true))]
                (try
                  (job/update-job-types job-runner [{:job_type "scheduled" :enabled false}])
                  (should (not (deref ready 300 false)))      ; not done as not scheduled to be done yet
                  (should (not (deref ready 500 false)))      ; not done as disabled

                  (job/update-job-types job-runner [{:job_type "scheduled" :enabled true}])
                  (should (deref ready 500 false))            ; done as enabled and scheduled to be done
                  (finally (.stop job-runner)))))

          (it "lists job types"
              (let [job-runner (.start (job/->PersistentJobRunner
                                         {"queued" {:handler (fn [_ _])
                                                    :type "queued"}}
                                         ds
                                         true))]
                (try
                  (should (= (job/get-job-types job-runner) '({:job_type "queued"
                                                               :enabled true})))
                  (finally (.stop job-runner)))))

          (it "lists queue lengths"
              (let [job-runner (.start (job/->PersistentJobRunner
                                         {"queued" {:handler (fn [_ _])
                                                    :type "queued"}}
                                         ds
                                         true))]
                (try
                  (job/update-job-types job-runner [{:job_type "scheduled" :enabled false}])
                  (should (= (job/get-queue-lengths job-runner) '({:job_type "queued"
                                                                   :length 0})))
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil)
                    (job/start-job job-runner connection "queued" nil)
                    (job/start-job job-runner connection "queued" nil))
                  (should (= (job/get-queue-lengths job-runner) '({:job_type "queued"
                                                                   :length 3})))
                  (finally (.stop job-runner)))))

          (it "removes old archived jobs"
              (let [ready (promise)
                    job-runner (.start (job/->PersistentJobRunner
                                         {"queued" {:handler (fn [_ _] (deliver ready true))
                                                    :type "queued"
                                                    :queue {:proletarian/polling-interval-ms 100}}}
                                         ds
                                         true))
                    archived-jobs-count #(:count
                                           (nth (jdbc/with-db-transaction
                                                  [connection {:datasource ds}]
                                                  (jdbc/query
                                                    connection
                                                    "SELECT count(*) AS count FROM proletarian_archived_jobs"))
                                                0))]
                (try
                  (jdbc/with-db-transaction
                    [connection {:datasource ds}]
                    (job/start-job job-runner connection "queued" nil))

                  @ready
                  ; run jobs end up in archived jobs table
                  (should-eventually #(= 1 (archived-jobs-count)) 2000)

                  (job/cleanup-archived-jobs job-runner 0)
                  ; which gets cleaned
                  (should (= 0 (archived-jobs-count)))

                  (finally (.stop job-runner)))))

          (after-all
            (tc/stop! container)))

(run-specs)