(ns ataru.log.access-log
  (:require [ataru.config.core :refer [config]]
            [ataru.util.app-utils :as app-utils]
            [clj-time.core :as t]
            [clojure.string :as s]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]))

(defonce service-name
  (case (app-utils/get-app-id)
    :virkailija "ataru_virkailija"
    :hakija "ataru_hakija"
    nil))

(defonce audit-log-config
  (assoc timbre/example-config
         :appenders {:file-appender
                     (assoc (rolling-appender {:path    (str (-> config :log :virkailija-base-path)
                                                             ; ^Virkailija and hakia paths are the same
                                                             "/accesslog_" service-name
                                                             ; Hostname will differentiate files in actual environments
                                                             (when (:hostname env)
                                                               (str "_" (:hostname env)))
                                                             ".log")
                                               :pattern :daily})
                            :output-fn (fn [{:keys [msg_]}] (force msg_)))}))

(defn info [str]
  (timbre/log* audit-log-config :info str))

(defn warn [str]
  (timbre/log* audit-log-config :warn str))

(defn error [str]
  (timbre/log* audit-log-config :error str))

(defn- extract-header
  [http-entity header-name]
  (get-in http-entity [:headers header-name] "-"))

(defn- extract-session
  [request]
  (let [cookies      (s/split (extract-header request "cookie") #";")
        ring-session (or (some #(when (s/starts-with? % "ring-session") %) cookies) "-")]
    (last (s/split ring-session #"="))))

(defn log
  [{:keys [info]}
   {:keys [request-method uri remote-addr query-string] :as req}
   {:keys [status] :as resp}
   totaltime]
  (let [method      (-> request-method name s/upper-case)
        request     (str method " " uri (when query-string (str "?" query-string)))
        log-map     {:timestamp           (.toString (t/to-time-zone (t/now) (t/time-zone-for-id "Europe/Helsinki")))
                     :responseCode        status
                     :request             request
                     :responseTime        totaltime
                     :requestMethod       method
                     :service             service-name
                     :environment         (-> config :public-config :environment-name)
                     :user-agent          (extract-header req "user-agent")
                     :caller-id           (extract-header req "caller-id")
                     :clientSubsystemCode (extract-header req "clientsubsystemcode")
                     :x-forwarded-for     (extract-header req "x-forwarded-for")
                     :x-real-ip           (extract-header req "x-real-ip")
                     :remote-ip           remote-addr
                     :session             (extract-session req)
                     :response-size       (extract-header resp "Content-Length")
                     :referer             (extract-header req "referer")}
        log-message (cheshire.core/generate-string log-map)]
    (info log-message)))