(ns lomake-editori.middleware.session-store
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [yesql.core :refer [defqueries]]
            [oph.soresu.common.db :refer [exec]])
  (:import (java.util UUID)))

(defqueries "sql/session-queries.sql")

(defn read-data [key]
  (:data (first  (exec :db get-session-query {:key key}))))

(defn add-data [key data]
  (exec :db add-session-query! {:key key :data data})
  key)

(defn save-data [key data]
  (exec :db update-session-query! {:key key :data data})
  key)

(defn delete-data [key]
  (exec :db delete-session-query! {:key key})
  key)

(defn generate-new-random-key [] (str (UUID/randomUUID)))

(deftype DatabaseStore []
    SessionStore
  (read-session [_ key]
    (read-data key))
  (write-session [_ key data]
    (if key
      (save-data key data)
      (add-data (generate-new-random-key) data)))
  (delete-session [_ key]
    (delete-data key)
    nil))

(defn create-store [] (DatabaseStore.))
