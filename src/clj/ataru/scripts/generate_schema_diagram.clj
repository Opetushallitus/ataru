(ns ataru.scripts.generate-schema-diagram
  (:require
    [ataru.db.migrations :as migrations]
    [environ.core :refer [env]]
    [clojure.java.shell :refer [sh]]
    [oph.soresu.common.config :refer [config]]))

(defn -main
  []
  (migrations/migrate)
  (let [db-config    (:db config)
        return-value (sh "./bin/generate-db-schema-diagram.sh"
                         (:server-name db-config)
                         (str (:port-number db-config))
                         (:database-name db-config)
                         "./target/db-schema"
                         (:ataru-version env))]
    (println return-value)
    (:exit return-value)))
