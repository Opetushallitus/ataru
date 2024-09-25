(ns ataru.virkailija.application.payment.payment-subs
  (:require [ataru.virkailija.application.application-selectors :refer [get-tutu-form?
                                                                        get-payment-amount-input
                                                                        get-payment-note-input]]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [clojure.string :as string]
            [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :payment/tutu-form?
  (fn [[_ key]]
   [(re-frame/subscribe [:application/form key])])
  (fn [[form]]
   (or
     (= "payment-type-tutu" (get-in form [:properties :payment :type]))
     (get-tutu-form? (:key form)))))

(re-frame/reg-sub
  :payment/astu-form?
  (fn [[_ key]]
    [(re-frame/subscribe [:application/form key])])
  (fn [[form]]
    (= "payment-type-astu" (get-in form [:properties :payment :type]))))

(re-frame/reg-sub
  :payment/tutu-form-selected?
  (fn [_ _]
   [(re-frame/subscribe [:application/selected-form])])
  (fn [[form]]
   (or (= "payment-type-tutu" (get-in form [:properties :payment :type]))
       (get-tutu-form? (:key form)))))

(re-frame/reg-sub
  :payment/astu-form-selected?
  (fn [_ _]
    [(re-frame/subscribe [:application/selected-form])])
  (fn [[form]]
    (= "payment-type-astu" (get-in form [:properties :payment :type]))))

(re-frame/reg-sub
  :payment/show-review-ui?
  (fn [_ _]
    [(re-frame/subscribe [:payment/astu-form-selected?])
     (re-frame/subscribe [:payment/tutu-form-selected?])])
  (fn [[astu-form? tutu-form?]]
    (or astu-form? tutu-form?)))

(re-frame/reg-sub
  :payment/note-input
  (fn [db [_ application-key]]
   (get-payment-note-input db application-key)))

(re-frame/reg-sub
  :payment/duedate-input
  (fn [db [_ application-key]]
    (or
      (get-in db [:payment :inputs application-key :due_date])
      (get-in db [:payment :applications application-key :decision :due_date])
      (let [date (time/from-now (time/days 14))]
        (format/unparse (format/formatters :date) date)))))

(re-frame/reg-sub
  :payment/amount-input
  (fn [db [_ application-key]]
  (get-payment-amount-input db application-key)))

(re-frame/reg-sub
  :payment/inputs-filled?
  (fn [[_ application-key]]
   [(re-frame/subscribe [:state-query [:payment :inputs application-key]])
    (re-frame/subscribe [:state-query [:payment :applications application-key :decision :amount]])
    (re-frame/subscribe [:payment/duedate-input])
    (re-frame/subscribe [:payment/astu-form-selected?])])
  (fn [[{:keys [note amount]} decision-amount due_date astu-form?]]
   (let [amount (or amount decision-amount)]
     (and
        (string? amount)
        (some? (re-matches #"\d{1,5}([.]\d{1,2})?" amount))
        (or
          astu-form?
          (and (string? note)
               (not (-> note string/trim string/blank?))))
        (some? due_date)))))

(re-frame/reg-sub
  :payment/payments
  (fn [db [_ application-key]]
   (get-in db [:payment :applications application-key])))

