(ns ataru.timbre-config
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [environ.core :refer [env]]
            [oph.soresu.common.config :refer [config]]))

(defn configure-logging! [logname]
  (when-not (env :dev?)
    (let [path (get-in config [:log :base-path])]
      (timbre/merge-config! {:appenders
                             {:rotor (rotor/rotor-appender
                                       {:max-size (* 10 1024 1024)
                                        :backlog  10
                                        :path     (str path "/logs" logname ".log")})}}))))
