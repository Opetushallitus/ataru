(ns ataru.hakija.subs
  (:require [re-frame.core :as re-frame]
            [ataru.hakija.application :refer [answers->valid-status
                                              wrapper-sections-with-validity
                                              applying-possible?]]))

(re-frame/reg-sub
  :state-query
  (fn [db [_ path]]
    (get-in db path)))

(re-frame/reg-sub
  :application/valid-status
  (fn [db]
    (answers->valid-status (-> db :application :answers) (-> db :application :ui))))

(re-frame/reg-sub
  :application/wrapper-sections
  (fn [db]
    (wrapper-sections-with-validity
      (:wrapper-sections db)
      (-> db :application :answers))))

(re-frame/reg-sub
 :application/can-apply?
 (fn [db]
   (applying-possible? (:form db))))

(re-frame/reg-sub
  :application/form-language
  (fn [db]
    (or
      (get-in db [:form :selected-language])
      :fi))) ; When user lands on the page, there isn't any language set until the form is loaded)

(re-frame/reg-sub
  :application/default-language
  (fn [db]
    (-> db
        (get-in [:form :languages])
        first)))

(re-frame/reg-sub
  :application/adjacent-field-row-amount
  (fn [db [_ field-descriptor]]
    (let [child-id   (-> (:children field-descriptor) first :id keyword)
          row-amount (-> (get-in db [:application :answers child-id :values] [])
                         count)]
      (if (= row-amount 0)
        1
        row-amount))))
