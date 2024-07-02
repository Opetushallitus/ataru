(ns ataru.kk-application-payment.kk-application-payment
  "Logic related to kk application processing payments, AKA hakemusmaksu. The basic spec is that
   non-exempt non-EU native applicants should be charged an application fee once per semester.
   NB! Semester is defined here by the start date of the actual first higher education semester,
   not the application date."
  (:require [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.util :as util]
            [taoensso.timbre :as log]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(def exemption-form-field-name
  "Unique id / field name for form field that indicates exemption from application fee"
  ; TODO: field name and format TBD
  :field-name-here)

(def application-payment-start-year
  "Application payments are charged from studies starting in (Autumn) 2025 or later."
  2025)

(def first-application-payment-hakuaika-start
  "Application payments are only charged from admissions starting in 2025 or later"
  (time/date-time 2025 1 1))

(def valid-terms
  "Semesters / terms for kk application payments: one payment is required per starting term."
  #{:kausi_k :kausi_s})

(def valid-payment-states
  #{:payment-not-required
    :payment-pending
    :payment-paid})

(defn start-term-valid?
  "Payments are only charged starting from term Autumn 2025"
  [term year]
  (let [term-kw  (keyword term)]
    (or
      (and (contains? valid-terms term-kw) (> year application-payment-start-year))
      (and (= term-kw :kausi_s) (= year application-payment-start-year)))))

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

; TODO: where should we check this?
(defn- haku-valid?
  "Application payments are only collected for admissions starting on or after 1.1.2025"
  [haku]
  (when-let [hakuaika-start (some-> haku :hakuaika :start coerce/from-long)]
    (not (time/before? (coerce/from-long hakuaika-start)
                       first-application-payment-hakuaika-start))))

(defn- is-eu-citizen? [person-service koodisto-cache oid]
  (let [person (person-service/get-person person-service oid)
        eu-area (->> (koodisto/get-koodisto-options koodisto-cache "valtioryhmat" 1 false)
                     (filter #(= "EU" (:value %)))
                     (first))
        eu-country-codes (set (map :value (:within eu-area)))]
    (if (> (count eu-country-codes) 0)
      (some #(contains? eu-country-codes (:kansalaisuusKoodi %)) (:kansalaisuus person))
      (throw (ex-info "Could not fetch country codes for EU area" {:person-oid oid})))))

(defn- resolve-actual-payment-state
  "Resolves a single payment state from the states of possibly multiple aliases for single person.
   Chooses on the benefit of the applicant in case of multiple conflicting states."
  [states]
  (when (not-empty states)
    (let [state-set (->> states
                         (map :state)
                         (map keyword)
                         set)
          get-state-data (fn [field-name] (first
                                            (filter #(= field-name (keyword (:state %))) states)))]
      (cond
        (= 1 (count state-set))                     (first states)
        (contains? state-set :payment-paid)         (get-state-data :payment-paid)
        (contains? state-set :payment-not-required) (get-state-data :payment-not-required)
        :else                                       (get-state-data :payment-pending)))))

(defn- exemption-in-application?
  [application]
  (let [answers (-> application :content :answers util/answers-by-key)]
    (when (contains? answers exemption-form-field-name)
      ; TODO need to define field format before we can parse exemptions
      ())))

(defn- exempt-via-applications?
  "Returns true if there is an exemption reason attached to one or more of the applications for the term.
   Also returns true, in case the person has no applications."
  [person-oids term year]
  ; TODO get person's applications by starting term and year
  (let [applications ()]
    (some true? (map exemption-in-application? applications))))

(defn resolve-payment-status
  "Determines a single payment status out of the statuses attached to possible aliases of the person.
   Returns full state data."
  [person-service person-oid term year]
  (let [linked-oids (get (person-service/linked-oids person-service [person-oid]) person-oid)
        aliases (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))]
    (resolve-actual-payment-state (get-payment-states aliases term year))))

(defn update-payment-status
  "Infers and sets new payment status for person according to their personal data and applications for term.
  Does not poll payments, never updates status to paid. Returns state id."
  [person-service koodisto-cache person-oid term year virkailija-oid]
  (let [linked-oids (get (person-service/linked-oids person-service [person-oid]) person-oid)
        master-oid (:master-oid linked-oids)
        aliases (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))
        payment-state (resolve-actual-payment-state (get-payment-states aliases term year))]
    (cond
      ; Treat paid as terminal state that will not be modified automatically any more
      (= (keyword (:state payment-state)) :payment-paid)
      (:id payment-state)

      (is-eu-citizen? person-service koodisto-cache master-oid)
      (set-application-fee-not-required person-oid term year virkailija-oid nil)

      (exempt-via-applications? aliases term year)
      (set-application-fee-not-required person-oid term year virkailija-oid nil)

      :else
      (set-application-fee-required person-oid term year virkailija-oid nil))))
