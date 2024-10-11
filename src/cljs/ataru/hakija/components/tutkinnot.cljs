(ns ataru.hakija.components.tutkinnot
  (:require [clojure.string :refer [join]]
            [re-frame.core :refer [subscribe reg-sub]]
            [ataru.translations.texts :refer [koski-tutkinnot-texts]]))

(def self-added-headers-by-ids
  {(keyword "itse-syotetty-tutkinto-nimi")   (:tutkinto-nimi-header-text koski-tutkinnot-texts)
   (keyword "itse-syotetty-koulutusohjelma") (:koulutusohjelma-header-text koski-tutkinnot-texts)
   (keyword "itse-syotetty-oppilaitos")      (:oppilaitos-header-text koski-tutkinnot-texts)
   (keyword "itse-syotetty-valmistumispvm")  (:valmistumispvm-header-text koski-tutkinnot-texts)})

(defn is-tutkinto-configuration-component? [field-descriptor]
  (= "tutkinto-properties" (:category field-descriptor)))

(defn itse-syotetty-tutkinnot-content [conf-field-descriptor]
  (get-in (some #(when (= "itse-syotetty" (:id %)) %) (:options conf-field-descriptor)) [:followups] []))

(defn- default-header-of [field-descriptors lang]
  (join ", " (map #(get-in self-added-headers-by-ids [(keyword (:id %)) lang]) (filter #(not (nil? %)) field-descriptors))))

;(defn- values-of [answers field-descriptors lang]
;  (join ", " (map #(get-in answers [(keyword (:id %)) lang]) (filter #(not (nil? %)) field-descriptors))))

(defn- self-added-exams [db]
  (into {} (map (fn [k] {k (get-in db [:application :answers k :value])}) (keys self-added-headers-by-ids))))

(reg-sub
  :application/show-default-self-added-exam-selections?
  (fn [db _]
    (let [self-added-ids (map name (keys self-added-headers-by-ids))]
      (some? (some #(some (fn [id] (= id (:id %))) self-added-ids) (:flat-form-content db))))))

(reg-sub
  :application/self-added-exams
  (fn [db _] (self-added-exams db)))

;TODO toteutus tulevan speksin mukaan
(defn non-koski-header [conf-field-descriptor]
  (let [lang @(subscribe [:application/form-language])
        ;answers @(subscribe [:application/self-added-exams])
        itse-syotetty-options (itse-syotetty-tutkinnot-content conf-field-descriptor)
        tutkinto-nimi-field (some #(when (= "itse-syotetty-tutkinto-nimi" (:id %)) %) itse-syotetty-options)
        koulutusohjelma-field (some #(when (= "itse-syotetty-koulutusohjelma" (:id %)) %) itse-syotetty-options)
        oppilaitos-field (some #(when (= "itse-syotetty-oppilaitos" (:id %)) %) itse-syotetty-options)
        valmistumis-pvm-field (some #(when (= "itse-syotetty-valmistumispvm" (:id %)) %) itse-syotetty-options)
        default-upper-row (default-header-of [tutkinto-nimi-field koulutusohjelma-field valmistumis-pvm-field] lang)
        default-lower-row (default-header-of [oppilaitos-field] lang)]

    ;;(js/console.log (str "!!!!!!!!!!!!!! " answers))
    (if (or default-upper-row default-lower-row)
      [:div.application__tutkinto-non-koski-header
       [:div.application__tutkinto-non-koski-header-inner
        [:span.application__tutkinto-non-koski-header-inner.upper-row default-upper-row]
        [:span default-lower-row]]]
      [nil]
  )))