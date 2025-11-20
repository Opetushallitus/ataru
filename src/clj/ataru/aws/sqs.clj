(ns ataru.aws.sqs
  (:require [ataru.config.core :refer [config]]
            [clojure.string]
            [com.stuartsierra.component :as component])
  (:import java.util.Collection
           software.amazon.awssdk.services.sqs.SqsClient
           software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
           software.amazon.awssdk.regions.Region
           [software.amazon.awssdk.services.sqs.model
            DeleteMessageBatchRequest
            Message ReceiveMessageRequest
            SendMessageRequest
            DeleteMessageBatchRequestEntry]))

(defrecord AmazonSQS [amazon-sqs credentials-provider]
  component/Lifecycle
  (start [this]
    (if (nil? amazon-sqs)
      (assoc this
             :amazon-sqs
             (-> (SqsClient/builder)
                 (.region (Region/of (get-in config [:aws :region] "eu-west-1")))
                 (.credentialsProvider ^AwsCredentialsProvider (:credentials-provider credentials-provider))
                 (.build)))
      this))
  (stop [this]
    (assoc this :amazon-sqs nil)))

(defn batch-receive [{:keys [amazon-sqs]} queue-url wait-duration]
  (let [^ReceiveMessageRequest request (-> (ReceiveMessageRequest/builder)
                                           (.queueUrl queue-url)
                                           (.waitTimeSeconds
                                             (.intValue (.getSeconds wait-duration)))
                                           (.build))]
    (->>
      (.receiveMessage ^SqsClient amazon-sqs request)
      .messages
      seq)))

(defn- batch-delete-request ^DeleteMessageBatchRequest [queue-url messages]
  (let [^Collection entries (map-indexed
                              (fn [i ^Message message]
                                (-> (DeleteMessageBatchRequestEntry/builder)
                                    (.id (str i))
                                    (.receiptHandle (.receiptHandle message))
                                    (.build))) messages)]
    (-> (DeleteMessageBatchRequest/builder)
        (.queueUrl queue-url)
        (.entries entries)
        (.build))))

(defn batch-delete [{:keys [amazon-sqs]} queue-url messages]
  (when (seq messages)
    (when-let [failed (->> (.deleteMessageBatch ^SqsClient amazon-sqs (batch-delete-request queue-url messages))
                           .failed
                           seq)]
      (throw
        (new RuntimeException
          (->> failed
               (map #(.getMessage %))
               (clojure.string/join "; ")))))))

(defn send-message [{:keys [amazon-sqs]} queue-url message-body]
  {:pre [(some? amazon-sqs)
         (some? queue-url)
         (some? message-body)]}
  (let [^SendMessageRequest request (-> (SendMessageRequest/builder)
                                        (.queueUrl queue-url)
                                        (.messageBody (str message-body))
                                        (.build))]
    (.sendMessage ^SqsClient amazon-sqs request)))
