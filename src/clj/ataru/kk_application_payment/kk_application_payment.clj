(ns ataru.kk-application-payment.kk-application-payment
  "Logic related to kk application processing payments, AKA hakemusmaksu. The basic spec is that
   non-exempt non-EU native applicants should be charged an application fee once per semester.
   NB! Semester is defined here by the start date of the actual first higher education semester,
   not the application date."
  (:require [ataru.cache.cache-service :as cache]
            [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.applications.application-store :as application-store]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.util :as util]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.kk-application-payment.utils :as utils]
            [ataru.config.core :refer [config]]
            [clj-time.format :as time-format]
            [clj-time.core :as time]
            [ataru.component-data.kk-application-payment-module :as payment-module]))

(def default-time-format (time-format/formatters :year-month-day))

(def kk-application-payment-origin "kkhakemusmaksu")
(def kk-application-payment-order-id-prefix "KKHA")

(def kk-application-payment-amount (get-in config [:kk-application-payments :processing-fee]))
(def kk-application-payment-due-days 7)

(def exemption-form-field-name
  "Unique id / field name for form field that indicates exemption from application fee"
  :kk-application-payment-option)

(def exemption-field-ok-values
  "Any of these values should be considered as exemption to payment"
  (set (map val payment-module/kk-application-payment-document-exempt-options)))

(def all-states
  {:not-required "not-required"
   :awaiting     "awaiting"
   :ok-by-proxy  "ok-by-proxy"
   :paid         "paid"
   :overdue      "overdue"})

(def all-reasons
  {:eu-citizen      "eu-citizen"
   :exemption-field "exemption-field"})

(defn get-due-date-for-todays-payment []
  (time-format/unparse default-time-format
                       (time/plus (time/today-at 12 0 0)
                                  (time/days kk-application-payment-due-days))))

; This could be done automatically on every DB select, but needed so rarely that let's just convert on demand.
(defn parse-due-date
  "Convert due date retrieved from db to local date, interpreting it in correct time zone"
  [due-date]
  (let [due-date-local (time/to-time-zone due-date (time/time-zone-for-id "Europe/Helsinki"))]
    (time/local-date (time/year due-date-local) (time/month due-date-local) (time/day due-date-local))))

(defn maksut-reference->maksut-order-id
  "Maksut order id is in format KKHA1234 where 1234 is the unique component of application key/oid"
  [reference]
  (let [aid (last (str/split reference #"[.]"))]
    (str kk-application-payment-order-id-prefix aid)))

(defn payment->maksut-reference
  "Maksut payment references for hakemusmaksu correspond to application id, like 1.2.246.562.8.00000000000022225700"
  [{:keys [application-key]}]
  application-key)

(defn generate-invoicing-data
  [payment application]
  (let [get-field  (fn [key] (->> (:answers application)
                                  (filter #(= key (:key %)))
                                  (map :value)
                                  first))]
    {:reference  (payment->maksut-reference payment)
     :origin     kk-application-payment-origin
     :amount     (str kk-application-payment-amount)
     :due-days   kk-application-payment-due-days
     :first-name (get-field "first-name")
     :last-name  (get-field "last-name")
     :email      (get-field "email")}))

(defn- validate-payment-data
  [{:keys [application-key state]}]
  (and (contains? (set (vals all-states)) state)
       (not-empty application-key)))

(defn- set-payment-state
  [{:keys [state application-key] :as payment-data}]
  (when (get-in config [:kk-application-payments :enabled?])
    (if (validate-payment-data payment-data)
      (let [payment (store/create-or-update-kk-application-payment! payment-data)]
        (log/info
          (str "Set kk application payment state of application " application-key " to " state))
        payment)
      (throw (ex-info "Parameter validation failed while setting kk application payment state"
                      {:application-key application-key :state state})))))

(defn mark-reminder-sent
  [application-key]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [count (store/mark-reminder-sent! application-key)]
      (if (= count 1)
        (log/info (str "Set kk application payment reminder e-mail sent for application " application-key))
        (throw (ex-info "Could not set kk application payment reminder e-mail sent for application"
                        {:application-key application-key :updated-rows count}))))))

(defn set-maksut-secret
  [application-key maksut-secret]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [count (store/update-maksut-secret! application-key maksut-secret)]
      (if (= count 1)
        (log/info (str "Set kk application payment maksut secret for application " application-key))
        (throw (ex-info "Could not set maksut secret for kk application payment"
                        {:application-key application-key :maksut-secret maksut-secret :updated-rows count}))))))

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

(defn set-application-fee-required
  "Sets kk processing fee required for the application."
  [application-key _]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:awaiting all-states)
                         :due-date       (get-due-date-for-todays-payment)
                         :total-sum      kk-application-payment-amount
                         :required-at    "now()"})))

(defn- set-application-fee-not-required
  [application-key reason previous-state]
  (set-payment-state
    (build-payment-data {:application-key application-key
                         :state          (:not-required all-states)
                         :reason         reason
                         ; Let's not store required timestamp separately if the application is directly approved
                         :required-at    (:required-at previous-state)
                         :approved-at    "now()"})))

