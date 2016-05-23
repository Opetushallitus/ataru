(ns manual-migrations
  (:require [oph.soresu.common.db.migrations :as migrations]))

(defn migrate
  [& _]
  (migrations/migrate :db "db.migration"))
