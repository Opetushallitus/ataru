(ns ataru.virkailija.application.information-request.virkailija-information-request-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/set-single-information-request-form-state
  (fn [db [_ state]]
    (assoc-in db [:application :single-information-request :form-status] state)))

(reg-event-db
  :application/set-single-information-request-subject
  (fn [db [_ subject]]
    (cond-> (assoc-in db [:application :single-information-request :subject] subject)
            (not= :enabled (-> db :application :single-information-request :form-status))
            (assoc-in [:application :single-information-request :form-status] :enabled))))

(reg-event-db
  :application/set-single-information-request-message
  (fn [db [_ message]]
    (cond-> (assoc-in db [:application :single-information-request :message] message)
            (not= :enabled (-> db :application :single-information-request :form-status))
            (assoc-in [:application :single-information-request :form-status] :enabled))))

(reg-event-db
  :application/set-send-update-link
  (fn [db [_ checkedNewValue] ]
    (assoc-in db [:application :send-update-link?-checkbox] checkedNewValue)))

(reg-event-fx
  :application/submit-single-information-request
  (fn [{:keys [db]}]

    (let [application-key (-> db :application :selected-application-and-form :application :key)
          message (-> db :application :single-information-request :message)
          subject (-> db :application :single-information-request :subject)
          add-update-link (get-in db [:application :send-update-link?-checkbox])]
      {:http {:method              :post
              :path                "/lomake-editori/api/applications/information-request"
              :params              (-> db :application :information-request
                                       (assoc :recipient-target "hakija")
                                       (assoc :application-key application-key)
                                       (assoc :subject subject)
                                       (assoc :message message)
                                       (assoc :add-update-link add-update-link)
                                       (dissoc :visible?))
              :handler-or-dispatch :application/handle-submit-information-request-response}})))

(reg-event-db
  :application/set-single-information-request-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :single-information-request :visible?] visible?)))
