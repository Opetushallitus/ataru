(ns ataru.virkailija.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [ataru.cljs-util :refer [debounce-subscribe wrap-scroll-to]]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.temporal :refer [time->str]]
            [ataru.virkailija.routes :as routes]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn form-row [form selected? used-in-haku-count]
  [:a.editor-form__row
   {:href  (str "/lomake-editori/editor/" (:key form))
    :class (when selected? "editor-form__selected-row")
    :on-click routes/anchor-click-handler}
   [:span.editor-form__list-form-name (:name form)]
   [:span.editor-form__list-form-time (time->str (:created-time form))]
   [:span.editor-form__list-form-editor (:created-by form)]
   (when (< 0 used-in-haku-count)
     [:span.editor-form__list-form-used-in-haku-count used-in-haku-count])])

(defn form-list []
  (let [forms            (debounce-subscribe 333 [:state-query [:editor :forms]])
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])
        forms-in-use      (subscribe [:state-query [:editor :forms-in-use]])]
    (fn []
      (into (if @selected-form-key
              [:div.editor-form__list]
              [:div.editor-form__list.editor-form__list_expanded])
            (for [[key form] @forms
                  :let [selected? (= key @selected-form-key)
                        used-in-haku-count (count (keys (get @forms-in-use (keyword key))))]]
              ^{:key (str "form-list-item-" key)}
              (if selected?
                [wrap-scroll-to [form-row form selected? used-in-haku-count]]
                [form-row form selected? used-in-haku-count]))))))

(defn- add-form []
  [:a.editor-form__control-button.editor-form__control-button--enabled
   {:on-click (fn [evt]
                (.preventDefault evt)
                (dispatch [:editor/add-form]))}
   "Uusi lomake"])

(defn- copy-form []
  (let [form-key  (subscribe [:state-query [:editor :selected-form-key]])
        disabled? (reaction (nil? @form-key))]
    (fn []
      [:a.editor-form__control-button
       {:on-click (fn [event]
                    (.preventDefault event)
                    (dispatch [:editor/copy-form]))
        :class    (if @disabled?
                    "editor-form__control-button--disabled"
                    "editor-form__control-button--enabled")}
       "Kopioi lomake"])))

(defn- remove-form []
  (let [form-key  (subscribe [:state-query [:editor :selected-form-key]])
        confirm?  (subscribe [:state-query [:editor :show-remove-confirm-dialog?]])
        disabled? (reaction (nil? @form-key))]
    (fn []
      [:a.editor-form__control-button
       {:on-click (fn [event]
                    (.preventDefault event)
                    (if @confirm?
                      (dispatch [:editor/remove-form])
                      (dispatch [:set-state [:editor :show-remove-confirm-dialog?] true])))
        :class    (cond
                    @confirm? "editor-form__control-button--confirm"
                    @disabled? "editor-form__control-button--disabled"
                    :else "editor-form__control-button--enabled")}
       (if @confirm?
         "Vahvista poisto"
         "Poista lomake")])))

(defn- form-controls []
  [:div.editor-form__form-controls-container
   [add-form]
   [copy-form]
   [remove-form]])

(defn- form-header-row []
  [:div.editor-form__form-header-row
   [:h1.editor-form__form-heading "Lomakkeet"]
   [form-controls]])

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
                               {:key           (str "editor-name-" (:key @form)) ; needed to trigger component-did-update
                                :type          "text"
                                :default-value @form-name
                                :placeholder   "Lomakkeen nimi"
                                :on-change     #(dispatch [:editor/change-form-name (.-value (.-target %))])}])})))

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
     {:href   (str js/config.applicant.service_url "/hakemus/" (:key form) "/" (name lang-kwd))
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

(defn form-in-use-warning
  [form]
  (let [forms-in-use (subscribe [:state-query [:editor :forms-in-use]])]
    (fn [form]
      (when-let [form-used-in-hakus ((keyword (:key form)) @forms-in-use)]
        [:div.editor-form__module-wrapper
         [:div.editor-form__module-fields
          [:span.editor-form__used-in-haku-count (str (count (keys form-used-in-hakus)))]
          [:span.editor-form__used-in-haku-heading "Lomake on käytössä!"]
          [:ul.editor-form__used-in-haku-list
           (for [haku (vals form-used-in-hakus)]
             [:li {:key (str "form-used-in-haku_" (:haku-oid haku))} [:a {:href (str "/tarjonta-service/url/goes/here/" (:haku-oid haku))} (:haku-name haku)]])]]]))))

(defn editor-panel []
  (let [form         (subscribe [:editor/selected-form])]
    (fn []
      (when @form ;; Do not attempt to show form edit controls when there is no selected form (form list is empty)
        [:div.panel-content
         [:div
          [editor-name]
          ^{:key (str "language-toolbar-" (:key @form))}
          [language-toolbar @form]
          ^{:key (str "form-in-use-warning-" (:key @form))}
          [form-in-use-warning @form]]
         [c/editor]]))))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [form-header-row]
      [form-list]]
     [editor-panel]])
