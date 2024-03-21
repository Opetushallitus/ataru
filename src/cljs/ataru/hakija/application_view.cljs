(ns ataru.hakija.application-view
  (:require [ataru.config :as config]
            [ataru.application-common.application-field-common :refer [markdown-paragraph]]
            [ataru.constants :as constants]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-view-icons :as icons]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.translations.texts :refer [general-texts]]
            [ataru.translations.translation-util :as translations]
            [re-frame.core :refer [subscribe dispatch]]
            [goog.string :as gstring]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [ataru.util :as util]
            [clojure.string :as string]))

(def ^:private language-names
  {:fi "Suomeksi"
   :sv "På svenska"
   :en "In English"})

(defn application-header []
  (let [form                               @(subscribe [:application/form])
        selected-lang                      (or (:selected-language form) :fi)
        languages                          (filter
                                            (partial not= selected-lang)
                                            (:languages form))
        submitted?                         @(subscribe [:application/submitted?])
        cannot-edit-because-in-processing? @(subscribe [:application/cannot-edit-because-in-processing?])
        editing?                           @(subscribe [:application/editing?])
        virkailija?                        @(subscribe [:application/virkailija?])
        lang                               @(subscribe [:application/form-language])
        closed?                            @(subscribe [:application/form-closed?])
        apply-dates                        (when-let [hakuaika @(subscribe [:application/haku-aika])]
                                             (if (:joustava-haku? hakuaika)
                                               (translations/get-hakija-translation :rolling-period lang)
                                               (if (:jatkuva-haku? hakuaika)
                                                 (translations/get-hakija-translation :continuous-period lang)
                                                 [:span (str (translations/get-hakija-translation :application-period lang)
                                                             " "
                                                             (-> hakuaika :label :start selected-lang)
                                                             " - "
                                                             (-> hakuaika :label :end selected-lang)
                                                             )
                                                  [:br]
                                                  (when (and (not (:on hakuaika))
                                                             (not virkailija?))
                                                    (str " (" (translations/get-hakija-translation :not-within-application-period lang) ")"))])))]
    [:div
     [:div.application__header-container
      [:h1.application__header
       {:data-test-id "application-header-label"}
       (or (-> form :tarjonta :haku-name selected-lang)
                                     (-> form :name selected-lang))]
      (when (and (not submitted?)
                 (not editing?)
                 (> (count languages) 0))
        [:span.application__header-text
         (doall
          (rest
           (mapcat (fn [language]
                     [[:span.application__header-language-link-separator
                       {:key (str "separator-" (name language))}
                       " | "]
                      [:span {:key (name language)}
                       [:a {:href @(subscribe [:application/language-version-link language])}
                        (get language-names language)]]])
                   languages)))])]
     (when closed?
       [:div.application__sub-header-container
        [:span.application__sub-header-dates
         (translations/get-hakija-translation :form-closed lang)]])
     (when (not-empty apply-dates)
       [:div.application__sub-header-container
        [:span.application__sub-header-dates apply-dates]])
     (when (and cannot-edit-because-in-processing?
                (not virkailija?))
       [:div.application__sub-header-container
        [:span.application__sub-header-modifying-prevented
         (translations/get-hakija-translation :application-processed-cant-modify lang)]])]))

(defn readonly-fields [_]
  (let [application (subscribe [:state-query [:application]])]
    (fn [form]
      [readonly-view/readonly-fields form @application])))

(defn- render-fields [_]
  (let [submit-status    (subscribe [:state-query [:application :submit-status]])
        preview-enabled? (subscribe [:state-query [:application :preview-enabled]])]
    (fn [form]
      (if (or (= :submitted @submit-status)
              @preview-enabled?)
        [readonly-fields form]
        (when form
          [editable-fields form submit-status])))))

