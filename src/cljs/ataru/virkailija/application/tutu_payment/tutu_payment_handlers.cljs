(ns ataru.virkailija.application.tutu-payment.tutu-payment-handlers
  (:require [clojure.string :refer [ends-with?]]
            [ataru.virkailija.virkailija-ajax :as ajax]
            ;[cljs-time.core :as c]
            [cljs-time.format :as f]
            [re-frame.core :as re-frame]))

(def iso-formatter (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSZZ"))

(def date-formatter (f/formatter "yyyy-MM-dd"))

(def time-formatter (f/formatter "HH:mm"))

(def datetime-formatter (f/formatter "yyyy-MM-dd HH:mm"))

(re-frame/reg-event-fx
 :tutu-payment/handle-fetch-payments
 (fn [{db :db} [_ body {:keys [application-key]}]]
   (let [oid-suffix-matcher #(first (filter (fn [x] (ends-with? (:order_id x) %)) body))
         payments {:processing (oid-suffix-matcher "-1")
                   :decision (oid-suffix-matcher "-2")}]
     ;(prn "GOT response" response)
     (prn "GOT body" application-key (type body) body)
     ;(prn "store to " application-key payments)

     {:db       (assoc-in db [:tutu-payment :applications application-key] payments)
      ;:dispatch [:liitepyynto-information-request/show-deadline application-key liitepyynto-key]
      })

   ))


;(re-frame/reg-event-fx
;  :liitepyynto-information-request/hide-deadline
;  (fn [{db :db} [_ application-key liitepyynto-key]]
;    (let [current-state (get-deadline-visibility-state db application-key liitepyynto-key)]
;      (when (or (= :visible current-state)
;                (= :appearing current-state))
;        {:db             (set-deadline-visibility-state db application-key liitepyynto-key :disappearing)
;         :dispatch-later [{:ms       200
;                           :dispatch [:liitepyynto-information-request/set-deadline-visibility-state
;                                      application-key
;                                      liitepyynto-key
;                                      :hidden]}]}))))

;(re-frame/reg-event-fx
;  :liitepyynto-information-request/show-deadline
;  (fn [{db :db} [_ application-key liitepyynto-key]]
;    (let [current-state (get-deadline-visibility-state db application-key liitepyynto-key)]
;      (when (or (= :hidden current-state)
;                (= :disappearing current-state))
;        {:db             (set-deadline-visibility-state db application-key liitepyynto-key :appearing)
;         :dispatch-later [{:ms       200
;                           :dispatch [:liitepyynto-information-request/set-deadline-visibility-state
;                                      application-key
;                                      liitepyynto-key
;                                      :visible]}]}))))

;(re-frame/reg-event-fx
;  :liitepyynto-information-request/set-deadline-toggle
;  (fn [{db :db} [_ application-key liitepyynto-key on?]]
;    {:dispatch [(cond on?
;                      :liitepyynto-information-request/show-deadline
;                      (some? (get-deadline-last-modified db application-key liitepyynto-key))
;                      :liitepyynto-information-request/delete-deadline
;                      :else
;                      :liitepyynto-information-request/hide-deadline)
;                application-key
;                liitepyynto-key]}))

(re-frame/reg-event-fx
 :tutu-payment/fetch-payments
 (fn [_ [_ application-key]]
   {:tutu-payment/fetch-payments
    {:application-key application-key}}))

(re-frame/reg-event-db
 :tutu-payment/set-note-input
 (fn [db [_ application-key value]]
   (assoc-in db [:tutu-payment :inputs application-key :note] value)))

(re-frame/reg-event-db
  :tutu-payment/set-duedate
  (fn [db [_ application-key value]]
    (assoc-in db [:tutu-payment :inputs application-key :due_date] value)))

(re-frame/reg-event-db
 :tutu-payment/set-amount
 (fn [db [_ application-key value]]
   (assoc-in db [:tutu-payment :inputs application-key :amount] value)))

(re-frame/reg-event-fx
 :tutu-payment/handle-processing-invoice
 (fn [_ [_ response _]]
   (prn "processing-invoice email" response)
   {}))


(re-frame/reg-event-fx
 :tutu-payment/handle-decision-invoice
 (fn [_ [_ response {:keys [application-key]}]]
   (let [{:keys [hakukohde-reviews]} response
         state-name :processing-state
         state-value (-> hakukohde-reviews :form state-name)]
     ;(prn "revs" hakukohde-reviews)
     ;(prn "revs2" state-value)

     {:dispatch-n [[:application/update-review-field state-name state-value]
                   [:application/review-updated response]
                   [:tutu-payment/fetch-payments application-key]]

     })))


;{:db       (-> db
;               (assoc-in [:application :events] events))
; :dispatch [:application/update-review-field state-name state-value]
; })))


;{:db       (assoc-in db [:application :review :hakukohde-reviews] (:hakukohde-reviews response))
; :dispatch [:application/review-updated response]})))

(re-frame/reg-event-fx
 :tutu-payment/resend-processing-invoice
 (fn [{db :db} _]
   (let [application (get-in db [:application :selected-application-and-form :application])
         key (:key application)
         data {:application-key key
               :locale (:lang application)}]

     (ajax/http :post
                "/lomake-editori/api/maksut/resend-maksu-link"
                :tutu-payment/handle-processing-invoice

                :id :resend-processing-invoice
                :handler-args  {:application-key key}
                :override-args {:params data}))
     {}))

(re-frame/reg-event-fx
 :tutu-payment/send-decision-invoice
 (fn [{db :db} [_ application-key]]
   (let [{:keys [due_date]} (get-in db [:tutu-payment :inputs application-key])
         application (get-in db [:application :selected-application-and-form :application])
         get-field  (fn [key] (->> (:answers application) key :value))
         message    @(re-frame/subscribe [:tutu-payment/note-input application-key])
         amount     @(re-frame/subscribe [:tutu-payment/amount-input application-key])
         data {:application-key application-key
               :first-name (get-field :first-name)
               :last-name (get-field :last-name)
               :email (get-field :email)
               :locale (:lang application)
               :amount amount
               :message message
               :due-date due_date
               :index 2}]

     (ajax/http :post
                "/lomake-editori/api/maksut/maksupyynto"
                :tutu-payment/handle-decision-invoice

                :id :send-decision-invoice
                :handler-args  {:application-key application-key}
                :override-args {:params data}))
   {}))

(re-frame/reg-fx
 :tutu-payment/fetch-payments
 (fn [{:keys [application-key]}]
   (prn "Ladataan hakemukseen liittyviä maksuja")
   (ajax/http :get
              (str "/lomake-editori/api/maksut/list/" application-key)
              :tutu-payment/handle-fetch-payments
              :id :fetch-payments
              :handler-args        {:application-key application-key})))
