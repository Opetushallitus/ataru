(ns ataru.aws.cloudwatch-spec
  (:require [ataru.aws.cloudwatch :as cloudwatch]
            [clj-test-containers.core :as tc]
            [speclj.core :refer :all]
            [taoensso.timbre :as log])
  (:import java.time.Instant
           java.util.Collection
           software.amazon.awssdk.services.cloudwatch.CloudWatchClient
           (software.amazon.awssdk.services.cloudwatch.model Dimension GetMetricDataRequest GetMetricDataResponse Metric MetricDataQuery MetricStat StandardUnit)))

(defn- get-metric [^CloudWatchClient cloudwatch-client metric]
  (let [^GetMetricDataRequest request (-> (GetMetricDataRequest/builder)
                    (.startTime (:start metric))
                    (.endTime (:end metric))
                    (.metricDataQueries
                      [(-> (MetricDataQuery/builder)
                           (.id "test-query")
                           (.metricStat ^MetricStat
                             (-> (MetricStat/builder)
                                 (.metric ^Metric
                                   (-> (Metric/builder)
                                       (.namespace (:namespace metric))
                                       (.metricName (:name metric))
                                       (.dimensions ^Collection
                                         (map (fn [dimension]
                                                (-> (Dimension/builder)
                                                    (.name (:name dimension))
                                                    (.value (:value dimension))
                                                    (.build)))
                                              (:dimensions metric)))
                                       (.build)))
                                 (.unit StandardUnit/COUNT)
                                 (.period (int (:period metric)))
                                 (.stat (:stat metric))
                                 (.build)))
                           (.build))])
                    (.build))
        ^GetMetricDataResponse response (.getMetricData cloudwatch-client request)]
    (-> response
        (.metricDataResults)
        (nth 0)
        (.values))))

(defn- should-eventually [f ^long timeout]
  (let [timed-out (promise)]
    (future (Thread/sleep timeout) (deliver timed-out true))
    (while (not (f))
      (when (deref timed-out 250 false)
        (should false)))))

(describe "cloudwatch"
          (tags :unit :validator)
          (before-all
            (log/merge-config! {:ns-filter {:allow #{"*"} :deny #{"org.testcontainers.*"
                                                                  "com.github.dockerjava.*"}}})
            (def container (-> (tc/create {:image-name    "localstack/localstack:3.0.1"
                                           :exposed-ports [4566]})
                               (tc/start!))))

          (it "should do store metrics"
              (let [namespace "test-ataru"
                    cloudwatch (.start (cloudwatch/map->AmazonCloudwatch {:namespace namespace :port (get (:mapped-ports container) 4566)}))
                    metric {:name "test-metric"
                            :dimensions [{:name "job-type"
                                          :value "test-job"}]}]
                (try
                  (cloudwatch/store-metrics cloudwatch [(assoc metric :value 1)])
                  (cloudwatch/store-metrics cloudwatch [(assoc metric :value 2)])

                  (should-eventually
                    #(= 3.0 (reduce + (get-metric
                                        (:client cloudwatch)
                                        {:namespace namespace
                                         :name "test-metric"
                                         :start (.minusSeconds (Instant/now) 60)
                                         :end (Instant/now)
                                         :period 60
                                         :stat "Sum"
                                         :dimensions [{:name "job-type"
                                                       :value "test-job"}]}))) 5000)
                  (finally (.stop cloudwatch)))))

          (after-all
            (tc/stop! container)))

(run-specs)
