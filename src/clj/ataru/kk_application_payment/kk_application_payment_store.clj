(ns ataru.kk-application-payment.kk-application-payment-store
  (:require [ataru.db.db :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/kk-application-payment-queries.sql")

(declare yesql-get-kk-application-payment-states-for-person-oids)
(declare yesql-upsert-kk-application-payment-state<!)
(declare yesql-add-kk-application-payment-event<!)
(declare yesql-get-kk-application-payment-events)
(declare yesql-get-open-kk-application-payment-states)

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn create-or-update-kk-application-payment-state!
  [person-oid, start-term, start-year, state]
  (exec-db :db yesql-upsert-kk-application-payment-state<! {:person_oid person-oid
                                                            :start_term start-term
                                                            :start_year start-year
                                                            :state      state}))

(defn get-kk-application-payment-states
  [person-oids start-term start-year]
  (->> (exec-db :db yesql-get-kk-application-payment-states-for-person-oids {:person_oids person-oids
                                                                             :start_term  start-term
                                                                             :start_year  start-year})
       (map ->kebab-case-kw)))

(defn create-kk-application-payment-event!
  [payment-state-id, new-state, event-type, virkailija-oid, message]
  (exec-db :db yesql-add-kk-application-payment-event<! {:kk_application_payment_state_id payment-state-id
                                                         :new_state                       new-state
                                                         :event_type                      event-type
                                                         :virkailija_oid                  virkailija-oid
                                                         :message                         message}))

(defn get-kk-application-payment-events
  [payment-state-ids]
  (->> (exec-db :db yesql-get-kk-application-payment-events
                {:kk_application_payment_state_ids payment-state-ids})
       (map ->kebab-case-kw)))

(defn get-open-kk-application-payment-states
  []
  (->> (exec-db :db yesql-get-open-kk-application-payment-states {})
       (map ->kebab-case-kw)))