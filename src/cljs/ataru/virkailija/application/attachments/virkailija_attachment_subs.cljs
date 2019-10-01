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

(re-frame/reg-sub
  :virkailija-attachments/liitepyynnot-for-selected-hakukohteet
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:application/selected-form-fields-by-id])
     (re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:state-query [:application :review :attachment-reviews]])])
  (fn [[selected-hakukohde-oids
        form-fields
        application
        liitepyynnot-for-hakukohteet]]
    (transduce (comp (map (fn [hakukohde-oid]
                            (when-let [liitepyynnot-for-hakukohde (-> hakukohde-oid keyword liitepyynnot-for-hakukohteet)]
                              (map (fn [[liitepyynto-key-str liitepyynto-state]]
                                     (let [liitepyynto-key (keyword liitepyynto-key-str)
                                           values          (-> application :answers liitepyynto-key :values)
                                           label           (-> form-fields liitepyynto-key :label)]
                                       {:key           liitepyynto-key
                                        :state         liitepyynto-state
                                        :values        values
                                        :label         label
                                        :hakukohde-oid hakukohde-oid}))
                                   liitepyynnot-for-hakukohde))))
                     (filter (comp not nil?))
                     (mapcat identity))
               conj
               selected-hakukohde-oids)))
