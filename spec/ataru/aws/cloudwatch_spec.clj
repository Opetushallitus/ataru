(ns ataru.aws.cloudwatch-spec
  (:require [speclj.core :refer :all]
            [clj-test-containers.core :as tc]
            [ataru.aws.cloudwatch :as cloudwatch]
            [taoensso.timbre :as log])
  (:import [com.amazonaws.services.cloudwatch.model GetMetricDataRequest MetricDataQuery MetricStat Metric Dimension StandardUnit]
           java.util.Date
           java.time.Instant))

(defn- get-metric [cloudwatch-client metric]
  (let [request (-> (GetMetricDataRequest.)
                    (.withStartTime (Date/from (:start metric)))
                    (.withEndTime (Date/from (:end metric)))
                    (.withMetricDataQueries
                      [(-> (MetricDataQuery.)
                           (.withId "test-query")
                           (.withMetricStat
                             (-> (MetricStat.)
                                 (.withMetric
                                   (-> (Metric.)
                                       (.withNamespace (:namespace metric))
                                       (.withMetricName (:name metric))
                                       (.withDimensions
                                         (map (fn [dimension]
                                                (-> (Dimension.)
                                                    (.withName (:name dimension))
                                                    (.withValue (:value dimension))))
                                              (:dimensions metric)))))
                                 (.withUnit (StandardUnit/Count))
                                 (.withPeriod (int (:period metric)))
                                 (.withStat (:stat metric)))))]))
        response (.getMetricData cloudwatch-client request)]
    (-> response
        (.getMetricDataResults)
        (nth 0)
        (.getValues))))

(defn- should-eventually [f timeout]
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