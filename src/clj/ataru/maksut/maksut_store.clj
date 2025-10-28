(ns ataru.maksut.maksut-store
  (:require [ataru.db.db :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword ->snake_case_keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]])
  (:import (org.postgresql.util PSQLException)))

(defqueries "sql/maksut-queries.sql")

(declare yesql-get-payment-reminders)
(declare yesql-add-payment-reminder<!)
(declare yesql-set-reminder-handled!)

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))
(def ^:private ->snake-case-kw (partial transform-keys ->snake_case_keyword))

(def unique-violation "23505")

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn get-payment-reminders []
  (->>
    (exec-db :db yesql-get-payment-reminders {})
    (map ->kebab-case-kw)))

(defn set-reminder-handled [id status]
  (->> {:id id :status status}
    (exec-db :db yesql-set-reminder-handled!)
    (->kebab-case-kw)))

(defn set-reminder-handled-in-tx [connection id status]
  (-> {:id id :status status}
    (yesql-set-reminder-handled! {:connection connection})
    (->kebab-case-kw)))

(defn add-payment-reminder [reminder]
  (try
    (->>
      reminder
      (->snake-case-kw)
      (exec-db :db yesql-add-payment-reminder<!)
      (->kebab-case-kw))
    (catch PSQLException e
      (if (= unique-violation (.getSQLState e))
        (log/info "payment-reminder Unique Violation " reminder)
        (throw e)))))