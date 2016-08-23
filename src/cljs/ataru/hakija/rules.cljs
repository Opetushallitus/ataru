(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]
            [ataru.hakija.hakija-ajax :refer [get]]))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (let [nationality (-> db :application :answers :nationality)
        hide-both-fields #(-> db
                             (update-in [:application :answers] dissoc :birth-date)
                             (update-in [:application :answers] dissoc :ssn)
                             (update-in [:application :ui :birth-date] assoc :visible? false)
                             (update-in [:application :ui :ssn] assoc :visible? false))]
    (if-let [value (and (:valid nationality) (not-empty (:value nationality)))]
      (match value
        "Suomi"
        (-> db
            (update-in [:application :answers] dissoc :birth-date)
            (update-in [:application :answers] assoc :ssn {:value nil :valid false})
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true)
            (update-in [:application :answers] assoc :gender {:value "" :valid false})
            (update-in [:application :ui :gender] assoc :visible? false))
        (_ :guard string?)
        (-> db
            (update-in [:application :answers] dissoc :ssn)
            (update-in [:application :answers] assoc :birth-date {:value nil :valid false})
            (update-in [:application :ui :ssn] assoc :visible? false)
            (update-in [:application :ui :birth-date] assoc :visible? true)
            (update-in [:application :answers] assoc :gender {:value "" :valid false})
            (update-in [:application :ui :gender] assoc :visible? true))
        :else (hide-both-fields))
      (hide-both-fields))))

(defn- select-gender-based-on-ssn
  [db _]
  (if (-> db :application :answers :ssn :valid)
    (let [ssn (-> db :application :answers :ssn :value)]
      (when-let [gender-sign (nth ssn 9)]
        (when-let [gender (if (<= 0 gender-sign) (if (= 0 (mod gender-sign 2)) "Nainen" "Mies"))]
          (update-in db [:application :answers] assoc :gender {:value gender :valid true}))))
    (update-in db [:application :answers] assoc :gender {:value "" :valid false})))

(defn- select-postal-office-based-on-postal-code
  [db _]
  (if (-> db :application :answers :postal-code :valid)
    (let [postal-code (-> db :application :answers :postal-code :value)]
      (get
        (str "/hakemus/api/postal-codes/" postal-code)
        :application/handle-postal-code-input
        :application/handle-postal-code-error)
      db)
    (-> db
        (update-in [:application :answers] assoc :postal-office {:valid false :value ""})
        (update-in [:application :ui] dissoc :postal-office))))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :select-gender-based-on-ssn
    select-gender-based-on-ssn
    :select-postal-office-based-on-postal-code
    select-postal-office-based-on-postal-code
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
