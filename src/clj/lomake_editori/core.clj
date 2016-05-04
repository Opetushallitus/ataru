(ns lomake-editori.core
  (:require [taoensso.timbre :refer [info]]
            [lomake-editori.clerk-routes :refer [clerk-routes]]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [aleph.http :as http]
            [com.stuartsierra.component :as component]))

(defn start-repl! []
  (when (:dev? env)
    (do
      (start-server :port 3333 :handler cider-nrepl-handler)
      (info "nREPL started on port 3333"))))

(defmacro ^:private try-f
  [& form]
  `(try ~@form (catch Exception _#)))

(defrecord Server []
  component/Lifecycle

  (start [component]
    (let [port    8350
          handler (if (:dev? env)
                    (wrap-reload (var clerk-routes))
                    clerk-routes)
          server  (http/start-server handler {:port port})]
      (do
        (a/go (start-repl!)))
      (info (str "Started server on port " port))
      (assoc component :server server)))

  (stop [component]
    (info "Stopping server")
    (try-f #(let [server (:server component)]
             (.close server)))
    (info "Stopped server")
    (assoc component :server nil)))

(defn new-server
  []
  (->Server))
