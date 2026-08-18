(ns clj-timbre-access-logging
  (:require [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.community.rolling :refer [rolling-appender]]))

(defn- file-access-log-config
  [path]
  (assoc timbre/default-config
         :appenders {:file-appender
                     (assoc (rolling-appender {:path    path
                                               :pattern :daily})
                            :output-fn #(force (:msg_ %)))}))

(defn timbre-access-logging-response
  [response log-config]
  (when-let [access-log-data (:access-log-data response)]
    (try
      (timbre/log* log-config :info (json/generate-string access-log-data))
      (catch Exception e
        (throw (new RuntimeException "Failed to log access log data" e)))))
  response)

(defn wrap-timbre-access-logging
  [handler {:keys [path]}]
  (let [log-config (file-access-log-config path)]
    (fn
      ([request]
       (timbre-access-logging-response (handler request) log-config))
      ([request respond raise]
       (handler request
                #(try
                   (respond (timbre-access-logging-response % log-config))
                   (catch Exception e
                     (raise e)))
                raise)))))
