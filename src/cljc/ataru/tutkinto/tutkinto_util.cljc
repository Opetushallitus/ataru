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

(defn koski-tutkinto-field-without-selections? [field-descriptor flat-form-content answers koski-tutkinto-levels-in-form]
  (let [tutkinto-id-fields (map #(str % "-" ktm/tutkinto-id-field-postfix) koski-tutkinto-levels-in-form)
        id-fields-wo-answers (filter #(not (util/non-blank-answer? ((keyword %) answers))) tutkinto-id-fields)
        question-groups-wo-answers (map #(str/replace % ktm/tutkinto-id-field-postfix ktm/question-group-of-level)
                                        id-fields-wo-answers)
        descendants-wo-selections (mapcat #(util/find-descendant-ids-by-parent-id flat-form-content %)
                                          question-groups-wo-answers)]
    (some? (some #{(:id field-descriptor)} descendants-wo-selections))))

(defn koski-tutkinto-level-without-selections? [field-descriptor answers koski-tutkinto-levels-in-form]
  (let [id (:id field-descriptor)]
    (if (some #{id} koski-tutkinto-levels-in-form)
      (not (util/non-blank-answer? ((keyword (str id "-" ktm/tutkinto-id-field-postfix)) answers)))
      false)))

(defn- find-option-by-id [fields id]
  (cond (empty? fields)
        nil
        (= id (:id (first fields)))
        (first fields)
        :else
        (recur (into (rest fields)
                     (concat (:children (first fields))
                             (:options (first fields))
                             (:followups (first fields))
                             ))
               id)))
(defn find-itse-syotetty-tutkinto-content [form]
  (let [itse-syotetty-option (find-option-by-id (:content form) ktm/itse-syotetty-option-id)]
    (:followups itse-syotetty-option)))

(defn tutkinnot-required-and-missing [flat-form-content ui answers]
  (let [tutkinto-conf-component (some #(when (ktm/is-tutkinto-configuration-component? %) %) flat-form-content)
        required? (and (get-in ui [:koski-tutkinnot-wrapper :visible?]) (:mandatory tutkinto-conf-component))]
    (if required?
      (not (util/any-answered? answers
                               (util/find-descendant-ids-by-parent-id flat-form-content (:id tutkinto-conf-component))))
      false
    )))

(defn- is-question-group-of-koski-level [field-descriptor]
  (let [id (:id field-descriptor)]
    (boolean (some #(and (str/starts-with? id %)
                         (str/ends-with? id ktm/question-group-of-level))
                   ktm/koski-tutkinto-tasot))))

(defn- find-itse-syotetty-field-ids-beneath [conf-field-id flat-form-content]
  (let [tutkinto-level-fields (filter #(= (:followup-of %) conf-field-id) flat-form-content)
        top-level-itse-syotetty-fields (filter #(not (is-question-group-of-koski-level %)) tutkinto-level-fields)]
    (mapcat #(if (util/answerable? %)
               [(:id %)]
               (util/find-descendant-ids-by-parent-id flat-form-content (:id %)))
            top-level-itse-syotetty-fields)))

(defn tutkinto-option-selected [_ field flat-form-content answers]
  (if (is-question-group-of-koski-level field)
    (boolean (util/any-answered? answers (map :id (util/find-children-from-flat-content field flat-form-content))))
    (boolean (util/any-answered? answers (find-itse-syotetty-field-ids-beneath (:followup-of field) flat-form-content)))))
