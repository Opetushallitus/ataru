(ns ataru.virkailija.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [ataru.cljs-util :refer [debounce-subscribe]]
            [ataru.virkailija.dev.lomake :as l]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.virkailija.soresu.component :as component]
            [ataru.virkailija.temporal :refer [time->str]]
            [taoensso.timbre :refer-macros [spy debug]]))


(defn form-list []
  (let [forms            (debounce-subscribe 333 [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      (into [:div.editor-form__list]
            (for [[id form] @forms]
              [:a.editor-form__row
               {:href     (str "#/editor/" id)
                :key      id
                :class    (when (= id
                                   @selected-form-id)
                            "editor-form__selected-row")}
               [:span.editor-form__list-form-name (:name form)]
               [:span.editor-form__list-form-time (time->str (:modified-time form))]
               [:span.editor-form__list-form-editor (:modified-by form)]])))))

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
