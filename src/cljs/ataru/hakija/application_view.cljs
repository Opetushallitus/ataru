(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.cljs-util :as util :refer [get-translation]]
            [re-frame.core :refer [subscribe dispatch]]
            [cemerick.url :as url]
            [cljs.core.match :refer-macros [match]]
            [cljs-time.core :refer [to-default-time-zone now after?]]
            [cljs-time.format :refer [unparse unparse-local formatter]]
            [cljs-time.coerce :refer [from-long]]
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
        secret                             (:modify (util/extract-query-params))
        virkailija?                        @(subscribe [:application/virkailija?])
        apply-dates                        (when-let [hakuaika @(subscribe [:application/haku-aika])]
                                             (if (:jatkuva-haku? hakuaika)
                                               (get-translation :continuous-period)
                                               [:span (str (get-translation :application-period)
                                                           " "
                                                           (-> hakuaika :label :start selected-lang)
                                                           " - "
                                                           (-> hakuaika :label :end selected-lang)
                                                           )
                                                [:br]
                                                (when (and (not (:on hakuaika))
                                                           (not virkailija?))
                                                  (str " (" (get-translation :not-within-application-period) ")"))]))]
    [:div
     [:div.application__header-container
      [:span.application__header (or (-> form :tarjonta :haku-name selected-lang)
                                     (-> form :name selected-lang))]
      (when (and (not submitted?)
                 (> (count languages) 0)
                 (nil? secret))
        [:span.application__header-text
         (map-indexed (fn [idx lang]
                        (cond-> [:span {:key (name lang)}
                                 [:a {:href (-> (.. js/window -location -href)
                                                (url/url)
                                                (assoc-in [:query "lang"] (name lang))
                                                str)}
                                  (get language-names lang)]]
                                (> (dec (count languages)) idx)
                                (conj [:span.application__header-language-link-separator " | "])))
                      languages)])]
     (when (not-empty apply-dates)
       [:div.application__sub-header-container
        [:span.application__sub-header-dates apply-dates]])
     (when (and cannot-edit-because-in-processing?
                (not virkailija?))
       [:div.application__sub-header-container
        [:span.application__sub-header-modifying-prevented
         (get-translation :application-processed-cant-modify)]])]))

(defn readonly-fields [form]
  (let [application (subscribe [:state-query [:application]])]
    (fn [form]
      [readonly-view/readonly-fields form @application])))

(defn- render-fields [form]
  (let [submit-status    (subscribe [:state-query [:application :submit-status]])
        preview-enabled? (subscribe [:state-query [:application :preview-enabled]])]
    (fn [form]
      (if (or (= :submitted @submit-status)
              @preview-enabled?)
        [readonly-fields form]
        (do
          (dispatch [:application/run-rules])                ; wtf
          (when form
            [editable-fields form submit-status]))))))

(defn application-contents []
  (let [form            (subscribe [:state-query [:form]])
        load-failure?   (subscribe [:state-query [:error :code]])
        can-apply?      (subscribe [:application/can-apply?])
        editing?        (subscribe [:state-query [:application :editing?]])
        expired         (subscribe [:state-query [:application :secret-expired?]])
        delivery-status (subscribe [:state-query [:application :secret-delivery-status]])]
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
          [:h2 (get-translation :expired-secret-heading)]
          [:p (get-translation :expired-secret-paragraph)]
          [:button.application__secret-resend-button
           {:disabled (some? @delivery-status)
            :on-click #(dispatch [:application/send-new-secret])}
           (if (= :completed @delivery-status)
             (get-translation :expired-secret-sent)
             (get-translation :expired-secret-button))]
          [:p (get-translation :expired-secret-contact)]])

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
    [:div.application__submitted-submit-notification
     [:div.application__submitted-submit-notification-inner
      [:h1.application__submitted-submit-notification-heading
       (get-translation :application-submitted)]]
     [:div.application__submitted-submit-notification-inner
      [:a.application__send-feedback-button.application__send-feedback-button--enabled
       {:on-click #(reset! hidden? true)}
       (get-translation :application-submitted-ok)]]]))

(defn feedback-form
  [feedback-hidden?]
  (let [star-hovered      (subscribe [:state-query [:application :feedback :star-hovered]])
        stars             (subscribe [:state-query [:application :feedback :stars]])
        rating-status     (subscribe [:state-query [:application :feedback :status]])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])]
    (fn []
      (let [rated?     (= :rating-given @rating-status)
            submitted? (= :feedback-submitted @rating-status)]
        [:div.application-feedback-form
         (when (not submitted?)
           [:div.application-feedback-form-handle
            [:a
             {:on-click #(dispatch [:application/rating-form-toggle])}
             (get-translation :feedback-handle)
             (if @feedback-hidden?
               [:i.zmdi.zmdi-chevron-up]
               [:i.zmdi.zmdi-chevron-down])]])
         (when (and (not @feedback-hidden?) (nil? @virkailija-secret))
           [:div.application-feedback-form-inner
            [:a.application-feedback-form__close-button
             {:on-click #(dispatch [:application/rating-form-toggle])}
             [:i.zmdi.zmdi-close.close-details-button-mark]]
            [:div.application-feedback-form-container
             (when (not submitted?)
               [:h2.application-feedback-form__header (get-translation :feedback-header)])
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
                    (get (get-translation :feedback-ratings) stars-selected)
                    (gstring/unescapeEntities "&nbsp;")))])
             (when (not submitted?)
               [:div.application-feedback-form__text-feedback-container
                [:textarea.application__form-text-input.application__form-text-area.application__form-text-area__size-medium
                 {:on-change   #(dispatch [:application/rating-update-feedback (.-value (.-target %))])
                  :placeholder (get-translation :feedback-text-placeholder)
                  :max-length  2000}]])
             (when (and (not submitted?)
                        rated?)
               [:a.application__send-feedback-button.application__send-feedback-button--enabled
                {:on-click (fn [evt]
                             (.preventDefault evt)
                             (dispatch [:application/rating-feedback-submit]))}
                (get-translation :feedback-send)])
             (when (and (not submitted?)
                        (not rated?))
               [:a.application__send-feedback-button.application__send-feedback-button--disabled
                (get-translation :feedback-send)])
             (when (not submitted?)
               [:div.application-feedback-form__disclaimer (get-translation :feedback-disclaimer)])
             (when submitted?
               [:div.application__thanks
                [:i.zmdi.zmdi-thumb-up.application__thanks-icon]
                [:span.application__thanks-text (get-translation :feedback-thanks)]])]])]))))

(defn- submitted-overlay
  []
  (let [submit-status               (subscribe [:state-query [:application :submit-status]])
        submit-notification-hidden? (r/atom false)
        feedback-hidden?            (subscribe [:state-query [:application :feedback :hidden?]])]
    (fn []
      (let [submitted? (= :submitted @submit-status)]
        [:div.application__submitted-overlay
         {:class (when (= :submitted @submit-status) "application__submitted-overlay--submitted")}
         [feedback-form feedback-hidden?]
         (when (and submitted?
                    (not @submit-notification-hidden?))
           [submit-notification submit-notification-hidden?])]))))

(defn error-display []
  (let [error-code (subscribe [:state-query [:error :code]])]
    (fn [] (if @error-code
             [:div.application__message-display
              {:class (if (= :network-offline @error-code)
                        "application__message-display--warning"
                        "application__message-display--error")}
              [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
              [:div.application__message-display--details (get-translation @error-code)]]
             nil))))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]
   [submitted-overlay]])
