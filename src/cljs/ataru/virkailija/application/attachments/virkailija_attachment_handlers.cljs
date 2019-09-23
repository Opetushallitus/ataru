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

(re-frame/reg-event-db
  :virkailija-attachments/select-attachment
  (fn [db [_ attachment-key]]
    (assoc-in db
              [:application :attachment-preview :selected-attachment-key]
              attachment-key)))

(defn esc-keypress-event-listener [event]
  (let [key-code (.-keyCode event)]
    (when (= key-code 27)
      (re-frame/dispatch [:virkailija-attachments/close-attachment-preview]))))

(re-frame/reg-event-fx
  :virkailija-attachments/open-attachment-preview
  [(re-frame/inject-cofx :virkailija/scroll-y)]
  (fn [{db :db scroll-y :scroll-y} [_ attachment-key]]
    {:db                                       (update-in db
                                                          [:application :attachment-preview]
                                                          merge
                                                          {:visible?                true
                                                           :selected-attachment-key attachment-key
                                                           :previous-scroll-y       scroll-y})
     :virkailija/setup-keypress-event-listener esc-keypress-event-listener
     :virkailija/scroll-y                      0}))

(re-frame/reg-event-fx
  :virkailija-attachments/close-attachment-preview
  (fn [{db :db}]
    (let [previous-scroll-y (-> db :application :attachment-preview :previous-scroll-y)]
      {:db                                        (update-in db
                                                             [:application :attachment-preview]
                                                             (comp (partial assoc {:visible? false} :selected-attachments)
                                                                   :selected-attachments))
       :virkailija/remove-keypress-event-listener esc-keypress-event-listener
       :virkailija/scroll-y                       previous-scroll-y})))

(re-frame/reg-event-fx
  :virkailija-attachments/remove-esc-keypress-event-listener
  (fn []
    {:virkailija/remove-keypress-event-listener esc-keypress-event-listener}))

(defn- attachment-idx [attachment-key attachment-keys-indexed]
  (->> attachment-keys-indexed
       (filter (fn [attachment]
                 (= (:key attachment) attachment-key)))
       (map :ui-idx)
       (first)))
