(ns ataru.virkailija.handlers
    (:require [re-frame.core :refer [reg-event-db dispatch]]
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
   (assoc db :active-panel active-panel)))

(reg-event-db
  :flasher
  (fn [db [_ flash]]
    ; workaround css animation restart
    (js/setTimeout
      (fn []
        (dispatch [:state-update
                   (fn [db]
                     (if (= flash (dissoc (:flash db) :expired?))
                       (update db :flash assoc :expired? true)))]))
      16)
    (assoc db :flash (assoc flash :expired? false))))
