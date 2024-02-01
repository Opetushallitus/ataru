(ns ataru.virkailija.application.tutu-payment.tutu-payment-subs
  (:require [ataru.virkailija.application.application-selectors :refer [get-tutu-form?
                                                                        get-tutu-payment-amount-input
                                                                        get-tutu-payment-note-input]]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(re-frame.core/reg-sub
 :tutu-payment/tutu-form?
 (fn [_ [_ key]]
   (get-tutu-form? key)))

(re-frame.core/reg-sub
 :tutu-payment/show-review-ui?
 (fn [db _]
   (let [current-form (get-in db [:application :selected-application-and-form :form :key])
         tutu-forms (string/split (aget js/config "tutu-payment-form-keys") #",")]
     (boolean
       (and
         (not-empty tutu-forms)
         (some #(= current-form %) tutu-forms))))))

(re-frame.core/reg-sub
 :tutu-payment/tutu-form-selected?
 (fn [db _]
   (let [selected-form (get-in db [:application :selected-form-key])
         tutu-forms (string/split (aget js/config "tutu-payment-form-keys") #",")]
     (boolean
       (and
         (not-empty tutu-forms)
         (some #(= selected-form %) tutu-forms))))))

(re-frame/reg-sub
 :tutu-payment/note-input
 (fn [db [_ application-key]]
   (get-tutu-payment-note-input db application-key)
 ))

(re-frame/reg-sub
  :tutu-payment/duedate-input
  (fn [db [_ application-key]]
    (or
      (get-in db [:tutu-payment :inputs application-key :due_date])
      (get-in db [:tutu-payment :applications application-key :decision :due_date])
      (let [date (time/from-now (time/days 14))]
        (format/unparse (format/formatters :date) date)))))

(re-frame/reg-sub
 :tutu-payment/amount-input
 (fn [db [_ application-key]]
  (get-tutu-payment-amount-input db application-key)))

(re-frame/reg-sub
 :tutu-payment/inputs-filled?
 (fn [[_ application-key]]
   [(re-frame/subscribe [:state-query [:tutu-payment :inputs application-key]])
    (re-frame/subscribe [:state-query [:tutu-payment :applications application-key :decision :amount]])
    (re-frame/subscribe [:tutu-payment/duedate-input])])
 (fn [[{:keys [note amount]} decision-amount due_date]]
   (let [amount (or amount decision-amount)]
     (and
        (string? amount)
        (some? (re-matches #"\d{1,5}([.]\d{1,2})?" amount))
        (string? note)
        (not (-> note string/trim string/blank?))
        (some? due_date)))))

(re-frame/reg-sub
 :tutu-payment/payments
 (fn [db [_ application-key]]
   (get-in db [:tutu-payment :applications application-key])))

