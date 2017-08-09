(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]
            [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.application-validators :as validators]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]))

(def ^:private no-required-answer {:valid false :value ""})

(defn- set-empty-invalid
  [a]
  (if (and (clojure.string/blank? (:value a))
           (not (:cannot-view a)))
    (assoc a :valid false)
    a))

(defn- have-finnish-ssn
  ^{:dependencies [:nationality]}
  [db]
  (let [{:keys [valid value]} (get-in db [:application :answers :nationality])]
    (if (and valid (not-empty value) (not= value finland-country-code))
          (-> db
              (update-in [:application :answers :have-finnish-ssn]
                         (fn [a]
                           (if (= "" (:value a))
                             (merge a {:valid true :value "true"})
                             a)))
              (assoc-in [:application :ui :have-finnish-ssn :visible?] true))
          (-> db
              (update-in [:application :answers :have-finnish-ssn]
                         merge {:valid true :value "true"})
              (assoc-in [:application :ui :have-finnish-ssn :visible?] false)))))

(defn- ssn
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (-> db
          (update-in [:application :answers :ssn] set-empty-invalid)
          (assoc-in [:application :ui :ssn :visible?] true))
      (-> db
          (update-in [:application :answers :ssn] merge {:valid true :value ""})
          (assoc-in [:application :ui :ssn :visible?] false)))))

(defn- parse-birth-date-from-ssn
  [ssn]
  (let [century-sign (nth ssn 6)
        day (subs ssn 0 2)
        month (subs ssn 2 4)
        year (subs ssn 4 6)
        century (case century-sign
                  "+" "18"
                  "-" "19"
                  "A" "20")]
    (str day "." month "." century year)))

(defn- parse-gender-from-ssn
  [ssn]
  (if (zero? (mod (js/parseInt (nth ssn 9)) 2))
    "2" ;; based on koodisto-values
    "1"))

(defn- birth-date-and-gender
  ^{:dependencies [:have-finnish-ssn :ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])
        ssn (get-in db [:application :answers :ssn])]
    (if (= "true" have-finnish-ssn)
      (let [[birth-date gender] (cond (and (:valid ssn)
                                           (not-empty (:value ssn)))
                                      [(parse-birth-date-from-ssn (:value ssn))
                                       (parse-gender-from-ssn (:value ssn))]
                                      (:cannot-view ssn)
                                      [(get-in db [:application :answers :birth-date :value])
                                       (get-in db [:application :answers :gender :value])]
                                      :else
                                      ["" ""])]
        (-> db
            (update-in [:application :answers :birth-date]
                       merge {:valid true :value birth-date})
            (update-in [:application :answers :gender]
                       merge {:valid true :value gender})
            (assoc-in [:application :ui :birth-date :visible?] false)
            (assoc-in [:application :ui :gender :visible?] false)))
      (-> db
          (update-in [:application :answers :birth-date] set-empty-invalid)
          (update-in [:application :answers :gender] set-empty-invalid)
          (assoc-in [:application :ui :birth-date :visible?] true)
          (assoc-in [:application :ui :gender :visible?] true)))))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      birth-date-and-gender))

(defn- update-gender-and-birth-date-based-on-ssn
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      birth-date-and-gender))

(defn- toggle-ssn-based-fields
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      birth-date-and-gender))

(defn- postal-office
  ^{:dependencies [:country-of-residence :postal-code]}
  [db]
  (let [answers (-> db :application :answers)
        country (-> answers :country-of-residence :value)
        is-finland? (or (= country finland-country-code)
                        (clojure.string/blank? country))
        postal-code (-> answers :postal-code)]
    (when (and is-finland? (:valid postal-code))
      (ajax/get (str "/hakemus/api/postal-codes/" (:value postal-code))
                      :application/handle-postal-code-input
                      :application/handle-postal-code-error))
    (-> db
        (update-in [:application :answers :postal-office]
                   merge {:valid (not is-finland?) :value ""})
        (assoc-in [:application :ui :postal-office :visible?] is-finland?)
        (assoc-in [:application :ui :postal-office :disabled?]
                  (and is-finland? (:valid postal-code))))))

(defn- home-town-and-city
  ^{:dependencies [:country-of-residence]}
  [db]
  (let [country (get-in db [:application :answers :country-of-residence :value])
        is-finland? (or (= country finland-country-code)
                        (clojure.string/blank? country))]
    (if is-finland?
      (-> db
          (update-in [:application :answers :home-town] set-empty-invalid)
          (update-in [:application :answers :city]
                     merge {:valid true :value ""})
          (assoc-in [:application :ui :home-town :visible?] true)
          (assoc-in [:application :ui :city :visible?] false))
      (-> db
          (update-in [:application :answers :home-town]
                     merge {:valid true :value ""})
          (update-in [:application :answers :city] set-empty-invalid)
          (assoc-in [:application :ui :home-town :visible?] false)
          (assoc-in [:application :ui :city :visible?] true)))))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (postal-office db))

(defn- prefill-preferred-first-name
  [db _]
  (let [answers          (-> db :application :answers)
        first-names      (-> answers :first-name :value)
        main-first-name  (-> answers :preferred-name :value)
        first-first-name (first (clojure.string/split first-names #" "))]
    (if (and
          first-first-name
          (clojure.string/blank? main-first-name))
      (update-in db [:application :answers :preferred-name] merge {:value first-first-name :valid true})
      db)))

(defn- change-country-of-residence
  [db _]
  (-> db
      home-town-and-city
      postal-office))

(defn- set-visibility-based-on-hakukohde
  [db [hakukohde-oid visible?]]
  (reduce-kv (fn [db answer-key answer]
               (cond-> db
                 (some #{hakukohde-oid} (:belongs-to-hakukohteet answer))
                 (assoc-in [:application :ui answer-key :visible?] visible?)))
             db
             (get-in db [:application :answers])))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :prefill-preferred-first-name
    prefill-preferred-first-name
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :update-gender-and-birth-date-based-on-ssn
    update-gender-and-birth-date-based-on-ssn
    :select-postal-office-based-on-postal-code
    select-postal-office-based-on-postal-code
    :toggle-ssn-based-fields
    toggle-ssn-based-fields
    :change-country-of-residence
    change-country-of-residence
    :set-visibility-based-on-hakukohde
    set-visibility-based-on-hakukohde
    nil))

(defn extract-rules [content]
  (->> (for [field content]
         (if-let [children (:children field)]
           (extract-rules children)
           (:rules field)))
       flatten
       (filter not-empty)
       vec))

(defn run-rule
  ([rule db]
   (run-rule hakija-rule-to-fn rule db))
  ([rule-to-fn rule db]
   {:pre [(map? rule)
          (map? db)]}
   (reduce-kv
     (fn [db-accumulator rule argument]
       (if-let [rule-fn (rule-to-fn rule)]
         (or (rule-fn db-accumulator argument)
             db-accumulator)
         db-accumulator))
     db
     rule)))

(defn run-all-rules [db]
  (reduce
    (fn [db-accumulator rule]
      (run-rule rule db-accumulator))
    db
    (extract-rules (-> db :form :content))))
