(ns ataru.scripts.generate-schema-diagram
  (:require
    [ataru.db.flyway-migration :as migration]
    [ataru.schema.form-schema]
    [clojure.java.shell :refer [sh]]
    [environ.core :refer [env]]
    [ataru.config.core :refer [config]]
    [ataru.log.audit-log :refer [new-dummy-audit-logger]]))

(defn generate-db-schema-diagram
  []
  (let [db-config    (:db config)
        return-value (sh "./bin/generate-db-schema-diagram.sh"
                         (:server-name db-config)
                         (str (:port-number db-config))
                         (:database-name db-config)
                         "./target/db-schema"
                         (:ataru-version env)
                         (:username db-config))]
    (println return-value)
    (:exit return-value)))

(defn -main
  []
  (migration/migrate (new-dummy-audit-logger))
  (generate-db-schema-diagram))
