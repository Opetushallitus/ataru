(ns ataru.virkailija.application.attachments.virkailija-attachment-subs
  (:require [re-frame.core :as re-frame]))

(defn attachment-preview-pages-to-display []
         (get (js->clj js/config) "attachment-preview-pages-to-display" 15))

(re-frame/reg-sub
  :virkailija-attachments/attachment-selected?
  (fn [db [_ attachment-key]]
    (let [attachment-selected? (-> db :application :attachment-skimming :selected-attachments (get (keyword attachment-key)))]
      (if (nil? attachment-selected?)
        true
        attachment-selected?))))

(defn- attachment-metadata [application attachment-key]
  (->> application
       :answers
       vals
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
       first))

(def browser-supported-imagetypes ["image/jpeg" "image/gif" "image/png"])

(defn- can-display-in-browser? [content-type]
  (some #(= content-type %) browser-supported-imagetypes))

(defn- has-preview? [metadata]
  (= "finished" (:preview-status metadata)))

(defn- file-display-capability [metadata]
  (if (->> (:content-type metadata)
           can-display-in-browser?)
    :show-in-browser
    (if (has-preview? metadata)
      :provide-preview
      :download-only)))

(re-frame/reg-sub
  :virkailija-attachments/file-display-capability
  (fn []
    [(re-frame/subscribe [:application/selected-application])])
  (fn [[application] [_ attachment-key]]
    (-> (attachment-metadata application attachment-key)
        (file-display-capability))))

(defn- preview-urls [metadata]
  (->> (:previews metadata)
       (map :key)
       (map #(str "/lomake-editori/api/files/content/" %))
       (take attachment-preview-pages-to-display)))

(re-frame/reg-sub
  :virkailija-attachments/attachment-preview-urls
  (fn []
    [(re-frame/subscribe [:application/selected-application])])
  (fn [[application] [_ attachment-key]]
    (-> (attachment-metadata application attachment-key)
        (preview-urls))))

(defn liitepyynnot-for-selected-hakukohteet
  [[selected-hakukohde-oids
    form-attachment-fields
    application
    liitepyynnot-for-hakukohteet]]
  (let [result
        (transduce (comp (map (fn [hakukohde-oid]
                                (when-let [liitepyynnot-for-hakukohde (-> hakukohde-oid keyword liitepyynnot-for-hakukohteet)]
                                  (->> form-attachment-fields
                                       (map (fn [{liitepyynto-key-str :id
                                                  liitepyynto-label :label}]
                                              (let [liitepyynto-key    (keyword liitepyynto-key-str)
                                                    values             (-> application :answers liitepyynto-key :values)
                                                    liitepyynto-state  (liitepyynnot-for-hakukohde liitepyynto-key)]
                                                {:key           liitepyynto-key
                                                 :state         liitepyynto-state
                                                 :values        values
                                                 :label         liitepyynto-label
                                                 :hakukohde-oid hakukohde-oid})))
                                       (filter #(contains? liitepyynnot-for-hakukohde (:key %)))))))
                         (filter (comp not nil?))
                         (mapcat identity))
                   conj
                   selected-hakukohde-oids)]
    result))

(re-frame/reg-sub
  :virkailija-attachments/liitepyynnot-for-selected-hakukohteet
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:application/selected-form-attachment-fields])
     (re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:state-query [:application :review :attachment-reviews]])])
  liitepyynnot-for-selected-hakukohteet)

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
