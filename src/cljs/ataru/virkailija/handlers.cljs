(ns ataru.virkailija.handlers
    (:require [re-frame.core :refer [register-handler dispatch]]
              [ataru.virkailija.db :as db]
              [ataru.virkailija.autosave :as autosave]
              [taoensso.timbre :refer-macros [spy debug]]))

(register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(register-handler
  :set-state
  (fn [db [_ path args]]
    (assert (or (vector? path)
                (seq? path)))
    (if (map? args)
      (update-in db path merge args)
      (assoc-in db path args))))

(register-handler
  :state-update
  (fn [db [_ f]]
    (or (f db)
        db)))

(register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (autosave/stop-autosave! (-> db :editor :autosave))
   (assoc db :active-panel active-panel)))

(register-handler
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))
