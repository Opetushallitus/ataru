(ns ataru.hakija.application-validators
  #?(:cljs (:require-macros [cljs.core.async.macros :as asyncm]))
  (:require [clojure.string]
            [ataru.email :as email]
            [ataru.ssn :as ssn]
            [ataru.translations.texts :as texts]
            [ataru.preferred-name :as pn]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]
            #?(:clj  [clojure.core.async :as async]
               :cljs [cljs.core.async :as async])
            #?(:clj  [clojure.core.async :as asyncm])
            #?(:clj  [clj-time.core :as c]
               :cljs [cljs-time.core :as c])
            #?(:clj  [clj-time.format :as f]
               :cljs [cljs-time.format :as f])
            #?(:clj  [clojure.core.match :refer [match]]
               :cljs [cljs.core.match :refer-macros [match]])))

(defn ^:private required?
  [{:keys [value]}]
  (if (or (seq? value) (vector? value))
    (not (empty? value))
    (not (clojure.string/blank? value))))

(defn- required-hakija?
  [{:keys [virkailija?] :as params}]
  (or (required? params)
      virkailija?))

(defn- residence-in-finland?
  [answers-by-key]
  (= (str finland-country-code)
     (str (-> answers-by-key :country-of-residence :value))))

(defn- have-finnish-ssn?
  [answers-by-key]
  (or (= "true" (get-in answers-by-key [:have-finnish-ssn :value]))
      (ssn/ssn? (get-in answers-by-key [:ssn :value]))))

(defn- ssn?
  [{:keys [has-applied value answers-by-key field-descriptor]}]
  (let [multiple?      (get-in field-descriptor
                               [:params :can-submit-multiple-applications]
                               true)
        haku-oid       (get-in field-descriptor
                               [:params :haku-oid])
        preferred-name (:preferred-name answers-by-key)
        original-value (get-in answers-by-key [(keyword (:id field-descriptor)) :original-value])
        modifying?     (some? original-value)]
    (asyncm/go
      (cond (not (ssn/ssn? value))
            [false []]
            (and (not multiple?)
                 (not (get-in answers-by-key [:ssn :cannot-view]))
                 (not (get-in answers-by-key [:ssn :cannot-modify]))
                 (not (and modifying? (= value original-value)))
                 (async/<! (has-applied haku-oid {:ssn value})))
            [false [(texts/ssn-applied-error (when (:valid preferred-name)
                                               (:value preferred-name)))]]
            :else
            [true []]))))

(defn- email?
  [{:keys [has-applied value answers-by-key field-descriptor]}]
  (let [multiple?      (get-in field-descriptor
                               [:params :can-submit-multiple-applications]
                               true)
        haku-oid       (get-in field-descriptor
                               [:params :haku-oid])
        preferred-name (:preferred-name answers-by-key)
        original-value (get-in answers-by-key [(keyword (:id field-descriptor)) :original-value])
        modifying?     (some? original-value)]
    (asyncm/go
      (cond (not (email/email? value))
            [false []]
            (and modifying? (= value original-value))
            [true []]
            (and (not multiple?)
                 (async/<! (has-applied haku-oid {:email value})))
            [false
             [((if modifying?
                 texts/email-applied-error-when-modifying
                 texts/email-applied-error) value (when (:valid preferred-name)
                                                    (:value preferred-name)))]]
            :else
            [true [(texts/email-check-correct-notification value)]]))))

(def ^:private postal-code-pattern #"^\d{5}$")

(defn ^:private postal-code?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (and (not (nil? value))
         (not (nil? (re-matches postal-code-pattern value))))
    (not (clojure.string/blank? value))))

