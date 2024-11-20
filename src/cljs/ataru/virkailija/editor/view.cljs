(ns ataru.virkailija.editor.view
  (:require [ataru.cljs-util :refer [wrap-scroll-to]]
            [ataru.util :as util]
            [clojure.string :as string]
            [ataru.virkailija.editor.core :as c]
            [ataru.virkailija.editor.subs]
            [ataru.virkailija.editor.demo.subs]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.temporal :as temporal]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [reagent.dom :as r-dom]
            [ataru.virkailija.date-time-picker :as date-time-picker]))

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
  (let [disabled? (subscribe [:editor/form-loading?])]
    (fn copy-form []
      [:button.editor-form__control-button
       {:data-test-id "copy-form-button"
        :on-click (fn [event]
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
     {:disabled true
      :data-tooltip (when @(subscribe [:editor/form-contains-applications?])
                      @(subscribe [:editor/virkailija-translation :form-contains-applications?]))}
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

(defn- editor-name-input [_ focus?]
  (let [form              (subscribe [:editor/selected-form])
        new-form-created? (subscribe [:state-query [:editor :new-form-created?]])
        form-locked?      (subscribe [:editor/form-locked?])]
    (r/create-class
      {:component-did-update (fn [this]
                               (when (and focus? @new-form-created?)
                                 (.focus (r-dom/dom-node this))
                                 (.select (r-dom/dom-node this))))
       :reagent-render       (fn [lang _]
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
      (string/upper-case (name lang))])])

(defn- editor-name []
  (let [[l & ls] @(subscribe [:editor/languages])]
    [:div
     ^{:key (str "editor-name-" l)}
     [editor-name-wrapper l true (not-empty ls)]
     (doall (for [l ls]
              ^{:key (str "editor-name-" l)}
              [editor-name-wrapper l false true]))]))

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

(defn- requires-kk-application-payment-label [haku]
  (let [label @(subscribe [:editor/virkailija-translation :requires-kk-application-payment])]
    (when (:admission-payment-required? haku)
      [:div.editor-form__requires-kk-application-payment
       [:span [:i.zmdi.zmdi-alert-triangle] (str " " label)]])))

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
  (let [user-info @(subscribe [:state-query [:editor :user-info]])
        demo-allowed? @(subscribe [:editor/demo-allowed])]
    [:div.editor-form__haku-preview-link
     [:a {:href         (str "/lomake-editori/api/preview/haku/"
                          (:oid haku)
                          "?lang=fi")
          :target       "_blank"
          :data-test-id "application-preview-link-fi"}
      @(subscribe [:editor/virkailija-translation :test-application])]
     [:span " | "]
     [:a {:href   (str js/config.applicant.service_url
                       "/hakemus/haku/" (:oid haku)
                       "?lang=fi")
          :target "_blank"}
      @(subscribe [:editor/virkailija-translation :form])]
     [:span " | "]
     [:a {:href   (str "/lomake-editori/applications/haku/" (:oid haku)
                       "?ensisijaisesti=false")
          :target "_blank"}
      @(subscribe [:editor/virkailija-translation :link-to-applications])]
     (when (:superuser? user-info)
       (link-to-feedback (str "/hakemus/haku/" (:oid haku))))
     (when demo-allowed?
       [:<>
        [:span " | "]
        [:a {:href   (str js/config.applicant.service_url
                          "/hakemus/haku/" (:oid haku)
                          "/demo?lang=fi")
            :target "_blank"
            :data-test-id "demo-link" }
          @(subscribe [:editor/virkailija-translation :demo-link])]])]))

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
         [requires-kk-application-payment-label haku]
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
  [:a {:on-click (fn [_]
                   (dispatch [:set-state [:editor :selected-form-key] nil])
                   (routes/navigate-to-click-handler "/lomake-editori/editor"))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn- demo-validity
  []
  (let [demo-validity-start     @(subscribe [:editor/demo-validity-start-str])
        demo-validity-start-max @(subscribe [:editor/demo-validity-start-max-str])
        demo-validity-end       @(subscribe [:editor/demo-validity-end-str])
        demo-validity-end-min   @(subscribe [:editor/demo-validity-end-min-str])
        demo-validity-end-max   @(subscribe [:editor/demo-validity-end-max-str])
        disabled?               @(subscribe [:editor/form-locked?])]
    [:div.editor-form__demo-validity-controls
     [:div.editor-form__date-picker-container
      [date-time-picker/date-picker
       "demo-validity-start"
       "editor-form__date-picker"
       demo-validity-start
       "invalid"
       #(dispatch [:editor/change-demo-validity-start %])
       {:max demo-validity-start-max
        :data-test-id "demo-validity-start"
        :disabled? disabled?}
       ]
      [:label.editor-form__date-picker-label
       {:for "demo-validity-start"}
       @(subscribe [:editor/virkailija-translation :demo-validity-start])]]
     [:div.editor-form__date-picker-container
      [date-time-picker/date-picker
       "demo-validity-end"
       "editor-form__date-picker"
       demo-validity-end
       "invalid"
       #(dispatch [:editor/change-demo-validity-end %])
       {:min demo-validity-end-min
        :max demo-validity-end-max
        :data-test-id "demo-validity-end"
        :disabled? disabled?}
       ]
      [:label.editor-form__date-picker-label
       {:for "demo-validity-end"}
       @(subscribe [:editor/virkailija-translation :demo-validity-end])]]]))

(defn- allow-hakeminen-tunnistautuneena-component
  []
  (let [id                      "toggle-allow-hakeminen-tunnistautuneena"
        current-value           @(subscribe [:editor/allow-hakeminen-tunnistautuneena?])
        superuser?              @(subscribe [:editor/superuser?])
        disabled?               (or @(subscribe [:editor/form-locked?])
                                    (not superuser?))]
    [:div.editor-form__checkbox-with-label
     [:input.editor-form__checkbox
      {:id        id
       :data-test-id "toggle-allow-hakeminen-tunnistautuneena"
       :checked   (true? (boolean current-value))
       :type      "checkbox"
       :disabled  disabled?
       :on-change #(dispatch [:editor/toggle-allow-hakeminen-tunnistautuneena])}]
     [:label.editor-form__checkbox-label
      {:for id}
      @(subscribe [:editor/virkailija-translation :hakeminen-tunnistautuneena-allowed-on-form])]]))
(defn- allow-only-yhteishaku-component
  []
  (let [id                      "toggle-allow-only-yhteishaku"
        allow-only-yhteishaut?  @(subscribe [:editor/allow-only-yhteishaut?])
        disabled?               @(subscribe [:editor/form-locked?])]
  [:div.editor-form__checkbox-with-label
   [:input.editor-form__checkbox
    {:id        id
     :checked   (true? (boolean allow-only-yhteishaut?))
     :type      "checkbox"
     :disabled  disabled?
     :on-change #(dispatch [:editor/toggle-allow-only-yhteishaut])}]
   [:label.editor-form__checkbox-label
    {:for id}
    @(subscribe [:editor/virkailija-translation :only-yhteishaku])]]))

(defn- lomakkeeseen-liittyy-maksutoiminto-component
  []
  (let [id                      "toggle-lomakkeeseen-liittyy-maksutoiminto"
        maksutiedot             @(subscribe [:editor/maksutiedot])
        maksutoiminto?          (not (empty? maksutiedot))
        superuser?              @(subscribe [:editor/superuser?])
        disabled?               (or @(subscribe [:editor/form-locked?])
                                    (not superuser?)
                                    (= (:type maksutiedot) "payment-type-kk"))]
    [:div
     [:div.editor-form__checkbox-with-label
      [:input.editor-form__checkbox
       {:id        id
        :checked   maksutoiminto?
        :type      "checkbox"
        :disabled  disabled?
        :on-change #(dispatch [:editor/toggle-lomakkeeseen-liittyy-maksutoiminto])
        :data-test-id "toggle-maksutoiminto"}]
      [:label.editor-form__checkbox-label
       {:for id}
       @(subscribe [:editor/virkailija-translation :lomakkeeseen-liittyy-maksutoiminto])]]
     (when maksutoiminto?
       [:div.editor-form__maksutoiminto-wrapper
        [:div
         [:div.editor-form__checkbox-with-label
          [:input.editor-form__radio
           {:type      "radio"
            :value     "payment-type-tutu"
            :checked   (= (:type maksutiedot) "payment-type-tutu")
            :id        "maksutyyppi-tutu-radio"
            :disabled  disabled?
            :on-change #(dispatch [:editor/change-maksutyyppi "payment-type-tutu"])
            :data-test-id "maksutyyppi-tutu-radio"}]
          [:label.editor-form__checkbox-label
           {:for   "maksutyyppi-tutu-radio"}
           @(subscribe [:editor/virkailija-translation :maksutyyppi-tutu-radio])]]
         (when (= (:type maksutiedot) "payment-type-tutu")
           [:div.editor-form__payment-amount-wrapper
            [:div.editor-form__text-field-wrapper
             [:label.editor-form__component-item-header
              @(subscribe [:editor/virkailija-translation :kasittelymaksu-input])]
             [:input.editor-form__text-field
              {:data-test-id "tutu-processing-fee-input"
               :type         "number"
               :value        (:processing-fee maksutiedot)
               :required     true
               :disabled     disabled?
               :on-change    #(dispatch [:editor/change-processing-fee (.-value (.-target %))])}]]])
        [:div.editor-form__checkbox-with-label
         [:input.editor-form__radio
          {:type      "radio"
           :value     "payment-type-astu"
           :checked   (= (:type maksutiedot) "payment-type-astu")
           :id        "maksutyyppi-astu-radio"
           :disabled  disabled?
           :on-change #(dispatch [:editor/change-maksutyyppi "payment-type-astu"])
           :data-test-id "maksutyyppi-astu-radio"}]
         [:label.editor-form__checkbox-label
          {:for   "maksutyyppi-astu-radio"}
          @(subscribe [:editor/virkailija-translation :maksutyyppi-astu-radio])]]
        [:div.editor-form__checkbox-with-label
         [:input.editor-form__radio
          {:type      "radio"
           :value     "payment-type-kk"
           :checked   (= (:type maksutiedot) "payment-type-kk")
           :id        "maksutyyppi-kk-radio"
           :disabled  true
           :data-test-id "maksutyyppi-kk-radio"}]
         [:label.editor-form__checkbox-label
          {:for   "maksutyyppi-kk-radio"}
          @(subscribe [:editor/virkailija-translation :maksutyyppi-kk-radio])]]]])]))

(defn- close-form-component
  []
  (let [id           "toggle-close-form"
        form-closed? @(subscribe [:editor/form-closed?])
        disabled?    @(subscribe [:editor/form-locked-or-has-haku?])]
    [:div.editor-form__checkbox-with-label
     [:input.editor-form__checkbox
      {:id id
       :checked (true? (boolean form-closed?))
       :type "checkbox"
       :disabled disabled?
       :on-change #(dispatch [:editor/toggle-close-form])}]
     [:label.editor-form__checkbox-label
      {:for id}
      @(subscribe [:editor/virkailija-translation :close-form])]]))

(defn- properties []
  (let [form-key              @(subscribe [:editor/selected-form-key])
        form-used-in-hakus    @(subscribe [:editor/form-used-in-hakus form-key])
        kk-payments-required? (some true? (map :admission-payment-required? form-used-in-hakus))]
    [:div.editor-form__component-wrapper
     [:div.editor-form__header-wrapper
      [:header.editor-form__component-header {:data-test-id "properties-header"}
       [:span.editor-form__component-main-header @(subscribe [:editor/virkailija-translation :properties])]]]
     [:div.editor-form__component-content-wrapper
      [:div.editor-form__module-fields
       [allow-only-yhteishaku-component]
       [allow-hakeminen-tunnistautuneena-component]
       (when-not kk-payments-required? [lomakkeeseen-liittyy-maksutoiminto-component])
       [close-form-component]]]
     (when @(subscribe [:editor/show-demo-config])
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
         [demo-validity]]])]))

(defn- editor-panel [form-key]
  [:div.editor-form__panel-container
   [close-form]
   [:div
    [editor-name]
    [form-usage form-key]]
   [properties]
   [c/editor]])

(defn- editor-panel-loading []
  [:div.editor-form__panel-container
   [close-form]
   [:div.editor-form__loading-indicator
    [:i.zmdi.zmdi-spinner]]])

(defn editor []
  (let [form-key @(subscribe [:editor/selected-form-key])
        form-loading @(subscribe [:editor/form-loading?])]
    [:div
     [:div.editor-form__container.panel-content
      [form-header-row]
      [form-list]]
     ^{:key "editor-panel"}
     (if form-loading
       [editor-panel-loading]
       (when form-key
         [editor-panel form-key]))
     (when (and form-key (not form-loading))
       ^{:key "form-toolbar"}
       [form-toolbar])]))
