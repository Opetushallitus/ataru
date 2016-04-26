(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [lomake-editori.temporal :refer [time->str coerce-timestamp]]
            [lomake-editori.handlers :refer [post]]
            [goog.date :as gd]
            [re-com.core :as re-com]
            [cljs.core.match :refer-macros [match]]
            [lomake-editori.dev.lomake :as l]
            [taoensso.timbre :refer-macros [spy debug]]))


(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (assoc-in db [:editor :selected-form]
              clicked-form)))

(register-handler
  :editor/add-form
  (fn [db _]
    (post "/lomake-editori/api/form"
          {:name   "Uusi lomake"
           :author {:last  "Testaaja" ;; placeholder
                    :first "Teppo"}}
          (fn [db new-form]
            (let [form-with-time (-> ((coerce-timestamp :modified-time) new-form)
                                     (assoc :author {:last  "Testaaja" ;; placeholder
                                                     :first "Teppo"}))]
              (-> db
                  (assoc-in [:editor :selected-form] form-with-time)
                  (assoc-in [:editor :forms (:id form-with-time)] form-with-time)))))
    db))

(register-handler
  :editor/change-form-name
  (fn [db [_ new-form-name]]
    (let [selected-form (-> db :editor :selected-form)
          name-before-edit (:name selected-form)]
      (update-in db [:editor :forms (:id selected-form)]
                 assoc :name
                 (if (empty? new-form-name)
                   name-before-edit
                   new-form-name)))))

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
