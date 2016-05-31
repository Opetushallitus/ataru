(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

(defn valid-status [db _]
  (reaction
    (let [application (:application @db)
          answer-validity (for [[_ answers] (:answers application)] (:valid answers))]
      {:valid (if (empty? answer-validity) false (every? true? answer-validity))})))

(register-sub
  :application/valid-status
  valid-status)
