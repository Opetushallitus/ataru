(ns ataru.virkailija.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [ataru.cljs-util :refer [debounce-subscribe wrap-scroll-to]]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.component-data.component :as component]
            [ataru.virkailija.temporal :refer [time->str]]
            [ataru.virkailija.routes :as routes]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn form-row [form selected? used-in-haku-count]
  [:a.editor-form__row
   {:class (when selected? "editor-form__selected-row")
    :on-click (partial routes/navigate-to-click-handler (str "/lomake-editori/editor/" (:key form)))}
   [:span.editor-form__list-form-name (some #(get-in form [:name %])
                                            [:fi :sv :en])]
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
  [:button.editor-form__control-button.editor-form__control-button--enabled
   {:on-click (fn [evt]
                (.preventDefault evt)
                (dispatch [:editor/add-form]))}
   "Uusi lomake"])

(defn- copy-form []
  (let [form-key  (subscribe [:state-query [:editor :selected-form-key]])
        disabled? (reaction (nil? @form-key))]
    (fn []
      [:button.editor-form__control-button
       {:on-click (fn [event]
                    (.preventDefault event)
                    (dispatch [:editor/copy-form]))
        :disabled @disabled?
        :class    (if @disabled?
                    "editor-form__control-button--disabled"
                    "editor-form__control-button--enabled")}
       "Kopioi lomake"])))

(defn- remove-form []
  (case @(subscribe [:editor/remove-form-button-state])
    :active
    [:button.editor-form__control-button--enabled.editor-form__control-button
     {:on-click #(dispatch [:editor/start-remove-form])}
     "Poista lomake"]
    :confirm
    [:button.editor-form__control-button--confirm.editor-form__control-button
     {:on-click #(dispatch [:editor/confirm-remove-form])}
     "Vahvista poisto"]
    :disabled
    [:button.editor-form__control-button--disabled.editor-form__control-button
     {:disabled true}
     "Poista lomake"]))

(defn- form-controls []
  [:div.editor-form__form-controls-container
   [add-form]
   [copy-form]
   [remove-form]])

(defn- form-header-row []
  [:div.editor-form__form-header-row
   [:h1.editor-form__form-heading "Lomakkeet"]
   [form-controls]])

(defn- editor-name-input [lang focus?]
  (let [form (subscribe [:editor/selected-form])
        new-form-created? (subscribe [:state-query [:editor :new-form-created?]])]
    (r/create-class
     {:component-did-update (fn [this]
                              (when (and focus? @new-form-created?)
                                (do
                                  (.focus (r/dom-node this))
                                  (.select (r/dom-node this)))))
      :reagent-render (fn [lang focus?]
                        [:input.editor-form__form-name-input
                         {:type        "text"
                          :value       (get-in @form [:name lang])
                          :placeholder "Lomakkeen nimi"
                          :on-change   #(do (dispatch [:editor/change-form-name lang (.-value (.-target %))])
                                            (dispatch [:set-state [:editor :new-form-created?] false]))
                          :on-blur     #(dispatch [:set-state [:editor :new-form-created?] false])}])})))

(defn- editor-name-wrapper [lang focus? lang-tag?]
  [:div.editor-form__form-name-input-wrapper
   [editor-name-input lang focus?]
   (when lang-tag?
     [:div.editor-form__form-name-input-lang
      (clojure.string/upper-case (name lang))])])

(defn- editor-name []
  (let [[l & ls] @(subscribe [:editor/languages])]
    [:div
     ^{:key (str "editor-name-" l)}
     [editor-name-wrapper l true (not-empty ls)]
     (doall (for [l ls]
              ^{:key (str "editor-name-" l)}
              [editor-name-wrapper l false true]))]))

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

(defn- fold-all []
  (let [all-folded? @(subscribe [:editor/all-folded])]
    [:div.editor-form__fold-all
     [:div.editor-form__fold-all-slider
      (if all-folded?
        {:class "editor-form__fold-all-slider-left"
         :on-click #(dispatch [:editor/unfold-all])}
        {:class "editor-form__fold-all-slider-right"
         :on-click #(dispatch [:editor/fold-all])})
      [:div.editor-form__fold-all-label-left
       "Osiot auki"]
      [:div.editor-form__fold-all-divider]
      [:div.editor-form__fold-all-label-right
       "Osiot kiinni"]]]))

(defn- preview-link [form lang-kwd & [text]]
  (let [text (if (nil? text)
               (-> lang-kwd name clojure.string/upper-case)
               text)]
    [:a.editor-form__preview-button-link
     {:key    (str "preview-" (name lang-kwd))
      :href   (str js/config.applicant.service_url "/hakemus/" (:key form) "?lang=" (name lang-kwd))
      :target "_blank"}
     [:i.zmdi.zmdi-open-in-new]
     [:span.editor-form__preview-button-text text]]))

(defn- form-toolbar [form]
  (let [fixed? (= :fixed @(subscribe [:state-query [:banner :type]]))
        languages @(subscribe [:editor/languages])]
    [:div.editor-form__toolbar
     [:div.editor-form__toolbar-left
      [:div.editor-form__language-controls
       (map (fn [lang-kwd]
              (lang-checkbox lang-kwd (some? (some #{lang-kwd} languages))))
            (keys lang-versions))]
      (if (= (count languages) 1)
        [:div.editor-form__preview-buttons
         (preview-link form (first languages) "Lomakkeen esikatselu")]
        [:div.editor-form__preview-buttons
         [:span "Lomakkeen esikatselu:"]
         (map (partial preview-link form) languages)])
      [form-owner-organization form]]
     [:div.editor-form__toolbar-right
      [fold-all]]]))

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

(defn- editor-panel [form]
  [:div.editor-form__panel-container
   [close-form]
   [:div
    [editor-name]
    [form-in-use-warning form]]
   [c/editor]])

(defn editor []
  (let [form @(subscribe [:editor/selected-form])]
    [:div
     [:div.editor-form__container.panel-content
      [form-header-row]
      [form-list]]
     (when form
       ^{:key "editor-panel"}
       [editor-panel form])
     (when form
       ^{:key "form-toolbar"}
       [form-toolbar form])]))
