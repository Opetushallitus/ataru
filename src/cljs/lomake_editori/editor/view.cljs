(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [cljs-uuid-utils.core :as uuid]
            [lomake-editori.dev.lomake :as l]))

(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (assoc-in db [:editor :selected-form]
      clicked-form)))

(register-handler
  :editor/add-form
  (fn [db _]
    (let [id (uuid/uuid-string (uuid/make-random-uuid))
          new-form  {:id id :name "Uusi lomake"}]
      (-> db
          (assoc-in [:editor :selected-form] new-form)
          (update-in [:editor :forms] assoc id new-form)))))

(register-handler
  :editor/change-form-name
  (fn [db [_ {:keys [id new-form-name]}]]
    (update-in db [:editor :forms form-id]
               assoc :name new-name)))

(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form :id]])]
    (fn []
      (into [:div.editor-form__list]
        (mapv (fn [[id form]]
                [:div.editor-form__row
                 {:class (when (= id @selected-form-id) "editor-form__selected-row")
                  :on-click #(dispatch [:editor/select-form form])} (:name form)])
              @forms)))))

(defn add-form []
  [:button.button {:on-click #(dispatch [:editor/add-form])} "Uusi lomake"])

(defn editor-panel []
  (let [selected-form (subscribe [:state-query [:editor :selected-form]])]
    (fn []
      [:div.panel-content
       [:div.editor-form__form-name-row
        [:input.editor-form__form-name-input {:type "text"
                                              :value (:name @selected-form)
                                              :on-change #(dispatch [:editor/change-form-name {:id (:id @selected-form) :new-form-name (.-value (.-target %))}])
                                              :placeholder "Lomakkeen nimi"}]
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
