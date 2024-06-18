(ns ataru.kk-application-payment.kk-application-payment
  "Logic related to kk application processing payments, AKA hakemusmaksu. The basic spec is that
   non-exempt non-EU native applicants should be charged an application fee once per semester.
   NB! Semester is defined here by the start date of the actual first higher education semester,
   not the application date."
  (:require [ataru.kk-application-payment.kk-application-payment-store :as store]
            [taoensso.timbre :as log]))

(def application-payment-start-year
  "Application payments are charged from studies starting on 2025 or later."
  2025)

(def terms
  "Semesters / terms for kk application payments: one payment is required per starting term."
  #{:kausi_k :kausi_s})

(def payment-states
  #{:payment-not-required
    :payment-pending
    :payment-paid})

(def event-types
  #{:state-updated})

(defn- set-payment-state
  [person-oid term year new-state virkailija-oid message]
  (if (and (contains? terms (keyword term))
           (contains? payment-states (keyword new-state))
           (>= (Integer/parseInt year) application-payment-start-year))
    (let [state-id (:id (store/create-or-update-kk-application-payment-state!
                          person-oid term year new-state))
          _ (store/create-kk-application-payment-event!
              state-id new-state "state-updated" virkailija-oid message)]
      (log/info
        (str "Set payment state of person " person-oid " for term " term " " year " to " new-state))
      state-id)
    (throw (ex-info "Parameter validation failed while setting payment state"
                    {:person-oid person-oid :term term :year year :state new-state}))))

(defn get-payment-state
  [person-oid term year]
  (first (store/get-kk-application-payment-states
           [person-oid] term year)))

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
