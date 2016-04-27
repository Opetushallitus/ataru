(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [lomake-editori.temporal :refer [time->str]]
            [lomake-editori.dev.lomake :as l]
            [taoensso.timbre :refer-macros [spy debug]]))


(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form :id]])]
    (fn []
      (into [:div.editor-form__list]
            (for [form (vals @forms)]
              [:div.editor-form__row
               {:key      (:id form)
                :class    (when (= (:id form) (str @selected-form-id)) "editor-form__selected-row")
                :on-click #(dispatch [:editor/select-form form])}
               [:span.editor-form__list-form-name (:name form)]
               [:span.editor-form__list-form-time (time->str (:modified-time form))]
               [:span.editor-form__list-form-editor (let [a (:author form)]
                                                      (str (:last a) " " (:first a)))]])))))

(defn add-form []
  [:button.button {:on-click #(dispatch [:editor/add-form])} "Uusi lomake"])

(defn editor-panel []
  (let [selected-form-id (subscribe [:state-query [:editor :selected-form :id]])
        forms            (subscribe [:state-query [:editor :forms]])
        selected-form    (reaction
                           (:name (get @forms @selected-form-id)))]
    (fn []
      [:div.panel-content
       [:div.editor-form__form-name-row
        [:input.editor-form__form-name-input
         {:type        "text"
          :value       @selected-form
          :placeholder "Lomakkeen nimi"
          :on-change   #(dispatch-sync [:editor/change-form-name (.-value (.-target %))])}]
        [:a.editor-form__preview-link {:href "#"} "Esikatsele lomake"]]
       [component/form-component
        (merge l/controller
               l/translations
               (l/field l/text-field)
               {:lang  :sv
                :value "Valmis arvo"})]])))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [form-list]
      [add-form]]
     [editor-panel]])
