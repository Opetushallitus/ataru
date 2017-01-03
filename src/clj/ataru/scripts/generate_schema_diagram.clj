(ns ataru.scripts.generate-schema-diagram
  (:require
    [ataru.db.migrations :as migrations]
    [ataru.schema.form-schema]
    [clojure.java.shell :refer [sh]]
    [environ.core :refer [env]]
    [oph.soresu.common.config :refer [config]]
    ;[schema-viz.core :as svc]
    ))

(defn generate-db-schema-diagram
  []
  (let [db-config    (:db config)
        return-value (sh "./bin/generate-db-schema-diagram.sh"
                         (:server-name db-config)
                         (str (:port-number db-config))
                         (:database-name db-config)
                         "./target/db-schema"
                         (:ataru-version env))]
    (println return-value)
    (:exit return-value)))

;(defn generate-form-schema-diagram
;  []
;  (svc/save-schemas (str "target/db-schema/ataru-form-" (:ataru-version env) ".png") {:ns 'ataru.schema.form-schema}))

(defn -main
  []
  (migrations/migrate)
  (generate-db-schema-diagram)
  ; disabled since graphviz is not installed on CI server:
  ; (generate-form-schema-diagram)
  )
