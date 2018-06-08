(ns ataru.hakija.banner
  (:require [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [ataru.util :as util]
            [ataru.cljs-util :refer [get-translation]]))

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
        languages           (subscribe [:application/default-languages])]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (first (get-translation :check-answers))
          [:b (count (:invalid-fields valid-status))]
          (last (get-translation :check-answers))]
         (when @show-details
           [:div
            [:div.application__invalid-fields-arrow-up]
            (into [:div.application__invalid-fields
                   [:span.application__close-invalid-fields
                    {:on-click toggle-show-details}
                    "x"]]
                  (map (fn [field]
                         (let [label (util/non-blank-val (:label field) @languages)]
                           [:a {:href (str "#scroll-to-" (name (:key field)))} [:div label]]))
                       (:invalid-fields valid-status)))])]))))

(defn sent-indicator [submit-status]
  (let [virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])]
    (fn [submit-status]
      (match [submit-status @virkailija-secret]
             [:submitting _] [:div.application__sent-indicator (get-translation :application-sending)]
             [:submitted (_ :guard #(nil? %))]
             [:div.application__sent-indicator.animated.fadeIn (get-translation :application-confirmation)]
             :else nil))))

(defn- edit-text [editing?
                  hakija-secret
                  virkailija-secret]
  (cond (and editing? (some? hakija-secret))
        (get-translation :application-hakija-edit-text)

        (and editing? (some? virkailija-secret))
        (get-translation :application-virkailija-edit-text)

        :else
        (get-translation :hakija-new-text)))

(defn send-button-or-placeholder [valid-status submit-status]
  (let [secret            (subscribe [:state-query [:application :secret]])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])
        editing           (subscribe [:state-query [:application :editing?]])
        values-changed?   (subscribe [:state-query [:application :values-changed?]])]
    (fn [valid-status submit-status]
      (match submit-status
             :submitted [:div.application__sent-placeholder.animated.fadeIn
                         [:i.zmdi.zmdi-check]
                         [:span.application__sent-placeholder-text
                          (get-translation (if (and @editing @virkailija-secret)
                                             :modifications-saved
                                             :application-sent))]]
             :else [:button.application__send-application-button
                    {:disabled (or (not (:valid valid-status))
                                   (contains? #{:submitting :submitted} submit-status)
                                   (and @editing (empty? @values-changed?)))
                     :on-click #(if @editing
                                  (dispatch [:application/edit])
                                  (dispatch [:application/submit]))}
                    (edit-text @editing @secret @virkailija-secret)]))))

(defn- preview-toggle
  [submit-status enabled?]
  (when (not submit-status)
    [:div.toggle-switch__row
     [:div.toggle-switch
      [:div.toggle-switch__slider
       {:on-click (fn [_] (dispatch [:state-update #(update-in % [:application :preview-enabled] not)]))
        :class    (if enabled? "toggle-switch__slider--right" "toggle-switch__slider--left")}
       [:div.toggle-switch__label-left (get-translation :preview)]
       [:div.toggle-switch__label-divider]
       [:div.toggle-switch__label-right (get-translation :preview)]]]]))

(defn- seconds-text [seconds]
  (when (pos? seconds)
    (str seconds " "
         (if (= 1 seconds)
           (get-translation :second)
           (get-translation :seconds)))))

(defn- minutes-text [minutes]
  (when (pos? minutes)
    (str minutes " "
         (if (= 1 minutes)
           (get-translation :minute)
           (get-translation :minutes)))))

(defn- minutes-seconds-text [minutes seconds]
  (str (minutes-text minutes) " "
       (seconds-text seconds)))

(defn- hours-minutes-text [hours minutes]
  (str hours " "
       (if (= 1 hours)
         (get-translation :hour)
         (get-translation :hours))
       " " (minutes-text minutes)))

(defn- new-time-left [hakuaika-end time-diff]
  (/ (- hakuaika-end (.getTime (js/Date.)) time-diff) 1000))

(defn- hakuaika-left []
  (let [hakuaika-end (subscribe [:state-query [:form :hakuaika-end]])
        time-diff    (subscribe [:state-query [:form :time-delta-from-server]])
        seconds-left (r/atom (new-time-left @hakuaika-end @time-diff))
        interval     (r/atom nil)]
    (reset! interval (js/setInterval (fn []
                                       (let [new-time (new-time-left @hakuaika-end @time-diff)]
                                         (if (or (nil? @hakuaika-end) (< 0 new-time))
                                           (reset! seconds-left new-time)
                                           (.clearInterval js/window @interval))))
                                     1000))
    (fn []
      (when (and (> (* 24 3600) @seconds-left) (<= 1 @seconds-left))
        (let [hours          (Math/floor (/ @seconds-left 3600))
              minutes        (Math/floor (/ (rem @seconds-left 3600) 60))
              seconds        (Math/floor (rem (rem @seconds-left 3600) 60))
              time-left-text (cond
                               (pos? hours) (hours-minutes-text hours minutes)
                               (pos? minutes) (minutes-seconds-text minutes seconds)
                               :else (seconds-text seconds))]
          [:div.application__hakuaika-left
           (str "Hakuaikaa jäljellä " time-left-text)])))))

(defn status-controls []
  (let [valid-status  (subscribe [:application/valid-status])
        submit-status (subscribe [:state-query [:application :submit-status]])
        can-apply?    (subscribe [:application/can-apply?])
        editing?      (subscribe [:state-query [:application :editing?]])
        preview-enabled? (subscribe [:state-query [:application :preview-enabled]])]
    (when (or @can-apply? @editing?)
      [:div.application__status-controls-container
       [:div.application__preview-control
        [preview-toggle @submit-status @preview-enabled?]]
       [:div.application__status-controls
        [send-button-or-placeholder @valid-status @submit-status]
        [invalid-field-status @valid-status]
        [sent-indicator @submit-status]]])))

(defn virkailija-fill-ribbon
  []
  (when (and (some? @(subscribe [:state-query [:application :virkailija-secret]]))
             (not @(subscribe [:state-query [:application :editing?]])))
    [:div.application__virkailija-fill-ribbon
     "Testihakemus / Virkailijatäyttö"]))

(defn banner [] [:div.application__banner-container
                 [virkailija-fill-ribbon]
                 [:div.application__top-banner-container
                  [:div.application-top-banner
                   [logo]
                   [hakuaika-left]
                   [status-controls]]]])
