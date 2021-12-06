(ns ataru.virkailija.application.tutu-payment.tutu-payment-subs
  (:require [re-frame.core :as re-frame]))

;(re-frame/reg-sub
;  :liitepyynto-information-request/deadline-toggle-on?
;  (fn [db [_ application-key liitepyynto-key]]
;    (let [state (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] :hidden)]
;      (or (= :visible state)
;          (= :appearing state)))))
;
;(re-frame/reg-sub
;  :liitepyynto-information-request/deadline-visible?
;  (fn [db [_ application-key liitepyynto-key]]
;    (let [state (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :visibility-state] :hidden)]
;      (or (= :visible state)
;          (= :disappearing state)))))

;TODO combine tutu-form? and show-review-ui?
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
    ;TODO pitäiskö REST-response camel-casettaa?

    (or
      (get-in db [:tutu-payment :inputs application-key :due_date])
      (get-in db [:tutu-payment :applications application-key :decision :due_date])
      "")))

(re-frame/reg-sub
 :tutu-payment/amount-input
 (fn [db [_ application-key]]

   (or
    (get-in db [:tutu-payment :inputs application-key :amount])
    (get-in db [:tutu-payment :applications application-key :decision :amount])
    "")))

(re-frame/reg-sub
 :tutu-payment/inputs-filled?
 (fn [db [_ application-key]]

   ;TODO make sure these fields are validated when the are written
   (let [{:keys [amount due_date]} (get-in db [:tutu-payment :inputs application-key])]
     (and
        (some? amount)
        (some? due_date)))))

(re-frame/reg-sub
 :tutu-payment/payments
 (fn [db [_ application-key]]
   (get-in db [:tutu-payment :applications application-key])))