(defn set-application-fee-not-required-for-eu-citizen
  "Sets kk processing fee not required for the application due to person being EU citizen."
  [application-key previous-state]
  (set-application-fee-not-required application-key (:eu-citizen all-reasons) previous-state))

(defn set-application-fee-not-required-for-exemption
  "Sets kk processing fee not required for the application due to exemption in application data."
  [application-key previous-state]
  (set-application-fee-not-required application-key (:exemption-field all-reasons) previous-state))

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

; TODO: this may still be needed later for yksilöity EU citizen handling.
;(defn- is-eu-citizen? [koodisto-cache person]
;  (let [eu-area (->> (koodisto/get-koodisto-options koodisto-cache "valtioryhmat" 1 false)
;                     (filter #(= "EU" (:value %)))
;                     (first))
;        eu-country-codes (set (map :value (:within eu-area)))]
;    (if (> (count eu-country-codes) 0)
;      (some #(contains? eu-country-codes (:kansalaisuusKoodi %)) (:kansalaisuus person))
;      (throw (ex-info "Could not fetch country codes for EU area" {:person-oid (:oid person)})))))

(defn- is-finnish-citizen? [person]
  (some #(= "246" (:kansalaisuusKoodi %)) (:kansalaisuus person)))

(defn- exemption-in-application?
  [application]
  (let [answers (util/application-answers-by-key application)]
    (if-let [exemption-answer (exemption-form-field-name answers)]
      (contains? exemption-field-ok-values (:value exemption-answer))
      false)))

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

(defn filter-haut-for-update
  "filter haut that should have their kk payment status checked and updated at call time"
  [tarjonta-service hakus]
  (let [valid-hakus (filter (partial haku-valid-for-kk-payments? tarjonta-service) hakus)
        active-hakus (filter utils/haku-active-for-updating valid-hakus)]
    active-hakus))

(defn get-haut-for-update
  "Get hakus that should have their kk payment status checked and updated at call time."
  [get-haut-cache tarjonta-service]
  (let [hakus (get-haut-with-tarjonta-data get-haut-cache tarjonta-service)
        active-hakus (filter-haut-for-update tarjonta-service hakus)]
    (log/info "Found" (count active-hakus) "active hakus for kk payment status updates")
    active-hakus))

(defn get-valid-payment-info-for-application-id
  "Return person and term data for an application id when application's haku is valid for updates"
  [tarjonta-service application-id]
  (let [application        (application-store/get-application application-id)
        latest-application (application-store/get-latest-application-by-key (:key application))
        haku-oid           (:haku latest-application)
        person-oid         (:person-oid latest-application)
        haku               (when haku-oid (tarjonta/get-haku tarjonta-service haku-oid))
        valid-haku?        (if haku
                             (haku-valid-for-kk-payments? tarjonta-service haku)
                             false)]
    (when valid-haku?
      [person-oid (:alkamiskausi haku) (:alkamisvuosi haku)])))

(defn- get-valid-haku-oids
  [get-haut-cache tarjonta-service term year]
  (->> (get-haut-for-start-term-and-year get-haut-cache tarjonta-service term year)
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
  [applications-payments is-finnish-citizen? has-exemption? has-existing-payment?]
  (let [map-payments (fn [new-state state-change-fn]
                       (doall
                         (remove nil? (map #(set-payment new-state state-change-fn %) applications-payments))))]
    (cond
      is-finnish-citizen?   (map-payments (:not-required all-states) set-application-fee-not-required-for-eu-citizen)
      has-exemption?        (map-payments (:not-required all-states) set-application-fee-not-required-for-exemption)
      has-existing-payment? (map-payments (:ok-by-proxy all-states) set-application-fee-ok-by-proxy)
      :else                 (map-payments (:awaiting all-states) set-application-fee-required))))

(defn update-payments-for-person-term-and-year
  "- Determines and sets new payment status for all person's applications on given starting term
     according to their personal data, possible OID linkings and their applications.
   - Does not poll payments, they should be updated separately.
   - Does not send notification e-mails.
   Returns a vector of changed states of all applications for possible further processing."
  [person-service tarjonta-service _ get-haut-cache person-oid term year]
  (let [valid-haku-oids (get-valid-haku-oids get-haut-cache tarjonta-service term year)
        linked-oids     (get (person-service/linked-oids person-service [person-oid]) person-oid)
        master-oid      (:master-oid linked-oids)
        person          (person-service/get-person person-service master-oid)
        aliases         (into [] (conj (:linked-oids linked-oids) (:master-oid linked-oids) person-oid))
        applications    (when (and (not-empty aliases) (not-empty valid-haku-oids))
                          (application-store/get-latest-applications-for-kk-payment-processing aliases valid-haku-oids))]
    (when (get-in config [:kk-application-payments :enabled?])
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
              is-finnish-citizen?    (is-finnish-citizen? person)
              has-exemption?         (some true? (map exemption-in-application? applications))
              has-existing-payment?  (contains? payment-state-set (:paid all-states))]
          {:person            person
           :existing-payments applications-payments
           :modified-payments (update-payments-for-applications
                                applications-payments is-finnish-citizen? has-exemption? has-existing-payment?)})))))

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
