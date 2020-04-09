(ns ataru.timbre-config
  (:require [ataru.util.app-utils :as app-utils]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :refer [println-appender]]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            [timbre-ns-pattern-level]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]])
  (:import [java.util TimeZone]))

(defn configure-logging! [app-id]
  (let [system-defaults {:min-level (if (:dev? env) :debug :info)}]
    (timbre/merge-config!
      {:appenders
                       {:println
                        (merge
                          (println-appender {:stream :std-out})
                          system-defaults)
                        :file-appender
                        (merge
                          (rolling-appender
                            {:path    (str (case (app-utils/get-app-id)
                                             :virkailija (-> config :log :virkailija-base-path)
                                             :hakija (-> config :log :hakija-base-path))
                                           "/app_" (case (app-utils/get-app-id)
                                                     :virkailija "ataru-editori"
                                                     :hakija "ataru-hakija")
                                           (when (:hostname env) (str "_" (:hostname env))))
                             :pattern :daily})
                          system-defaults)}
       :middleware     [(timbre-ns-pattern-level/middleware {"com.amazonaws.*"                :info
                                                             "com.zaxxer.hikari.HikariConfig" :debug
                                                             "com.zaxxer.hikari.*"            :info
                                                             "io.netty.*"                     :info
                                                             "org.apache.http.*"              :info
                                                             "org.flywaydb.*"                 :info})]
       :timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                        :timezone (TimeZone/getTimeZone "Europe/Helsinki")}
       :output-fn      (partial timbre/default-output-fn {:stacktrace-fonts {}})})))
