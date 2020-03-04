(ns ataru.hakija.banner
  (:require [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [goog.string :as s]
            [cljs.core.match :refer-macros [match]]
            [ataru.util :as util]
            [ataru.translations.translation-util :as translations]))

(defn logo []
  (let [lang (subscribe [:application/form-language])]
    (case @lang
      :fi [:div.logo-fi {:title "Opintopolku.fi"}]
      :sv [:div.logo-sv {:title "Studieinfo.fi"}]
      :en [:div.logo-en {:title "Studyinfo.fi"}])))

(defn- form-field-type [form-fields key]
  (->> form-fields
       (filter (comp (partial = key) keyword :id))
       (map :fieldType)
       (first)))

(defn invalid-field-status []
  (let [show-details        (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)
        languages           (subscribe [:application/default-languages])
        lang                (subscribe [:application/form-language])]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (first (translations/get-hakija-translation :check-answers @lang))
          [:b (count (:invalid-fields valid-status))]
          (last (translations/get-hakija-translation :check-answers @lang))]
         (when @show-details
           [:div
            [:div.application__invalid-fields-arrow-up]
            (into [:div.application__invalid-fields
                   [:span.application__close-invalid-fields
                    {:on-click toggle-show-details}
                    "x"]]
                  (map (fn [field]
                         (let [label (util/non-blank-val (:label field) @languages)]
                           [:a {:href (str "#scroll-to-" (name (:key field)))} [:div (if (empty? label)
                                                                                       (translations/get-hakija-translation :missing-input @lang)
                                                                                       label)]]))
                       (:invalid-fields valid-status)))])]))))

(defn sent-indicator [submit-status]
  (let [virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])
        lang              (subscribe [:application/form-language])]
    (fn [submit-status]
      (match [submit-status @virkailija-secret]
             [:submitting _] [:div.application__sent-indicator (translations/get-hakija-translation :application-sending @lang)]
             [:submitted (_ :guard #(nil? %))]
             [:div.application__sent-indicator.animated.fadeIn (translations/get-hakija-translation :application-confirmation @lang)]
             :else nil))))

(defn- edit-text [editing?
                  hakija-secret
                  virkailija-secret
                  lang]
  (cond (and editing? (some? hakija-secret))
        (translations/get-hakija-translation :application-hakija-edit-text lang)

        (and editing? (some? virkailija-secret))
        (translations/get-hakija-translation :application-virkailija-edit-text lang)

        :else
        (translations/get-hakija-translation :hakija-new-text lang)))

(defn send-button-or-placeholder [valid-status submit-status]
  (let [secret                (subscribe [:state-query [:application :secret]])
        virkailija-secret     (subscribe [:state-query [:application :virkailija-secret]])
        transmitting?         (subscribe [:application/attachments-uploading?])
        editing               (subscribe [:state-query [:application :editing?]])
        values-changed?       (subscribe [:state-query [:application :values-changed?]])
        validators-processing (subscribe [:state-query [:application :validators-processing]])
        secret-expired?       (subscribe [:state-query [:application :secret-expired?]])
        lang                  (subscribe [:application/form-language])]
    (fn [valid-status submit-status]
      (match submit-status
             :submitted [:div.application__sent-placeholder.animated.fadeIn
                         [:i.zmdi.zmdi-check]
                         [:span.application__sent-placeholder-text
                          (translations/get-hakija-translation
                            (if (and @editing @virkailija-secret)
                              :modifications-saved
                              :application-sent)
                            @lang)]]
             :else [:button.application__send-application-button
                    {:disabled (or @transmitting?
                                   (not-empty (:invalid-fields valid-status))
                                   (contains? #{:submitting :submitted} submit-status)
                                   (and @editing (empty? @values-changed?))
                                   @secret-expired?
                                   (not (empty? @validators-processing)))
                     :on-click #(if @editing
                                  (dispatch [:application/edit])
                                  (dispatch [:application/submit]))}
                    (edit-text @editing @secret @virkailija-secret @lang)]))))

