(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [ataru.timbre-config :as timbre-config]
            [ataru.log.audit-log :as audit-log]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.db.flyway-migration :as migration]
            [environ.core :refer [env]]
            [taoensso.timbre :as log]
            [nrepl.server :as nrepl])
  (:gen-class))

(def systems {"ataru-editori" virkailija-system/new-system
              "ataru-hakija"  hakija-system/new-system})

(defn- get-init-system [app-id]
  (if-let [init-system (get systems app-id)]
    init-system
    (binding [*out* *err*]
      (println (str "No system map found for " app-id ". "
                    "Valid keys: ataru-editori, ataru-hakija"))
      (System/exit 1))))

(def repl-ports {"ataru-editori" "3333"
                 "ataru-hakija"  "3335"})

(defn get-repl-port [app-id]
      (Integer/parseInt (get env :ataru-repl-port (get repl-ports app-id))))

(defn start-repl! [repl-port, dev?]
      (when (:dev? env)
            (nrepl/start-server
              :port repl-port
              :bind "0.0.0.0"
              :handler (if dev?
                         ((requiring-resolve 'com.gfredericks.debug-repl/wrap-debug-repl) (nrepl.server/default-handler))
                         (nrepl.server/default-handler)))
            (log/report "nREPL started on port" repl-port)))

(defn -main [& _]
  (let [app-id         (:app env)
        init-system    (get-init-system app-id)
        audit-logger   (audit-log/new-audit-logger app-id)
        on-termination (promise)]
    (.addShutdownHook (Runtime/getRuntime) (new Thread (fn [] (deliver on-termination nil))))
    (timbre-config/configure-logging! app-id (:hostname env))
    (log/info "Starting application" app-id (if (:dev? env) "dev" ""))
    (log/info "Running migrations")
    (migration/migrate audit-logger)
    (start-repl! (get-repl-port app-id) (:dev? env))
    (if (= "true" (:reloaded env))
      @on-termination
      (let [system (component/start (init-system audit-logger))]
           @on-termination
           (log/info "Stopping application" app-id (if (:dev? env) "dev" ""))
           (component/stop system)))))