(def ^:private whitespace-pattern #"\s*")
(def ^:private phone-pattern #"^\+?\d{4,}$")
(def ^:private finnish-date-pattern #"^\d{1,2}\.\d{1,2}\.\d{4}$")

(defn ^:private phone?
  [{:keys [value]}]
  (if-not (nil? value)
    (let [parsed (clojure.string/replace value whitespace-pattern "")]
      (not (nil? (re-matches phone-pattern parsed))))
    false))

#?(:clj
   (def parse-date
     (let [formatter (f/formatter "dd.MM.YYYY" (c/time-zone-for-id "Europe/Helsinki"))]
       (fn [d]
         (try
           (f/parse formatter d)
           (catch Exception _ nil)))))
   :cljs
   (def parse-date
     (let [formatter (f/formatter "d.M.YYYY")]
       (fn [d]
         ;; Unfortunately cljs-time.format allows
         ;; garbage data (e.g. XXXXX11Y11Z1997BLAH),
         ;; so we have to protect against that with a regexp
         (if (re-matches finnish-date-pattern d)
           (try (f/parse formatter d)
                (catch :default _ nil))
           nil)))))

(defn ^:private date?
  [value]
  (boolean
    (some->>
      value
      parse-date)))

(defn ^:private past-date?
  [{:keys [value]}]
  (boolean
    (and (date? value)
         (some-> (parse-date value)
                 (c/before? (c/today-at-midnight))))))

(defn- postal-office?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- birthplace?
  [{:keys [value answers-by-key]}]
  (if (have-finnish-ssn? answers-by-key)
    (clojure.string/blank? value)
    (not (clojure.string/blank? value))))

(defn- home-town?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    (not (clojure.string/blank? value))
    true))

(defn- city?
  [{:keys [value answers-by-key]}]
  (if (residence-in-finland? answers-by-key)
    true
    (not (clojure.string/blank? value))))

(defn- parse-value
  "Values in answers are a flat string collection when submitted, but a
  collection of maps beforehand (in front-end db) :("
  [value]
  (cond
    (every? string? value) value
    (every? map? value) (map :value value)))

(defn- hakukohteet?
  [{:keys [value field-descriptor]}]
  (let [hakukohde-options          (:options field-descriptor)
        num-answers                (count value)
        answers-subset-of-options? (clojure.set/subset? (set (parse-value value)) (set (map :value hakukohde-options)))]
    (if (pos? (count hakukohde-options))
      (if-let [max-hakukohteet (-> field-descriptor :params :max-hakukohteet)]
        (and (< 0 num-answers (inc max-hakukohteet)) answers-subset-of-options?)
        (and (pos? num-answers) answers-subset-of-options?))
      true)))

(def numeric-matcher #"[+-]?(0|[1-9][0-9]*)([,.][0-9]+)?")

(defn- numeric-value?
  [field-descriptor value]
  (if (clojure.string/blank? value)
    true
    (let [[_ integer-part decimal-part] (re-matches numeric-matcher value)
          decimal-places (-> field-descriptor :params :decimals)]
      (cond
        (not integer-part) false

        (and decimal-part
             (not decimal-places))
        false

        (and decimal-part
             (> (count decimal-part)
                (inc decimal-places)))                      ; inc to conside separator!
        false

        :else true))))

(defn- numeric?
  [{:keys [value field-descriptor]}]
  (if (sequential? value)
    (every? true? (map #(numeric? {:field-descriptor field-descriptor :value %}) value))
    (numeric-value? field-descriptor value)))

(def pure-validators {:required        required?
                      :required-hakija required-hakija?
                      :postal-code     postal-code?
                      :postal-office   postal-office?
                      :phone           phone?
                      :past-date       past-date?
                      :main-first-name pn/main-first-name?
                      :birthplace      birthplace?
                      :home-town       home-town?
                      :city            city?
                      :hakukohteet     hakukohteet?
                      :numeric         numeric?})

(def async-validators {:ssn ssn?
                       :email email?})

(defn validate
  [{:keys [validator] :as params}]
  (if-let [pure-validator ((keyword validator) pure-validators)]
    (let [valid? (pure-validator params)]
      (asyncm/go [valid? []]))
    (if-let [async-validator ((keyword validator) async-validators)]
      (async-validator params)
      (asyncm/go [false []]))))
