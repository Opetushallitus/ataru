(ns ataru.virkailija.authentication.cas-ticketstore
  (:require [ataru.db.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/cas-ticketstore-queries.sql")

(defn login [ticket]
  (exec :db yesql-add-ticket-query! {:ticket ticket}))

(defn logout [ticket]
  (exec :db yesql-remove-ticket-query! {:ticket ticket}))

(defn logged-in? [ticket]
  (first (exec :db yesql-ticket-exists-query {:ticket ticket})))
