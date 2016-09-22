(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]
            [ataru.hakija.application :refer [answers->valid-status
                                              wrapper-sections-with-validity]]))

(register-sub
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

(defn valid-status [db _]
  (reaction
    (answers->valid-status (-> @db :application :answers) (-> @db :application :ui))))

(register-sub
  :application/valid-status
  valid-status)

(defn wrapper-sections [db _]
  (reaction (wrapper-sections-with-validity
              (:wrapper-sections @db)
              (-> @db :application :answers))))

(register-sub
  :application/wrapper-sections
  wrapper-sections)

(defn- form-language [db _]
  (reaction
    (or
      (get-in @db [:form :selected-language])
      :fi))) ; When user lands on the page, there isn't any language set until the form is loaded

(register-sub
  :application/form-language
  form-language)
