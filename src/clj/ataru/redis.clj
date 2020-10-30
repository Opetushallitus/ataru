(ns ataru.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine.connections :as carcon]
            [taoensso.nippy :refer [*thaw-serializable-allowlist*]]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]))

(defrecord Redis [connection-opts]
  component/Lifecycle
  (start [this]
    (alter-var-root
     #'*thaw-serializable-allowlist*
     (constantly #{"*"}))
    (if (nil? connection-opts)
      (let [uri (get-in config [:redis :uri])]
        (log/info "Redis" uri)
        (assoc this :connection-opts {:pool (carcon/conn-pool nil)
                                      :spec {:uri uri}}))
      this))
  (stop [this]
    (when (some? connection-opts)
      (.close (:pool connection-opts)))
    (assoc this :connection-opts nil)))
