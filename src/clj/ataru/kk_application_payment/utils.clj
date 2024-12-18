(ns ataru.kk-application-payment.utils
  (:require [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [ataru.util :as util]
            [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]
            [ataru.translations.translation-util :as translations]
            [selmer.parser :as selmer]
            [ataru.component-data.kk-application-payment-module :refer [kk-application-payment-wrapper-key kk-application-payment-module]]
            [ataru.component-data.person-info-module :refer [person-info-module]]
            [ataru.config.core :refer [config]]))

(def payment-config
  (get config :kk-application-payments))

(def first-application-payment-hakuaika-start
  "Application payments are only charged from admissions starting on certain day"
  (time/from-time-zone (time/date-time
                         (get payment-config :start-year 2025)
                         (get payment-config :start-month 1)
                         (get payment-config :start-day 1))
                       (time/time-zone-for-id "Europe/Helsinki")))

(def haku-update-grace-days
  "Number of days payment statuses related to haku should still be checked and updated after hakuaika has ended"
  180)

(def application-payment-start-year
  "Application payments are charged from studies starting in (Autumn) 2025 or later."
  2025)

(def valid-terms
  "Semesters / terms for kk application payments: one payment is required per starting term."
  #{:kausi_k :kausi_s})

(defn payment-email [lang email data {:keys [template-path subject-key subject-suffix]}]
  (let [template-path    template-path
        translations     (translations/get-translations lang)
        emails           (list email)
        subject          (if subject-suffix
                           (str (subject-key translations) " " subject-suffix)
                           (subject-key translations))
        body             (selmer/render-file template-path
                                             (merge data translations))]
    (when (not-empty emails)
      {:from       "no-reply@opintopolku.fi"
       :recipients emails
       :subject    subject
       :body       body})))

(defn get-application-language
  [application]
  (-> application (get :lang "fi") keyword))

(defn get-application-email
  [application]
  (->> (get application :answers)
       (filter #(= (:key %) "email"))
       first
       :value))

(defn start-term-valid?
  "Payments are only charged starting from term Autumn 2025, check that given start is on or after that."
  [term year]
  (when (and term year)
    ; Input term may be either non-versioned plain "kausi_s" or versioned "kausi_s#1", handle both
    (let [term-kw (keyword (first (str/split term #"#")))]
      (or
        (and (contains? valid-terms term-kw) (> year application-payment-start-year))
        (and (= term-kw :kausi_s) (= year application-payment-start-year))))))

(defn time-is-before-some-hakuaika-grace-period?
  "Returns true if time 'now' is before specified grace days for one or more hakuaikas, for given haku"
  [haku grace-days now]
  (let [hakuajat-end                (if-let [hakuajat (:hakuajat haku)]
                                      (map :end hakuajat)
                                      [(coerce/from-long (get-in haku [:hakuaika :end]))])
        end-times-with-grace-period (map
                                      #(time/with-time-at-start-of-day
                                         (time/plus % (time/days grace-days)))
                                      hakuajat-end)]
    (boolean
      (some #(not (time/before? % now)) end-times-with-grace-period))))

(defn haku-active-for-updating
  "Check whether valid haku is recent enough that payments related to its applications may still need updating.
   Returns all hakus that have their last application end date max grace days before today."
  [haku]
  (time-is-before-some-hakuaika-grace-period?
    haku (+ haku-update-grace-days 1) (time/now)))

(defn requires-higher-education-application-fee?
  "Returns true if application fee should be charged for given haku"
  [tarjonta-service haku hakukohde-oids]
  (let [hakuajat-start (if-let [hakuajat (:hakuajat haku)]
                         (map :start hakuajat)
                         ; Support old tarjonta, although we shouldn't have to handle these (?) some tests still use it.
                         [(coerce/from-long (get-in haku [:hakuaika :start]))])
        studies-start-term (:alkamiskausi haku)
        studies-start-year (:alkamisvuosi haku)
        hakukohteet (tarjonta/get-hakukohteet
                      tarjonta-service
                      (remove nil? hakukohde-oids))]
    (and
      (boolean (start-term-valid? studies-start-term studies-start-year))
      (boolean (some #(not (time/before? % first-application-payment-hakuaika-start))
                     hakuajat-start))
      (boolean haku)
      (boolean hakukohteet)
      ; Kohdejoukko must be korkeakoulutus
      (and (string? (:kohdejoukko-uri haku))
           (str/starts-with? (:kohdejoukko-uri haku) "haunkohdejoukko_12#"))
      ; "Kohdejoukon tarkenne must be empty or siirtohaku
      (or (nil? (:kohdejoukon-tarkenne-uri haku))
          (str/starts-with? (:kohdejoukon-tarkenne-uri haku) "haunkohdejoukontarkenne_1#"))
      ; Must be tutkintoon johtava
      (boolean (some true? (map #(:tutkintoon-johtava? %) hakukohteet))))))

(defn has-payment-module? [form]
  (->> (:content form)
       (map :id)
       (some #(= kk-application-payment-wrapper-key %))
       boolean))

(defn inject-payment-module-to-form [form]
  (let [sections (:content form)
        update-fn (fn[section]
                    (if (= :person-info (keyword (:module section)))
                      (person-info-module :onr-kk-application-payment)
                      section))
        payment-section (kk-application-payment-module)
        index-of-person-info-module (util/first-index-of #(= :person-info (keyword %)) (map :module sections))
        index-to-insert (if (<= 0 index-of-person-info-module)
                          (+ index-of-person-info-module 1)
                          2)
        updated-content (map update-fn sections)
        ; lisätään maksumoduuli hakukohde ja henkilötieto-osioiden jälkeen:
        updated-content (concat (take index-to-insert updated-content) [payment-section] (drop index-to-insert updated-content))]
    (assoc form :content updated-content)))

(defn update-payment-module-in-form
  [form]
  (let [sections (:content form)
        payment-section (kk-application-payment-module)
        update-fn (fn[section]
                    (if (= (:id section) kk-application-payment-wrapper-key)
                      payment-section
                      section))
        updated-content (map update-fn sections)]
    (assoc form :content updated-content)))
