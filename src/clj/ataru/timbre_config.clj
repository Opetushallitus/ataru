(ns ataru.timbre-config
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.community.rolling :refer [rolling-appender]]
            [timbre-ns-pattern-level]
            [ataru.config.core :refer [config]])
  (:import [java.util TimeZone]))

(defn configure-logging! [app-id hostname]
  (timbre/merge-config!
    {:appenders
                     {:standard-out     {:enabled? false}
                      :println          nil
                      :file-appender
                      (rolling-appender
                       {:path    (str (case app-id
                                        "ataru-editori"
                                        (str (-> config :log :virkailija-base-path)
                                             "/app_ataru-editori")
                                        "ataru-hakija"
                                        (str (-> config :log :hakija-base-path)
                                             "/app_ataru-hakija"))
                                      (when hostname (str "_" hostname)))
                         :pattern :daily})}
     :middleware     [(timbre-ns-pattern-level/middleware {"com.zaxxer.hikari.HikariConfig" :debug
                                                           "fi.vm.sade.javautils.nio.cas"          :debug
                                                           "fi.vm.sade.javautils.nio.cas.impl"     :debug
                                                           :all                             :info})]
     :timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                      :timezone (TimeZone/getTimeZone "Europe/Helsinki")}
     :output-fn      (partial timbre/default-output-fn {:stacktrace-fonts {}})}))
