(ns lomake-editori.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [lomake-editori.dev.lomake :as l]
            [lomake-editori.editor.core :as c]
            [lomake-editori.editor.subs]
            [lomake-editori.soresu.component :as component]
            [lomake-editori.temporal :refer [time->str]]
            [taoensso.timbre :refer-macros [spy debug]]))


(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      (into [:div.editor-form__list]
            (for [form (vals @forms)]
              [:div.editor-form__row
               {:key      (:id form)
                :class    (when (= (:id form)
                                   @selected-form-id)
                            "editor-form__selected-row")
                :on-click #(dispatch [:editor/select-form form])}
               [:span.editor-form__list-form-name (:name form)]
               [:span.editor-form__list-form-time (time->str (:modified-time form))]
               [:span.editor-form__list-form-editor (let [a (:author form)]
                                                      ;; Use this when there's real data:
                                                      ;;(str (:last a) " " (:first a))
                                                      ""
                                                      )]])))))

(defn add-form []
  [:div.editor-form__add-new
   [:a {:on-click (fn [evt] (.preventDefault evt) (dispatch [:editor/add-form])) :href "#"} "Luo uusi lomake"]])

(defn editor-name [form-name]
  (let [typing? (r/atom false)]
    (r/create-class
      {:display-name "editor-name"
       :component-did-update (fn [element]
                               (when-not @typing?
                                 (doto (r/dom-node element)
                                   (.focus)
                                   (.select))))
       :reagent-render       (fn [form-name]
                               [:input.editor-form__form-name-input
                                {:type                "text"
                                 :value               form-name
                                 :placeholder         "Lomakkeen nimi"
                                 :on-blur             #(do (reset! typing? false)
                                                           nil)
                                 :on-change           #(do
                                                         (reset! typing? true)
                                                         (dispatch-sync [:editor/change-form-name (.-value (.-target %))]))}])})))

(defn editor-panel []
  (let [form (subscribe [:editor/selected-form])]
    (when @form ;; Do not attempt to show form edit controls when there is no selected form (form list is empty)
      [:div.panel-content
       [:div
        [editor-name (:name @form)]]
       [:div.editor-form__preview-link-row [:a.editor-form__preview-link {:href (str "#/editor/" (:id @form))} "Esikatsele lomake"]]
       [c/editor]])))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [add-form]
      [form-list]]
     [editor-panel]])