(defn- has-applied-lander [form selected-lang]
  (let [lang         (subscribe [:application/form-language])
        header       (or (-> form :tarjonta :haku-name selected-lang)
                         (-> form :name selected-lang))
        auth-type    @(subscribe [:state-query [:oppija-session :auth-type]])
        strong-auth? (= constants/auth-type-strong auth-type)]
    [:div.application__hakeminen-tunnistautuneena-has-applied-lander-wrapper
     [:h1 (translations/get-hakija-translation :ht-has-applied-lander-header @lang)]
     [:div.application__hakeminen-tunnistautuneena-has-applied-lander-haku-header header]
     [:p.application__:hakeminen-tunnistautuneena-has-applied-lander-paragraph (translations/get-hakija-translation :ht-has-applied-lander-paragraph1 @lang)]
     [:p.application__:hakeminen-tunnistautuneena-has-applied-lander-paragraph (translations/get-hakija-translation (if strong-auth?
                                                                                                                      :ht-has-applied-lander-paragraph2
                                                                                                                      :ht-has-applied-lander-paragraph2-eidas) @lang)]
     [:p.application__:hakeminen-tunnistautuneena-has-applied-lander-paragraph (translations/get-hakija-translation :ht-has-applied-lander-paragraph3 @lang)]
     (when strong-auth?
       [:button.application__oma-opintopolku-button
        {:on-click #(dispatch [:application/redirect-to-oma-opintopolku])}
        (translations/get-hakija-translation :ht-siirry-oma-opintopolkuun @lang)])]))

(defn- hakeminen-tunnistautuneena-lander [form lang]
  (let [header (or (-> form :tarjonta :haku-name lang)
                   (-> form :name lang))]
    [:div.application__hakeminen-tunnistautuneena-lander-wrapper
     [:h1 (translations/get-hakija-translation :ht-lander-header lang)]
     [:div.application__hakeminen-tunnistautuneena-lander-haku-header header]
     [:p.application__hakeminen-tunnistautuneena-lander-top-text
      (translations/get-hakija-translation :ht-lander-top-text lang)]
     [:div.application__hakeminen-tunnistautuneena-tunnistaudu-wrapper
      [:div.application__hakeminen-tunnistautuneena-lander-header-wrapper
       [:img.logo-suomi-fi
        {:src "/hakemus/images/suomifi_tunnus.svg"}]
       [:h2 (translations/get-hakija-translation :ht-tunnistaudu-ensin-header lang)]]
      [:p.application__hakeminen-tunnistautuneena-lander-main-text
       [:span (translations/get-hakija-translation :ht-tunnistaudu-ensin-text lang)]]
      [:p.application__hakeminen-tunnistautuneena-lander-main-text
       [:span (translations/get-hakija-translation :ht-tunnistaudu-ensin-text-2 lang)]]
      [:button.application__tunnistaudu-button
       {:on-click     #(dispatch [:application/redirect-to-tunnistautuminen (name lang)])
        :data-test-id "tunnistautuminen-button"}
       [icons/icon-lock] (translations/get-hakija-translation :ht-kirjaudu-sisaan lang)]]
     [:div.application__hakeminen-tunnistautuneena-separator-wrapper
      [:hr.application__hakeminen-tunnistautuneena-partial-line-left]
      [:div.application__hakeminen-tunnistautuneena-separator-text
       (translations/get-hakija-translation :ht-tai lang)]
      [:hr.application__hakeminen-tunnistautuneena-partial-line-right]]
     [:div.application__hakeminen-tunnistautuneena-jatka-tunnistautumatta-wrapper
      [:h2 (translations/get-hakija-translation :ht-jatka-tunnistautumatta-header lang)]
      [:p (translations/get-hakija-translation :ht-jatka-tunnistautumatta-text lang)]
      [:button.application__tunnistaudu-button
       {:on-click     #(dispatch [:application/set-tunnistautuminen-declined])
        :data-test-id "decline-tunnistautuminen-button"}
       (translations/get-hakija-translation :ht-ilman-kirjautumista lang)]]]))

(defn application-contents []
  (let [form                      (subscribe [:state-query [:form]])
        can-apply?                (subscribe [:application/can-apply?])
        editing?                  (subscribe [:state-query [:application :editing?]])
        expired                   (subscribe [:state-query [:application :secret-expired?]])
        delivery-status           (subscribe [:state-query [:application :secret-delivery-status]])
        lang                      (subscribe [:application/form-language])
        secret-link-valid-days    (config/get-public-config [:secret-link-valid-days])
        demo?                     (subscribe [:application/demo?])
        demo-modal-open?          (subscribe [:application/demo-modal-open?])
        has-applied-to-haku?      (subscribe [:state-query [:application :has-applied]])
        ht-lander-active?         (subscribe [:application/hakeminen-tunnistautuneena-lander-active?])
        loading-complete?         (subscribe [:application/loading-complete?])]
    (fn []
      (let [root-element (if @demo?
                           :div.application__form-content-area.application__form-content-area--demo
                           :div.application__form-content-area)]
        [root-element
         (when @demo-modal-open?
           {:visibility "hidden"
            :display "none"})
         (when @expired
           [:div.application__secret-expired
            [:div.application__secret-expired-icon
             [:i.zmdi.zmdi-lock-outline]]
            [:h2 (translations/get-hakija-translation :expired-secret-heading @lang)]
            [:p (translations/get-hakija-translation :expired-secret-paragraph @lang secret-link-valid-days)]
            [:button.application__secret-resend-button
             {:disabled (some? @delivery-status)
              :on-click #(dispatch [:application/send-new-secret])}
             (if (= :completed @delivery-status)
               (translations/get-hakija-translation :expired-secret-sent @lang)
               (translations/get-hakija-translation :expired-secret-button @lang))]
            [:p (translations/get-hakija-translation :expired-secret-contact @lang)]])
         (if-not @loading-complete?
           [:div.application__form-loading-spinner
            [:i.zmdi.zmdi-hc-3x.zmdi-spinner.spin]]
           (if @ht-lander-active?
             (hakeminen-tunnistautuneena-lander @form @lang)
             (if @has-applied-to-haku?
               (has-applied-lander @form @lang)
               [:div.application__lomake-wrapper
                ^{:key (:id @form)}
                (when (not @demo-modal-open?)
                  [application-header])
                (when (and (not @demo-modal-open?) (or @can-apply? @editing?))
                  ^{:key "form-fields"}
                  [render-fields @form])])))]))))

(defn- star-number-from-event
  [event]
  (-> event
      (aget "target" "dataset" "starN")
      (js/parseInt 10)))

(defn- submit-notification-ht
  [hidden?]
  (fn []
    (let [lang @(subscribe [:application/form-language])
          auth-type    @(subscribe [:state-query [:oppija-session :auth-type]])
          strong-auth? (= constants/auth-type-strong auth-type)
          answers @(subscribe [:state-query [:application :answers]])]
      [:div.application__submitted-submit-notification-ht-overlay
       [:div.application__submitted-submit-notification-ht
        {:role "alertdialog"
         :aria-modal "true"
         :aria-labelledby "submitted-submit-notification-heading submitted-submit-notification-confirmation"}
        [:div.application__submitted-submit-notification-inner-ht
         [:h1.application__submitted-submit-notification-heading
          {:id "submitted-submit-notification-heading"}
          (translations/get-hakija-translation
            :ht-application-submitted
            lang)]]
        (when (-> answers
                  (get-in [:email :value])
                  (string/blank?)
                  not)
          [:div.application__submitted-submit-notification-heading
           {:id "submitted-submit-notification-confirmation"
            :role "text"}
           (translations/get-hakija-translation (if strong-auth?
                                                  :ht-application-confirmation
                                                  :ht-application-confirmation-eidas) lang)])
        [:div.application__submitted-submit-notification-inner-ht
         [:button.application__overlay-button.application__overlay-button
          {:tab-index    "1"
           :on-click     #(reset! hidden? true)
           :data-test-id "send-feedback-button"
           :auto-focus ""}
          (translations/get-hakija-translation :ht-katso-hakemustasi lang)]
         [:button.application__overlay-button.application__overlay-button
          {:tab-index    "2"
           :on-click     #(dispatch [:application/redirect-to-logout (name lang)])
           :data-test-id "logout-button"
           :auto-focus ""}
          [:i.material-icons-outlined.logout
           {:title (translations/get-hakija-translation :ht-kirjaudu-ulos lang)} "logout"]
          (translations/get-hakija-translation :ht-kirjaudu-ulos lang)]]]])))

(defn- submit-notification
  [hidden? demo?]
  (fn []
    (let [lang @(subscribe [:application/form-language])
          answers @(subscribe [:state-query [:application :answers]])]
      [:div.application__submitted-submit-notification
       {:role "alertdialog"
        :aria-modal "true"
        :aria-labelledby "submitted-submit-notification-heading submitted-submit-notification-confirmation"}
       [:div.application__submitted-submit-notification-inner
        [:h1.application__submitted-submit-notification-heading
         {:id "submitted-submit-notification-heading"}
         (translations/get-hakija-translation
          (if @demo? :application-submitted-demo :application-submitted)
          lang)]]
       (when (-> answers
                 (get-in [:email :value])
                 (string/blank?)
                 not)
         [:div.application__submitted-submit-notification-heading
          {:id "submitted-submit-notification-confirmation"
           :role "text"}
          (translations/get-hakija-translation :application-confirmation lang)])
       [:div.application__submitted-submit-notification-inner
        [:button.application__overlay-button.application__overlay-button--enabled
         {:tab-index    "1"
          :on-click     #(reset! hidden? true)
          :data-test-id "send-feedback-button"
          :auto-focus ""}
         (translations/get-hakija-translation :application-submitted-ok lang)]]])))

(defn- submit-notification-payment
  [_ _]
  (fn []
    (let [lang @(subscribe [:application/form-language])]
      [:div.application__submitted-submit-payment
       [:div.application__submitted-submit-payment-inner
        {:role "dialog"
         :aria-modal "true"}
        [:div.application__submitted-submit-payment-icon
          [icons/icon-check]]
          [:h1.application__submitted-submit-notification-heading
           (translations/get-hakija-translation :application-submitted-payment lang)]
          [:div.application__submitted-submit-notification-heading
            (translations/get-hakija-translation :application-submitted-payment-text lang)]
          [:div.application__submitted-submit-notification-paragraph
            (translations/get-hakija-translation :application-submitted-payment-text-2 lang)]

          [:div.application__submitted-submit-payment-button-container
            [:button.application__maksut-button
             {:on-click     #(dispatch [:application/redirect-to-maksut])
              :data-test-id "maksut-button"}
             [icons/icon-card] (translations/get-hakija-translation :payment-button lang)]
           ]
        ]]
      )
    ))

(defn feedback-form
  [feedback-hidden?]
  (let [submit-status     (subscribe [:state-query [:application :submit-status]])
        star-hovered      (subscribe [:state-query [:application :feedback :star-hovered]])
        stars             (subscribe [:state-query [:application :feedback :stars]])
        rating-status     (subscribe [:state-query [:application :feedback :status]])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])
        show-feedback?    (reaction (and (= :submitted @submit-status)
                                         (not @feedback-hidden?)))
        lang              (subscribe [:application/form-language])]
    (fn []
      (let [rated?     (= :rating-given @rating-status)
            submitted? (= :feedback-submitted @rating-status)]
        (when (and @show-feedback? (nil? @virkailija-secret))
          [:div.application-feedback-form
           {:role "dialog"
            :aria-modal "true"
            :aria-labelledby "application-feedback-form__header"}
           [:button.a-button.application-feedback-form__close-button
            {:on-click     #(dispatch [:application/rating-form-toggle])
             :data-test-id "close-feedback-form-button"
             :aria-label   (get (:close general-texts) @lang)}
            [:i.zmdi.zmdi-close.close-details-button-mark]]
           [:div.application-feedback-form-container
            (when (not submitted?)
              {:aria-live "polite"})
            (when (not submitted?)
              [:h2.application-feedback-form__header
               {:id "application-feedback-form__header"}
               (translations/get-hakija-translation :feedback-header @lang)])
            (when (not submitted?)
              [:div.application-feedback-form__rating-container.animated.zoomIn
               {:on-click      #(dispatch [:application/rating-submit (star-number-from-event %)])
                :on-key-down   (fn [e]
                                 (when (or (= " " (.-key e))
                                           (= "Enter" (.-key e)))
                                   (.preventDefault e)
                                   (dispatch [:application/rating-submit (star-number-from-event e)])))
                :on-mouse-out  #(dispatch [:application/rating-hover 0])
                :on-mouse-over #(dispatch [:application/rating-hover (star-number-from-event %)])
                :role          "radiogroup"}
               (let [stars-active (or @stars @star-hovered 0)]
                 (map (fn [n]
                        (let [star-classes (if (< n stars-active)
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--active.zmdi.zmdi-star
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--inactive.zmdi.zmdi-star-outline)
                              star-number (inc n)]
                          [star-classes
                           {:key          (str "rating-star-" n)
                            :tab-index    "0"
                            :role         "radio"
                            :aria-checked (= @stars star-number)
                            :aria-label   (if (< 0 star-number 6)
                                            (get (translations/get-hakija-translation :feedback-ratings @lang) star-number)
                                            "")
                            :data-star-n  star-number}])) (range 5)))])
            (when (not submitted?)
              [:div.application-feedback-form__rating-text
               (let [stars-selected (or @stars @star-hovered)]
                 (if (and (int? stars-selected)
                          (< 0 stars-selected 6))
                   (get (translations/get-hakija-translation :feedback-ratings @lang) stars-selected)
                   (gstring/unescapeEntities "&nbsp;")))])
            (when (not submitted?)
              [:div.application-feedback-form__text-feedback-container
               [:textarea.application__form-text-input.application__form-text-area.application__form-text-area__size-medium
                {:on-change   #(dispatch [:application/rating-update-feedback (.-value (.-target %))])
                 :placeholder (translations/get-hakija-translation :feedback-text-placeholder @lang)
                 :max-length  2000
                 :tab-index   "0"}]])
            (when (and (not submitted?)
                       rated?)
              [:button.application__overlay-button.application__overlay-button--enabled
               {:on-click   (fn [evt]
                              (.preventDefault evt)
                              (dispatch [:application/rating-feedback-submit]))
                :tab-index  "0"
                :aria-label (translations/get-hakija-translation :feedback-send @lang)}
               (translations/get-hakija-translation :feedback-send @lang)])
            (when (and (not submitted?)
                       (not rated?))
              [:button.application__overlay-button.application__overlay-button
               {:disabled true}
               (translations/get-hakija-translation :feedback-send @lang)])
            (when (not submitted?)
              [:div.application-feedback-form__disclaimer (translations/get-hakija-translation :feedback-disclaimer @lang)])
            (when submitted?
              [:div.application__thanks
               [:i.zmdi.zmdi-thumb-up.application__thanks-icon]
               [:span.application__thanks-text (translations/get-hakija-translation :feedback-thanks @lang)]])]])))))

