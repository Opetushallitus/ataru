(ns ataru.virkailija.handlers
    (:require [re-frame.core :refer [register-handler dispatch]]
              [ataru.virkailija.autosave :as autosave]
              [ataru.virkailija.db :as db]
              [ataru.virkailija.virkailija-ajax :refer [http]]
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
  :drop-flash
  (fn [db _]
    (update db :flasher
            (comp vec (fn [flashes] (vec (rest flashes)))))))

(register-handler
  :flasher
  (do
    (js/setInterval (fn [] (dispatch [:drop-flash])) 2000)
    (fn [db [_ {:keys [message] :as flash}]]
      (or (when message
            (update
              db
              :flasher
              (comp vec (fn [flashes] (conj flashes flash)))))
          db))))
