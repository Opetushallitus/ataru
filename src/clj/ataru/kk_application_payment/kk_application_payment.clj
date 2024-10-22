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
            [ataru.config.core :refer [config]]
            [clj-time.format :as time-format]
            [clj-time.core :as time]))

(def default-format (time-format/formatters :year-month-day))

(def kk-application-payment-origin "kkhakemusmaksu")
(def kk-application-payment-amount (get-in config [:form-payment-info :kk-processing-fee]))
(def kk-application-payment-due-days 7)

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
  {:not-required "not-required"
   :awaiting     "awaiting"
   :ok-by-proxy  "ok-by-proxy"
   :paid         "paid"
   :overdue      "overdue"})

(def all-reasons
  {:eu-citizen      "eu-citizen"
   :exemption-field "exemption-field"})

(defn get-due-date []
  (time-format/unparse default-format
                       (time/plus (time/now)
                                  (time/days kk-application-payment-due-days))))

; TODO finalize after big OK-687 changes
(defn payment-status-to-reference
  "Maksut payment references for hakemusmaksu are like 1.2.246.562.24.123456-kausi_s-2025"
  [{:keys [person-oid start-term start-year]}]
  (str/join "-" [person-oid start-term start-year]))

; TODO finalize after big OK-687 changes
(defn generate-invoicing-data
  [person term year]
  {:reference (payment-status-to-reference {:person-oid (:oid person)
                                            :start-term term
                                            :start-year year})
   :origin     kk-application-payment-origin
   :amount     kk-application-payment-amount
   :due-days   kk-application-payment-due-days
   :first-name (:first-name person)
   :last-name  (:last-name person)
   :email      ""                                              ; TODO: from application data
   })

(defn- validate-payment-data
  [{:keys [application-key state]}]
  (and (contains? (set (vals all-states)) state)
       (not-empty application-key)))

(defn- set-payment-state
  [{:keys [state application-key] :as payment-data}]
  (if (validate-payment-data payment-data)
    (let [payment (store/create-or-update-kk-application-payment! payment-data)]
      (log/info
        (str "Set payment state of application " application-key " to " state))
      payment)
    (throw (ex-info "Parameter validation failed while setting payment state"
                    {:application-key application-key :state state}))))

(defn get-raw-payments
  [application-keys]
  (if (not-empty application-keys)
    (store/get-kk-application-payments application-keys)
    []))

(defn get-raw-payment-history
  [application-keys]
  (if (not-empty application-keys)
    (store/get-kk-application-payments-history application-keys)
    []))

(defn- build-payment-data
  [state-data]
  (merge
    {:application-key      nil
     :state                nil
     :reason               nil
     :due-date             nil
     :total-sum            nil
     :maksut-secret        nil
     :required-at          nil
     :notification-sent-at nil
     :approved-at          nil}
    state-data))

; TODO SET MAKSUT SECRET

(defn set-application-fee-required
  "Sets kk processing fee required for the application."
  [application-key _]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:awaiting all-states)
                         :due-date       (get-due-date)
                         :total-sum      kk-application-payment-amount
                         :required-at    "now()"})))

(defn set-application-fee-not-required-for-eu-citizen
  "Sets kk processing fee not required for the application due to person being EU citizen."
  [application-key previous-state]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:not-required all-states)
                         :reason         (:eu-citizen all-reasons)
                         :required-at    (or (:required-at previous-state) "now()")
                         :approved-at    "now()"})))

(defn set-application-fee-not-required-for-exemption
  "Sets kk processing fee not required for the application due to exemption in application data."
  [application-key previous-state]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:not-required all-states)
                         :reason         (:exemption-field all-reasons)
                         :required-at    (or (:required-at previous-state) "now()")
                         :approved-at    "now()"})))

(defn set-application-fee-paid
  "Sets kk processing fee paid for the application."
  [application-key previous-state]
  (set-payment-state
    (build-payment-data {:application-key       application-key
                         :approved-at          "now()"
                         :state                (:paid all-states)
                         :reason               (:reason previous-state)
                         :due-date             (:due-date previous-state)
                         :total-sum            (:total-sum previous-state)
                         :maksut-secret        (:maksut-secret previous-state)
                         :required-at          (:required-at previous-state)
                         :notification-sent-at (:notification-sent-at previous-state)})))

(defn set-application-fee-ok-by-proxy
  "Sets kk processing fee paid via another application."
  [application-key previous-state]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:ok-by-proxy all-states)
                         :required-at    (or (:required-at previous-state) "now()")
                         :approved-at    "now()"})))

(defn set-application-fee-overdue
  "Sets kk processing fee overdue for the target term."
  [application-key previous-state]
  (set-payment-state
    (build-payment-data {:application-key       application-key
                         :state                (:overdue all-states)
                         :due-date             (:due-date previous-state)
                         :total-sum            (:total-sum previous-state)
                         :maksut-secret        (:maksut-secret previous-state)
                         :required-at          (:required-at previous-state)
                         :notification-sent-at (:notification-sent-at previous-state)})))

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

