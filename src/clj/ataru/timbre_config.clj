(ns ataru.timbre-config
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [environ.core :refer [env]]
            [ataru.config.core :refer [config]])
  (:import [java.util TimeZone]))

(defn configure-logging! [app-id]
  (let [timestamp-opts {:pattern  "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                        :timezone (TimeZone/getTimeZone "Europe/Helsinki")}]
    (if (env :dev?)
      (timbre/merge-config! {:appenders
                             {:println
                              {:ns-blacklist ["ataru.cache.redis-cache"]}
                              :cache-error-appender
                              (assoc (appenders/println-appender)
                                     :min-level :warn
                                     :ns-whitelist ["ataru.cache.redis-cache"])}
                             :timestamp-opts timestamp-opts})
      (let [log-kwd  (case app-id
                       :virkailija :virkailija-base-path
                       :hakija     :hakija-base-path)
            path     (get-in config [:log log-kwd])
            filename (str (name app-id) ".log")]
        (timbre/merge-config! {:appenders
                               {:println      {:enabled? false}
                                :standard-out {:enabled? false}
                                :spit         {:enabled? false}
                                :rotor        (rotor/rotor-appender
                                               {:max-size (* 10 1024 1024)
                                                :backlog  10
                                                :path     (str path "/" filename)})}
                               :timestamp-opts timestamp-opts})))))
