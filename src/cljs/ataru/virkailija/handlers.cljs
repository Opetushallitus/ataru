(ns ataru.virkailija.handlers
    (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
              [ataru.virkailija.autosave :as autosave]
              [ataru.virkailija.db :as db]
              [taoensso.timbre :refer-macros [spy debug]]))

(reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(reg-event-db
  :set-state
  (fn [db [_ path args]]
    (assert (or (vector? path)
                (seq? path)))
    (if (map? args)
      (update-in db path merge args)
      (assoc-in db path args))))

(reg-event-db
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (autosave/stop-autosave! (-> db :editor :autosave))
   (-> db
       (assoc :active-panel active-panel)
       (assoc-in [:editor :show-remove-confirm-dialog?] false))))

(reg-event-fx
  :flasher
  (fn [{:keys [db]} [_ flash]]
    (-> {:db db}
        (assoc :delayed-dispatch
          {:dispatch-vec [:state-update (fn [db]
                                          (if (= flash (dissoc (:flash db) :expired?))
                                            (update db :flash assoc :expired? true)))]
           :timeout      16})
        (assoc-in [:db :flash] (assoc flash :expired? false)))))
