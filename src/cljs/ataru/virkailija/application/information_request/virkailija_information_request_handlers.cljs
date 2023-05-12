(ns ataru.virkailija.application.information-request.virkailija-information-request-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/set-single-information-request-form-state
  (fn [db [_ state]]
    (assoc-in db [:application :single-information-request :form-status] state)))

(reg-event-fx
  :application/cancel-single-information-request
  (fn [{:keys [db]} _]
    (when (= :confirm (get-in db [:application :single-information-request :form-status]))
      {:dispatch [:application/set-single-information-request-form-state :enabled]})))

(reg-event-fx
  :application/confirm-single-information-request
  (fn [_ _]
    (println "suoritan 'confirm-single-information-request'")
    {:dispatch       [:application/set-single-information-request-form-state :confirm]
     :dispatch-later [{:dispatch [:application/cancel-single-information-request]
                       :ms       3000}]}))

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


(reg-event-fx
  :application/submit-mass-information-request123
  (fn [{:keys [db]} [_ recipient-target]]
    (println "recipient-target = " recipient-target)
    (let [message-and-subject (-> db :application :mass-information-request
                                  (select-keys [:message :subject]))
          application-keys    (map :key (get-in db [:application :applications]))]
      {:dispatch [:application/set-mass-information-request-form-state :submitting]
       :http     {:method              :post
                  :path                "/lomake-editori/api/applications/mass-information-request"
                  :params              {:application-keys    application-keys
                                        :recipient-target    recipient-target
                                        ;:recipient-target    "janne.lindberg@gofore.com"
                                        :message-and-subject message-and-subject}
                  :handler-or-dispatch :application/handle-submit-mass-information-request-response}})))
(reg-event-db
  :application/set-send-update-link
  ; (fn [[db] checkedNewValue]
  (fn [db [_ checkedNewValue] ]
    (println "saatiin checked arvo " checkedNewValue)
    (assoc-in db [:application :send-update-link?-checkbox] checkedNewValue)
    ))
(reg-event-fx
  :application/submit-single-information-request
  (fn [{:keys [db]}]
    (let [application-key (-> db :application :selected-application-and-form :application :key)
          ;  message-and-subject
          ;          (-> db :application :single-information-request :message :subject
          ;    (select-keys [:message :subject]))
          message (-> db :application :single-information-request :message)
          subject (-> db :application :single-information-request :subject)
          ;      application-keys (map :key (get-in db [:application :applications]))
          ]
      (println "message = " message)

      (if (get-in db [:application :send-update-link?-checkbox])
        (println "Lisätään muokkauslinkki")
        (println "Ei lisätä muokkauslinkkiä")

         )
      ;      (println "subject = " + :subject)
      {;:db   (assoc-in db [:application :information-request :state] :submitting)
       :http {:method              :post
              :path                "/lomake-editori/api/applications/information-request"
              :params              (-> db :application :information-request
                                       ;                    (select-keys [:message :subject])
                                       (assoc :recipient-target "hakija")
                                       (assoc :application-key application-key)
                                       (assoc :subject subject)
                                       ;(assoc :subject "testiotsikko")
                                       (assoc :message message)
                                       ;  (assoc :message "viestin sisältä")
                                       (dissoc :state)
                                       (dissoc :visible?)
                                       )
              :handler-or-dispatch :application/handle-submit-information-request-response}})))

(comment
  (reg-event-fx
    :application/submit-single-information-request
    (fn [{:keys [db]} [_ recipient-target]]
      (let [message-and-subject (-> db :application :single-information-request
                                    (select-keys [:message :subject]))
          application-keys    (map :key (get-in db [:application :applications]))]
      (println "Suoritan application/submit-single-information-request")
      {:dispatch [:application/set-single-information-request-form-state :submitting]
       :http     {:method              :post
                  :path                "/lomake-editori/api/applications/mass-information-request"
                  :params              {:application-keys    application-keys
                                        :recipient-target    recipient-target
                                        ;:recipient-target    "janne.lindberg@gofore.com"
                                        :message-and-subject message-and-subject}
                  :handler-or-dispatch :application/handle-submit-single-information-request-response}}))))

(reg-event-fx
  :application/handle-submit-single-information-request-response
  (fn [_ _]
    {:dispatch       [:application/set-single-information-request-form-state :submitted]
     :dispatch-later [{:ms       3000
                       :dispatch [:application/reset-submit-single-information-request-state]}]}))

(reg-event-fx
  :application/reset-submit-single-information-request-state
  (fn [{:keys [db]} _]
    {:dispatch-n [[:application/set-single-information-request-message ""]
                  [:application/set-single-information-request-subject ""]
                  [:application/set-single-information-request-form-state :enabled]
                  (when-let [current-application (-> db :application :selected-key)]
                    [:application/fetch-application current-application])]
     :db         (update-in db [:application :applications]
                            (partial map #(assoc % :new-application-modifications 0)))}))

(reg-event-db
  :application/set-single-information-request-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :single-information-request :visible?] visible?)))
