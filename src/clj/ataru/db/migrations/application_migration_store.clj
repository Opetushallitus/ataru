(ns ataru.db.migrations.application-migration-store
  (:require [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :as t]
            [oph.soresu.common.db :as db]
            [yesql.core :as sql]))

(sql/defqueries "sql/migration-1.24-queries.sql")

(defn get-all-applications
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  []
  (mapv (partial t/transform-keys k/->kebab-case-keyword)
        (db/exec :db yesql-get-all-applications {})))

(defn set-application-key-to-application-review
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [review-id key]
  (db/exec :db yesql-set-application-key-to-application-review! {:application_key key :id review-id}))

(defn set-application-key-to-application-event
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [event-id key]
  (db/exec :db yesql-set-application-key-to-application-events! {:application_key key :id event-id}))

(defn get-application-confirmation-emails
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [application-id]
  (mapv (partial t/transform-keys k/->kebab-case-keyword)
        (db/exec :db yesql-get-application-confirmation-emails {:application_id application-id})))

(defn set-application-key-to-application-confirmation-email
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [confirmation-id key]
  (db/exec :db yesql-set-application-key-to-application-confirmation-emails! {:application_key key :id confirmation-id}))

(defn get-application-events-by-application-id
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [application-id]
  (mapv (partial t/transform-keys k/->kebab-case-keyword)
        (db/exec :db yesql-get-application-events-by-application-id {:application_id application-id})))

(defn get-application-review-by-application-id
  "Used by migration version 1.24 and should be removed after
   the migration has been run on production database."
  [application-id]
  (->> (db/exec :db yesql-get-application-review-by-application-id {:application_id application-id})
       (first)
       (t/transform-keys k/->kebab-case-keyword)))
