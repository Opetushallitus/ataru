(ns ataru.tutkinto.tutkinto-util
  (:require [ataru.translations.translation-util :as tu]
            [ataru.util :as util]
            [re-frame.core :refer [subscribe]]
            [clojure.string :as str]
            [ataru.component-data.koski-tutkinnot-module :as ktm]
            [ataru.translations.texts :refer [koski-tutkinnot-texts]]))

(defn get-tutkinto-idx
  ([level id]
   (get-tutkinto-idx level id @(subscribe [:application/answers])))
  ([level id answers]
  (let [tutkinto-id (str level "-" ktm/tutkinto-id-field-postfix)
        checked-tutkinto-ids (flatten (get-in answers [(keyword tutkinto-id) :value]))]
    (when (some #{id} checked-tutkinto-ids)
      (.indexOf checked-tutkinto-ids id)))))

(defn get-question-group-of-level [conf-field-descriptor level]
  (let [level-item (some #(when (= level (:id %)) %) (:options conf-field-descriptor))
        level-question-group-id (str level "-" ktm/question-group-of-level)]
    (some #(when (= level-question-group-id (:id %)) %) (:followups level-item))))

(defn id-field-of-level [question-group-of-level level]
  (let [id (str level "-" ktm/tutkinto-id-field-postfix)]
    (some #(when (= id (:id %)) %) (:children question-group-of-level))))

(defn get-tutkinto-field-mappings [lang]
  (map-indexed (fn [idx field] {:id idx
                                :text (tu/get-translation (:label-id field) lang koski-tutkinnot-texts false)
                                :koski-tutkinto-field (:koski-tutkinto-field field)
                                :multi-lang? (:multi-lang? field)})
               [{:label-id :tutkinto-followup-label :koski-tutkinto-field :tutkintonimi :multi-lang? true}
                {:label-id :koulutusohjelma-followup-label :koski-tutkinto-field :koulutusohjelmanimi :multi-lang? true}
                {:label-id :oppilaitos-followup-label :koski-tutkinto-field :toimipistenimi :multi-lang? true}
                {:label-id :valmistumispvm-followup-label :koski-tutkinto-field :valmistumispvm :multi-lang? false}]))

(defn itse-syotetty-tutkinnot-content [conf-field-descriptor]
  (get-in (some #(when (= ktm/itse-syotetty-option-id (:id %)) %) (:options conf-field-descriptor)) [:followups] []))

(defn find-itse-syotetty-content-beneath [field-descriptor]
  (let [tutkinto-conf-component (some #(when (ktm/is-tutkinto-configuration-component? %) %) (:children field-descriptor))]
    (itse-syotetty-tutkinnot-content tutkinto-conf-component)))

(defn koski-tutkinto-levels-in-form
  [form]
  (let [koski-tutkinto-levels (filter #(not (= ktm/itse-syotetty-option-id %))
                                      (get-in form [:properties :tutkinto-properties :selected-option-ids] []))]
    (when (seq koski-tutkinto-levels)
      koski-tutkinto-levels)))

(defn- descending [a b]
  (compare b a))

(defn sort-koski-tutkinnot [koski-tutkinnot]
  (sort-by #(last (str/split (:id %) #"_")) descending koski-tutkinnot))

(defn resolve-excel-content [wrapper-field-desciptor form-properties]
  (let [levels (get-in form-properties [:tutkinto-properties :selected-option-ids])
        tutkinto-conf-component (some #(when (ktm/is-tutkinto-configuration-component? %) %)
                                      (:children wrapper-field-desciptor))
        selected-levels (filter (fn [option] (some #{(:id option)} levels)) (:options tutkinto-conf-component))]
    (flatten (map :followups selected-levels))))

(defn is-koski-tutkinto-id-field? [field-id]
  (boolean (when (and field-id (str/ends-with? field-id ktm/tutkinto-id-field-postfix))
             (let [tutkinto-level (str/replace field-id (str "-" ktm/tutkinto-id-field-postfix) "")]
               (some #{tutkinto-level} ktm/koski-tutkinto-tasot)))))

(defn koski-tutkinnot-in-application? [application]
  (let [tutkinto-id-answers (filter #(is-koski-tutkinto-id-field? (:key %)) (:answers application))]
    (some #(util/non-blank-answer? %) tutkinto-id-answers)))

(defn save-koski-tutkinnot? [form]
  (get-in form [:properties :tutkinto-properties :save-koski-tutkinnot] false))