(ns lomake-editori.core
  (:require [taoensso.timbre :refer [info]]
            [lomake-editori.handler :refer [handler]]
            [lomake-editori.db.migrations :as migrations]
            [clojure.core.async :as a]
            [environ.core :refer [env]]
            [ring.middleware.reload :refer [wrap-reload]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :refer [start-server]]
            [aleph.http :as http])
  (:gen-class))

(def server (atom nil))

(defn start-repl! []
  (when (:dev? env)
    (do
      (start-server :port 3333 :handler cider-nrepl-handler)
      (info "nREPL started on port 3333"))))

(defn run-migrations []
  (migrations/migrate :db "db.migration"))

(defn- try-f [f] (try (f) (catch Exception ignored nil)))

(defn -main [& [prt & _]]
  (let [port (or (try-f (fn [] (Integer/parseInt prt)))
                 3450)]
    (do
      (a/go (start-repl!))
      (run-migrations)
      (info "Starting server on port" port)
      (reset! server
              (http/start-server
               (if (:dev? env)
                 (wrap-reload (var handler))
                 handler)
               {:port port}))
      (info "Started server on port" port)
      (println "Press <enter> to win prize")
      (let [_ (read-line)]
        (.close @server)))))
