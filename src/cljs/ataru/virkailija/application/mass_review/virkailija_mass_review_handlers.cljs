(ns ataru.virkailija.application.mass-review.virkailija-mass-review-handlers
  (:require [ataru.virkailija.application.application-selectors :refer [hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma]]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(reg-event-db
  :application/set-mass-update-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :mass-update :visible?] visible?)))

(reg-event-db
 :application/set-mass-update-popup-tab
 (fn [db [_ tab-name]]
   (assoc-in db [:application :mass-update :open-tab] tab-name)))

(reg-event-db
 :application/set-mass-review-notes-popup-visibility
 (fn [db [_ visible?]]
   (assoc-in db [:application :mass-review-notes :visible?] visible?)))

(reg-event-fx
  :application/mass-update-application-reviews
  (fn [{:keys [db]} [_ from-state to-state]]
    {:http {:method              :post
            :params              {:application-keys (map :key (get-in db [:application :applications]))
                                  :from-state       from-state
                                  :to-state         to-state
                                  :hakukohde-oid    (or (-> db :application :rajaus-hakukohteella)
                                                        (-> db :application :selected-hakukohde))
                                  :hakukohde-oids-for-hakukohderyhma (hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma db)}
            :path                "/lomake-editori/api/applications/mass-update"
            :handler-or-dispatch :application/handle-mass-update-application-reviews}}))

(reg-event-fx
 :application/mass-inactivate-applications
 (fn [{:keys [db]} _]
   {:http {:method              :post
           :params              {:application-keys (map :key (get-in db [:application :applications]))
                                 :message          (get-in db [:application :mass-inactivation :message])}
           :path                "/lomake-editori/api/applications/mass-inactivate"
           :handler-or-dispatch :application/handle-mass-inactivate-applications}}))

(reg-event-db
 :application/set-mass-review-notes
 (fn [db [_ review-note]]
   (assoc-in db [:application :mass-review-notes :review-notes] review-note)))

(reg-event-db
 :application/set-mass-review-notes-form-state
 (fn [db [_ state]]
   (assoc-in db [:application :mass-review-notes :form-status] state)))

(reg-event-fx
 :application/cancel-mass-review-notes
 (fn [{:keys [db]} _]
   (when (= :confirm (get-in db [:application :mass-review-notes :form-status]))
     {:dispatch [:application/set-mass-review-notes-form-state :enabled]})))

(reg-event-fx
 :application/confirm-mass-review-notes
 (fn [_ _]
   {:dispatch       [:application/set-mass-review-notes-form-state :confirm]
    :dispatch-later [{:dispatch [:application/cancel-mass-review-notes]
                      :ms       3000}]}))

(reg-event-fx
 :application/mass-update-application-review-notes
 (fn [{:keys [db]} [_ review-note]]
   {:dispatch [:application/set-mass-review-notes-form-state :submitting]
    :http {:method              :post
           :params              {:application-keys (map :key (get-in db [:application :applications]))
                                 :notes      review-note
                                 :hakukohde    (or (-> db :application :rajaus-hakukohteella)
                                                       (-> db :application :selected-hakukohde))}
           :path                "/lomake-editori/api/applications/mass-notes"
           :handler-or-dispatch :application/handle-submit-mass-review-notes-response
           :override-args       {:error-handler #(dispatch [:application/handle-submit-mass-review-notes-error])}}}))

(reg-event-fx
 :application/handle-submit-mass-review-notes-response
 (fn [_ _]
   {:dispatch       [:application/set-mass-review-notes-form-state :submitted]
    :dispatch-later [{:ms       3000
                      :dispatch [:application/reset-submit-mass-review-notes-state]}]}))

(reg-event-fx
 :application/handle-submit-mass-review-notes-error
 (fn [_ _]
   {:dispatch       [:application/set-mass-review-notes-form-state :submit-error]
    :dispatch-later [{:ms       3000
                      :dispatch [:application/reset-submit-mass-review-notes-state]}]}))

(reg-event-fx
 :application/reset-submit-mass-review-notes-state
 (fn [{:keys [db]} _]
   {:dispatch-n [[:application/set-mass-review-notes ""]
                 [:application/set-mass-review-notes-form-state :enabled]
                 (when-let [current-application (-> db :application :selected-key)]
                   [:application/fetch-application current-application])]
    :db         (update-in db [:application :applications]
                           (partial map #(assoc % :new-application-modifications 0)))}))

(reg-event-fx
 :application/handle-mass-inactivate-applications
 (fn [_ _]
   {:dispatch-n [[:application/set-mass-inactivation-message ""]
                 [:application/reload-applications]]}))


(reg-event-db
 :application/set-mass-inactivation-message
 (fn [db [_ message]]
   (assoc-in db [:application :mass-inactivation :message] message)))
