(ns ataru.virkailija.application.tutu-payment.tutu-payment-handlers
  (:require [ataru.application-common.fx :refer [http]]
            [clojure.string :refer [ends-with?]]
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
 (fn [{db :db} [_ application-key response]]
   (let [body (-> response
                  :body
                  (js->clj :keywordize-keys true))
         oid-suffix-matcher #(first (filter (fn [x] (ends-with? (:order_id x) %)) body))
         payments {:processing (oid-suffix-matcher "-1")
                   :decision (oid-suffix-matcher "-2")}]
     ;(prn "GOT response" response)
     ;(prn "GOT body" (type body) body)
     ;(prn "store to " application-key payments)

     {:db       (assoc-in db [:tutu-payment :applications application-key] payments)
      ;:dispatch [:liitepyynto-information-request/show-deadline application-key liitepyynto-key]
      })))


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
 :tutu-payment/handle-decision-invoice
 (fn [_ [_ response]]
   (let [{:keys [hakukohde-reviews]} response
         state-name :processing-state
         state-value (-> hakukohde-reviews :form state-name)]
     ;(prn "last event" (last events))
     ;(prn "revs" hakukohde-reviews)
     ;(prn "revs2" state-value)

     {:dispatch-n [[:application/update-review-field state-name state-value]
                   [:application/review-updated response]]

     })))


;{:db       (-> db
;               (assoc-in [:application :events] events))
; :dispatch [:application/update-review-field state-name state-value]
; })))


;{:db       (assoc-in db [:application :review :hakukohde-reviews] (:hakukohde-reviews response))
; :dispatch [:application/review-updated response]})))


(re-frame/reg-event-fx
 :tutu-payment/send-decision-invoice
 (fn [{db :db} [_ application-key]]
   (let [{:keys [due_date]} (get-in db [:tutu-payment :inputs application-key])
         application (get-in db [:application :selected-application-and-form :application])
         get-field  (fn [key] (->> (:answers application) key :value))
         amount     @(re-frame/subscribe [:tutu-payment/amount-input application-key])
         ;review-id  (get-in db [:application :review :id]) ;TODO might need to pass the full review?
         data {;:review-id review-id
               :application-key application-key
               :first-name (get-field :first-name)
               :last-name (get-field :last-name)
               :email (get-field :email)
               :amount amount
               :due_date due_date
               :index 2}
         ]
     (prn "send-decision-invoice" (:answers application))
     (prn "data" data)

     (ajax/http :post
                "/lomake-editori/api/maksut/maksupyynto"
                ;:application/review-updated
                :tutu-payment/handle-decision-invoice
                :override-args {:params data})


;     (http (aget js/config "virkailija-caller-id")
;            {:method        :post
;             :url           "http://localhost:8350/lomake-editori/api/maksut/maksupyynto"
;             ;:url           "http://localhost:9099/maksut/api/lasku-tutu"
;             ;{:deadline (f/unparse iso-formatter deadline)}
;             :post-data     data
;             :handler       [:tutu-payment/handle-decision-invoice]
;             ;:error-handler [:liitepyynto-information-request/re-fetch-and-show-error application-key]
;             })

     )
   {}

   ))

;(re-frame/reg-event-db
;  :liitepyynto-information-request/hide-deadline-error
;  (fn [db [_ application-key liitepyynto-key]]
;    (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :error?] false)))
;
;(re-frame/reg-event-fx
;  :liitepyynto-information-request/show-deadline-error
;  (fn [{db :db} [_ application-key liitepyynto-key]]
;    {:db             (assoc-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :error?] true)
;     :dispatch-later [{:ms       5000
;                       :dispatch [:liitepyynto-information-request/hide-deadline-error
;                                  application-key
;                                  liitepyynto-key]}]}))

;(defn- parse-deadline [deadline]
;  (let [datetime (->> deadline
;                      (f/parse iso-formatter)
;                      c/to-default-time-zone)]
;    [(f/unparse date-formatter datetime)
;     (f/unparse time-formatter datetime)]))

;(re-frame/reg-event-fx
;  :liitepyynto-information-request/set-deadlines
;  (fn [{db :db} [_ application-key response]]
;    (let [last-modified (get-in response [:headers "last-modified"])]
;      {:db         (reduce (fn [db deadline]
;                             (let [liitepyynto-key           (keyword (:field-id deadline))
;                                   [date-string time-string] (parse-deadline (:deadline deadline))]
;                               (set-deadline db application-key liitepyynto-key date-string time-string last-modified)))
;                           db
;                           (:body response))
;       :dispatch-n (map (fn [deadline]
;                          [:liitepyynto-information-request/show-deadline
;                           application-key
;                           (keyword (:field-id deadline))])
;                        (:body response))})))

