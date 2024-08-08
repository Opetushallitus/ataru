(ns ataru.kk-application-payment.kk-application-payment
  "Logic related to kk application processing payments, AKA hakemusmaksu. The basic spec is that
   non-exempt non-EU native applicants should be charged an application fee once per semester.
   NB! Semester is defined here by the start date of the actual first higher education semester,
   not the application date."
  (:require [ataru.cache.cache-service :as cache]
            [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.applications.application-store :as application-store]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.util :as util]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.kk-application-payment.utils :as utils]))

; TODO: when the exact field is defined, make sure this is the final agreed id
; TODO: -> before the main feature branch gets merged to master
(def exemption-form-field-name
  "Unique id / field name for form field that indicates exemption from application fee"
  :vapautus_hakemusmaksusta)

; TODO: when the exact field is defined, check that these are correct
; TODO: -> before the main feature branch gets merged to master
(def exemption-field-ok-values
  "Any of these values should be considered as exemption to payment"
  #{"0" "1" "2" "3" "4" "5" "6"})

(def valid-payment-states
  #{:payment-not-required
    :payment-pending
    :payment-paid-for-linked-oid
    :payment-paid})

(defn- set-payment-state
  [person-oid term year new-state virkailija-oid message]
  (if (and (contains? valid-payment-states (keyword new-state))
           (utils/start-term-valid? term year))
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

(defn set-application-fee-paid-for-alias
  "Sets kk processing fee paid via another alias for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year "payment-paid-for-linked-oid" virkailija-oid message))

(defn- haku-valid?
  "Application payments are only collected for admissions starting on or after 1.1.2025
   and for hakus with specific properties."
  [tarjonta-service haku]
  (let [hakukohde-oids (or (->> (:hakukohteet haku)
                                (map #(:oid %))
                                (filter some?)
                                (not-empty))
                           (:hakukohteet haku))]
    (utils/requires-higher-education-application-fee? tarjonta-service haku hakukohde-oids)))

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
  (let [answers (util/application-answers-by-key application)]
    (if-let [exemption-answer (exemption-form-field-name answers)]
      (contains? exemption-field-ok-values (:value exemption-answer))
      (throw (ex-info "Missing exemption answer" {:application application})))))

(defn- get-haut-for-start-term-and-year
  "Get hakus according to study start term and year. Only for internal use, does not do authorization."
  [get-haut-cache tarjonta-service start-term start-year]
  (->> (cache/get-from get-haut-cache :haut)
       (map :haku)
       distinct
       (keep #(tarjonta/get-haku tarjonta-service %))
       (filter #(and (= start-year (:alkamisvuosi %))
                     (str/starts-with? (:alkamiskausi %) start-term)))))

(defn resolve-payment-status
  "Determines a single payment status out of the statuses attached to possible aliases of the person.
   Returns full state data."
  [person-service person-oid term year]
  (let [linked-oids (get (person-service/linked-oids person-service [person-oid]) person-oid)
        aliases (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))]
    (resolve-actual-payment-state (get-payment-states aliases term year))))

(defn update-payment-status
  "Infers and sets new payment status for person according to their personal data and applications for term.
  Does not poll payments. Returns state id."
  [person-service tarjonta-service koodisto-cache haku-cache person-oid term year virkailija-oid]
  (let [hakus (get-haut-for-start-term-and-year haku-cache tarjonta-service term year)
        valid-hakus (filter (partial haku-valid? tarjonta-service) hakus)
        valid-haku-oids (map :oid valid-hakus)
        linked-oids (get (person-service/linked-oids person-service [person-oid]) person-oid)
        master-oid (:master-oid linked-oids)
        aliases (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))
        original-state (first (get-payment-states [person-oid] term year))
        resolved-state (resolve-actual-payment-state (get-payment-states aliases term year))
        applications (when (and (not-empty aliases) (not-empty valid-haku-oids))
                       (application-store/get-latest-applications-for-kk-payment-processing aliases valid-haku-oids))]
    ; No need to do updates if the state wouldn't change, there are no applications or application fee already paid
    (println "Original state " original-state ", resolved state " resolved-state)
    (if (or
          (= 0 (count applications))
          (= (keyword (:state original-state)) :payment-paid))
      (:id original-state)
      (cond
        ; If a payment was made via linked oid, mark "paid by proxy" so the state can change if linking changes
        (= (keyword (:state resolved-state)) :payment-paid)
        (if (= (keyword (:state original-state)) :payment-paid)
          (:id original-state)
          (set-application-fee-paid-for-alias person-oid term year virkailija-oid nil))

        (is-eu-citizen? person-service koodisto-cache master-oid)
        (set-application-fee-not-required person-oid term year virkailija-oid nil)

        (some true? (map exemption-in-application? applications))
        (set-application-fee-not-required person-oid term year virkailija-oid nil)

        :else
        (set-application-fee-required person-oid term year virkailija-oid nil)))))

