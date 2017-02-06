(ns ataru.hakija.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]
            [ataru.hakija.application :refer [answers->valid-status
                                              wrapper-sections-with-validity]]))

(re-frame/reg-sub-raw
  :state-query
  (fn [db [_ path]]
    (reaction (get-in @db path))))

(defn valid-status [db _]
  (reaction
    (answers->valid-status (-> @db :application :answers) (-> @db :application :ui))))

(re-frame/reg-sub-raw
  :application/valid-status
  valid-status)

(defn wrapper-sections [db _]
  (reaction (wrapper-sections-with-validity
              (:wrapper-sections @db)
              (-> @db :application :answers))))

(re-frame/reg-sub-raw
  :application/wrapper-sections
  wrapper-sections)

(defn- form-language [db _]
  (reaction
    (or
      (get-in @db [:form :selected-language])
      :fi))) ; When user lands on the page, there isn't any language set until the form is loaded

(re-frame/reg-sub-raw
  :application/form-language
  form-language)

(defn- default-language [db _]
  (-> @db
      (get-in [:form :languages])
      first
      reaction))

(re-frame/reg-sub-raw
  :application/default-language
  default-language)

(defn- adjacent-field-row-amount [db [_ field-descriptor]]
  (let [child-id   (-> (:children field-descriptor) first :id keyword)
        row-amount (-> (get-in db [:application :answers child-id :values] [])
                       count)]
    (if (= row-amount 0)
      1
      row-amount)))

(re-frame/reg-sub
  :application/adjacent-field-row-amount
  adjacent-field-row-amount)
