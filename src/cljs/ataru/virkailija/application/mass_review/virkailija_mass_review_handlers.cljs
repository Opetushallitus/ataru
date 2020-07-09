(ns ataru.virkailija.application.mass-review.virkailija-mass-review-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
  :application/set-mass-update-popup-visibility
  (fn [db [_ visible?]]
    (assoc-in db [:application :mass-update :visible?] visible?)))

(reg-event-fx
  :application/mass-update-application-reviews
  (fn [{:keys [db]} [_ from-state to-state]]
    {:http {:method              :post
            :params              {:application-keys (map :key (get-in db [:application :applications]))
                                  :from-state       from-state
                                  :to-state         to-state
                                  :hakukohde-oid    (or (-> db :application :rajaus-hakukohteella)
                                                        (-> db :application :selected-hakukohde))}
            :path                "/lomake-editori/api/applications/mass-update"
            :handler-or-dispatch :application/handle-mass-update-application-reviews}}))

