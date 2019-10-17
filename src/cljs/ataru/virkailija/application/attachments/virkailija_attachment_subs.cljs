(ns ataru.virkailija.application.attachments.virkailija-attachment-subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  :virkailija-attachments/attachment-selected?
  (fn [db [_ attachment-key]]
    (let [attachment-selected? (-> db :application :attachment-skimming :selected-attachments (get (keyword attachment-key)))]
      (if (nil? attachment-selected?)
        true
        attachment-selected?))))

(def allowed-files-matcher #"(?i)\.(jpg|jpeg|png)$")

(defn- file-previewability [metadata]
  (if (= "finished" (:preview-status metadata))
    :provide-preview
    :download-only))

(defn- file-display-capability [metadata]
  (if (->> (or metadata [])
           :filename
           (re-find allowed-files-matcher))
    :show-in-browser
    (file-previewability metadata)))

(re-frame/reg-sub
  :virkailija-attachments/file-display-capability
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
                                    (= (:key attachment) attachment-key))))
                    conj)
         (first)
         (file-display-capability))))

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

(re-frame/reg-sub
  :virkailija-attachments/selected-attachment-and-liitepyynto
  (fn []
    [(re-frame/subscribe [:virkailija-attachments/liitepyynnot-for-selected-hakukohteet])
     (re-frame/subscribe [:state-query [:application :attachment-skimming :selected-attachment-key]])])
  (fn [[liitepyynnot-for-selected-hakukohteet selected-attachment-key]]
    (->> liitepyynnot-for-selected-hakukohteet
         (transduce (comp (mapcat (fn [liitepyynto]
                                    (let [values      (:values liitepyynto)
                                          attachments (cond->> values
                                                        (every? vector? values)
                                                        (flatten))]
                                      (map (fn [attachment]
                                             {:liitepyynto (dissoc liitepyynto :values)
                                              :attachment  attachment})
                                           attachments))))
                          (filter (comp (partial = selected-attachment-key)
                                        :key
                                        :attachment)))
                    conj)
         (first))))
