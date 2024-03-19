(ns ataru.http.server
  (:require [taoensso.timbre :as log]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [aleph.http :as http]
            [aleph.flow :as flow]
            [com.stuartsierra.component :as component]))

(defrecord Server []
  component/Lifecycle

  (start [this]
    (let [server-setup (:server-setup this)
          port         (:port server-setup)
          handler      (cond-> (get-in this [:handler :routes])
                               (:dev? env) (wrap-reload))
          executor     (flow/utilization-executor 0.9 512)
          server       (http/start-server handler {:port port
                                                   :executor executor})]
      (log/report (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (log/report "Stopping server")
    (try (.close (:server this)) (catch Exception _ nil))
    (log/report "Stopped server")
    (assoc this :server nil)))

(defn new-server
  []
  (->Server))
