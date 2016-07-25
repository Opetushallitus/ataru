(ns ataru.virkailija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame :refer [register-sub subscribe]]))

(register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(register-sub
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

(register-sub
  :flash
  (fn [db _]
    (reaction (first (get @db :flasher)))))

