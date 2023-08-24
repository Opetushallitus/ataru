(ns ataru.aws.sns
  (:require [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import [com.amazonaws.services.sns.message
            SnsMessageManager
            SnsNotification
            SnsSubscriptionConfirmation
            SnsUnknownMessage
            SnsUnsubscribeConfirmation]
           [java.io InputStream ByteArrayInputStream]))

(defrecord SNSMessageManager [sns-message-manager]
  component/Lifecycle
  (start [this]
    (if (nil? sns-message-manager)
      (assoc this :sns-message-manager (new SnsMessageManager
                                            (:region (:aws config))))
      this))
  (stop [this]
    (assoc this :sns-message-manager nil)))

(defmulti handle-message (fn [_ s] (class s)))

(defmethod handle-message InputStream [sns-message-manager input-stream]
  (->> input-stream
       (.parseMessage (:sns-message-manager sns-message-manager))
       (handle-message sns-message-manager)))

(defmethod handle-message String [sns-message-manager string]
  (->> (.getBytes string "UTF-8")
       (new ByteArrayInputStream)
       (handle-message sns-message-manager)))

(defmethod handle-message SnsNotification
  [_ message]
  message)

(defmethod handle-message SnsSubscriptionConfirmation
  [_ message]
  (log/info "Confirming subscription to" (.getTopicArn message))
  (.confirmSubscription message)
  (log/info "Subscription to" (.getTopicArn message) "confirmed"))

(defmethod handle-message SnsUnknownMessage
  [_ message]
  (log/error "Unknown SNS message" message))

(defmethod handle-message SnsUnsubscribeConfirmation
  [_ message]
  (log/warn "Unsubscribed from" (.getTopicArn message)))
