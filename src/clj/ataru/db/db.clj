(ns ataru.db.db
  (:require [ataru.config.core :refer [config config-name]]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :refer [make-datasource]]
            [ataru.db.extensions]
            [taoensso.timbre :as log]))

(defn- datasource-spec
  "Merge configuration defaults and db config. Latter overrides the defaults"
  [ds-key]
  (merge {:auto-commit        false
          :read-only          false
          :connection-timeout 30000
          :validation-timeout 5000
          :idle-timeout       600000
          :max-lifetime       1800000
          :minimum-idle       10
          :maximum-pool-size  10
          :pool-name          "db-pool"
          :adapter            "postgresql"
          :currentSchema      (-> config ds-key :schema)}
         (-> (ds-key config)
             (dissoc :schema))))

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
(defmacro exec-all [ds-key query-list]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key)}]
     (last (for [[query# params#] (partition 2 ~query-list)]
             (query# params# {:connection connection#})))))
