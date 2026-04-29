(ns ataru.db.db
  (:require [ataru.config.core :refer [config config-name]]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :refer [make-datasource]]
            [ataru.db.extensions]
            [taoensso.timbre :as log])
  (:import (software.amazon.jdbc.util SqlState)
           (com.zaxxer.hikari HikariConfig)))

(defn- jdbc-url [db-config schema]
  (str "jdbc:aws-wrapper:postgresql://"
       (:server-name db-config)
       ":"
       (:port-number db-config)
       "/"
       (:database-name db-config)
       (when schema
         (str "?currentSchema=" schema))))

(defn- datasource-spec
  "Merge configuration defaults and db config. Latter overrides the defaults"
  [ds-key]
  (let [db-config (ds-key config)
        schema    (:schema db-config)]
    (merge {:auto-commit            false
            :read-only              false
            :connection-timeout     120000
            :validation-timeout     5000
            :idle-timeout           600000
            :max-lifetime           1800000
            :minimum-idle           10
            :maximum-pool-size      10
            :pool-name              "db-pool"
            :jdbc-url               (jdbc-url db-config schema)
            :driver-class-name      "software.amazon.jdbc.Driver"
            :configure              (fn [^HikariConfig config]
                                      (.setExceptionOverrideClassName
                                       config
                                       "software.amazon.jdbc.util.HikariCPSQLException"))}
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

(def ^:private max-failover-retries 3)

(defn- failover-exception? [^java.sql.SQLException e]
  (contains? #{(.getState SqlState/COMMUNICATION_LINK_CHANGED)
               (.getState SqlState/CONNECTION_FAILURE_DURING_TRANSACTION)} (.getSQLState e)))

(defn- with-failover-retry [datasource transactionally? f]
  (jdbc/with-db-connection [conn {:datasource datasource}]
    (loop [attempt 0]
      (when (>= attempt max-failover-retries)
        (throw (ex-info "Max database failover retries exceeded" {:attempts attempt})))
      (let [[retry? result]
            (try
              [false (if transactionally?
                       (jdbc/with-db-transaction [tx conn] (f tx))
                       (f conn))]
              (catch java.sql.SQLException e
                (if (failover-exception? e)
                  (do (log/warn "Database failover detected, SQLState:" (.getSQLState e)
                                "- retrying (attempt" (inc attempt) "of" max-failover-retries ")")
                      [true nil])
                  (throw e))))]
        (if retry?
          (recur (inc attempt))
          result)))))

(defn exec [ds-key query params]
  (with-failover-retry (get-datasource ds-key) true
    (fn [connection] (query params {:connection connection}))))

(defn exec-conn [ds-key query params]
  (with-failover-retry (get-datasource ds-key) false
    (fn [connection] (query params {:connection connection}))))
