(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]))

(def logo-image
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(def logo [:div.logo-elements logo-image logo-text])

(defn invalid-field-status [valid-status]
  (let [show-details (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (str (count (:invalid-fields valid-status)) " pakollista tietoa puuttuu")]
          (when @show-details
            [:div
             [:div.application__invalid-fields-arrow-up]
             (into [:div.application__invalid-fields
                    [:span.application__close-invalid-fields
                     {:on-click toggle-show-details}
                     "x"]]
                (mapv (fn [field] [:a {:href (str "#scroll-to-" (name (:key field)))} [:div (-> field :label :fi)]])
                      (:invalid-fields valid-status)))])]))))

(defn sent-indicator [submit-status]
  (match submit-status
         :submitting [:div.application__sent-indicator "Hakemusta lähetetään"]
         :submitted  [:div.application__sent-indicator.animated.fadeIn "Saat vahvistuksen sähköpostiisi"]
         :else nil))

(defn send-button-or-placeholder [valid-status submit-status]
  (match submit-status
         :submitted [:div.application__sent-placeholder.animated.fadeIn
                     [:i.zmdi.zmdi-check]
                     [:span.application__sent-placeholder-text "Hakemus lähetetty"]]
         :else      [:button.application__send-application-button
                      {:disabled (or (not (:valid valid-status)) (contains? #{:submitting :submitted} submit-status))
                       :on-click #(dispatch [:application/submit-form])}
                      "LÄHETÄ HAKEMUS"]))

(defn status-controls []
  (let [valid-status (subscribe [:application/valid-status])
        submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn []
      [:div.application__status-controls
       [send-button-or-placeholder @valid-status @submit-status]
       [invalid-field-status @valid-status]
       [sent-indicator @submit-status]])))

(defn wrapper-section-link [ws]
  [:a.application__banner-wrapper-section-link
   {:href (str "#scroll-to-" (:id ws))
    :class (if (:valid ws) "" "application__banner-wrapper-section-link-not-valid")}
   (-> ws :label :fi)])

(defn wrapper-section [ws]
  (if (:valid ws)
    [:div.application__banner-wrapper-section
     [:img.application__banner-wrapper-section-valid-img {:src "images/icon_check.png"}]
     [wrapper-section-link ws]]
    [:div.application__banner-wrapper-section.application__banner-wrapper-section-not-valid
     [wrapper-section-link ws]]))

(defn wrapper-sections []
  (let [wrapper-sections (subscribe [:application/wrapper-sections])]
    (fn []
      (when @wrapper-sections
        (into [:div.application__banner-wrapper-sections-content]
              (mapv wrapper-section @wrapper-sections))))))

(defn banner [] [:div.application__banner-container
                 [:div.application__top-banner-container [:div.application-top-banner logo [status-controls]]]
                 [:div.application__banner-wrapper-sections [wrapper-sections]]])
