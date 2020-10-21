(ns ataru.virkailija.editor.view
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [ataru.cljs-util :refer [wrap-scroll-to]]
            [ataru.component-data.component :as component]
            [ataru.util :as util]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.temporal :as temporal]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [reagent.dom :as r-dom]))

(defn form-row [key selected?]
  [:a.editor-form__row
   {:class    (when selected? "editor-form__selected-row")
    :on-click (partial routes/navigate-to-click-handler (str "/lomake-editori/editor/" key))}
   [:span.editor-form__list-form-name @(subscribe [:editor/form-name key])]
   [:span.editor-form__list-form-time (temporal/time->str @(subscribe [:editor/form-created-time key]))]
   [:span.editor-form__list-form-editor @(subscribe [:editor/form-created-by key])]
   (when @(subscribe [:editor/this-form-locked? key])
     [:i.zmdi.zmdi-lock.editor-form__list-form-locked])])

(defn form-list []
  (let [form-keys         (subscribe [:editor/form-keys])
        selected-form-key (subscribe [:editor/selected-form-key])]
    (fn form-list []
      (into (if @selected-form-key
              [:div.editor-form__list]
              [:div.editor-form__list.editor-form__list_expanded])
            (for [key  @form-keys
                  :let [selected? (= key @selected-form-key)]]
              ^{:key (str "form-list-item-" key)}
              (if selected?
                [wrap-scroll-to [form-row key selected?]]
                [form-row key selected?]))))))

(defn- add-form []
  [:button.editor-form__control-button.editor-form__control-button--enabled
   {:data-test-id "add-form-button"
    :on-click     (fn [evt]
                    (.preventDefault evt)
                    (dispatch [:editor/add-form]))}
   @(subscribe [:editor/virkailija-translation :new-form])])

(defn- copy-form []
  (let [form-key    (subscribe [:state-query [:editor :selected-form-key]])
        disabled?   (reaction (nil? @form-key))]
    (fn copy-form []
      [:button.editor-form__control-button
       {:on-click (fn [event]
                    (.preventDefault event)
                    (dispatch [:editor/copy-form]))
        :disabled @disabled?
        :class    (if @disabled?
                    "editor-form__control-button--disabled"
                    "editor-form__control-button--enabled")}
       @(subscribe [:editor/virkailija-translation :copy-form])])))

