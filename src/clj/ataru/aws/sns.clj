(ns ataru.aws.sns
  (:require [cheshire.core :as json]
            [taoensso.timbre :as log]))

(defn handle-message [string]
  (let [message (json/parse-string string true)]
    (if-let [type (:Type message)]
      (if (= "Notification" type)
        (:Message message)
        (log/error "Unknown SNS message type" type "in" string))
      (log/error "Unknown SNS message" string))))