(defn- preview-toggle
  [submit-status enabled?]
  (let [toggle-fn (fn [_] (dispatch [:state-update #(update-in % [:application :preview-enabled] not)]))
        lang      @(subscribe [:application/form-language])]
    (when (not submit-status)
      [:div.application__preview-toggle-container
       [:a.application__preview-link
        {:class (when enabled? "application__preview-link--disabled")
         :on-click toggle-fn}
        (translations/get-hakija-translation :edit-answers lang)]
       [:a.application__preview-link
        {:class (when-not enabled? "application__preview-link--disabled")
         :on-click toggle-fn}
        (translations/get-hakija-translation :preview-answers lang)]])))

(defn- new-time-left [hakuaika-end time-diff]
  (/ (- hakuaika-end (.getTime (js/Date.)) time-diff) 1000))

(defn- round [value step]
  (let [inv (/ 1.0 step)]
    (/ (Math/ceil (* value inv)) inv)))

(defn hakuaika-left-text [seconds-left lang]
  (let [hours     (Math/floor (/ seconds-left 3600))
        minutes   (Math/floor (/ (rem seconds-left 3600) 60))
        text-code (cond
                    (< 23 hours)                        nil
                    (< seconds-left 0)                  :application-period-expired
                    (and (zero? hours) (> 15 minutes))  :application-period-less-than-15-min-left
                    (and (zero? hours) (> 30 minutes))  :application-period-less-than-30-min-left
                    (and (zero? hours) (> 45 minutes))  :application-period-less-than-45-min-left
                    (zero? hours)                       :application-period-less-than-hour-left
                    (> 24 hours)                        :application-period-less-than-day-left)]
    (if text-code
      [:div.application__hakuaika-left
       (translations/get-hakija-translation text-code lang)])))

(defn- hakuaika-left []
  (let [hakuaika-end  (subscribe [:state-query [:form :hakuaika-end]])
        time-diff     (subscribe [:state-query [:form :time-delta-from-server]])
        seconds-left  (r/atom (new-time-left @hakuaika-end @time-diff))
        interval      (r/atom nil)
        lang          @(subscribe [:application/form-language])]
    (reset! interval (js/setInterval (fn []
                                       (let [new-time (new-time-left @hakuaika-end @time-diff)]
                                         (if (or (nil? @hakuaika-end) (< 0 new-time))
                                           (reset! seconds-left new-time)
                                           (.clearInterval js/window @interval))))
                                     1000))
    (fn []
      (hakuaika-left-text @seconds-left lang))))

(defn status-controls [submit-status]
  (let [valid-status (subscribe [:application/valid-status])
        can-apply?   (subscribe [:application/can-apply?])
        editing?     (subscribe [:state-query [:application :editing?]])]
    (fn []
      (when (or @can-apply? @editing?)
        [:div.application__status-controls-container
         [:div.application__status-controls
          [send-button-or-placeholder @valid-status @submit-status]
          [invalid-field-status @valid-status]
          [sent-indicator @submit-status]]]))))

(defn virkailija-fill-ribbon
  []
  (when (and (some? @(subscribe [:state-query [:application :virkailija-secret]]))
             (not @(subscribe [:state-query [:application :editing?]])))
    [:div.application__virkailija-fill-ribbon
     "Testihakemus / Virkailijatäyttö"]))

(defn banner []
  (let [submit-status    (subscribe [:state-query [:application :submit-status]])
        preview-enabled? (subscribe [:state-query [:application :preview-enabled]])]
    (fn []
      [:div.application__banner-container
       [virkailija-fill-ribbon]
       [:div.application__top-banner-container
        [:div.application-top-banner
         [logo]
         [hakuaika-left]
         [:div.application__preview-control
          [preview-toggle @submit-status @preview-enabled?]]
         [status-controls submit-status]]]])))
