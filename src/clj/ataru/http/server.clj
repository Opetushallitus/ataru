(ns ataru.http.server
  (:require [taoensso.timbre :refer [info]]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.tools.nrepl.server :as nrepl]
            [aleph.http :as http]
            [com.stuartsierra.component :as component]))

; When restarting, we want to keep the same repl running, otherwise our repl-session is lost
; and restarting the repl is meaningless
(def repl-started (atom false))

(defn start-repl! [repl-port]
  (when (and (:dev? env) (not @repl-started))
    (do
      (nrepl/start-server :port repl-port)
      (reset! repl-started true)
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
                         (:dev? env) (wrap-reload))
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
