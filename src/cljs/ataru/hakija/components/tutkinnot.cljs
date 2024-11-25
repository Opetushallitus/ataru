(ns ataru.hakija.components.tutkinnot
  (:require [clojure.string :refer [join ends-with?]]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.translations.translation-util :as tu]
            [ataru.util :as util]
            [ataru.component-data.koski-tutkinnot-module :as ktm]))

(defn selected-koski-tutkinnot-content [conf-field-descriptor]
  (let [selected-tutkinto-levels @(subscribe [:application/selected-tutkinto-levels])
        selected-koski-levels (filterv #(not (= ktm/itse-syotetty-option-id %)) selected-tutkinto-levels)]
    (filterv
      (fn [option] (some? (some #(when (= (:id option) %) %) selected-koski-levels)))
      (:options conf-field-descriptor))))

(defn get-tutkinto-of-level [level-id id]
  (let [tutkinnot-of-level @(subscribe [:application/koski-tutkinnot-of-level level-id])
        id-val (if (coll? id) (first id) id)]
    (some #(when (= (:id %) id-val) %) tutkinnot-of-level)))

(defn find-answer-from-koskidata [field-descriptor koski-data lang]
  (let [id (:id field-descriptor)]
    (cond (ends-with? id ktm/tutkinto-nimi-field-postfix)
          (util/from-multi-lang (:tutkintonimi koski-data) lang)
          (ends-with? id ktm/koulutusohjelma-field-postfix)
          (util/from-multi-lang (:koulutusohjelmanimi koski-data) lang)
          (ends-with? id ktm/oppilaitos-field-postfix)
          (util/from-multi-lang (:toimipistenimi koski-data) lang)
          (ends-with? id ktm/valmistumispvm-field-postfix)
          (:valmistumispvm koski-data)
          :else
          "")))

(defn itse-syotetty-tutkinnot-content [conf-field-descriptor]
  (get-in (some #(when (= ktm/itse-syotetty-option-id (:id %)) %) (:options conf-field-descriptor)) [:followups] []))

(defn tutkinto-group [label field-descriptor idx can-remove lang child-components]
  [:div.application__tutkinto-group-container
   [:div.application__tutkinto-header
    label
    (when can-remove
      [:div.application__tutkinto-header.remove-tutkinto
       [:a.application__tutkinto-header.remove-tutkinto.link
        {:on-click (fn add-question-group-row [event]
                     (.preventDefault event)
                     (dispatch [:application/remove-question-group-row
                                field-descriptor
                                idx]))}
        [:span.application__tutkinto-header.remove-tutkinto.button-text (tu/get-hakija-translation :poista lang)]
        [:i.zmdi.zmdi-delete.application__tutkinto-header.remove-tutkinto.button-icon]]])]
   [:div.application__form-multi-choice-followups-outer-container
    {:tab-index 0}
    [:div.application__form-multi-choice-followups-indicator]
    (into [:div.application__tutkinto-entity-container] child-components)]])

(defn add-button [on-click lang]
  [:div.application__add-tutkinto
   [:button.application__add-tutkinto.button
    {:on-click on-click}
    [:i.zmdi.zmdi-plus.application__add-tutkinto.button-icon]
    [:span.application__add-tutkinto.button-text (tu/get-hakija-translation :add-tutkinto lang)]]])

(defn add-tutkinto-button [field-descriptor lang]
  (add-button (fn [event]
                (.preventDefault event)
                (dispatch [:application/add-question-group-row field-descriptor]))
              lang))

(defn fixed-tutkinto-item [_ tutkinto _ _]
  (let [lang @(subscribe [:application/form-language])
        localized-val (fn [field] (get-in tutkinto [field (keyword lang)]))
        upper-row (join ", "
                        (filterv #(some? %) [(localized-val :tutkintonimi) (localized-val :koulutusohjelmanimi)
                                             (:valmistumispvm tutkinto)]))
        lower-row (localized-val :toimipistenimi)]
    (fn [parent-field-descriptor _ idx checked?]
      (let [set-checked-as-needed (fn [] (if checked?
                                           {:class " checked-koski-tutkinto"}
                                           nil))]
        [:div.application__fixed-koski-tutkinto-item (set-checked-as-needed)
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:input.application__form-checkbox.embedded
           (merge {:id        (str "checkbox-" (:id parent-field-descriptor) "-" idx)
                   :type      "checkbox"
                   :read-only true
                   :checked   checked?
                   :role      "option"})]]
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:span.application__fixed-koski-tutkinto-item.inner-content.upper-row (set-checked-as-needed) upper-row]
          (when lower-row
            [:span lower-row])]]))))
