(ns ataru.db.db
  (:require [ataru.config.core :refer [config config-name]]
            [clojure.java.jdbc :as jdbc]
            [hikari-cp.core :refer [make-datasource]]
            [environ.core :refer [env]]
            [ataru.db.extensions]
            [clojure.string :as string]
            [taoensso.timbre :as log]))

(defn- read-only-query? [query]
  (not (string/ends-with? query "!")))

(defn- ataru-editori? []
  (= "ataru-editori" (:app env)))

(defn- datasource-spec
  "Merge configuration defaults and db config. Latter overrides the defaults"
  [ds-key read-only?]
  (merge {:auto-commit        false
          :read-only          read-only?
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

(defn get-datasource
  ([ds-key]
   (get-datasource ds-key false))
  ([ds-key read-only?]
   (let [ds-key (if read-only?
                  :db-read-only
                  ds-key)]
     (swap! datasource (fn [datasources]
                         (if (not (contains? datasources ds-key))
                           (let [ds (make-datasource (datasource-spec ds-key read-only?))]
                             (assoc datasources ds-key ds))
                           datasources)))
     (ds-key @datasource))))

(defn- get-next-exception-or-original [original-exception]
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

(defn use-read-only-datasource? [query]
  (and (read-only-query? query)
       (ataru-editori?)))

(defmacro exec-on-primary [ds-key query params]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key false)}]
                             (~query ~params {:connection connection#})))

(defmacro exec [ds-key query params]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key (use-read-only-datasource? (-> ~query meta :name)))}]
     (~query ~params {:connection connection#})))

