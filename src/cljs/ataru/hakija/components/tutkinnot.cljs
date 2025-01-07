(ns ataru.hakija.components.tutkinnot
  (:require [re-frame.core :refer [dispatch]]
            [ataru.translations.translation-util :as tu]))

(defn is-tutkinto-configuration-component? [field-descriptor]
  (= "tutkinto-properties" (:category field-descriptor)))

(defn itse-syotetty-tutkinnot-content [conf-field-descriptor]
  (get-in (some #(when (= "itse-syotetty" (:id %)) %) (:options conf-field-descriptor)) [:followups] []))

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

(defn add-tutkinto-button [field-descriptor lang]
  [:div.application__add-tutkinto
   [:button.application__add-tutkinto.button
    {:on-click (fn [event]
                 (.preventDefault event)
                 (dispatch [:application/add-question-group-row field-descriptor]))}
    [:i.zmdi.zmdi-plus.application__add-tutkinto.button-icon]
    [:span.application__add-tutkinto.button-text (tu/get-hakija-translation :add-tutkinto lang)]]])

;TODO
(defn fixed-tutkinto-container []
  [:div.application__fixed-koski-tutkinto-container
   ])