;(re-frame/reg-event-fx
;  :liitepyynto-information-request/set-deadline
;  (fn [{db :db} [_ application-key response]]
;    (let [[date-string time-string] (parse-deadline (get-in response [:body :deadline]))]
;      {:db       (set-deadline-date db application-key date-string)
;       :dispatch [:liitepyynto-information-request/show-deadline
;                  application-key
;                  liitepyynto-key]})))

;(re-frame/reg-event-db
;  :liitepyynto-information-request/ignore-error
;  (fn [db _] db))
;
;(re-frame/reg-event-fx
;  :liitepyynto-information-request/re-fetch-and-show-error
;  (fn [_ [_ application-key liitepyynto-key _]]
;    {:liitepyynto-information-request/get-deadline
;     {:application-key application-key
;      :liitepyynto-key liitepyynto-key}
;     :dispatch [:liitepyynto-information-request/show-deadline-error
;                application-key
;                liitepyynto-key]}))


;(re-frame/reg-event-fx
;  :liitepyynto-information-request/debounced-save-deadline
;  (fn [_ [_ application-key liitepyynto-key]]
;    {:dispatch-debounced
;     {:timeout  500
;      :id       [:liitepyynto-information-request/save-deadline
;                 application-key
;                 liitepyynto-key]
;      :dispatch [:liitepyynto-information-request/save-deadline
;                 application-key
;                 liitepyynto-key]}}))
;
;(re-frame/reg-event-fx
;  :liitepyynto-information-request/save-deadline
;  (fn [{db :db} [_ application-key liitepyynto-key]]
;    (let [date          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :date])
;          time          (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :time])
;          last-modified (get-in db [:liitepyynto-information-request application-key liitepyynto-key :deadline :last-modified])]
;      (when-let [datetime (try
;                            (->> (str date " " time)
;                                 (f/parse-local datetime-formatter)
;                                 c/to-utc-time-zone)
;                            (catch js/Error e
;                              nil))]
;        {:liitepyynto-information-request/save-deadline
;         {:application-key application-key
;          :liitepyynto-key liitepyynto-key
;          :deadline        datetime
;          :last-modified   last-modified}}))))


;(re-frame/reg-fx
; :liitepyynto-information-request/get-deadline
; (fn [{:keys [application-key liitepyynto-key]}]
;   (http (aget js/config "virkailija-caller-id")
;         {:method        :get
;          :url           (str "/lomake-editori/api/applications/" application-key
;                              "/field-deadline/" (name liitepyynto-key))
;          :handler       [:liitepyynto-information-request/set-deadline
;                          application-key
;                          liitepyynto-key]
;          :error-handler [:liitepyynto-information-request/unset-deadline
;                          application-key
;                          liitepyynto-key]})))


;TODO call /maksut service
(re-frame/reg-fx
 :tutu-payment/fetch-payments
 (fn [{:keys [application-key]}]
   (prn "XXX dispatching :tutu-payment/handle-fetch-payments" application-key)
   (http (aget js/config "virkailija-caller-id")
         {:method        :get
          :url           (str "/maksut/api/lasku-tutu/" application-key)
          :handler       [:tutu-payment/handle-fetch-payments application-key]
          ;:error-handler [:liitepyynto-information-request/unset-deadline application-key]
          })

   ))


;(re-frame/reg-fx
; :liitepyynto-information-request/save-deadline
; (fn [{:keys [application-key liitepyynto-key deadline last-modified]}]
;   (http (aget js/config "virkailija-caller-id")
;         {:method        :put
;          :url           (str "/lomake-editori/api/applications/" application-key
;                              "/field-deadline/" (name liitepyynto-key))
;          :post-data     {:deadline (f/unparse iso-formatter deadline)}
;          :headers       (if (some? last-modified)
;                           {"If-Unmodified-Since" last-modified}
;                           {"If-None-Match" "*"})
;          :handler       [:liitepyynto-information-request/set-deadline
;                          application-key
;                          liitepyynto-key]
;          :error-handler [:liitepyynto-information-request/re-fetch-and-show-error
;                          application-key
;                          liitepyynto-key]})))
;
;(re-frame/reg-fx
; :liitepyynto-information-request/delete-deadline
; (fn [{:keys [application-key liitepyynto-key last-modified]}]
;   (http (aget js/config "virkailija-caller-id")
;         {:method        :delete
;          :url           (str "/lomake-editori/api/applications/" application-key
;                              "/field-deadline/" (name liitepyynto-key))
;          :headers       {"If-Unmodified-Since" last-modified}
;          :handler       [:liitepyynto-information-request/unset-deadline
;                          application-key
;                          liitepyynto-key]
;          :error-handler [:liitepyynto-information-request/re-fetch-and-show-error
;                          application-key
;                          liitepyynto-key]})))
