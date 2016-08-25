(ns ataru.virkailija.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler]]
            [reagent.core :as r]
            [ataru.cljs-util :refer [debounce-subscribe wrap-scroll-to]]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.temporal :refer [time->str]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn form-row [form selected?]
  [:a.editor-form__row
   {:href  (str "#/editor/" (:id form))
    :class (when selected? "editor-form__selected-row")}
   [:span.editor-form__list-form-name (:name form)]
   [:span.editor-form__list-form-time (time->str (:modified-time form))]
   [:span.editor-form__list-form-editor (:created-by form)]])

(defn form-list []
  (let [forms            (debounce-subscribe 333 [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])]
    (fn []
      (into (if @selected-form-id
              [:div.editor-form__list]
              [:div.editor-form__list.editor-form__list_expanded])
            (for [[id form] @forms
                  :let [selected? (= id @selected-form-id)]]
              ^{:key id}
              (if selected?
                [wrap-scroll-to [form-row form selected?]]
                [form-row form selected?]))))))

(defn add-form []
  [:div.editor-form__add-new
   [:a {:on-click (fn [evt]
                    (.preventDefault evt)
                    (dispatch [:editor/add-form]))
        :href "#"}
    "Luo uusi lomake"]])

(defn editor-name []
  (let [form              (subscribe [:editor/selected-form])
        new-form-created? (subscribe [:state-query [:editor :new-form-created?]])
        form-name         (reaction (:name @form))]
    (r/create-class
      {:display-name        "editor-name"
       :component-did-mount (fn [element]
                              (when @new-form-created?
                                (do
                                  (doto (r/dom-node element)
                                    (.focus)
                                    (.select))
                                  (dispatch [:set-state [:editor :new-form-created?] false]))))
       :reagent-render      (fn []
                              [:input.editor-form__form-name-input
                               {:key         (:id @form) ; needed to trigger component-did-update
                                :type        "text"
                                :default-value @form-name
                                :placeholder "Lomakkeen nimi"
                                :on-change   #(dispatch [:editor/change-form-name (.-value (.-target %))])}])})))

(defn editor-panel []
  (let [form            (subscribe [:editor/selected-form])]
    (fn []
      (when @form ;; Do not attempt to show form edit controls when there is no selected form (form list is empty)
        [:div.panel-content
         [:div
          [editor-name]]
         [:div.editor-form__link-row
          [:div
           [:span [:a.editor-form__preview-link
                                                {:href   (str js/config.applicant.service_url "/hakemus/" (:id @form))
                                                 :target "_blank"}
                                                "Esikatsele lomake"]]]]
         [c/editor]]))))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [add-form]
      [form-list]]
     [editor-panel]])
