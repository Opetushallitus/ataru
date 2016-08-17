(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]))

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
        "FIN"
        (-> db
            (update-in [:application :answers] dissoc :birth-date)
            (update-in [:application :answers] assoc :ssn {:value nil :valid false})
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true))

        (_ :guard string?)
        (-> db
            (update-in [:application :answers] dissoc :ssn)
            (update-in [:application :answers] assoc :birth-date {:value nil :valid false})
            (update-in [:application :ui :ssn] assoc :visible? false)
            (update-in [:application :ui :birth-date] assoc :visible? true))
        :else (hide-both-fields))
      (hide-both-fields))))

(defn- select-gender-based-on-ssn
  [db _]
  (when (-> db :application :answers :ssn :valid)
    (let [ssn (-> db :application :answers :ssn :value)]
      (when-let [gender-sign (when (= (count ssn) 11) (nth ssn 9))]
        (when-let [gender (if (<= 0 gender-sign) (if (= 0 (mod gender-sign 2)) "female" "male"))]
          (update-in db [:application :answers] assoc :gender {:value gender :valid true}))))))

(defn- hakija-rule-to-fn [rule]
  (case rule
    :swap-ssn-birthdate-based-on-nationality
    swap-ssn-birthdate-based-on-nationality
    :select-gender-based-on-ssn
    select-gender-based-on-ssn
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
