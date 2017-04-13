(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]
            [ataru.hakija.hakija-ajax :as ajax]))

(def ^:private no-required-answer {:valid false :value ""})

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (let [nationality      (-> db :application :answers :nationality)
        hide-both-fields #(-> db
                              (update-in [:application :answers :birth-date] merge no-required-answer)
                              (update-in [:application :answers :ssn] merge no-required-answer)
                              (update-in [:application :ui :birth-date] assoc :visible? false)
                              (update-in [:application :ui :ssn] assoc :visible? false)
                              (update-in [:application :ui :have-finnish-ssn] assoc :visible? false)
                              (update-in [:application :ui :gender] assoc :visible? false))]
    (if-let [value (and (:valid nationality) (not-empty (:value nationality)))]
      (match value
        "246"
        (-> db
            (update-in [:application :answers :ssn] merge no-required-answer)
            (update-in [:application :answers :gender] merge no-required-answer)
            (update-in [:application :answers :birth-date] merge no-required-answer)
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true)
            (update-in [:application :ui :gender] assoc :visible? false)
            (update-in [:application :ui :have-finnish-ssn] assoc :visible? false))
        (_ :guard string?)
        (-> db
            (update-in [:application :answers :ssn] merge no-required-answer)
            (update-in [:application :answers :gender] merge no-required-answer)
            (update-in [:application :answers :birth-date] merge no-required-answer)
            (update-in [:application :answers :have-finnish-ssn] merge {:value "true"})
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true)
            (update-in [:application :ui :gender] assoc :visible? false)
            (update-in [:application :ui :have-finnish-ssn] assoc :visible? true))
        :else (hide-both-fields))
      (hide-both-fields))))

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

(defn- update-gender-and-birth-date-based-on-ssn
  [db _]

  (if (and
        (-> db :application :answers :ssn :valid)
        (not (clojure.string/blank? (-> db :application :answers :ssn :value))))
    (let [ssn (-> db :application :answers :ssn :value)
          birth-date (parse-birth-date-from-ssn ssn)
          lang (get-in db [:form :selected-language])]
      (when-let [gender-sign (js/parseInt (nth ssn 9))]
        (when-let [gender (if (<= 0 gender-sign) (if (= 0 (mod gender-sign 2))
                                                   "2" ;; based on koodisto-values
                                                   "1"))]
          (-> db
              (update-in [:application :answers :gender] merge {:value gender :valid true})
              (update-in [:application :answers :birth-date] merge {:value birth-date :valid true})))))
    (update-in db [:application :answers :gender] merge no-required-answer)))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (if (-> db :application :answers :postal-code :valid)
    (let [postal-code (-> db :application :answers :postal-code :value)]
      (when-not (clojure.string/blank? postal-code)
        (ajax/get
          (str "/hakemus/api/postal-codes/" postal-code)
          :application/handle-postal-code-input
          :application/handle-postal-code-error))
      db)
    (-> db
        (update-in [:application :answers :postal-office] merge no-required-answer)
        (update-in [:application :ui] dissoc :postal-office))))

(defn- toggle-ssn-based-fields
  [db _]
  (if (= "true" (-> db :application :answers :have-finnish-ssn :value))
    (-> db
        (assoc-in [:application :ui :ssn :visible?] true)
        (assoc-in [:application :ui :gender :visible?] false)
        (assoc-in [:application :ui :birth-date :visible?] false))
    (-> db
        (assoc-in [:application :ui :ssn :visible?] false)
        (assoc-in [:application :ui :gender :visible?] true)
        (assoc-in [:application :ui :birth-date :visible?] true)
        (update-in [:application :answers :ssn] merge {:value "" :valid true}))))

(defn- toggle-ssn-based-fields-for-existing-application
  [db _]
  (let [have-ssn?   (not (clojure.string/blank? (get-in db [:application :answers :ssn :value])))
        nationality (get-in db [:application :answers :nationality :value])
        secret      (get-in db [:application :secret])]
    (match [secret nationality]
      [(_ :guard nil?) _]
      db

      [_ "246"]
      (do
        (-> db
            (assoc-in [:application :ui :ssn :visible?] true)
            (assoc-in [:application :ui :gender :visible?] false)
            (assoc-in [:application :ui :birth-date :visible?] false)
            (assoc-in [:application :ui :have-finnish-ssn :visible?] false)
            (assoc-in [:application :answers :have-finnish-ssn :value] "true")))

      :else
      (do
        (-> db
            (assoc-in [:application :ui :ssn :visible?] have-ssn?)
            (assoc-in [:application :ui :gender :visible?] (not have-ssn?))
            (assoc-in [:application :ui :birth-date :visible?] (not have-ssn?))
            (assoc-in [:application :ui :have-finnish-ssn :visible?] true)
            (assoc-in [:application :answers :have-finnish-ssn :value] (str have-ssn?)))))))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :update-gender-and-birth-date-based-on-ssn
    update-gender-and-birth-date-based-on-ssn
    :select-postal-office-based-on-postal-code
    select-postal-office-based-on-postal-code
    :toggle-ssn-based-fields
    toggle-ssn-based-fields
    :toggle-ssn-based-fields-for-existing-application
    toggle-ssn-based-fields-for-existing-application
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
