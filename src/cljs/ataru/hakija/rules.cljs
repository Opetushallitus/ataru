(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]))

(defn swap-ssn-birthdate-based-on-nationality
  [db _]
  (let [nationality (-> db :application :answers :nationality)]
    (when (:valid nationality)
      (match (:value nationality)
        "Suomi"
        (-> db
            (update-in [:application :answers :birth-date] dissoc :value)
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? true))

        (_ :guard string?)
        (-> db
            (update-in [:application :answers :ssn] dissoc :value)
            (update-in [:application :ui :ssn] assoc :visible? false)
            (update-in [:application :ui :birth-date] assoc :visible? true))

        nil
        (-> db
            (update-in [:application :answers :birth-date] dissoc :value)
            (update-in [:application :answers :ssn] dissoc :value)
            (update-in [:application :ui :birth-date] assoc :visible? false)
            (update-in [:application :ui :ssn] assoc :visible? false))))))

(defn- hakija-rule-to-fn [rule]
  (case rule
         :swap-ssn-birthdate-based-on-nationality
         swap-ssn-birthdate-based-on-nationality
         nil))

(defn run-rule
  ([rule db]
   (run-rule hakija-rule-to-fn rule db))
  ([rule-to-fn rule db]
   {:pre [(map? rule)
          (map? db)]}
   (reduce-kv
     (fn [db-accumulator rule argument]
       (if-let [rule-fn (rule-to-fn rule)]
         (rule-fn db-accumulator argument)
         db-accumulator))
     db
     rule)))
