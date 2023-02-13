(ns ataru.virkailija.application.attachments.virkailija-attachment-subs
  (:require [ataru.util :as util]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [clojure.set :as set]))

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
                                  (if (util/is-question-group-answer? values)
                                    (mapcat identity values)
                                    values)))
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
       (take (attachment-preview-pages-to-display))))

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
  (for [field         form-attachment-fields
        hakukohde-oid selected-hakukohde-oids
        :let          [liitepyynto-key (keyword (:id field))
                       liitepyynto-state (get-in liitepyynnot-for-hakukohteet [(keyword hakukohde-oid) liitepyynto-key])]
        :when         (some? liitepyynto-state)]
    {:key           liitepyynto-key
     :state         liitepyynto-state
     :values        (get-in application [:answers liitepyynto-key :values])
     :label         (:label field)
     :hakukohde-oid hakukohde-oid}))

(re-frame/reg-sub
  :virkailija-attachments/liitepyynnot-for-selected-hakukohteet
  (fn []
    [(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])
     (re-frame/subscribe [:application/selected-form-attachment-fields])
     (re-frame/subscribe [:application/selected-application])
     (re-frame/subscribe [:state-query [:application :review :attachment-reviews]])])
  liitepyynnot-for-selected-hakukohteet)

(defn- to-liitteet-with-hakukohde
  [attachment-answers hakutoiveet liitekoodisto]
  (let [hakukohteen-tiedot-fn (fn [hk] {:oid (:oid hk)
                                        :name (:name hk)
                                        :tarjoaja (:tarjoaja-name hk)})
        toive-to-liitteet-fn (fn [hk] (->> hk
                                           :liitteet
                                           flatten
                                           (filter #(true? (:toimitetaan-erikseen %)))
                                           (map #(assoc % :hakukohde (hakukohteen-tiedot-fn hk)))))
        get-koodi-fn (fn [liite] (->> liitekoodisto
                                      (filter #(string/includes? (:tyyppi liite) (:uri %)))
                                      (first)))
        is-in-answers (fn [liite]
                        (let [liitetyyppi (:tyyppi liite)
                              hakukohde (get-in liite [:hakukohde :oid])
                              matching-answers-by-tyyppi (->> attachment-answers
                                                              (filter #(string/includes? liitetyyppi (:attachment-type %))))
                              matching-answers-with-no-duplication (->> matching-answers-by-tyyppi
                                                                        (filter #(not (or (:original-followup %) (:original-question %)))))
                              matching-answers-with-duplication (set/difference matching-answers-by-tyyppi matching-answers-with-no-duplication)
                              answer-has-hakukohde (fn [answer]
                                                     (or (= hakukohde (:duplikoitu-kysymys-hakukohde-oid answer))
                                                         (= hakukohde (:duplikoitu-followup-hakukohde-oid answer))))]
                          (cond
                            (< 0 (count matching-answers-with-no-duplication))
                            true

                            (< 0 (count (filter #(answer-has-hakukohde %) matching-answers-with-duplication)))
                            true

                            :else
                            false)))]
    (->> hakutoiveet
         (map toive-to-liitteet-fn)
         flatten
         (filter is-in-answers)
         (map #(assoc % :tyyppi-label (get (get-koodi-fn %) :label (:tyyppi %))))
         (group-by :tyyppi))))

(defn- answers-with-attachments
  [answers attachment-fields]
  (let [answer-with-attachment-field (fn [answer]
                                         (let [answer-key (:key answer)
                                               answer-original (:original-question answer)
                                               answer-followup (:original-followup answer)
                                               matches-fn (fn [field-id]
                                                            (or (= field-id answer-key)
                                                                (and answer-original (string/includes? answer-original field-id))
                                                                (and answer-followup (string/includes? answer-followup field-id))))
                                               matching-attachment-field (->> attachment-fields
                                                                              (filter #(matches-fn (:id %)))
                                                                              first)]
                                           (when matching-attachment-field
                                             (assoc answer :attachment-type (get-in matching-attachment-field [:params :attachment-type])))))
        answers-with-attachments (->> answers
                                      (map answer-with-attachment-field)
                                      (remove nil?))]
    answers-with-attachments))

(re-frame/reg-sub
  :virkailija-attachments/liitepyynnot-hakemuksen-hakutoiveille
  (fn []
    [(re-frame/subscribe [:application/valitun-hakemuksen-hakukohteet])
     (re-frame/subscribe [:application/hakukohteet])
     (re-frame/subscribe [:editor/get-attachment-types-koodisto])
     (re-frame/subscribe [:application/selected-application-answers])
     (re-frame/subscribe [:application/selected-form])
     (re-frame/subscribe [:application/forms])])
  (fn [[hakemuksen-hakutoiveet hakukohteet liitekoodisto answers form forms]]
    (let [hakutoiveet (->> hakemuksen-hakutoiveet
                           (map #(get hakukohteet %)))
          fields-with-attachments (->> form
                                       :key
                                       (get forms)
                                       :flat-form-fields
                                       (filter #(true? (get-in % [:params :mail-attachment?]))))
          answers-with-attachments (answers-with-attachments (vals answers) fields-with-attachments)]
      (to-liitteet-with-hakukohde answers-with-attachments hakutoiveet liitekoodisto))))

(re-frame/reg-sub
  :virkailija-attachments/selected-attachment-and-liitepyynto
  (fn []
    [(re-frame/subscribe [:virkailija-attachments/liitepyynnot-for-selected-hakukohteet])
     (re-frame/subscribe [:state-query [:application :attachment-skimming :selected-attachment-key]])])
  (fn [[liitepyynnot-for-selected-hakukohteet selected-attachment-key]]
    (->> liitepyynnot-for-selected-hakukohteet
         (transduce (comp (mapcat (fn [liitepyynto]
                                    (let [values      (:values liitepyynto)
                                          attachments (if (util/is-question-group-answer? values)
                                                        (mapcat identity values)
                                                        values)]
                                      (map (fn [attachment]
                                             {:liitepyynto (dissoc liitepyynto :values)
                                              :attachment  attachment})
                                           attachments))))
                          (filter (comp (partial = selected-attachment-key)
                                        :key
                                        :attachment)))
                    conj)
         (first))))
