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
            [ataru.kk-application-payment.utils :as utils]
            [ataru.config.core :refer [config]]))

(def kk-application-payment-origin "kkhakemusmaksu")

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

(def all-states
  {:not-required       "payment-not-required"
   :awaiting           "awaiting-payment"
   :ok-via-linked-oid  "payment-ok-via-linked-oid"
   :paid               "payment-paid"
   :overdue            "payment-overdue"})

(def all-event-types
      {:updated "state-updated"})

(def ok-states
  #{nil
    (:not-required      all-states)
    (:ok-via-linked-oid all-states)
    (:paid              all-states)})

; TODO finalize after big OK-687 changes
(defn payment-status-to-reference
  "Maksut payment references for hakemusmaksu are like 1.2.246.562.24.123456-kausi_s-2025"
  [{:keys [person-oid start-term start-year]}]
  (str/join "-" [person-oid start-term start-year]))

; TODO finalize after big OK-687 changes
(defn generate-invoicing-data
  [person term year]
  (let [amount (get-in config [:form-payment-info :kk-processing-fee])]
    {:reference (payment-status-to-reference {:person-oid (:oid person)
                                              :start-term term
                                              :start-year year})
     :origin kk-application-payment-origin
     :due-date ""                                           ; TODO
     :first-name (:first-name person)
     :last-name (:last-name person)
     :email ""                                              ; TODO: from application data
     :amount amount}))

(defn- set-payment-state
  [person-oid term year new-state virkailija-oid message]
  (if (and (contains? (set (vals all-states)) new-state)
           (utils/start-term-valid? term year))
    (let [state-id (:id (store/create-or-update-kk-application-payment-state!
                          person-oid term year new-state))]
      (store/create-kk-application-payment-event! state-id new-state (:updated all-event-types) virkailija-oid message)
      (log/info
        (str "Set payment state of person " person-oid " for term " term " " year " to " new-state))
      state-id)
    (throw (ex-info "Parameter validation failed while setting payment state"
                    {:person-oid person-oid :term term :year year :state new-state}))))

