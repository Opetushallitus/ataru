(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [lomake-editori.dev.lomake :as l]))

(register-handler :editor/select-form (fn [db [_ clicked-row-id]]
                                        (assoc-in db [:editor :selected-form-id] clicked-row-id)))


(defn form-list []
  (let [forms (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      (into [:div.editor-form__list]
        (mapv (fn [form]
                [:div.editor-form__row
                 {:class (when (= (:id form) @selected-form-id) "editor-form__selected-row")
                  :on-click #(dispatch [:editor/select-form (:id form)])} (:name form)])
              @forms)))))

(defn add-form []
  [:button.button "Uusi lomake"])

(defn editor-panel []
  (fn []
    [:div.panel-content
     [component/form-component
      (merge l/controller
             l/translations
             (l/field l/text-field)
             {:lang  :sv
              :value "Valmis arvo"})]]))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [form-list]
      [add-form]]
     [editor-panel]])
