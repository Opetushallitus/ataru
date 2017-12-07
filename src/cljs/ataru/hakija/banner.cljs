(ns ataru.hakija.banner
  (:require [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [ataru.translations.translation-util :refer [get-translation]]))

(defn logo []
  (let [lang (subscribe [:application/form-language])]
    [:img.logo
     (case @lang
       :fi {:src "/hakemus/images/opintopolku_large-fi.png"
            :alt "Opintopolku.fi"}
       :sv {:src "/hakemus/images/opintopolku_large-sv.png"
            :alt "Studieinfo.fi"}
       :en {:src "/hakemus/images/opintopolku_large-en.png"
            :alt "Studyinfo.fi"})]))

(defn- form-field-type [form-fields key]
  (->> form-fields
       (filter (comp (partial = key) keyword :id))
       (map :fieldType)
       (first)))

(defn invalid-field-status []
  (let [show-details        (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)
        lang                (subscribe [:application/form-language])
        invalid-fields-text (get-translation :check-answers @lang)
        default-lang        (subscribe [:application/default-language])
        form-fields         (reaction (util/flatten-form-fields @(subscribe [:state-query [:form :content]])))]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (first invalid-fields-text)
          [:b (count (:invalid-fields valid-status))]
          (last invalid-fields-text)]
         (when @show-details
           [:div
            [:div.application__invalid-fields-arrow-up]
            (into [:div.application__invalid-fields
                   [:span.application__close-invalid-fields
                    {:on-click toggle-show-details}
                    "x"]]
                  (map (fn [field]
                         (let [label (or (get-in field [:label @lang])
                                         (get-in field [:label @default-lang]))]
                           [:a {:href (str "#scroll-to-" (name (:key field)))} [:div label]]))
                       (:invalid-fields valid-status)))])]))))

(defn sent-indicator [submit-status]
  (let [lang              (subscribe [:application/form-language])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])]
    (fn [submit-status]
      (match [submit-status @virkailija-secret]
             [:submitting _] [:div.application__sent-indicator (get-translation :application-sending @lang)]
             [:submitted (_ :guard #(nil? %))]
             [:div.application__sent-indicator.animated.fadeIn (get-translation :application-confirmation @lang)]
             :else nil))))

(defn- edit-text [hakija-secret
                  virkailija-secret
                  lang]
  (cond (some? hakija-secret)
        (get-translation :application-hakija-edit-text lang)

        (some? virkailija-secret)
        (get-translation :application-virkailija-edit-text lang)

        :else
        (get-translation :hakija-new-text lang)))

(defn send-button-or-placeholder [valid-status submit-status]
  (let [lang              (subscribe [:application/form-language])
        secret            (subscribe [:state-query [:application :secret]])
        virkailija-secret (subscribe [:state-query [:application :virkailija-secret]])
        editing           (reaction (or (some? @secret) (some? @virkailija-secret)))
        values-changed?   (subscribe [:state-query [:application :values-changed?]])]
    (fn [valid-status submit-status]
      (match submit-status
             :submitted [:div.application__sent-placeholder.animated.fadeIn
                         [:i.zmdi.zmdi-check]
                         [:span.application__sent-placeholder-text (get-translation (if @virkailija-secret :modifications-saved :application-sent) @lang)]]
             :else [:button.application__send-application-button
                    {:disabled (or (not (:valid valid-status))
                                   (contains? #{:submitting :submitted} submit-status)
                                   (and @editing (empty? @values-changed?)))
                     :on-click #(if @editing
                                  (dispatch [:application/edit])
                                  (dispatch [:application/submit]))}
                    (edit-text @secret @virkailija-secret @lang)]))))

(defn status-controls []
  (let [valid-status         (subscribe [:application/valid-status])
        submit-status        (subscribe [:state-query [:application :submit-status]])
        can-apply?           (subscribe [:application/can-apply?])]
    (when @can-apply?
      [:div.application__status-controls
       [send-button-or-placeholder @valid-status @submit-status]
       [invalid-field-status @valid-status]
       [sent-indicator @submit-status]])))

(defn banner [] [:div.application__banner-container
                 [:div.application__top-banner-container
                  [:div.application-top-banner [logo] [status-controls]]]])
