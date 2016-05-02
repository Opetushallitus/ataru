(ns lomake-editori.db.migrations
  (:require [com.stuartsierra.component :as component]
            [oph.soresu.common.db.migrations :as migrations])
  (:use [oph.soresu.common.config :only [config]]))

(defn ^:private migrate [ds-key & migration-paths]
  (apply (partial migrations/migrate ds-key) migration-paths))

(defrecord Migration []
  component/Lifecycle

  (start [component]
    (migrations/migrate :db "db.migration")
    component)

  (stop [component]
    component))

(defn new-migration
  []
  (->Migration))
