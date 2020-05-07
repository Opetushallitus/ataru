(ns ataru.core
  (:require [com.stuartsierra.component :as component]
            [clj-time.jdbc] ; for java.sql.Timestamp / org.joda.time.DateTime coercion
            [ataru.timbre-config :as timbre-config]
            [ataru.log.audit-log :as audit-log]
            [ataru.virkailija.virkailija-system :as virkailija-system]
            [ataru.hakija.hakija-system :as hakija-system]
            [ataru.db.migrations :as migrations]
            [environ.core :refer [env]]
            [taoensso.timbre :refer [info]])
  (:gen-class))

(defn- wait-forever
  []
  @(promise))

(def ^:private app-systems {"ataru-editori" virkailija-system/init-new-system
                            "ataru-hakija"  hakija-system/init-new-system})

(def system (atom {:system nil
                   :system-fn nil}))

(defn start []
  (swap! system (fn [{:keys [app-id system system-fn audit-logger] :as old-system}]
                  (if system
                    (do
                      (info "System already started")
                      old-system)
                    (do
                      (info "Running migrations")
                      (migrations/migrate audit-logger)
                      (info "Starting system" app-id)
                      (assoc old-system :system (component/start (system-fn))))))))

(defn stop []
  (swap! system (fn [{:keys [system] :as old-system}]
                  (when system
                    (component/stop system))
                  (dissoc old-system :system))))

(defn restart []
  (stop)
  (start))

(defn -main [& args]
  (let [app-id         (:app env)
        init-system-fn (get app-systems app-id)]
    (timbre-config/configure-logging!)
    (info "Starting application" app-id (if (:dev? env) "dev" ""))
    (when-not init-system-fn
      (println "ERROR: No system map found for application" app-id "exiting. Valid keys: "
               (apply str (interpose ", " (map name (keys app-systems)))))
      (System/exit 1))
    (let [audit-logger (audit-log/new-audit-logger)]
      (reset! system {:app-id       app-id
                      :audit-logger audit-logger
                      :system-fn    (init-system-fn audit-logger)})
      (start)
      (wait-forever))))
