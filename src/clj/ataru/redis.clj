(ns ataru.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine.connections :as carcon]
            [ataru.config.core :refer [config]]))

(defrecord Redis [connection-opts]
  component/Lifecycle
  (start [this]
    (if (nil? connection-opts)
      (assoc this :connection-opts {:pool (carcon/conn-pool nil)
                                    :spec {:uri (get-in config [:redis :uri])}})
      this))
  (stop [this]
    (when (some? connection-opts)
      (.close (:pool connection-opts)))
    (assoc this :connection-opts nil)))
