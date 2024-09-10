(ns ataru.virkailija.application.payment.payment-handlers
  (:require [ataru.virkailija.application.application-selectors :refer [get-payment-amount-input
                                                                        get-payment-note-input]]
            [ataru.virkailija.virkailija-ajax :as ajax]
            [cljs-time.format :as f]
            [clojure.string :refer [ends-with?]]
            [re-frame.core :as re-frame]))

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))

(def date-formatter (f/formatter "yyyy-MM-dd"))

(def time-formatter (f/formatter "HH:mm"))

(def datetime-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(re-frame/reg-event-fx
  :payment/handle-fetch-payments
  (fn [{db :db} [_ body {:keys [application-key]}]]
   (let [oid-suffix-matcher #(first (filter (fn [x] (ends-with? (:order_id x) %)) body))
         payments {:processing (oid-suffix-matcher "-1")
                   :decision (oid-suffix-matcher "-2")}]

     {:db (assoc-in db [:payment :applications application-key] payments)})))

(re-frame/reg-event-fx
  :payment/fetch-payments
  (fn [_ [_ application-key]]
   {:payment/fetch-payments
    {:application-key application-key}}))

(re-frame/reg-event-db
 :payment/set-note-input
 (fn [db [_ application-key value]]
   (assoc-in db [:payment :inputs application-key :note] value)))

(re-frame/reg-event-db
  :payment/set-duedate
  (fn [db [_ application-key value]]
    (assoc-in db [:payment :inputs application-key :due_date] value)))

(re-frame/reg-event-db
 :payment/set-amount
 (fn [db [_ application-key value]]
   (assoc-in db [:payment :inputs application-key :amount] value)))

(re-frame/reg-event-fx
  :payment/handle-processing-invoice
  (fn [_ [_ _ _]]
   {}))

(re-frame/reg-event-fx
  :payment/handle-decision-invoice
  (fn [_ [_ response {:keys [application-key]}]]
   (let [{:keys [hakukohde-reviews]} response
         state-name :processing-state
         state-value (-> hakukohde-reviews :form state-name)]

     {:dispatch-n [[:application/update-review-field state-name state-value]
                   [:application/review-updated response]
                   [:payment/fetch-payments application-key]]})))


(re-frame/reg-event-fx
  :payment/resend-processing-invoice
  (fn [{db :db} _]
   (let [application (get-in db [:application :selected-application-and-form :application])
         key (:key application)
         data {:application-key key
               :locale (:lang application)}]

     (ajax/http :post
                "/lomake-editori/api/maksut/resend-maksu-link"
                :payment/handle-processing-invoice

                :id :resend-processing-invoice
                :handler-args {:application-key key}
                :override-args {:params data}))
     {}))

(re-frame/reg-event-fx
  :payment/send-decision-invoice
  (fn [{db :db} [_ application-key payment-type]]
   (let [{:keys [due_date]} (get-in db [:payment :inputs application-key])
         application (get-in db [:application :selected-application-and-form :application])
         get-field  (fn [key] (->> (:answers application) key :value))
         message    (get-payment-note-input db application-key)
         amount     (get-payment-amount-input db application-key)
         data {:reference application-key
               :first-name (get-field :first-name)
               :last-name (get-field :last-name)
               :email (get-field :email)
               :locale (:lang application)
               :amount amount
               :message message
               :due-date due_date
               :due-days 14
               :origin payment-type
               :index 2}]

     (ajax/http :post
                "/lomake-editori/api/maksut/maksupyynto"
                :payment/handle-decision-invoice

                :id :send-decision-invoice
                :handler-args {:application-key application-key}
                :override-args {:params data}))
   {}))

(re-frame/reg-fx
  :payment/fetch-payments
  (fn [{:keys [application-key]}]
   (prn "Ladataan hakemukseen liittyviä maksuja")
   (ajax/http :get
              (str "/lomake-editori/api/maksut/list/" application-key)
              :payment/handle-fetch-payments
              :id :fetch-payments
              :handler-args {:application-key application-key})))
