(ns ataru.virkailija.application.attachments.virkailija-attachment-handlers
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
  :virkailija-attachments/toggle-attachment-selection
  (fn [db [_ attachment-keys]]
    (update-in db
               [:application :attachment-preview :selected-attachments]
               (fn [selected-attachments]
                 (reduce (fn [acc attachment-key]
                           (let [attachment-key      (keyword attachment-key)
                                 currently-selected? (-> acc attachment-key)
                                 will-be-selected?   (if (nil? currently-selected?)
                                                       false
                                                       (not currently-selected?))]
                             (assoc acc attachment-key will-be-selected?)))
                         selected-attachments
                         attachment-keys)))))
