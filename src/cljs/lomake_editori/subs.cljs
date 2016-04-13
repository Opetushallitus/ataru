(ns lomake-editori.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame :refer [register-sub]]))

(register-sub
 :active-panel
 (fn [db _]
   (reaction (:active-panel @db))))

(register-sub
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

