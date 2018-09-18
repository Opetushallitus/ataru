(ns ataru.aws.sqs
  (:require [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component])
  (:import com.amazonaws.services.sqs.AmazonSQSClientBuilder
           [com.amazonaws.services.sqs.model
            ReceiveMessageRequest
            DeleteMessageBatchRequestEntry]))

(defrecord AmazonSQS [amazon-sqs credentials-provider]
  component/Lifecycle
  (start [this]
    (if (nil? amazon-sqs)
      (assoc this
             :amazon-sqs (-> (AmazonSQSClientBuilder/standard)
                             (.withRegion (:region (:aws config)))
                             (.withCredentials
                              (:credentials-provider credentials-provider))
                             .build))
      this))
  (stop [this]
    (assoc this :amazon-sqs nil)))

(defn batch-receive [{:keys [amazon-sqs]} queue-url wait-duration]
  (->> (-> (new ReceiveMessageRequest)
           (.withQueueUrl queue-url)
           (.withWaitTimeSeconds
            (.intValue (.getSeconds wait-duration))))
       (.receiveMessage amazon-sqs)
       .getMessages
       seq))

(defn batch-delete [{:keys [amazon-sqs]} queue-url messages]
  (when (seq messages)
    (when-let [failed (->> messages
                           (map-indexed
                            (fn [i message]
                              (new DeleteMessageBatchRequestEntry
                                   (str i)
                                   (.getReceiptHandle message))))
                           (.deleteMessageBatch amazon-sqs queue-url)
                           .getFailed
                           seq)]
      (throw (new RuntimeException (->> failed
                                        (map #(.getMessages %))
                                        (clojure.string/join "; ")))))))
