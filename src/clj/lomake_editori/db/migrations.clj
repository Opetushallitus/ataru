(ns lomake-editori.db.migrations
  (:require [oph.soresu.common.db.migrations :as migrations])
  (:use [oph.soresu.common.config :only [config]]))

(defn migrate [ds-key & migration-paths]
  (apply (partial migrations/migrate ds-key) migration-paths))
