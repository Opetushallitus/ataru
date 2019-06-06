(ns ataru.middleware.session-store
  (:require [ring.middleware.session.store :refer [SessionStore]]
            [yesql.core :refer [defqueries]]
            [ataru.db.db :refer [exec]])
  (:import (java.util UUID)))

(defqueries "sql/session-queries.sql")

(defn read-data [key]
  (when-let [data (:data (first (exec :db yesql-get-session-query {:key key})))]
    (assoc data :key key)))

(defn add-data [key data]
  (exec :db yesql-add-session-query! {:key key :data (dissoc data :key)})
  key)

(defn save-data [key data]
  (exec :db yesql-update-session-query! {:key key :data (dissoc data :key)})
  key)

(defn delete-data [key]
  (exec :db yesql-delete-session-query! {:key key})
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
