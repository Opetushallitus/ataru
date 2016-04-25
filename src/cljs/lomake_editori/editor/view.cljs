(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [cljs.core.match :refer-macros [match]]
            [cljs-uuid-utils.core :as uuid]
            [cljs-time.core :as time]
            [cljs-time.format :as time-format]
            [lomake-editori.dev.lomake :as l]
            [taoensso.timbre :refer-macros [spy]]))

(def ^:private time-formatter (time-format/formatter "dd.MM.yyyy HH:mm"))

(defn- time->str [raw-timestamp]
  (time-format/unparse time-formatter (time-format/parse raw-timestamp)))

(register-handler
  :editor/select-form
  (fn [db [_ clicked-form]]
    (assoc-in db [:editor :selected-form]
      clicked-form)))

(register-handler
  :editor/add-form
  (fn [db _]
    (let [id (uuid/uuid-string (uuid/make-random-uuid))
          new-form  {:id id :name "Uusi lomake" :modified-time (time-format/unparse (time-format/formatters :date-time) (time/now))}]
      (-> db
          (assoc-in [:editor :selected-form] new-form)
          (update-in [:editor :forms] assoc id new-form)))))

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
        (mapv (fn [[id form]]
                [:div.editor-form__row
                 {:class (when (= id @selected-form-id) "editor-form__selected-row")
                  :on-click #(dispatch [:editor/select-form form])}
                 [:span.editor-form__list-form-name  (str (:name form))]
                 [:span.editor-form__list-form-time (time->str (:modified-time form))]
                 ])
              @forms)))))

(defn add-form []
  [:button.button {:on-click #(dispatch [:editor/add-form])} "Uusi lomake"])

(defn editor-panel []
  (let [selected-form-id (subscribe [:state-query [:editor :selected-form :id]])
        selected-form (reaction @(subscribe [:state-query [:editor :forms @selected-form-id :name]]))]
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
