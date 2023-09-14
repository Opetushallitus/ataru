(ns ataru.cas-oppija.cas-oppija-session-store
  (:require
    [ataru.db.db :refer [exec get-datasource]]
    [taoensso.timbre :as log]
    [yesql.core :refer [defqueries]])
  (:import (java.util UUID)))

(defqueries "sql/oppija-session-queries.sql")

(defn generate-new-random-key [] (str (UUID/randomUUID)))
(defn persist-session! [key ticket data]
  (log/info "Persisting session with key" key ", ticket" ticket ", data" data)
  (exec :db yesql-add-oppija-session-query! {:key key
                                             :ticket ticket
                                             :data data}))

(defn read-session [key]
  (log/info "Read session by key" key)
  (when key
    (first (exec :db yesql-read-oppija-session-query {:key key}))))

(defn delete-session! [ticket]
  (log/warn "Deleting session with ticket " ticket)
  (exec :db yesql-delete-oppija-session-by-ticket-query! {:ticket ticket}))