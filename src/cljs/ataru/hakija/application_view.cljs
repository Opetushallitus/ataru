(ns ataru.hakija.application-view
  (:require [ataru.config :as config]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.translations.translation-util :as translations]
            [re-frame.core :refer [subscribe dispatch]]
            [goog.string :as gstring]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]))

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
        apply-dates                        (when-let [hakuaika @(subscribe [:application/haku-aika])]
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
                                                  (str " (" (translations/get-hakija-translation :not-within-application-period lang) ")"))]))]
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

(defn application-contents []
  (let [form                   (subscribe [:state-query [:form]])
        load-failure?          (subscribe [:state-query [:error :code]])
        can-apply?             (subscribe [:application/can-apply?])
        editing?               (subscribe [:state-query [:application :editing?]])
        expired                (subscribe [:state-query [:application :secret-expired?]])
        delivery-status        (subscribe [:state-query [:application :secret-delivery-status]])
        lang                   (subscribe [:application/form-language])
        secret-link-valid-days (config/get-public-config [:secret-link-valid-days])]
    (fn []
      [:div.application__form-content-area
       (when-not (or @load-failure?
                     @form)
         [:div.application__form-loading-spinner
          [:i.zmdi.zmdi-hc-3x.zmdi-spinner.spin]])
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

       ^{:key (:id @form)}
       [application-header]

       (when (or @can-apply? @editing?)
         ^{:key "form-fields"}
         [render-fields @form])])))

(defn- star-number-from-event
  [event]
  (-> event
      (aget "target" "dataset" "starN")
      (js/parseInt 10)))

(defn- submit-notification
  [hidden?]
  (fn []
    (let [lang @(subscribe [:application/form-language])]
      [:div.application__submitted-submit-notification
       [:div.application__submitted-submit-notification-inner
        [:h1.application__submitted-submit-notification-heading
         (translations/get-hakija-translation :application-submitted lang)]]
       [:div.application__submitted-submit-notification-inner
        [:button.application__send-feedback-button.application__send-feedback-button--enabled
         {:on-click     #(reset! hidden? true)
          :data-test-id "send-feedback-button"}
         (translations/get-hakija-translation :application-submitted-ok lang)]]])))

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
           [:a.application-feedback-form__close-button
            {:on-click #(dispatch [:application/rating-form-toggle])
             :data-test-id "close-feedback-form-button"}
            [:i.zmdi.zmdi-close.close-details-button-mark]]
           [:div.application-feedback-form-container
            (when (not submitted?)
              [:h2.application-feedback-form__header (translations/get-hakija-translation :feedback-header @lang)])
            (when (not submitted?)
              [:div.application-feedback-form__rating-container.animated.zoomIn
               {:on-click      #(dispatch [:application/rating-submit (star-number-from-event %)])
                :on-mouse-out  #(dispatch [:application/rating-hover 0])
                :on-mouse-over #(dispatch [:application/rating-hover (star-number-from-event %)])}
               (let [stars-active (or @stars @star-hovered 0)]
                 (map (fn [n]
                        (let [star-classes (if (< n stars-active)
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--active.zmdi.zmdi-star
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--inactive.zmdi.zmdi-star-outline)]
                          [star-classes
                           {:key         (str "rating-star-" n)
                            :data-star-n (inc n)}])) (range 5)))])
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
                 :max-length  2000}]])
            (when (and (not submitted?)
                       rated?)
              [:button.application__send-feedback-button.application__send-feedback-button--enabled
               {:on-click (fn [evt]
                            (.preventDefault evt)
                            (dispatch [:application/rating-feedback-submit]))}
               (translations/get-hakija-translation :feedback-send @lang)])
            (when (and (not submitted?)
                       (not rated?))
              [:button.application__send-feedback-button.application__send-feedback-button
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
        submit-notification-hidden? (r/atom false)
        feedback-hidden?            (subscribe [:state-query [:application :feedback :hidden?]])]
    (fn []
      (when (and (= :submitted @submit-status)
                 (or (not @feedback-hidden?)
                     (not @submit-notification-hidden?)))
        [:div.application__submitted-overlay
         (when (not @feedback-hidden?) [feedback-form feedback-hidden?])
         (when (not @submit-notification-hidden?) [submit-notification submit-notification-hidden?])]))))

(defn error-display []
  (let [error-code (subscribe [:state-query [:error :code]])
        lang       (subscribe [:application/form-language])]
    (fn [] (if-let [error-code @error-code]
             [:div.application__message-display
              {:class (if (some #(= error-code %) [:inactivated :network-offline])
                        "application__message-display--warning"
                        "application__message-display--error")}
              [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
              [:div.application__message-display--details (translations/get-hakija-translation error-code @lang)]]))))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]
   [submitted-overlay]])
