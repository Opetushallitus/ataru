(ns ataru.hakija.rules
  (:require [cljs.core.match :refer-macros [match]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn swap-ssn-birthdate-based-on-nationality
  [fields-to-swap db])

(defn- hakija-rule-to-fn [rule]
  (match rule
         :swap-ssn-birthdate-based-on-nationality
         swap-ssn-birthdate-based-on-nationality
         :else nil))

(defn run-rules
  ([rules db]
   (run-rules hakija-rule-to-fn rules db))
  ([rule-to-fn rules db]
   {:pre [(map? rules)
          (map? db)]}
   (reduce-kv
     (fn [db-accumulator rule argument]
       (if-let [rule-fn (rule-to-fn rule)]
         (rule-fn db-accumulator argument)
         db-accumulator))
     db
     rules)))
