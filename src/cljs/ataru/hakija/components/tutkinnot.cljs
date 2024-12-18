(ns ataru.hakija.components.tutkinnot
  (:require [clojure.string :refer [join]]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.translations.translation-util :as tu]))

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
        [:i.zmdi.zmdi-delete.application__tutkinto-button-icon]
        [:span.application__tutkinto-button-text (tu/get-hakija-translation :poista lang)]]])]
   [:div.application__form-multi-choice-followups-outer-container
    {:tab-index 0}
    [:div.application__form-multi-choice-followups-indicator]
    (into [:div.application__tutkinto-entity-container] child-components)]])

(defn add-button [on-click lang]
  [:div.application__show-additional-tutkinnot
   [:button.application__show-additional-tutkinnot.button
    {:on-click on-click}
    [:i.zmdi.zmdi-plus.application__tutkinto-button-icon]
    [:span.application__show-additional-tutkinnot.button-text (tu/get-hakija-translation :add-tutkinto lang)]]])

(defn add-tutkinto-button [field-descriptor lang]
  (add-button (fn [event]
                (.preventDefault event)
                (dispatch [:application/add-question-group-row field-descriptor]))
              lang))

(defn hide-additional-tutkinnot-button [on-click lang]
  [:div.application__hide-additional-tutkinnot
   [:a.application__hide-additional-tutkinnot.link
    {:on-click on-click}
    [:i.zmdi.zmdi-delete.application__tutkinto-button-icon]
    [:span.application__tutkinto-button-text (tu/get-hakija-translation :poista-osio lang)]]])

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
