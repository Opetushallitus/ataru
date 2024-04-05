(ns ataru.aws.cloudwatch
  (:require [ataru.config.core :refer [config]]
            [clojure.string]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
           [com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth AWSStaticCredentialsProvider]
           [com.amazonaws.auth BasicAWSCredentials]
           java.util.Date
           [com.amazonaws.services.cloudwatch.model Dimension MetricDatum PutMetricDataRequest StandardUnit]))

(defprotocol MetricStore
  (store-metrics [this datums]
    "Stores metrics"))

(defrecord AmazonCloudwatch [credentials-provider namespace port]
  component/Lifecycle
  (start [this]
    (assoc this :client
                (if (:dev? env)
                  (-> (AmazonCloudWatchClientBuilder/standard)
                      (.withEndpointConfiguration (AwsClientBuilder$EndpointConfiguration. (str "http://localhost:" (or port "4566")) "us-east-1"))
                      (.withCredentials (AWSStaticCredentialsProvider. (BasicAWSCredentials. "localstack" "localstack")))
                      (.build))
                  (-> (AmazonCloudWatchClientBuilder/standard)
                      (.withRegion (:region (:aws config)))
                      (.withCredentials
                        (:credentials-provider credentials-provider))
                      (.build)))
                :namespace namespace))
  (stop [this]
    (assoc this :client nil :namespace nil))

  MetricStore
  (store-metrics [this datums]
    (let [client (:client this)
          date (Date.)
          datums (map (fn [datum]
                        (-> (MetricDatum.)
                            (.withMetricName (:name datum))
                            (.withValue (double (:value datum)))
                            (.withDimensions (map (fn [dimension]
                                                    (-> (Dimension.)
                                                        (.withName (:name dimension))
                                                        (.withValue (:value dimension))))
                                                  (:dimensions datum)))
                            (.withStorageResolution (int 1))
                            (.withTimestamp date)
                            (.withUnit (StandardUnit/Count)))) datums)
          request (-> (PutMetricDataRequest.)
                      (.withNamespace (str namespace))
                      (.withMetricData datums))]
    (.putMetricData client request))))