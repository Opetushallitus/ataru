(ns ataru.background-job.job-spec
  (:require [speclj.core :refer :all]
            [clj-test-containers.core :as tc]
            [hikari-cp.core :refer [make-datasource]]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [ataru.background-job.job :as job])
  (:import java.time.Instant))

(describe "background job"
          (tags :unit :validator)
          (before-all
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
                                      :adapter       "postgresql"}))
            (jdbc/with-db-transaction
              [connection {:datasource ds}]
              (log/info (jdbc/execute! connection [(slurp "resources/db/migration/V20240305000000__add_proletarian_schema.sql")]))
              (log/info (jdbc/query connection ["SELECT * FROM proletarian_jobs;"]))))

          (it "should run immediately when queued"
              (let [result (atom 0)
                    job-runner (job/->PersistentJobRunner
                                 {"queued" {:handler (fn [_ _] (swap! result inc))
                                            :type "queued"
                                            :queue {:proletarian/polling-interval-ms 100}}}
                                 ds
                                 true)]
                (.start job-runner)
                (jdbc/with-db-transaction
                  [connection {:datasource ds}]
                  (job/start-job job-runner connection "queued" nil)
                  (job/start-job job-runner connection "queued" nil))
                (Thread/sleep 300)
                (.stop job-runner)
                (should (= @result 2))))

          (it "should run when scheduled"
              (let [result (atom 0)
                    job-runner (job/->PersistentJobRunner
                                 {"scheduled" {:handler (fn [_ _] (swap! result inc))
                                               :type "scheduled"
                                               :schedule [(Instant/now) (.plusMillis (Instant/now) 100)]
                                               :queue {:proletarian/polling-interval-ms 100}}}
                                 ds
                                 true)]
                (.start job-runner)
                (Thread/sleep 300)
                (.stop job-runner)
                (should (= @result 2))))

          (after-all
            (tc/stop! container)))

(run-specs)