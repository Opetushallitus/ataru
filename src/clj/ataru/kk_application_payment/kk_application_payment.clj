(ns ataru.kk-application-payment.kk-application-payment
  "Logic related to kk application processing payments, AKA hakemusmaksu. The basic spec is that
   non-exempt non-EU native applicants should be charged an application fee once per semester.
   NB! Semester is defined here by the start date of the actual first higher education semester,
   not the application date."
  (:require [ataru.kk-application-payment.kk-application-payment-store :as store]
            [taoensso.timbre :as log]))

; TODO: application payments should be only charged for hakus starting on or after 1.1.2025

(def application-payment-start-year
  "Application payments are charged from studies starting on 2025 or later."
  2025)

(def valid-terms
  "Semesters / terms for kk application payments: one payment is required per starting term."
  #{:kausi_k :kausi_s})

(def valid-payment-states
  #{:payment-not-required
    :payment-pending
    :payment-paid})

(def valid-event-types
  #{:state-updated})

(defn start-term-valid?
  "Payments are only ever charged from Autumn 2025 onwards"
  [term year]
  (let [term-kw  (keyword term)]
    (or
      (and (contains? valid-terms term-kw) (> year application-payment-start-year))
      (and (= term-kw :kausi_s) (= year application-payment-start-year) ))))

(defn- set-payment-state
  [person-oid term year new-state virkailija-oid message]
  (if (and (contains? valid-payment-states (keyword new-state))
           (start-term-valid? term year))
    (let [state-id (:id (store/create-or-update-kk-application-payment-state!
                          person-oid term year new-state))]
      (store/create-kk-application-payment-event! state-id new-state "state-updated" virkailija-oid message)
      (log/info
        (str "Set payment state of person " person-oid " for term " term " " year " to " new-state))
      state-id)
    (throw (ex-info "Parameter validation failed while setting payment state"
                    {:person-oid person-oid :term term :year year :state new-state}))))

(defn get-payment-states
  [person-oids term year]
  (store/get-kk-application-payment-states person-oids term year))

(defn get-payment-events
  [state-ids]
  (store/get-kk-application-payment-events state-ids))

(defn set-application-fee-required
  "Sets kk processing fee required for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year "payment-pending" virkailija-oid message))

(defn set-application-fee-not-required
  "Sets kk processing fee required for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year "payment-not-required" virkailija-oid message))

(defn set-application-fee-paid
  "Sets kk processing fee paid for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year "payment-paid" virkailija-oid message))
