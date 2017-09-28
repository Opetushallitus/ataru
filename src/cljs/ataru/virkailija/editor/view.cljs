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
   {:class (when selected? "editor-form__selected-row")
    :on-click (partial routes/navigate-to-click-handler (str "/lomake-editori/editor/" (:key form)))}
   [:span.editor-form__list-form-name (:name form)]
   [:span.editor-form__list-form-time (time->str (:created-time form))]
   [:span.editor-form__list-form-editor (:created-by form)]
   (when (< 0 used-in-haku-count)
     [:span.editor-form__list-form-used-in-haku-count used-in-haku-count])])

(defn form-list []
  (let [forms             (debounce-subscribe 333 [:state-query [:editor :forms]])
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])
        forms-in-use      (subscribe [:state-query [:editor :forms-in-use]])]
    (fn []
      (into (if @selected-form-key
              [:div.editor-form__list]
              [:div.editor-form__list.editor-form__list_expanded])
            (for [[key form] @forms
                  :when (not (:deleted form))
                  :let [selected?          (= key @selected-form-key)
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
    [:div.editor-form__checkbox-with-label
     {:key id}
     [:input.editor-form__checkbox
      {:id      id
       :checked checked?
       :type    "checkbox"
       :on-change (fn [_]
                    (dispatch [:editor/toggle-language lang-kwd]))}]
     [:label.editor-form__checkbox-label.editor-form__language-checkbox-label
      {:for id}
      (get lang-versions lang-kwd)]]))

(defn- lang-kwd->link [form lang-kwd & [text]]
  (let [text (if (nil? text)
               (-> lang-kwd name clojure.string/upper-case)
               text)]
    [:a
     {:key    (str "preview-" (name lang-kwd))
      :href   (str js/config.applicant.service_url "/hakemus/" (:key form) "?lang=" (name lang-kwd))
      :target "_blank"}
     text]))

(defn- get-org-name [org]
  (str
   (get-in org [:name :fi])
   (if (= "group" (:type org))
     " (ryhmä)"
     "")))

(defn- get-org-name-for-oid [oid orgs] (get-org-name (first (filter #(= oid (:oid %)) orgs))))

(defn form-owner-organization [form]
  (let [organizations (subscribe [:state-query [:editor :user-info :organizations]])
        many-orgs     (fn [orgs] (> (count orgs) 1))
        opened?       (r/atom false)
        toggle-open   (fn [evt] (swap! opened? not))]
    (fn [form]
      (let [selected-org-name (get-org-name-for-oid (:organization-oid form) @organizations)]
        ;; If name is not available, selected org is a suborganization. Currently it's unsure
        ;; if we want to show the form's organization at all in that case. If we do, we'll have to pass
        ;; organization name from server with the form and fetch it from organization service
        ;; and probably start caching those
        (when (not-empty selected-org-name)
          [:div.editor-form__owner-control
           [:span.editor-form__owner-label "Omistaja: "]
           [:a
            {:on-click toggle-open
             :class (if (many-orgs @organizations) "" "editor-form__form-owner-selection-disabled-link")}
            selected-org-name]
           (when (and @opened? (many-orgs @organizations))
             [:div.editor-form__form-owner-selection-anchor
              [:div.editor-form__owner-selection-arrow-up]
              (into [:div.editor-form__form-owner-selection--opened
                     {:on-click toggle-open}]
                    (map (fn [org]
                           [:div.editor-form__owner-selection-row
                            {:on-click (fn [evt] (dispatch [:editor/change-form-organization (:oid org)]))}
                            (get-org-name org)])
                         @organizations))])])))))

(defn form-toolbar [form]
  (let [languages (subscribe [:editor/languages])
        organizations (subscribe [:state-query [:editor :user-info :organizations]])]
    (fn [form]
      (let [languages @languages]
        [:div.editor-form__toolbar
         [:div.editor-form__language-controls
          [:div.editor-form__form-toolbar-checkbox-container
           (map (fn [lang-kwd]
                  (lang-checkbox lang-kwd (some? (some #{lang-kwd} languages))))
                (keys lang-versions))]
          [:span.editor-form__form-toolbar-header-text
           (if (= (count languages) 1)
             (lang-kwd->link form (first languages) "Lomakkeen esikatselu")
             [:span
              "Lomakkeen esikatselu  "
              (map (partial lang-kwd->link form) languages)])]]
         [form-owner-organization form]]))))

(defn form-in-use-warning
  [form]
  (let [forms-in-use         (subscribe [:state-query [:editor :forms-in-use]])
        hakija-haku-base-url (str js/config.applicant.service_url "/hakemus/haku/")]
    (fn [form]
      (when-let [form-used-in-hakus (get @forms-in-use (keyword (:key form)))]
        [:div.editor-form__in_use_notification.animated.flash
         [:span.editor-form__used-in-haku-heading "Tämä lomake on haun käytössä"]
         [:ul.editor-form__used-in-haku-list
          (for [haku (vals form-used-in-hakus)]
            [:li {:key (str "form-used-in-haku_" (:haku-oid haku))}
             [:a {:href   (str "/tarjonta-app/index.html#/haku/" (:haku-oid haku))
                  :target "_blank"} (some #(get (:haku-name haku) %) [:fi :sv :en])]
             [:span " | "]
             [:a {:href   (str hakija-haku-base-url (:haku-oid haku))
                  :target "_blank"} "Lomake"]])]]))))

(defn- close-form []
  [:a {:on-click (fn [event]
                   (dispatch [:set-state [:editor :selected-form-key] nil])
                   (routes/navigate-to-click-handler "/lomake-editori/editor"))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn editor-panel []
  (let [form         (subscribe [:editor/selected-form])]
    (fn []
      (when @form ;; Do not attempt to show form edit controls when there is no selected form (form list is empty)
        [:div.editor-form__panel-container
         [close-form]
         [:div
          [editor-name]
          ^{:key (str "form-toolbar-" (:key @form))}
          [form-toolbar @form]
          ^{:key (str "form-in-use-warning-" (:key @form))}
          [form-in-use-warning @form]]
         [c/editor]]))))

(defn editor []
    [:div
     [:div.editor-form__container.panel-content
      [form-header-row]
      [form-list]]
     [editor-panel]])
