(ns ataru.db.flyway-migration
  (:gen-class)
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [taoensso.timbre :as log]
            [ataru.log.audit-log :as audit-log]
            [ataru.db.migration-implementations :refer [audit-logger]])
  (:import [org.flywaydb.core Flyway]))
(defn migrate
  [audit-logger-to-use]
  (if (= "use dummy-audit-logger!" audit-logger-to-use)
    (reset! audit-logger audit-log/new-dummy-audit-logger)
    (reset! audit-logger audit-logger-to-use))
  (let [schema-name (-> config :db :schema)
        flyway      (doto (Flyway.)
                      (.setSchemas (into-array String [schema-name]))
                      (.setDataSource (db/get-datasource :db))
                      (.setLocations (into-array String ["db.migration" "ataru.db.migrations"])))]
    (try (.migrate flyway)
         (catch Throwable e
           (log/error e)
           (throw e)))))