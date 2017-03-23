(ns ataru.db.db
  (:require [ataru.config.core :refer [config]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [hikari-cp.core :refer :all]
            [ataru.db.extensions]
            [pandect.algo.sha256 :refer :all])
  (:import [java.security SecureRandom]))

(def random (SecureRandom.))

(defn generate-hash-id []
                       (sha256 (.generateSeed random (/ 512 8))))

(defn escape-like-pattern [pattern]
                          (string/replace pattern #"(\\|%|_)" "\\\\$1"))

(defn- datasource-spec [ds-key]
  "Merge configuration defaults and db config. Latter overrides the defaults"
  (merge {:auto-commit false
          :read-only false
          :connection-timeout 30000
          :validation-timeout 5000
          :idle-timeout 600000
          :max-lifetime 1800000
          :minimum-idle 10
          :maximum-pool-size 10
          :pool-name "db-pool"
          :adapter "postgresql"
          :currentSchema (-> config ds-key :schema)}
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

(defn close-datasource! [ds-key]
                        (swap! datasource (fn [datasources]
                                            (if (contains? datasources ds-key)
                                              (let [ds (ds-key datasources)]
                                                (close-datasource ds)
                                                (dissoc datasources ds-key))
                                              datasources))))

(defn get-next-exception-or-original [original-exception]
                                     (try (.getNextException original-exception)
                                          (catch IllegalArgumentException iae
                                            original-exception)))

(defn clear-db-and-grant! [ds-key schema-name grant-user]
                          (let [ds-key (keyword ds-key)]
                            (if (:allow-db-clear? (:server config))
                              (try (apply (partial jdbc/db-do-commands {:datasource (get-datasource ds-key)} true)
                                          (concat [(str "drop schema if exists " schema-name " cascade")
                                                   (str "create schema " schema-name)]
                                                  (if grant-user
                                                    [(str "grant usage on schema " schema-name " to " grant-user)
                                                     (str "alter default privileges in schema " schema-name " grant select on tables to " grant-user)])))
                                   (catch Exception e (log/error (get-next-exception-or-original e) (.toString e))))
                              (throw (RuntimeException. (str "Clearing database is not allowed! "
                                                             "check that you run with correct mode. "
                                                             "Current config name is " (config-name)))))))

(defn clear-db! [ds-key schema-name]
                (clear-db-and-grant! ds-key schema-name []))

(defmacro exec [ds-key query params]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key)}]
                             (~query ~params {:connection connection#})))

(defmacro exec-all [ds-key query-list]
  `(jdbc/with-db-transaction [connection# {:datasource (get-datasource ~ds-key)}]
                             (last (for [[query# params#] (partition 2 ~query-list)]
                                     (query# params# {:connection connection#})))))