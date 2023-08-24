(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [clj-time.jdbc] ; for java.sql.Timestamp / org.joda.time.DateTime coercion
            [ataru.timbre-config :as timbre-config]
            [ataru.log.audit-log :as audit-log]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.db.migrations :as migrations]
            [environ.core :refer [env]]
            [taoensso.timbre :as log])
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

(defn -main [& _]
  (let [app-id         (:app env)
        init-system    (get-init-system app-id)
        audit-logger   (audit-log/new-audit-logger app-id)
        on-termination (promise)]
    (.addShutdownHook (Runtime/getRuntime) (new Thread (fn [] (deliver on-termination nil))))
    (timbre-config/configure-logging! app-id (:hostname env))
    (log/info "Starting application" app-id (if (:dev? env) "dev" ""))
    (log/info "Running migrations")
    (migrations/migrate audit-logger)
    (let [system (component/start (init-system audit-logger))]
      @on-termination
      (log/info "Stopping application" app-id (if (:dev? env) "dev" ""))
      (component/stop system))))
