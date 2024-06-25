(ns ataru.redis
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine.connections :as carcon]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]))

(defrecord Redis [connection-opts]
  component/Lifecycle
  (start [this]
    (if (nil? connection-opts)
      (let [uri (get-in config [:redis :uri])]
        (log/info "Redis" uri)
        (assoc this :connection-opts {:pool (carcon/conn-pool nil)
                                      :spec {:uri uri}}))
      this))
  (stop [_]
    ; Redis connection pool cannot be closed because carmine memoizes pool creation
    ; and will thus return the same (closed) pool when using reloaded.repl workflow.
    ; Thus, when changing the redis component or configuration a JVM restart is needed.
    ))