(defn get-raw-payment-states
  [person-oids term year]
  (if (and (not-empty person-oids) term year)
    (let [simplified-term (first (str/split term #"#"))]
      (store/get-kk-application-payment-states person-oids simplified-term year))
    []))

(defn get-raw-payment-events
  [state-ids]
  (store/get-kk-application-payment-events state-ids))

(defn set-application-fee-required
  "Sets kk processing fee required for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year (:awaiting all-states) virkailija-oid message))

(defn set-application-fee-not-required
  "Sets kk processing fee required for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year (:not-required all-states) virkailija-oid message))

(defn set-application-fee-paid
  "Sets kk processing fee paid for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year (:paid all-states) virkailija-oid message))

(defn set-application-fee-ok-via-linked-oid
  "Sets kk processing fee paid or exempt via another alias for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year (:ok-via-linked-oid all-states) virkailija-oid message))

(defn set-application-fee-overdue
  "Sets kk processing fee overdue for the target term."
  [person-oid term year virkailija-oid message]
  (set-payment-state person-oid term year (:overdue all-states) virkailija-oid message))

(defn- haku-valid-for-kk-payments?
  "Application payments are only collected for admissions starting on or after 1.1.2025
   and for hakus with specific properties."
  [tarjonta-service haku]
  (let [hakukohde-oids (or (->> (:hakukohteet haku)
                                (map #(:oid %))
                                (filter some?)
                                (not-empty))
                           (:hakukohteet haku))]
    (utils/requires-higher-education-application-fee? tarjonta-service haku hakukohde-oids)))

(defn- is-eu-citizen? [koodisto-cache person]
  (let [eu-area (->> (koodisto/get-koodisto-options koodisto-cache "valtioryhmat" 1 false)
                     (filter #(= "EU" (:value %)))
                     (first))
        eu-country-codes (set (map :value (:within eu-area)))]
    (if (> (count eu-country-codes) 0)
      ; TODO: should this actually be (:nationality person) as opposed to test data? Check!
      (some #(contains? eu-country-codes (:kansalaisuusKoodi %)) (:kansalaisuus person))
      (throw (ex-info "Could not fetch country codes for EU area" {:person-oid (:oid person)})))))

(defn- resolve-actual-payment-state
  "Resolves a single payment state from the states of possibly multiple aliases for single person.
   Chooses on the benefit of the applicant in case of multiple conflicting states."
  [states]
  (when (not-empty states)
    (let [state-set (->> states
                         (map :state)
                         set)
          get-state-data (fn [field-name] (first
                                            (filter #(= field-name (:state %)) states)))]
      (cond
        (= 1 (count state-set))                          (first states)
        (contains? state-set (:paid all-states))         (get-state-data (:paid all-states))
        (contains? state-set (:not-required all-states)) (get-state-data (:not-required all-states))
        (contains? state-set (:awaiting all-states))     (get-state-data (:awaiting all-states))
        :else                                            (get-state-data (:overdue all-states))))))

(defn- exemption-in-application?
  [application]
  (let [answers (util/application-answers-by-key application)]
    (if-let [exemption-answer (exemption-form-field-name answers)]
      (contains? exemption-field-ok-values (:value exemption-answer))
      (throw (ex-info "Missing exemption answer" {:application application})))))

(defn- get-haut-with-tarjonta-data
  [get-haut-cache tarjonta-service]
  (->> (cache/get-from get-haut-cache :haut)
       (map :haku)
       distinct
       (keep #(tarjonta/get-haku tarjonta-service %))))

(defn- get-haut-for-start-term-and-year
  "Get hakus according to study start term and year. Only for internal use, does not do authorization."
  [get-haut-cache tarjonta-service start-term start-year]
  (->> (get-haut-with-tarjonta-data get-haut-cache tarjonta-service)
       (filter #(and (= start-year (:alkamisvuosi %))
                     (str/starts-with? (:alkamiskausi %) start-term)))))

(defn get-haut-for-update
  "Get hakus that should have their kk payment status checked and updated at call time."
  [get-haut-cache tarjonta-service]
  (let [hakus (get-haut-with-tarjonta-data get-haut-cache tarjonta-service)
        valid-hakus (filter (partial haku-valid-for-kk-payments? tarjonta-service) hakus)
        active-hakus (filter utils/haku-active-for-updating valid-hakus)]
    (log/info "Found" (count active-hakus) "active hakus for kk payment status updates")
    active-hakus))

(defn update-payment-status
  "- Infers and sets new payment status for person according to their personal data, possible OID linkings
     and applications for term.
   - Does not poll payments, they should be updated separately.
   - Does not send notification e-mails, so please use via eg. status updater job.
   - Returns a map with state id and old + new state names."
  [person-service tarjonta-service koodisto-cache haku-cache person-oid term year virkailija-oid]
  (let [hakus           (get-haut-for-start-term-and-year haku-cache tarjonta-service term year)
        valid-hakus     (filter (partial haku-valid-for-kk-payments? tarjonta-service) hakus)
        valid-haku-oids (map :oid valid-hakus)
        linked-oids     (get (person-service/linked-oids person-service [person-oid]) person-oid)
        master-oid      (:master-oid linked-oids)
        person          (person-service/get-person person-service master-oid)
        aliases         (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))
        original-state  (first (get-raw-payment-states [person-oid] term year))
        resolved-state  (resolve-actual-payment-state (get-raw-payment-states aliases term year))
        applications    (when (and (not-empty aliases) (not-empty valid-haku-oids))
                          (application-store/get-latest-applications-for-kk-payment-processing aliases valid-haku-oids))]
      (cond
        ; No need to do updates if there are no applications or application fee already paid
        (= 0 (count applications))
        {:id (:id original-state)
         :old-state (:state original-state)
         :new-state (:state original-state)
         :person person}

        (= (:state original-state) (:paid all-states))
        {:id (:id original-state)
         :old-state (:state original-state)
         :new-state (:state original-state)
         :person person}

        ; If a payment was made via linked oid, use separate state that can change if linking changes
        (= (:state resolved-state) (:paid all-states))
        (if (= (:state original-state) (:state resolved-state))
          {:id (:id original-state)
           :old-state (:state original-state)
           :new-state (:state original-state)
           :person person}
          {:id (set-application-fee-ok-via-linked-oid person-oid term year virkailija-oid
                                                      (str "Linked OID: " (:person-oid resolved-state)))
           :old-state (:state original-state)
           :new-state (:ok-via-linked-oid all-states)
           :person person})

        ; EU citizens get a free pass
        (is-eu-citizen? koodisto-cache person)
        {:id (set-application-fee-not-required person-oid term year virkailija-oid nil)
         :old-state (:state original-state)
         :new-state (:not-required all-states)
         :person person}

        ; If any of the linked OIDs has an exemption in application, all of them are naturally exempt.
        (some true? (map exemption-in-application? applications))
        {:id (set-application-fee-not-required person-oid term year virkailija-oid nil)
         :old-state (:state original-state)
         :new-state (:not-required all-states)
         :person person}

        :else
        {:id (set-application-fee-required person-oid term year virkailija-oid nil)
         :old-state (:state original-state)
         :new-state (:awaiting all-states)
         :person person})))

(defn get-kk-payment-states
  "Returns higher education application fee related info to application list belonging to same haku."
  ([applications tarjonta person-oid-key]
   (let [person-oids        (keep person-oid-key applications)
         studies-start-term (:alkamiskausi tarjonta)
         studies-start-year (:alkamisvuosi tarjonta)
         payment-states     (when (and person-oids studies-start-term studies-start-year)
                              (get-raw-payment-states
                                person-oids studies-start-term studies-start-year))]
     (into {}
           (map #(vector (:person-oid %) %) payment-states))))
  ([applications tarjonta]
   (get-kk-payment-states applications tarjonta :person-oid)))

(defn get-kk-payment-state
  "Returns higher education application fee related info to single application.
  If return-payment-events is truthy, also returns specific processing events."
  [application tarjonta-data return-payment-events]
  (if-let [person-oid (:person-oid application)]
    (let [payment-status (when (and application tarjonta-data)
                           (get (get-kk-payment-states [application] (:tarjonta tarjonta-data)) person-oid))
          payment-events (when (and payment-status return-payment-events)
                           (get-raw-payment-events (:id payment-status)))]
      (cond-> {}
              payment-status (assoc :status
                                    (select-keys payment-status [:person-oid :start-term :start-year :state :created-time]))
              payment-events (assoc :events
                                    (map #(select-keys % [:new-state :event-type :virkailija-oid :message :created-time])
                                         payment-events))))
    {}))

(defn- filter-kk-haku-applications-by-state
  [applications haku person-oid-key filter-states]
  (let [payment-states-by-person-oid (get-kk-payment-states applications haku person-oid-key)]
    (filter (fn [application]
              (let [kk-payment-state (get-in payment-states-by-person-oid
                                             [(person-oid-key application) :state])]
                (contains? filter-states kk-payment-state)))
            applications)))

; TODO: requires-higher-education-application-fee filtering?
(defn filter-application-list-by-kk-payment-state
  "Filters an application list by kk payment state. Add nil to states-to-keep if you want to also include applications
   with no payment data. Applications should include haku oid and person oid in the fields with
   respective parametrized names."
  [tarjonta-service applications person-oid-key haku-oid-key states-to-keep]
  (let [applications-with-person-oid-and-haku (filter
                                                #(and (some? (person-oid-key %)) (some? (haku-oid-key %)))
                                                applications)
        remaining-applications (remove
                                 #(and (some? (person-oid-key %)) (some? (haku-oid-key %)))
                                 applications)
        applications-by-haku (group-by haku-oid-key applications-with-person-oid-and-haku)
        haku-oids (keys applications-by-haku)
        hakus-by-oid (into {}
                           (map #(vector % (tarjonta/get-haku tarjonta-service %)) haku-oids))]
    (->> haku-oids
         (map #(filter-kk-haku-applications-by-state
                 (get applications-by-haku %) (get hakus-by-oid %)
                 person-oid-key states-to-keep))
         flatten
         (concat remaining-applications))))

(defn filter-out-unpaid-kk-applications
  "Filters out applications by persons that have not yet paid a mandatory fee for the application haku's starting period.
   Applications should include haku oid and person oid in the fields with respective parametrized names."
  [tarjonta-service applications person-oid-key haku-oid-key]
  (filter-application-list-by-kk-payment-state tarjonta-service applications
                                               person-oid-key haku-oid-key
                                               ok-states))