(ns ataru.virkailija.application.tutu-payment.tutu-payment-subs
  (:require
    [cljs-time.core :as time]
    [cljs-time.format :as format]
    [clojure.string :as string]
    [re-frame.core :as re-frame]))

(re-frame.core/reg-sub
 :tutu-payment/tutu-form?
 (fn [_ [_ key]]
   (let [tutu-form (aget js/config "tutu-payment-form-key")]
     (and
      (not-empty tutu-form)
      (= tutu-form key)))))

(re-frame.core/reg-sub
 :tutu-payment/show-review-ui?
 (fn [db _]
   (let [current-form (get-in db [:application :selected-application-and-form :form :key])
         tutu-form    (aget js/config "tutu-payment-form-key")]

     (and
      (not-empty tutu-form)
      (= tutu-form current-form)))))

(re-frame/reg-sub
 :tutu-payment/note-input
 (fn [db [_ application-key]]
   (or
    (get-in db [:tutu-payment :inputs application-key :note])
    "")))

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

   (or
    (get-in db [:tutu-payment :inputs application-key :amount])
    (get-in db [:tutu-payment :applications application-key :decision :amount])
    "")))

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

