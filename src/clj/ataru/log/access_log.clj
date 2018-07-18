(ns ataru.log.access-log
  (:require [taoensso.timbre :as timbre]
            [ataru.config.core :refer [config]]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            [clj-time.core :as t]))

(defonce audit-log-config
  (assoc timbre/example-config
         :appenders {:file-appender
                     (assoc (rolling-appender {:path    (str (-> config :log :access-log-base-path)
                                                             "_"
                                                             ; Hostname will differentiate files in actual environments
                                                             (environ.core/env :hostname)
                                                             ".log")
                                               :pattern :daily})
                            :output-fn (fn [{:keys [msg_]}] (force msg_)))}))

(defn info [str]
  (timbre/log* audit-log-config :info str))

(defn warn [str]
  (timbre/log* audit-log-config :warn str))

(defn error [str]
  (timbre/log* audit-log-config :error str))

(defn logger
  [{:keys [info]}
   {:keys [request-method uri remote-addr query-string] :as req}
   {:keys [status] :as resp}
   totaltime]
  (let [method       (-> request-method name clojure.string/upper-case)
        request-path (str uri (when query-string (str "?" query-string)))
        agent        (get-in req [:headers "user-agent"] "-")
        size         (get-in resp [:headers "Content-Length"] "-")
        log-map      {:timestamp         (.toString (t/to-time-zone (t/date-time 1986 10 22)
                                                                    (t/time-zone-for-id "Europe/Helsinki")))
                      :responseCode      status
                      :resuest           (str method " " request-path)
                      :responseTime      totaltime
                      :requestMethod     method
                      :service           "ataru_virkailija"
                      :user-agent        agent
                      :caller-id         "-"
                      :clientSubsystemId "-"
                      :remote-ip         remote-addr
                      :response-size     size}
        log-message  (cheshire.core/generate-string log-map)]
    (info log-message)))