(defn- remove-form []
  (case @(subscribe [:editor/remove-form-button-state])
    :active
    [:button.editor-form__control-button--enabled.editor-form__control-button
     {:on-click #(dispatch [:editor/start-remove-form])}
     @(subscribe [:editor/virkailija-translation :delete-form])]
    :confirm
    [:div.editor-form__component-button-group
     [:button.editor-form__control-button--confirm.editor-form__control-button
      {:on-click #(dispatch [:editor/confirm-remove-form])}
      @(subscribe [:editor/virkailija-translation :confirm-delete])]
     [:button.editor-form__control-button--enabled.editor-form__control-button
      {:on-click #(dispatch [:editor/unstart-remove-form])}
      @(subscribe [:editor/virkailija-translation :cancel-form-delete])]]
    :disabled
    [:button.editor-form__control-button--disabled.editor-form__control-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :delete-form])]))

(defn- form-controls []
  [:div.editor-form__form-controls-container
   [add-form]
   [copy-form]
   [remove-form]])

(defn- form-header-row []
  [:div.editor-form__form-header-row
   [:h1.editor-form__form-heading @(subscribe [:editor/virkailija-translation :forms])]
   [form-controls]])

(defn- editor-name-input [lang focus?]
  (let [form              (subscribe [:editor/selected-form])
        new-form-created? (subscribe [:state-query [:editor :new-form-created?]])
        form-locked?      (subscribe [:editor/form-locked?])]
    (r/create-class
      {:component-did-update (fn [this]
                               (when (and focus? @new-form-created?)
                                 (do
                                   (.focus (r-dom/dom-node this))
                                   (.select (r-dom/dom-node this)))))
       :reagent-render       (fn [lang focus?]
                               [:input.editor-form__form-name-input
                                {:data-test-id "form-name-input"
                                 :type         "text"
                                 :value        (get-in @form [:name lang])
                                 :disabled     @form-locked?
                                 :placeholder  @(subscribe [:editor/virkailija-translation :form-name])
                                 :on-change    #(do (dispatch [:editor/change-form-name lang (.-value (.-target %))])
                                                    (dispatch [:set-state [:editor :new-form-created?] false]))
                                 :on-blur      #(dispatch [:set-state [:editor :new-form-created?] false])}])})))

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

(defn- get-org-name [org]
  (str (get-in org [:name :fi])
       (if (= "group" (:type org))
         (str " (" @(subscribe [:editor/virkailija-translation :group]) ")")
         "")))

(defn- get-org-name-for-oid [oid orgs] (get-org-name (first (filter #(= oid (:oid %)) orgs))))

(defn- fold-all []
  [:div
   [:span.editor-form__fold-clickable-text
    {:on-click #(dispatch [:editor/fold-all])}
    @(subscribe [:editor/virkailija-translation :close])]
   [:span.editor-form__fold-description-text " / "]
   [:span.editor-form__fold-clickable-text
    {:on-click #(dispatch [:editor/unfold-all])}
    @(subscribe [:editor/virkailija-translation :open])]
   [:span.editor-form__fold-description-text (str " " @(subscribe [:editor/virkailija-translation :questions]))]])

(defn- preview-link [form-key lang-kwd]
  [:a.editor-form__preview-button-link
   {:key          (str "preview-" (name lang-kwd))
    :href         (str "/lomake-editori/api/preview/form/" form-key
                       "?lang=" (name lang-kwd))
    :target       "_blank"
    :data-test-id (str "application-preview-link-" (name lang-kwd))}
   [:i.zmdi.zmdi-open-in-new]
   [:span.editor-form__preview-button-text
    (clojure.string/upper-case (name lang-kwd))]])

(defn- link-to-feedback [path]
  [:span
   [:span " | "]
   [:a.editor-form__form-admin-preview-link
    {:href   (str "/palaute/"
                  "?q=" (str js/config.applicant.service_url path))
     :target "_blank"}
    @(subscribe [:editor/virkailija-translation :link-to-feedback])]])

(defn- lock-form-editing []
  (let [form-locked-info @(subscribe [:editor/form-locked-info])
        lock-state       (:lock-state form-locked-info)
        enabled?         (and (not (some #{:opening :closing}
                                         [lock-state]))
                              (or (not @(subscribe [:editor/yhteishaku?]))
                                  @(subscribe [:editor/superuser?])))]
    [:div.editor-form__preview-buttons
     (when (not= lock-state :open)
       [:div.editor-form__form-editing-locked
        @(subscribe [:editor/virkailija-translation :form-locked])
        [:i.zmdi.zmdi-lock.editor-form__form-editing-lock-icon]
        [:div.editor-form__form-editing-locked-by
         (str "("
              (:locked-by form-locked-info)
              " "
              (-> form-locked-info :locked temporal/str->googdate temporal/time->short-str)
              ")")]])
     [:a#lock-form.editor-form__fold-clickable-text
      (if enabled?
        {:on-click #(dispatch [:editor/toggle-form-editing-lock])}
        {:disabled true})
      (if (= lock-state :locked)
        @(subscribe [:editor/virkailija-translation :remove-lock])
        @(subscribe [:editor/virkailija-translation :lock-form]))]]))

(defn- disable-autosave
  []
  (let [autosave-enabled (subscribe [:editor/autosave-enabled?])]
    (fn []
      [:div.editor-form__preview-buttons
       [:a.editor-form__autosave-toggle-link
        {:on-click #(dispatch [:editor/toggle-autosave])
         :class    (when-not @autosave-enabled "editor-form__autosave-toggle-link--autosave-off")}
        (if @autosave-enabled
          @(subscribe [:editor/virkailija-translation :autosave-enabled])
          @(subscribe [:editor/virkailija-translation :autosave-disabled]))]])))

(defn- lang-checkbox [lang-kwd]
  (let [id (str "lang-checkbox-" (name lang-kwd))]
    [:div.editor-form__checkbox-with-label
     {:key id}
     [:input.editor-form__checkbox
      {:id        id
       :checked   (some? (some #{lang-kwd} @(subscribe [:editor/languages])))
       :type      "checkbox"
       :on-change (fn [_] (dispatch [:editor/toggle-language lang-kwd]))}]
     [:label.editor-form__checkbox-label.editor-form__language-checkbox-label
      {:for id}
      @(subscribe [:editor/virkailija-translation (case lang-kwd
                                                    :fi :finnish
                                                    :sv :swedish
                                                    :en :english)])]]))

(defn- form-toolbar []
  [:div.editor-form__toolbar
   [:div.editor-form__toolbar-left
    [:div.editor-form__language-controls
     [lang-checkbox :fi]
     [lang-checkbox :sv]
     [lang-checkbox :en]]
    [:div.editor-form__preview-buttons
     [:a.editor-form__email-template-editor-link
      {:on-click #(dispatch [:editor/toggle-email-template-editor])}
      @(subscribe [:editor/virkailija-translation :edit-email-templates])]]
    [lock-form-editing]
    [disable-autosave]]
   [:div.editor-form__toolbar-right
    [fold-all]]])

(defn- in-language [term lang]
  (util/non-blank-val term [lang :fi :sv :en]))

(defn- used-in-haku-list-haku-name [haku]
  (let [lang @(subscribe [:editor/virkailija-lang])]
    [:div.editor-form__used-in-haku-list-haku-name
     [:span
      (str (in-language (:name haku) lang) " ")
      [:a.editor-form__haku-admin-link
       {:href   (:haun-tiedot-url haku)
        :target "_blank"}
       [:i.zmdi.zmdi-open-in-new]]]]))

(defn- haku-preview-link [haku]
  (let [user-info @(subscribe [:state-query [:editor :user-info]])]
    [:div.editor-form__haku-preview-link
     [:a {:href   (str "/lomake-editori/api/preview/haku/"
                       (:oid haku)
                       "?lang=fi")
          :target "_blank"}
      @(subscribe [:editor/virkailija-translation :test-application])]
     [:span " | "]
     [:a {:href   (str js/config.applicant.service_url
                       "/hakemus/haku/" (:oid haku)
                       "?lang=fi")
          :target "_blank"}
      @(subscribe [:editor/virkailija-translation :form])]
     (when (:superuser? user-info)
       (link-to-feedback (str "/hakemus/haku/" (:oid haku))))]))

(defn- form-in-use-in-hakus [form-used-in-hakus]
  [:div.editor-form__form-link-container.animated.flash
   [:h3.editor-form__form-link-heading
    [:i.zmdi.zmdi-alert-circle-o]
    (str " "
         (if (empty? (rest form-used-in-hakus))
           @(subscribe [:editor/virkailija-translation :used-by-haku])
           @(subscribe [:editor/virkailija-translation :used-by-haut])))]
   [:ul.editor-form__used-in-haku-list
    (doall
      (for [haku form-used-in-hakus]
        ^{:key (str "haku-" (:oid haku))}
        [:li
         [used-in-haku-list-haku-name haku]
         [haku-preview-link haku]]))]])

(defn- form-not-in-use-in-hakus [form-key]
  (let [languages @(subscribe [:editor/languages])
        user-info @(subscribe [:state-query [:editor :user-info]])]
    [:div.editor-form__form-link-container
     [:h3.editor-form__form-link-heading
      [:i.zmdi.zmdi-alert-circle-o]
      (str " " @(subscribe [:editor/virkailija-translation :link-to-form]))]
     [:a.editor-form__form-preview-link
      {:href   (str js/config.applicant.service_url
                    "/hakemus/" form-key
                    "?lang=fi")
       :target "_blank"}
      @(subscribe [:editor/virkailija-translation :form])]
     [:span " | "]
     [:a.editor-form__form-admin-preview-link @(subscribe [:editor/virkailija-translation :test-application])]
     (map (partial preview-link form-key) languages)
     (when (:superuser? user-info)
       (link-to-feedback (str "/hakemus/" form-key)))]))

(defn- hakus-present-or-still-fetching [hakus]
  (or (not (empty? hakus))
      (nil? hakus)))

(defn- form-usage [form-key]
  (let [form-used-in-hakus @(subscribe [:editor/form-used-in-hakus form-key])]
    (if (hakus-present-or-still-fetching form-used-in-hakus)
      [form-in-use-in-hakus form-used-in-hakus]
      [form-not-in-use-in-hakus form-key])))

(defn- close-form []
  [:a {:on-click (fn [event]
                   (dispatch [:set-state [:editor :selected-form-key] nil])
                   (routes/navigate-to-click-handler "/lomake-editori/editor"))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn- editor-panel [form-key]
  [:div.editor-form__panel-container
   [close-form]
   [:div
    [editor-name]
    [form-usage form-key]]
   [c/editor]])

(defn editor []
  (let [form-key @(subscribe [:editor/selected-form-key])]
    [:div
     [:div.editor-form__container.panel-content
      [form-header-row]
      [form-list]]
     (when form-key
       ^{:key "editor-panel"}
       [editor-panel form-key])
     (when form-key
       ^{:key "form-toolbar"}
       [form-toolbar])]))
