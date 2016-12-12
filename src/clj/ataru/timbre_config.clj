(ns ataru.timbre-config
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [environ.core :refer [env]]
            [oph.soresu.common.config :refer [config]]))

(defn configure-logging! [app-id]
  (when-not (env :dev?)
    (let [log-kwd  (case app-id
                     :virkailija    :virkailija-base-path
                     :hakija        :hakija-base-path
                     :ataru-editori :virkailija-base-path
                     :ataru-hakija  :hakija-base-path)
          path     (get-in config [:log log-kwd])
          filename (str (name app-id) ".log")]
      (timbre/merge-config! {:appenders
                             {:println      {:enabled? false}
                              :standard-out {:enabled? false}
                              :spit         {:enabled? false}
                              :rotor        (rotor/rotor-appender
                                              {:max-size (* 10 1024 1024)
                                               :backlog  10
                                               :path     (str path "/" filename)})}}))))
