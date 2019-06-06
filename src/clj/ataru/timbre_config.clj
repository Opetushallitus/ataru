(ns ataru.timbre-config
  (:require [ataru.util.app-utils :as app-utils]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :refer [println-appender]]
            [taoensso.timbre.appenders.3rd-party.rolling :refer [rolling-appender]]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]])
  (:import [java.util TimeZone]))

(defn configure-logging! [app-id]
  (timbre/merge-config!
   {:appenders
    {:println
     (println-appender {:stream :std-out})
     :file-appender
     (rolling-appender
      {:path    (str (case (app-utils/get-app-id)
                       :virkailija (-> config :log :virkailija-base-path)
                       :hakija     (-> config :log :hakija-base-path))
                     "/app_" (case (app-utils/get-app-id)
                               :virkailija "ataru-editori"
                               :hakija     "ataru-hakija")
                     (when (:hostname env) (str "_" (:hostname env))))
       :pattern :daily})}
    :timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                     :timezone (TimeZone/getTimeZone "Europe/Helsinki")}}))
