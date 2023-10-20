(ns ataru.cas-oppija.cas-oppija-session-store
  (:require
    [ataru.db.db :refer [exec exec-conn]]
    [taoensso.timbre :as log]
    [yesql.core :refer [defqueries]])
  (:import (java.util UUID)))

(defqueries "sql/oppija-session-queries.sql")

(declare yesql-add-oppija-session-query!)
(declare yesql-read-oppija-session-query)
(declare yesql-delete-oppija-session-by-ticket-query!)
(declare yesql-delete-oppija-session-query!)

(defn generate-new-random-key [] (str (UUID/randomUUID)))
(defn persist-session! [key ticket data]
  (log/info "Persisting session with key" key ", ticket" ticket ", data" data)
  (exec :db yesql-add-oppija-session-query! {:key key
                                             :ticket ticket
                                             :data data}))

(defn read-session [key]
  (log/info "Read session by key" key)
  (when key
    (if-let [full-session (first (exec-conn :db yesql-read-oppija-session-query {:key key}))]
      (assoc full-session :logged-in true)
      {:logged-in false})))

(defn delete-session-by-ticket! [ticket]
  (log/warn "Deleting session with ticket " ticket)
  (exec-conn :db yesql-delete-oppija-session-by-ticket-query! {:ticket ticket}))

(defn delete-session-by-key! [key]
  (log/warn "Deleting session by key " key)
  (exec-conn :db yesql-delete-oppija-session-query! {:key key}))