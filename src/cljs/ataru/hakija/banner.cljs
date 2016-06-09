(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(def logo-image
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(def logo [:div.logo-elements logo-image logo-text])

(defn invalid-field-status [valid-status]
  (let [invalid-fields (:invalid-fields valid-status)]
    (when (seq invalid-fields)
      [:div.application__invalid-field-status (str (count invalid-fields) " pakollista tietoa puuttuu")
          (into [:div.application__invalid-fields]
            (mapv (fn [field] [:div (-> field :label :fi)]) invalid-fields))])))

(defn status-controls []
  (let [valid-status (subscribe [:application/valid-status])
        submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn []
      [:div.application__status-controls
       [invalid-field-status @valid-status]
       [:button.application__send-application-button
        {:disabled (or (not (:valid @valid-status)) (contains? #{:submitting :submitted} @submit-status))
         :on-click #(dispatch [:application/submit-form])}
        "LÄHETÄ HAKEMUS"]
       (match @submit-status
              :submitting [:div.application__sent-indicator "Hakemusta lähetetään"]
              :submitted [:div.application__sent-indicator "Hakemus lähetetty"]
              :else nil)])))

(defn wrapper-section-link [ws]
  [:a.application__banner-wrapper-section-link
   {:href (str "#wrapper-" (:id ws))
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

(defn banner [] [:div
                 [:div.top-banner.application-top-banner logo [status-controls]]
                 [:div.application__banner-wrapper-sections [wrapper-sections]]])
