(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))
