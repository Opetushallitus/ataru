(ns ataru.log.access-log
  (:require [ataru.config.core :refer [config]]
            [ataru.util.app-utils :as app-utils]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clojure.string :as s]
            [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :refer [println-appender]]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]])
  (:import java.util.TimeZone))

(defonce service-name
  (case (app-utils/get-app-id)
    :virkailija "ataru-editori"
    :hakija     "ataru-hakija"))

(defonce environment-name
  (-> config :public-config :environment-name))

(defonce access-log-config
  (letfn [(get-log-path
           []
           (str (case (app-utils/get-app-id)
                  :virkailija (-> config :log :virkailija-base-path)
                  :hakija     (-> config :log :hakija-base-path))
                "/access_" service-name
                ;; Hostname will differentiate files in actual environments
                (when (:hostname env) (str "_" (:hostname env)))))

          (file-logger
           [{:keys [msg_]}]
           (force msg_))

          (stdout-logger
           [data]
           (let [timestamp (force (:timestamp_ data))
                 message   (force (:msg_ data))
                 event     (dissoc (try
                                     (json/parse-string message)
                                     (catch Exception e
                                       (throw (ex-info (str "Failed to deserialize access log event to JSON. Malformed input? Message:\n" message)
                                                       {:data data} e))))
                                   :timestamp)]
             (json/generate-string
              {:eventType "access"
               :timestamp timestamp
               :event     event}))
           )]
    (assoc timbre/example-config
     :appenders {:file-appender   (assoc (rolling-appender {:path    (get-log-path)
                                                            :pattern :daily})
                                         :output-fn file-logger)
                 :stdout-appender (assoc (println-appender {:stream :std-out})
                                         :output-fn stdout-logger)}
     :timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                      :timezone (TimeZone/getTimeZone "Europe/Helsinki")})))

(defn- extract-header
  [http-entity header-name]
  (get-in http-entity [:headers header-name] "-"))

(defn- extract-session
  [request]
  (let [cookies      (s/split (extract-header request "cookie") #";")
        ring-session (or (some #(when (s/starts-with? % "ring-session") %) cookies) "-")]
    (last (s/split ring-session #"="))))

(defn wrap-with-access-logging
  [handler]
  (fn [request]
    (let [start    (t/now)
          response (handler request)
          end      (t/now)]
      (try
        (timbre/log*
         access-log-config
         :info
         (cheshire.core/generate-string
          {:timestamp       (.toString (t/to-time-zone end (t/time-zone-for-id "Europe/Helsinki")))
           :responseCode    (:status response)
           :request         (str (s/upper-case (:request-method request)) " "
                                 (:uri request)
                                 (when (:query-string request)
                                   (str "?" (:query-string request))))
           :responseTime    (t/in-millis (t/interval start end))
           :requestMethod   (s/upper-case (:request-method request))
           :service         service-name
           :environment     environment-name
           :user-agent      (extract-header request "user-agent")
           :caller-id       (extract-header request "caller-id")
           :x-forwarded-for (extract-header request "x-forwarded-for")
           :x-real-ip       (extract-header request "x-real-ip")
           :remote-ip       (:remote-addr request)
           :session         (extract-session request)
           :response-size   (extract-header response "Content-Length")
           :referer         (extract-header request "referer")}))
        (catch Exception e
          (timbre/error e "Failed to access log")))
      response)))
