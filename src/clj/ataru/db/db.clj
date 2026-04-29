(ns ataru.db.db
  (:require [ataru.config.core :refer [config config-name]]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :refer [make-datasource]]
            [ataru.db.extensions]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(defn- jdbc-url [db-config schema]
  (let [aurora? (not= "localhost" (:server-name db-config))
        params  (cond-> []
                  aurora? (into [; activates aurora-pg topology detection (via replica_host_status)
                                 ; and enables the failover2 + efm2 + auroraStaleDns plugin chain
                                 "wrapperDialect=aurora-pg"
                                 ; how long the wrapper tries to complete failover before giving up with
                                 ; FailoverFailedSQLException — HikariCP connection-timeout must exceed this
                                 "failoverTimeoutMs=120000"
                                 ; how often to poll replica_host_status during failover to find the new writer
                                 "failoverClusterTopologyRefreshRateMs=2000"
                                 ; how often to attempt a connection to the newly promoted writer
                                 "failoverWriterReconnectIntervalMs=2000"
                                 ; normal topology refresh rate — determines how quickly a writer loss is detected
                                 "clusterTopologyRefreshRateMs=30000"])
                  schema  (conj (str "currentSchema=" schema)))]
    (str "jdbc:aws-wrapper:postgresql://"
         (:server-name db-config)
         ":"
         (:port-number db-config)
         "/"
         (:database-name db-config)
         (when (seq params)
           (str "?" (string/join "&" params))))))

(defn- datasource-spec
  "Merge configuration defaults and db config. Latter overrides the defaults"
  [ds-key]
  (let [db-config (ds-key config)
        schema    (:schema db-config)
        aurora?   (not= "localhost" (:server-name db-config))]
    (merge {:auto-commit        false
            :read-only          false
            ; must exceed failoverTimeoutMs (120000) so HikariCP doesn't give up before the wrapper succeeds
            :connection-timeout (if aurora? 150000 30000)
            :validation-timeout 5000
            :idle-timeout       600000
            :max-lifetime       1800000
            :minimum-idle       10
            :maximum-pool-size  10
            ; detect dead idle connections proactively after failover (0 = disabled for localhost)
            :keepalive-time     (if aurora? 30000 0)
            :pool-name          "db-pool"
            :jdbc-url           (jdbc-url db-config schema)
            :driver-class-name  "software.amazon.jdbc.Driver"
            ; After a successful writer failover the wrapper throws FailoverSuccessSQLException (08S02),
            ; which HikariCP would normally treat as fatal and evict the connection — even though the
            ; wrapper already reconnected it to the new writer. HikariCPSQLException overrides that:
            ; it returns DO_NOT_EVICT for 08S02 and 08007 so the connection stays in the pool
            ; and the caller can retry the operation on the already-recovered connection.
            :exception-override-class-name "software.amazon.jdbc.util.HikariCPSQLException"}
           (-> db-config
               (dissoc :schema
                       :adapter
                       :database-name
                       :server-name
                       :port-number)))))

(defonce datasource (atom {}))

(defn get-datasource [ds-key]
  (swap! datasource (fn [datasources]
                      (if (not (contains? datasources ds-key))
                        (let [ds (make-datasource (datasource-spec ds-key))]
                          (assoc datasources ds-key ds))
                        datasources)))
  (ds-key @datasource))

(defn get-next-exception-or-original [original-exception]
  (try (.getNextException original-exception)
       (catch IllegalArgumentException _
         original-exception)))

(defn clear-db! [ds-key schema-name]
  (let [ds-key (keyword ds-key)]
    (if (:allow-db-clear? (:server config))
      (try (jdbc/db-do-commands {:datasource (get-datasource ds-key)} true
                                [(str "drop schema if exists " schema-name " cascade")
                                 (str "create schema " schema-name)])
           (catch Exception e (log/error (get-next-exception-or-original e))))
      (throw (RuntimeException. (str "Clearing database is not allowed! "
                                     "check that you run with correct mode. "
                                     "Current config name is " (config-name)))))))

(defmacro exec [ds-key query params]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key)}]
     (~query ~params {:connection connection#})))

(defmacro exec-conn [ds-key query params]
  `(jdbc/with-db-connection [connection# {:datasource (get-datasource ~ds-key)}]
     (~query ~params {:connection connection#})))