(defn- submitted-overlay
  []
  (let [submit-status               (subscribe [:state-query [:application :submit-status]])
        submit-details              (subscribe [:state-query [:application :submit-details]])
        submit-notification-hidden? (r/atom false)
        feedback-hidden?            (subscribe [:state-query [:application :feedback :hidden?]])
        demo?                       (subscribe [:application/demo?])
        logged-in?                  (subscribe [:state-query [:oppija-session :logged-in]])]
    (fn []
      [:div.application__submitted-overlay-wrapper
       (when (and (= :submitted @submit-status)
                  (or (not @feedback-hidden?)
                      (not @submit-notification-hidden?)))
         [:div.application__submitted-overlay
          (when (not @submit-notification-hidden?)
            (if @submit-details
              [submit-notification-payment submit-notification-hidden? @submit-details]
              (if (and (not @demo?)
                       @logged-in?)
                [submit-notification-ht submit-notification-hidden?]
                [submit-notification submit-notification-hidden? demo?])))
          (when (not @feedback-hidden?) [feedback-form feedback-hidden?])])])))

(defn- modal-info-element-overlay-inner
  [field]
  (let [modal-hidden (r/atom false)]
    (fn []
      (let [languages              (subscribe [:application/default-languages])
            application-identifier (subscribe [:application/application-identifier])
            header                 (util/non-blank-val (:label field) @languages)
            text                   (util/non-blank-val (:text field) @languages)
            button-text            (util/non-blank-val (:button-text field) @languages)]
        (when (and field (not @modal-hidden))
          [:div.application__notification-overlay
           [:div.application__notification-container
            [:h1.application__notification-title
             (when (not-empty header)
               header)]
            [markdown-paragraph text (-> field :params :info-text-collapse) @application-identifier]
            [:button.application__notification-button.application__notification-button--enabled
             {:on-click #(reset! modal-hidden true)}
             button-text]]])))))

(defn- ht-session-expired []
  (let [lang                       (subscribe [:application/form-language])
        expired?                   (subscribe [:state-query [:oppija-session :expired]])
        session-expires-in-minutes (subscribe [:state-query [:oppija-session :session-expires-in-minutes-warning]])
        submit-status              (subscribe [:state-query [:application :submit-status]])]
    (if (and @expired?
             (not (= :submitted @submit-status)))
      [:div.application__ht-session-expired-overlay
       [:div.application__ht-session-expired-container
        [:i.material-icons-outlined
         {:title "danger"}
         "error_outline"]
        [:h1.application__ht-session-expired-title
         (translations/get-hakija-translation :ht-session-expired-header @lang)]
        [:p.application__ht-session-expired-main-text (translations/get-hakija-translation :ht-session-expired-text @lang)]
        [:button.application__ht-session-expired-button.application__ht-session-expired-button--enabled
         {:on-click #(dispatch [:application/redirect-to-opintopolku-etusivu (name @lang)])}
         (translations/get-hakija-translation :ht-session-expired @lang)]]]
      (when (and @session-expires-in-minutes
                 (not (= :submitted @submit-status)))
        [:div.application__ht-session-expires-soon-overlay
         [:div.application__ht-session-expires-soon-wrapper
          [:div.application__ht-session-expires-soon-header-container
           [:i.material-icons-outlined
            {:title "danger"}
            "error_outline"]
           [:h1 (translations/get-hakija-translation :ht-session-expiring-header @lang)]]
          [:p.application__ht-session-expires-soon-paragraph (translations/get-hakija-translation :ht-session-expiring-text-variable @lang @session-expires-in-minutes)]
          [:button.application__ht-session-expires-soon-refresh-button
           {:on-click #(dispatch [:application/close-session-expires-warning-dialog])}
           (translations/get-hakija-translation :ht-jatka-palvelun-kayttoa @lang)]]]))))

(defn- ht-notification-modal []
  (let [lang (subscribe [:application/form-language])
        params @(subscribe [:state-query [:application :notification-modal]])
        {:keys [header main-text button-text on-click]} params]
    (when params
      [:div.application__ht-notification-overlay
       [:div.application__ht-notification-container
        [:div.application__ht-notification-top-container
         [:h1.application__ht-notification-title
          (when (not-empty header)
            header)]
         [:div.application__ht-notification-close
          [:i.material-icons-outlined
           {:on-click   #(dispatch [:application/set-active-notification-modal nil])
            :aria-label (translations/get-hakija-translation :close @lang)
            :title      (translations/get-hakija-translation :close @lang)}
           "close"]]]
        (when (not-empty main-text)
          [:p.application__ht-notification-main-text main-text])
        [:button.application__ht-notification-button.application__ht-notification-button--enabled
         {:on-click on-click}
         button-text]]])))

(defn- modal-info-element-overlay
  []
  (when-let [field @(subscribe [:application/first-visible-modal-info-element])]
    [modal-info-element-overlay-inner field]))

(defn error-display []
  (let [error-code (subscribe [:state-query [:error :code]])
        error-lang (subscribe [:state-query [:error :lang]])
        form-lang  (subscribe [:application/form-language])]
    (fn [] (when-let [error-code @error-code]
             [:div.application__message-display
              {:class (if (some #(= error-code %) [:inactivated :network-offline])
                        "application__message-display--warning"
                        "application__message-display--error")}
              [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
              (let [lang (or @error-lang @form-lang)]
                [:div.application__message-display--details (translations/get-hakija-translation error-code lang)])]))))

(defn ht-error-display []
  (let [error-code (subscribe [:state-query [:oppija-session :error]])
        lang       (subscribe [:application/form-language])]
    (fn [] (when-let [error-code @error-code]
             [:div.application__message-display
              {:class (if (some #(= error-code %) [:inactivated :network-offline])
                        "application__message-display--warning"
                        "application__message-display--error")}
              [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
              [:div.application__message-display--details (translations/get-hakija-translation error-code @lang)]]))))

(defn demo-overlay
  []
  (let [form? (subscribe [:application/form])
        demo-open? (subscribe [:application/demo-open?])
        demo-requested? (subscribe [:application/demo-requested?])
        demo-modal-open? (subscribe [:application/demo-modal-open?])]
    (fn []
      (when (and @demo-requested? @demo-modal-open? @form?)
        (let [demo-lang (subscribe [:application/demo-lang])
              url (when-let [konfo-base (config/get-public-config [:konfo :service_url])]
                    (str konfo-base "/konfo/" @demo-lang "/"))
              [text1 text2] (translations/get-hakija-translation :demo-closed-notification (keyword @demo-lang))]
          (if (and @demo-requested? @demo-open?)
            [:div.application__notification-overlay
             [:div.application__notification-container
             {:role "alertdialog"
              :aria-modal "true"
              :aria-live "polite"
              :aria-labelledby "demo-notification-title demo-notification-p"}
              [:h1.application__notification-title
               {:id "demo-notification-title"}
               (translations/get-hakija-translation :demo-notification-title (keyword @demo-lang))]
              [:p
               {:id "demo-notification-p"}
               (translations/get-hakija-translation :demo-notification (keyword @demo-lang))]
              [:button.application__overlay-button.application__overlay-button--enabled.application__notification-button
               {:on-click        #(dispatch [:application/close-demo-modal])
                :data-test-id    "dismiss-demo-notification-button"
                :tab-index       "1"}
               (translations/get-hakija-translation :dismiss-demo-notification (keyword @demo-lang))]]]

            [:div.application__notification-overlay
             [:div.application__notification-container
             {:role "alertdialog"
              :aria-modal "true"
              :aria-live "polite"}
              [:h1.application__notification-title
               (translations/get-hakija-translation :demo-closed-title (keyword @demo-lang))]
              [:p text1
               [:a {:href url}
                (translations/get-hakija-translation :demo-closed-link (keyword @demo-lang))]
               text2]]]))))))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]
   [submitted-overlay]
   [demo-overlay]
   [ht-session-expired]
   [ht-notification-modal]
   [modal-info-element-overlay]])
