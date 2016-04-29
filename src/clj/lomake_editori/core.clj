(ns lomake-editori.core
  (:require [taoensso.timbre :refer [info]]
            [lomake-editori.clerk-routes :refer [clerk-routes]]
            [lomake-editori.db.migrations :as migrations]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [aleph.http :as http]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn start-repl! []
  (when (:dev? env)
    (do
      (start-server :port 3333 :handler cider-nrepl-handler)
      (info "nREPL started on port 3333"))))

(defn run-migrations []
  ;; Only run migrations when in dev mode for now
  ;; Deployment has to catch up before we can run migrations on test/prod
  ;; servers
  (migrations/migrate :db "db.migration"))

(defn- try-f [f] (try (f) (catch Exception ignored nil)))

(defrecord Server []
  component/Lifecycle

  (start [component]
    (let [port    8350
          handler clerk-routes
          server  (http/start-server handler {:port port})]
      (do
        (a/go (start-repl!))
        (a/go (run-migrations)))
      (info (str "Started server on port " port))
      (assoc component :server server)))

  (stop [component]
    (info "Stopping server")
    (try-f #(let [server (:server component)]
             (.close server)))
    (info "Stopped server")
    (assoc component :stop-fn nil)))

(defn new-server
  []
  (->Server))
