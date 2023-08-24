(ns ataru.aws.sqs
  (:require [ataru.config.core :refer [config]]
            [clojure.string]
            [com.stuartsierra.component :as component])
  (:import com.amazonaws.services.sqs.AmazonSQSClientBuilder
           [com.amazonaws.services.sqs.model
            ReceiveMessageRequest
            SendMessageRequest
            DeleteMessageBatchRequestEntry]))

(defrecord AmazonSQS [amazon-sqs credentials-provider]
  component/Lifecycle
  (start [this]
    (if (nil? amazon-sqs)
      (assoc this
             :amazon-sqs
             (-> (AmazonSQSClientBuilder/standard)
                 (.withRegion (:region (:aws config)))
                 (.withCredentials
                   (:credentials-provider credentials-provider))
                 .build))
      this))
  (stop [this]
    (assoc this :amazon-sqs nil)))

(defn batch-receive [{:keys [amazon-sqs]} queue-url wait-duration]
  (->>
   (-> (new ReceiveMessageRequest)
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
      (throw
        (new RuntimeException
          (->> failed
               (map #(.getMessages %))
               (clojure.string/join "; ")))))))

(defn send-message [{:keys [amazon-sqs]} queue-url message-body]
  {:pre [(some? amazon-sqs)
         (some? queue-url)
         (some? message-body)]}
  (.sendMessage amazon-sqs (-> (new SendMessageRequest)
                               (.withQueueUrl queue-url)
                               (.withMessageBody (str message-body)))))
