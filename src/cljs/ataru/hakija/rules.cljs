(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]
            [ataru.hakija.hakija-ajax :as ajax]
            [ataru.hakija.application-validators :as validators]
            [ataru.preferred-name :as pn]
            [ataru.koodisto.koodisto-codes :refer [finland-country-code]]))

(def ^:private no-required-answer {:valid false :value ""})

(defn- set-empty-validity
  [a valid?]
  (if (and (clojure.string/blank? (:value a))
           (not (:cannot-view a)))
    (assoc a :valid valid?)
    a))

(defn- hide-field
  ([db id]
   (hide-field db id ""))
  ([db id value]
   (-> db
       (update-in [:application :answers id] merge {:valid true :value value})
       (assoc-in [:application :ui id :visible?] false))))

(defn- show-field
  ([db id]
   (show-field db id false))
  ([db id valid?]
   (-> db
       (update-in [:application :answers id] set-empty-validity valid?)
       (assoc-in [:application :ui id :visible?] true))))

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
          (hide-field db :have-finnish-ssn "true"))))

(defn- ssn
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (show-field db :ssn)
      (hide-field db :ssn))))

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
            (hide-field :birth-date birth-date)
            (hide-field :gender gender)))
      (-> db
          (show-field :birth-date)
          (show-field :gender)))))

(defn- passport-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :passport-number)
      (show-field db :passport-number true))))

(defn- national-id-number
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :national-id-number)
      (show-field db :national-id-number true))))

(defn- birthplace
  ^{:dependencies [:have-finnish-ssn]}
  [db]
  (let [have-finnish-ssn (get-in db [:application :answers :have-finnish-ssn :value])]
    (if (= "true" have-finnish-ssn)
      (hide-field db :birthplace)
      (show-field db :birthplace))))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender))

(defn- update-gender-and-birth-date-based-on-ssn
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender))

(defn- toggle-ssn-based-fields
  [db _]
  (-> db
      have-finnish-ssn
      ssn
      passport-number
      national-id-number
      birthplace
      birth-date-and-gender))

(defn- postal-office
  ^{:dependencies [:country-of-residence :postal-code]}
  [db]
  (let [answers (-> db :application :answers)
        country (-> answers :country-of-residence :value)
        is-finland? (or (= country finland-country-code)
                        (clojure.string/blank? country))
        postal-code (-> answers :postal-code)
        auto-input? (and is-finland?
                         (not (clojure.string/blank? (:value postal-code)))
                         (:valid postal-code))]
    (when auto-input?
      (ajax/get (str "/hakemus/api/postal-codes/" (:value postal-code))
                :application/handle-postal-code-input
                :application/handle-postal-code-error))
    (-> db
        (update-in [:application :answers :postal-office]
                   merge {:valid (not is-finland?) :value ""})
        (assoc-in [:application :ui :postal-office :visible?] is-finland?)
        (assoc-in [:application :ui :postal-office :disabled?] auto-input?))))

(defn- home-town-and-city
  ^{:dependencies [:country-of-residence]}
  [db]
  (let [country (get-in db [:application :answers :country-of-residence :value])
        is-finland? (or (= country finland-country-code)
                        (clojure.string/blank? country))]
    (if is-finland?
      (-> db
          (show-field :home-town)
          (hide-field :city))
      (-> db
          (hide-field :home-town)
          (show-field :city)))))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (postal-office db))

(defn- prefill-preferred-first-name
  [db _]
  (let [answers        (-> db :application :answers)
        first-name     (-> answers :first-name :value (clojure.string/split #" ") first)
        preferred-name (-> answers :preferred-name :value)]
    (cond
      (and first-name (clojure.string/blank? preferred-name))
      (update-in db [:application :answers :preferred-name] merge {:value first-name :valid true})

      (and first-name (not (clojure.string/blank? preferred-name)))
      (update-in db [:application :answers :preferred-name] merge {:valid (pn/main-first-name? preferred-name answers nil)})

      :else db)))

(defn- change-country-of-residence
  [db _]
  (-> db
      home-town-and-city
      postal-office))

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
