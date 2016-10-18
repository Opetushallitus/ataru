(ns ataru.virkailija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub-raw
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(re-frame/reg-sub-raw
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

