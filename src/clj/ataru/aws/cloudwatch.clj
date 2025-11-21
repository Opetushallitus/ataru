(ns ataru.aws.cloudwatch
  (:require [ataru.config.core :refer [config]]
            [clojure.string]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import java.net.URI
           (java.util Collection Date)
           (software.amazon.awssdk.auth.credentials StaticCredentialsProvider AwsBasicCredentials AwsCredentialsProvider)
           software.amazon.awssdk.regions.Region
           software.amazon.awssdk.services.cloudwatch.CloudWatchClient
           (software.amazon.awssdk.services.cloudwatch.model Dimension MetricDatum PutMetricDataRequest StandardUnit)))

(defprotocol MetricStore
  (store-metrics [this datums]
    "Stores metrics"))

(defrecord AmazonCloudwatch [credentials-provider namespace port]
  component/Lifecycle
  (start [this]
    (assoc this :client
                (if (:dev? env)
                  (-> (CloudWatchClient/builder)
                      (.region Region/US_EAST_1)
                      (.credentialsProvider (StaticCredentialsProvider/create (AwsBasicCredentials/create "localstack" "localstack")))
                      (.endpointOverride (URI/create (str "http://localhost:" (or port "4566"))))
                      (.build))
                  (-> (CloudWatchClient/builder)
                      (.region (Region/of (:region (:aws config))))
                      (.credentialsProvider ^AwsCredentialsProvider (:credentials-provider credentials-provider))
                      (.build)))
                :namespace namespace))
  (stop [this]
    (assoc this :client nil :namespace nil))

  MetricStore
  (store-metrics [this datums]
    (let [client (:client this)
          date (Date.)
          datums (map (fn [datum]
                        (-> (MetricDatum/builder)
                            (.metricName (:name datum))
                            (.value (double (:value datum)))
                            (.dimensions ^Collection (map (fn ^Dimension [dimension]
                                                    (-> (Dimension/builder)
                                                        (.name (:name dimension))
                                                        (.value (:value dimension))
                                                        (.build)))
                                                          (:dimensions datum)))
                            (.storageResolution (int 1))
                            (.timestamp (.toInstant date))
                            (.unit StandardUnit/COUNT)
                            (.build))) datums)
          request (-> (PutMetricDataRequest/builder)
                      (.namespace (str namespace))
                      (.metricData ^Collection datums)
                      (.build))]
    (.putMetricData client request))))
