(ns ataru.virkailija.authentication.cas-ticketstore
  (:require [oph.soresu.common.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/cas-ticketstore-queries.sql")

(defn login [ticket]
  (exec :db add-ticket-query! {:ticket ticket}))

(defn logout [ticket]
  (exec :db remove-ticket-query! {:ticket ticket}))

(defn logged-in? [ticket]
  (first (exec :db ticket-exists-query {:ticket ticket})))
