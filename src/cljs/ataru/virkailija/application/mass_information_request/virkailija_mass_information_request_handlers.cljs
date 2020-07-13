(ns ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/set-mass-information-request-form-state
  (fn [db [_ state]]
    (assoc-in db [:application :mass-information-request :form-status] state)))

(reg-event-fx
  :application/cancel-mass-information-request
  (fn [{:keys [db]} _]
    (when (= :confirm (get-in db [:application :mass-information-request :form-status]))
      {:dispatch [:application/set-mass-information-request-form-state :enabled]})))

(reg-event-fx
  :application/confirm-mass-information-request
  (fn [_ _]
    {:dispatch       [:application/set-mass-information-request-form-state :confirm]
     :dispatch-later [{:dispatch [:application/cancel-mass-information-request]
                       :ms       3000}]}))

(reg-event-db
  :application/set-mass-information-request-subject
  (fn [db [_ subject]]
    (cond-> (assoc-in db [:application :mass-information-request :subject] subject)
            (not= :enabled (-> db :application :mass-information-request :form-status))
            (assoc-in [:application :mass-information-request :form-status] :enabled))))

(reg-event-db
  :application/set-mass-information-request-message
  (fn [db [_ message]]
    (cond-> (assoc-in db [:application :mass-information-request :message] message)
            (not= :enabled (-> db :application :mass-information-request :form-status))
            (assoc-in [:application :mass-information-request :form-status] :enabled))))

(reg-event-db
  :application/set-excel-request-included-ids
  (fn [db [_ included-ids]]
    (assoc-in db [:application :excel-request :included-ids] included-ids)))

(reg-event-fx
  :application/submit-mass-information-request
  (fn [{:keys [db]} _]
    (let [message-and-subject (-> db :application :mass-information-request
                                  (select-keys [:message :subject]))
          application-keys    (map :key (get-in db [:application :applications]))]
      {:dispatch [:application/set-mass-information-request-form-state :submitting]
       :http     {:method              :post
                  :path                "/lomake-editori/api/applications/mass-information-request"
                  :params              {:application-keys    application-keys
                                        :message-and-subject message-and-subject}
                  :handler-or-dispatch :application/handle-submit-mass-information-request-response}})))

(reg-event-fx
  :application/handle-submit-mass-information-request-response
  (fn [_ _]
    {:dispatch       [:application/set-mass-information-request-form-state :submitted]
     :dispatch-later [{:ms       3000
                       :dispatch [:application/reset-submit-mass-information-request-state]}]}))

(reg-event-fx
  :application/reset-submit-mass-information-request-state
  (fn [{:keys [db]} _]
    {:dispatch-n [[:application/set-mass-information-request-message ""]
                  [:application/set-mass-information-request-subject ""]
                  [:application/set-mass-information-request-form-state :enabled]
                  (when-let [current-application (-> db :application :selected-key)]
                    [:application/fetch-application current-application])]
     :db         (update-in db [:application :applications]
                            (partial map #(assoc % :new-application-modifications 0)))}))

(reg-event-db
  :application/set-mass-information-request-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :mass-information-request :visible?] visible?)))