(defn- exemption-in-application?
  [application]
  (let [answers (util/application-answers-by-key application)]
    ; TODO: should we allow not answering the question at all in the end? Eg. for Finnish nationality.
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

(defn- get-valid-haku-oids
  [haku-cache tarjonta-service term year]
  (->> (get-haut-for-start-term-and-year haku-cache tarjonta-service term year)
       (filter (partial haku-valid-for-kk-payments? tarjonta-service))
       (map :oid)))

(defn- set-payment
  [new-state state-change-fn {:keys [application payment]}]
  (let [current-state   (:state payment)
        application-key (:key application)]
    (cond
      (= current-state new-state)
      (log/info "Application" application-key "already has kk payment status" current-state ", not changing state")

      (= current-state (:paid all-states))
      (log/info "Application" application-key "already has kk payment paid, not changing state")

      ; N.B. even if you pay an another application, if a previous application is overdue, the state must not change.
      (= current-state (:overdue all-states))
      (log/info "Application" application-key "is already overdue, not changing state")

      :else
      (state-change-fn (:key application) payment))))

(defn- update-payments-for-applications
  [applications-payments is-eu-citizen? has-exemption? has-existing-payment?]
  (let [map-payments (fn [new-state state-change-fn]
                       (doall
                         (remove nil? (map #(set-payment new-state state-change-fn %) applications-payments))))]
    (cond
      is-eu-citizen?        (map-payments (:not-required all-states) set-application-fee-not-required-for-eu-citizen)
      has-exemption?        (map-payments (:not-required all-states) set-application-fee-not-required-for-exemption)
      has-existing-payment? (map-payments (:ok-by-proxy all-states) set-application-fee-ok-by-proxy)
      :else                 (map-payments (:awaiting all-states) set-application-fee-required))))

(defn update-payments-for-person-term-and-year
  "- Determines and sets new payment status for all person's applications on given starting term
     according to their personal data, possible OID linkings and their applications.
   - Does not poll payments, they should be updated separately.
   - Does not send notification e-mails.
   Returns a vector of changed states of all applications for possible further processing."
  [person-service tarjonta-service koodisto-cache haku-cache person-oid term year]
  (let [valid-haku-oids (get-valid-haku-oids haku-cache tarjonta-service term year)
        linked-oids     (get (person-service/linked-oids person-service [person-oid]) person-oid)
        master-oid      (:master-oid linked-oids)
        person          (person-service/get-person person-service master-oid)
        aliases         (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))
        applications    (when (and (not-empty aliases) (not-empty valid-haku-oids))
                          (application-store/get-latest-applications-for-kk-payment-processing aliases valid-haku-oids))]
    (if (= 0 (count applications))
      []
      (let [payment-by-application (into {}
                                         (map (fn [payment] [(:application-key payment) payment]))
                                         (get-raw-payments (map :key applications)))
            applications-payments  (map (fn [application]
                                          {:application application
                                           :payment     (get payment-by-application (:key application))})
                                        applications)
            payment-state-set      (->> (vals payment-by-application) (map :state) set)
            is-eu-citizen?         (is-eu-citizen? koodisto-cache person)
            has-exemption?         (some true? (map exemption-in-application? applications))
            has-existing-payment?  (contains? payment-state-set (:paid all-states))]
        (update-payments-for-applications applications-payments is-eu-citizen? has-exemption? has-existing-payment?)))))

(defn get-kk-payment-state
  "Returns higher education application fee related info to single application.
  If return-payment-events is truthy, also returns specific processing events."
  [application return-payment-events]
  (let [key-filter [:application-key :state :reason :due-date :total-sum
                    :created-at :modified-at :required-at :reminder-sent-at :approved-at]
        payment (first (get-raw-payments [(:key application)]))
        history (when return-payment-events (get-raw-payment-history [(:key application)]))]
    (cond-> {}
            payment (assoc :payment
                           (select-keys payment key-filter))
            history (assoc :history
                           (map #(select-keys % key-filter)
                                history)))))

(defn get-kk-payment-states
  "Returns higher education application fee related info to application list."
  ([applications application-key-field]
   (let [application-keys   (map application-key-field applications)
         payments           (get-raw-payments application-keys)]
     (into {}
           (map #(vector (:application-key %) %) payments))))
  ([applications]
   (get-kk-payment-states applications :key)))

(defn remove-kk-applications-with-unapproved-payments
  "Filters out applications that have payment info but yet been approved (paid, exempted) for their respective admissions.
   Returns applications that either have no payment info or have been approved."
  [applications application-key-field]
  (let [payments (get-kk-payment-states applications application-key-field)
        approved-filter-fn (fn [application]
                             (let [payment (get payments (application-key-field application))]
                               (or
                                 (nil? payment)
                                 (some? (:approved-at payment)))))]
    (filter approved-filter-fn applications)))
