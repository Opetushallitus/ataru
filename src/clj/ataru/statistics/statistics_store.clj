(ns ataru.statistics.statistics-store
  (:require [yesql.core :refer [defqueries]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [ataru.db.db :as db]))

(defqueries "sql/statistics-queries.sql")

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn get-application-stats
  [start-time]
  (->> (exec-db :db yesql-get-application-stats {:start_time start-time})
       (map ->kebab-case-kw)))
