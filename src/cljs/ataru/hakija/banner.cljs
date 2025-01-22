(ns ataru.hakija.banner
  (:require [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [ataru.hakija.application-view-icons :as icons]
            [cljs.core.match :refer-macros [match]]
            [ataru.translations.translation-util :as translations]
            [clojure.string :as string]))

(defn logo []
  (let [lang (subscribe [:application/form-language])]
    (case @lang
      :fi [:div.logo-fi {:title "Opintopolku.fi"}]
      :sv [:div.logo-sv {:title "Studieinfo.fi"}]
      :en [:div.logo-en {:title "Studyinfo.fi"}])))

(defn invalid-field-status []
  (let [show-details        (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)
        languages           (subscribe [:application/default-languages])
        lang                (subscribe [:application/form-language])
        valid-status        (subscribe [:application/valid-status])]
    (fn []
      (when (seq (:invalid-fields @valid-status))
        [:div.application__invalid-field-status
         {:aria-hidden "true"}
         [:span.application__invalid-field-status-title
          {;:role "status"
           ;:aria-atomic "true"
           :aria-hidden "true"
           :on-click toggle-show-details}
          (first (translations/get-hakija-translation :check-answers @lang))
          [:b (count (:invalid-fields @valid-status))]
          (last (translations/get-hakija-translation :check-answers @lang))]
         (when @show-details
           [:div
            [:div.application__invalid-fields-arrow-up]
            (into [:div.application__invalid-fields
                   [:span.application__close-invalid-fields
                    {:on-click toggle-show-details}
                    "x"]]
                  (map (fn [field]
                         (let [label (util/non-blank-val (:label field) @languages)
                               label-text (if (empty? label)
                                            (translations/get-hakija-translation :missing-input @lang)
                                            label)]
                           (if (:key field)
                             [:a {:href (str "#scroll-to-" (name (:key field)))} [:div label-text]]
                             [:div.application__validation-error-item label-text])))
                       (:invalid-fields @valid-status)))])]))))

(defn sent-indicator []
  (let [submit-status     (subscribe [:state-query [:application :submit-status]])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])
        demo?             (subscribe [:application/demo?])
        answers           (subscribe [:state-query [:application :answers]])
        lang              (subscribe [:application/form-language])]
    (fn []
      (match [@submit-status @virkailija-secret]
             [:submitting _]
             [:div.application__sent-indicator
              {:role "alert"}
              (translations/get-hakija-translation :application-sending @lang)]

             [:submitted (_ :guard #(nil? %))]
             (if @demo?
               [:div.application__sent-indicator.animated.fadeIn
                {:role "text"}
                (translations/get-hakija-translation :application-confirmation-demo @lang)]
               (when (-> @answers
                         (get-in [:email :value])
                         (string/blank?)
                         not)
                 [:div.application__sent-indicator.animated.fadeIn
                  {:role "text"}
                  (translations/get-hakija-translation :application-confirmation @lang)]))

             :else nil))))

(defn- edit-text [editing?
                  demo?
                  hakija-secret
                  virkailija-secret
                  lang]
  (cond (and editing? (some? hakija-secret))
        (translations/get-hakija-translation :application-hakija-edit-text lang)

        (and editing? (some? virkailija-secret))
        (translations/get-hakija-translation :application-virkailija-edit-text lang)

        demo?
        (translations/get-hakija-translation :submit-demo lang)

        :else
        (translations/get-hakija-translation :hakija-new-text lang)))

(defn logged-in-indicator-or-placeholder []
  (let [lang (subscribe [:application/form-language])
        logged-in-name (subscribe [:state-query [:oppija-session :display-name]])
        logged-in? (subscribe [:state-query [:oppija-session :logged-in]])
        menu-open? (subscribe [:state-query [:oppija-session :logout-menu-open]])
        submit-status (subscribe [:state-query [:application :submit-status]])
        already-applied? (subscribe [:state-query [:application :has-applied]])
        logout-link-fn (fn []
                         (dispatch [:application/toggle-logout-menu])
                         (dispatch [:application/set-active-notification-modal {:header      (translations/get-hakija-translation :ht-logout-confirmation-header @lang)
                                                                                :main-text   (when (and
                                                                                                     (not @already-applied?)
                                                                                                     (not= @submit-status :submitted))
                                                                                               (translations/get-hakija-translation :ht-logout-confirmation-text @lang))
                                                                                :button-text (translations/get-hakija-translation :ht-kirjaudu-ulos @lang)
                                                                                :on-click    (fn [_] (dispatch [:application/redirect-to-logout (name @lang)]))}]))]
    (when (and @logged-in-name @logged-in?)
      [:div.application__logged-in-banner-wrapper
       [icons/icon-account]
       [:div.application__logged-in-name-container
        {:on-click #(dispatch [:application/toggle-logout-menu])}
        @logged-in-name
        [:div.application__dropdown-toggle
         (if @menu-open?
           [icons/icon-arrow-drop-up]
           [icons/icon-arrow-drop-down])]]
       [:div.application__logout-dropdown-wrapper
        [:div.application__logout-dropdown-content
         {:class (if @menu-open? :application__logout-dropdown-content-open :application__logout-dropdown-content-closed)}
         [:div.application__banner-logout-link
          {:on-click     #(logout-link-fn)
           :data-test-id "tunnistautuminen-button"}
          [:i.material-icons
           {:title (translations/get-hakija-translation :ht-kirjaudu-ulos @lang)} "logout"]
          (translations/get-hakija-translation :ht-kirjaudu-ulos @lang)]]]])))

(defn send-button-or-placeholder []
  (let [submit-status         @(subscribe [:state-query [:application :submit-status]])
        secret                @(subscribe [:state-query [:application :secret]])
        virkailija-secret     @(subscribe [:state-query [:application :virkailija-secret]])
        transmitting?         @(subscribe [:application/attachments-uploading?])
        editing               @(subscribe [:state-query [:application :editing?]])
        edits?                @(subscribe [:application/edits?])
        validators-processing @(subscribe [:state-query [:application :validators-processing]])
        secret-expired?       @(subscribe [:state-query [:application :secret-expired?]])
        lang                  @(subscribe [:application/form-language])
        invalid-fields?       @(subscribe [:application/invalid-fields?])
        demo?                 @(subscribe [:application/demo?])]
    (match submit-status
      :submitted [:div.application__sent-placeholder.animated.fadeIn
                  {:role "alert"}
                  [:i.zmdi.zmdi-check]
                  [:span.application__sent-placeholder-text
                   (translations/get-hakija-translation
                     (cond
                       (and editing virkailija-secret) :modifications-saved
                       demo? :application-sent-demo
                       :else :application-sent)
                     lang)]]
      :else [:button.application__send-application-button
             {:disabled (or transmitting?
                            invalid-fields?
                            (= :submitting submit-status)
                            (and editing (not edits?))
                            secret-expired?
                            (not (empty? validators-processing)))
              :on-click #(if editing
                           (dispatch [:application/edit])
                           (dispatch [:application/submit]))
              :data-test-id "send-application-button"}
             (edit-text editing demo? secret virkailija-secret lang)])))

(defn- preview-toggle
  []
  (let [toggle-fn     (fn [_] (dispatch [:state-update #(update-in % [:application :preview-enabled] not)]))
        submit-status    @(subscribe [:state-query [:application :submit-status]])
        enabled?         @(subscribe [:state-query [:application :preview-enabled]])
        demo-modal-open? @(subscribe [:application/demo-modal-open?])
        lang             @(subscribe [:application/form-language])]
    (when (not (or demo-modal-open? submit-status))
      [:div.application__preview-toggle-container
       [:button.application__preview-link
        {:disabled (not enabled?)
         :on-click toggle-fn}
        (translations/get-hakija-translation :edit-answers lang)]
       [:button.application__preview-link
        {:disabled enabled?
         :on-click toggle-fn}
        (translations/get-hakija-translation :preview-answers lang)]])))

(defn- new-time-left [hakuaika-end time-diff]
  (when (and (some? hakuaika-end) (some? time-diff))
    (/ (- hakuaika-end (.getTime (js/Date.)) time-diff) 1000)))

(defn hakuaika-left-text [seconds-left lang]
  (let [hours     (Math/floor (/ seconds-left 3600))
        minutes   (Math/floor (/ (rem seconds-left 3600) 60))
        text-code (cond
                    (< 23 hours)                        nil
                    (nil? seconds-left)                 nil
                    (< seconds-left 0)                  :application-period-expired
                    (and (zero? hours) (> 15 minutes))  :application-period-less-than-15-min-left
                    (and (zero? hours) (> 30 minutes))  :application-period-less-than-30-min-left
                    (and (zero? hours) (> 45 minutes))  :application-period-less-than-45-min-left
                    (zero? hours)                       :application-period-less-than-hour-left
                    (> 24 hours)                        :application-period-less-than-day-left)]
    (when text-code
      [:div.application__hakuaika-left
       (translations/get-hakija-translation text-code lang)])))

(defn- hakuaika-left []
  (let [hakuaika-end  (subscribe [:state-query [:form :hakuaika-end]])
        time-diff     (subscribe [:state-query [:form :time-delta-from-server]])
        seconds-left  (r/atom (new-time-left @hakuaika-end @time-diff))
        interval      (r/atom nil)]
    (reset! interval (js/setInterval (fn []
                                       (let [new-time (new-time-left @hakuaika-end @time-diff)]
                                         (if (or (nil? @hakuaika-end) (< 0 new-time))
                                           (reset! seconds-left new-time)
                                           (.clearInterval js/window @interval))))
                                     1000))
    (fn []
      (hakuaika-left-text @seconds-left @(subscribe [:application/form-language])))))

(defn status-controls []
  (let [can-apply? (subscribe [:application/can-apply?])
        demo-modal-open? (subscribe [:application/demo-modal-open?])
        editing?   (subscribe [:state-query [:application :editing?]])]
    (fn []
      (when (and (or @can-apply? @editing?) (not @demo-modal-open?))
        [:div.application__status-controls-container
         [:div.application__status-controls
          [send-button-or-placeholder]
          [invalid-field-status]
          [sent-indicator]]]))))

(defn virkailija-fill-ribbon
  []
  (when (and (some? @(subscribe [:state-query [:application :virkailija-secret]]))
             (not @(subscribe [:state-query [:application :editing?]])))
    [:div.application__virkailija-fill-ribbon
     "Testihakemus / Virkailijatäyttö"]))

(defn- notification-banner
  [text]
  [:div.application__notification-banner-container
   [:div.application__notification-banner
    text]])

(defn- demo-notification-banner
  []
  (let [lang @(subscribe [:application/form-language])
        demo? @(subscribe [:application/demo?])]
    (when demo?
      [notification-banner (translations/get-hakija-translation :demo lang)])))

(defn banner []
  (let [form?             @(subscribe [:application/form])
        ht-lander-active? @(subscribe [:application/hakeminen-tunnistautuneena-lander-active?])
        ht-error? @(subscribe [:state-query [:application :has-applied]]);todo add other potential errors here
        control-active?   (and form? (not (or ht-lander-active? ht-error?)))]
    [:div.application__banner-container
     {:aria-live "polite"}
     [virkailija-fill-ribbon]
     [:div.application__top-banner-container
      [:div.application-top-banner
       [logo]
       [hakuaika-left]
       (when control-active?
         [:div.application__preview-control
          [preview-toggle]])
       (when control-active?
         [status-controls])
       [logged-in-indicator-or-placeholder]]]
     [demo-notification-banner]]))
