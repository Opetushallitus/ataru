(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [cljs-uuid-utils.core :as uuid]
            [lomake-editori.dev.lomake :as l]))

(register-handler :editor/select-form (fn [db [_ clicked-row-id]]
                                        (assoc-in db [:editor :selected-form-id] clicked-row-id)))

(register-handler :editor/add-form (fn [db _]
                                     (let [new-form {:id (uuid/uuid-string (uuid/make-random-uuid)) :name "Uusi lomake"}
                                           new-forms (conj (-> db (:editor) (:forms)) new-form)]
                                       (println new-form)
                                       (-> db
                                           (assoc-in [:editor :selected-form-id] (:id new-form))
                                           (assoc-in [:editor :forms] new-forms)))))

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
  [:button.button {:on-click #(dispatch [:editor/add-form])} "Uusi lomake"])

(defn editor-panel []
  (let [forms (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])
        selected-form (fn [] (first (filter #(= @selected-form-id (:id %)) @forms)))]
    (fn []
      [:div.panel-content
       [:div.editor-form__form-name-row
        [:input.editor-form__form-name-input {:type "text" :value (:name (selected-form)) :placeholder "Lomakkeen nimi"}]
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
