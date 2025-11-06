(ns ataru.aws.sns
  (:require [cheshire.core :as json]
            [clojure.string :as s]
            [taoensso.timbre :as log]))

(defn handle-message [string]
  (let [message (json/parse-string string true)]
    (if-let [type (:Type message)]
      (if (= "Notification" type)
        (-> message
            (:Message)
            (s/replace "\\" "")
            (json/parse-string true))
        (log/error "Unknown SNS message type" type "in" string))
      (log/error "Unknown SNS message" string))))
