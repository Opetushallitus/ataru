(ns clj-stdout-access-logging
  (:require [cheshire.core :as json]))

(defn stdout-access-logging-response
  [response new-line]
  (when-let [access-log-data (:access-log-data response)]
    (try
      (print (str (json/generate-string {:eventType "access"
                                         :timestamp (:timestamp access-log-data)
                                         :event     access-log-data})
                  new-line))
      (flush)
      (catch Exception e
        (throw (new RuntimeException "Failed to log access log data" e)))))
  response)

(defn wrap-stdout-access-logging
  [handler]
  (let [new-line (System/getProperty "line.separator")]
    (fn
      ([request]
       (stdout-access-logging-response (handler request) new-line))
      ([request respond raise]
       (handler request
                #(try
                   (respond (stdout-access-logging-response % new-line))
                   (catch Exception e
                     (raise e)))
                raise)))))
