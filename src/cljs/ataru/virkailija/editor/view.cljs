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
   {:href  (str "#/editor/" (:key form))
    :class (when selected? "editor-form__selected-row")}
   [:span.editor-form__list-form-name (:name form)]
   [:span.editor-form__list-form-time (time->str (:created-time form))]
   [:span.editor-form__list-form-editor (:created-by form)]])

(defn form-list []
  (let [forms            (debounce-subscribe 333 [:state-query [:editor :forms]])
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])]
    (fn []
      (into (if @selected-form-key
              [:div.editor-form__list]
              [:div.editor-form__list.editor-form__list_expanded])
            (for [[key form] @forms
                  :let [selected? (= key @selected-form-key)]]
              ^{:key key}
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
                               {:key         (:key @form) ; needed to trigger component-did-update
                                :type        "text"
                                :default-value @form-name
                                :placeholder "Lomakkeen nimi"
                                :on-change   #(dispatch [:editor/change-form-name (.-value (.-target %))])}])})))

(def ^:private lang-versions
  {:fi "Suomi"
   :sv "Ruotsi"
   :en "Englanti"})

(defn- lang-checkbox [lang-kwd checked?]
  (let [id (str "lang-checkbox-" (name lang-kwd))]
    [:div
     {:key id}
     [:input.editor-form__checkbox
      {:id      id
       :checked checked?
       :type    "checkbox"
       :on-change (fn [_]
                    (dispatch [:editor/toggle-language lang-kwd]))}]
     [:label.editor-form__checkbox-label.editor-form__language-toolbar-checkbox
      {:for id}
      (get lang-versions lang-kwd)]]))

(defn- lang-kwd->link [form lang-kwd & [text]]
  (let [text (if (nil? text)
               (get lang-versions lang-kwd)
               text)]
    [:a
     {:href   (str js/config.applicant.service_url "/hakemus/" (:key form))
      :target "_blank"}
     text]))

(defn language-toolbar [form]
  (let [languages (subscribe [:editor/languages])
        visible?  (r/atom true)]
    (fn [form]
      (let [languages @languages]
        [:div.editor-form__language-toolbar-outer
         [:div.editor-form__language-toolbar-inner
          [:a
           {:on-click (fn [_]
                        (swap! visible? not)
                        nil)}
           "Kieliversiot "
           [:i.zmdi.zmdi-chevron-down
            {:class (if @visible? "zmdi-chevron-up" "zmdi-chevron-down")}]]
          [:span.editor-form__language-toolbar-header-text
           (if (= (count languages) 1)
             (lang-kwd->link form (first languages) "Esikatselu")
             [:span
              "Esikatselu: "
              (map-indexed (fn [idx lang-kwd]
                             (cond-> [:span
                                      {:key idx}
                                      (lang-kwd->link form lang-kwd)]
                               (> (dec (count languages)) idx)
                               (conj [:span " | "])))
                           languages)])]]
         [:div.editor-form__language-toolbar-checkbox-container
          (when-not @visible?
            {:style {:display "none"}})
          (map (fn [lang-kwd]
                 (lang-checkbox lang-kwd (some #{lang-kwd} languages)))
               (keys lang-versions))]]))))

(defn editor-panel []
  (let [form (subscribe [:editor/selected-form])]
    (fn []
      (when @form ;; Do not attempt to show form edit controls when there is no selected form (form list is empty)
        [:div.panel-content
         [:div
          [editor-name]
          [language-toolbar @form]]
         [c/editor]]))))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [add-form]
      [form-list]]
     [editor-panel]])
