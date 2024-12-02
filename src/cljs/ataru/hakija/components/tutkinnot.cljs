(ns ataru.hakija.components.tutkinnot
  (:require [clojure.string :refer [join ends-with?]]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.translations.translation-util :as tu]
            [ataru.component-data.koski-tutkinnot-module :as ktm]
            [ataru.translations.texts :refer [koski-tutkinnot-texts]]))

(defn get-tutkinto-idx [level id]
  (let [tutkinto-id (str level "-" ktm/tutkinto-id-field-postfix)
        checked-tutkinto-ids (flatten (:value @(subscribe [:application/answer tutkinto-id])))]
    (when (some #(when (= % id) %) checked-tutkinto-ids)
      (.indexOf checked-tutkinto-ids id))))

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
                                :koski-tutkinto-field (:koski-tutkinto-field field)})
       [{:label-id :tutkinto-followup-label :koski-tutkinto-field :tutkintonimi}
        {:label-id :koulutusohjelma-followup-label :koski-tutkinto-field :koulutusohjelmanimi}
        {:label-id :oppilaitos-followup-label :koski-tutkinto-field :toimipistenimi}
        {:label-id :valmistumispvm-followup-label :koski-tutkinto-field :valmistumispvm}]))

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

(defn fixed-tutkinto-item [tutkinto _ _]
  (let [lang @(subscribe [:application/form-language])
        localized-val (fn [field] (get-in tutkinto [field (keyword lang)]))
        upper-row (join ", "
                        (filterv #(some? %) [(localized-val :tutkintonimi) (localized-val :koulutusohjelmanimi)
                                             (:valmistumispvm tutkinto)]))
        lower-row (localized-val :toimipistenimi)]
    (fn [_ id checked?]
      (let [set-checked-as-needed (fn [] (if checked?
                                           {:class " checked-koski-tutkinto"}
                                           nil))]
        [:div.application__fixed-koski-tutkinto-item (set-checked-as-needed)
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:input.application__form-checkbox.embedded
           (merge {:id        (str "checkbox-" id)
                   :type      "checkbox"
                   :read-only true
                   :checked   checked?
                   :role      "option"})]]
         [:div.application__fixed-koski-tutkinto-item.inner-content (set-checked-as-needed)
          [:span.application__fixed-koski-tutkinto-item.inner-content.upper-row (set-checked-as-needed) upper-row]
          (when lower-row
            [:span lower-row])]]))))