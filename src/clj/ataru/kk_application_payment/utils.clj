(ns ataru.kk-application-payment.utils
  (:require [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clojure.string :as str]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(def application-payment-start-year
  "Application payments are charged from studies starting in (Autumn) 2025 or later."
  2025)

(def valid-terms
  "Semesters / terms for kk application payments: one payment is required per starting term."
  #{:kausi_k :kausi_s})

(defn start-term-valid?
  "Payments are only charged starting from term Autumn 2025"
  [term year]
  (when (and term year)
    ; Input term may be either non-versioned plain "kausi_s" or versioned "kausi_s#1", handle both
    (let [term-kw (keyword (first (str/split term #"#")))]
      (or
        (and (contains? valid-terms term-kw) (> year application-payment-start-year))
        (and (= term-kw :kausi_s) (= year application-payment-start-year))))))

(def first-application-payment-hakuaika-start
  "Application payments are only charged from admissions starting in 2025 or later"
  (time/date-time 2025 1 1))

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
