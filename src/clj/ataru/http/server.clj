(ns ataru.http.server
  (:require [taoensso.timbre :refer [info]]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [aleph.http :as http]
            [com.stuartsierra.component :as component]))

(defn start-repl! [repl-port]
  (when (:dev? env)
    (do
      (start-server :port repl-port :handler cider-nrepl-handler)
      (info "nREPL started on port" repl-port))))

(defmacro ^:private try-f
  [& form]
  `(try ~@form (catch Exception _#)))

(defrecord Server []
  component/Lifecycle

  (start [this]
    (let [server-setup (:server-setup this)
          port         (:port server-setup)
          repl-port    (:repl-port server-setup)
          handler      (cond-> (get-in this [:handler :routes])
                         (:dev? env) wrap-reload)
          server       (http/start-server handler {:port port})]
      (do
        (a/go (start-repl! repl-port)))
      (info (str "Started server on port " port))
      (assoc this :server server)))

  (stop [this]
    (info "Stopping server")
    (try-f (let [server (:server this)]
            (.close server)))
    (info "Stopped server")
    (assoc this :server nil)))

(defn new-server
  []
  (->Server))
