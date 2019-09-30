(ns ataru.virkailija.application.attachments.virkailija-attachment-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :virkailija-attachments/attachment-selected?
  (fn [db [_ attachment-key]]
    (let [attachment-selected? (-> db :application :attachment-preview :selected-attachments (get (keyword attachment-key)))]
      (if (nil? attachment-selected?)
        true
        attachment-selected?))))

(def allowed-files-matcher #"(?i)\.(jpg|jpeg|png)$")

(defn- can-display-file? [filename]
  (if filename
    (->> filename
         (re-find allowed-files-matcher)
         (boolean))
    false))

(re-frame/reg-sub
  :virkailija-attachments/can-display-file?
  (fn []
    [(re-frame/subscribe [:application/selected-application])])
  (fn [[application] [_ attachment-key]]
    (->> application
         :answers
         (vals)
         (transduce (comp (filter (fn [answer]
                                    (= (:fieldType answer) "attachment")))
                          (map :values)
                          (mapcat (fn [values]
                                    (cond-> values
                                      (every? vector? values)
                                      (flatten))))
                          (filter (fn [attachment]
                                    (= (:key attachment) attachment-key)))
                          (map :filename))
                    conj)
         (first)
         (can-display-file?))))
