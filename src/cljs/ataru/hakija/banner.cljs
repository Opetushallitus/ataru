(ns ataru.hakija.banner
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]))

(def logo-image
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(def logo [:div.logo-elements logo-image logo-text])

(defn invalid-field-status [valid-status]
  (let [show-details (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)
        lang (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (str (count (:invalid-fields valid-status)) (case @lang
                                                        :fi " pakollista tietoa puuttuu"
                                                        :sv " obligatoriska uppgifter saknas"
                                                        :en " mandatory fields are missing"))]
          (when @show-details
            [:div
             [:div.application__invalid-fields-arrow-up]
             (into [:div.application__invalid-fields
                    [:span.application__close-invalid-fields
                     {:on-click toggle-show-details}
                     "x"]]
                (mapv (fn [field]
                        (let [label (or (get-in field [:label @lang])
                                        (get-in field [:label @default-lang]))]
                          [:a {:href (str "#scroll-to-" (name (:key field)))} [:div label]]))
                      (:invalid-fields valid-status)))])]))))

(defn sent-indicator [submit-status]
  (let [lang (subscribe [:application/form-language])]
    (fn [submit-status]
      (match submit-status
             :submitting [:div.application__sent-indicator (case @lang
                                                             :fi "Hakemusta lähetetään"
                                                             :sv "Ansökan skickas"
                                                             :en "The application is being sent")]
             :submitted [:div.application__sent-indicator.animated.fadeIn (case @lang
                                                                            :fi "Saat vahvistuksen sähköpostiisi"
                                                                            :sv "Du får en bekräftelse till din e-post"
                                                                            :en "Confirmation email will be sent to the email address you've provided")]
             :else nil))))

(defn send-button-or-placeholder [valid-status submit-status]
  (let [lang    (subscribe [:application/form-language])
        secret  (subscribe [:state-query [:application :secret]])
        editing (reaction (some? @secret))]
    (fn [valid-status submit-status]
      (match submit-status
             :submitted [:div.application__sent-placeholder.animated.fadeIn
                         [:i.zmdi.zmdi-check]
                         [:span.application__sent-placeholder-text (case @lang
                                                                     :fi "Hakemus lähetetty"
                                                                     :sv "Ansökan har skickats"
                                                                     :en "The application has been sent")]]
             :else [:button.application__send-application-button
                    {:disabled (or (not (:valid valid-status)) (contains? #{:submitting :submitted} submit-status))
                     :on-click #(if @editing
                                  (dispatch [:application/edit])
                                  (dispatch [:application/submit]))}
                    (case @lang
                      :fi (if @editing "LÄHETÄ MUUTOKSET" "LÄHETÄ HAKEMUS")
                      :sv (if @editing "SCICKA FÖRÄNDRINGAR" "SKICKA ANSÖKAN")
                      :en (if @editing "SEND MODIFICATIONS" "SEND APPLICATION"))]))))

(defn status-controls []
  (let [valid-status  (subscribe [:application/valid-status])
        submit-status (subscribe [:state-query [:application :submit-status]])
        can-apply?    (subscribe [:application/can-apply?])]
    (fn []
      (when @can-apply?
        [:div.application__status-controls
         [send-button-or-placeholder @valid-status @submit-status]
         [invalid-field-status @valid-status]
         [sent-indicator @submit-status]]))))

(defn wrapper-section-link [ws]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [ws]
      [:a.application__banner-wrapper-section-link
       {:href  (str "#scroll-to-" (:id ws))
        :class (if (:valid ws) "" "application__banner-wrapper-section-link-not-valid")}
       (or (get-in ws [:label @lang])
           (get-in ws [:label @default-lang]))])))

(defn wrapper-section [ws]
  (if (:valid ws)
    [:div.application__banner-wrapper-section
     [:img.application__banner-wrapper-section-valid-img {:src "/hakemus/images/icon_check.png"}]
     [wrapper-section-link ws]]
    [:div.application__banner-wrapper-section.application__banner-wrapper-section-not-valid
     [wrapper-section-link ws]]))

(defn wrapper-sections []
  (let [wrapper-sections (subscribe [:application/wrapper-sections])
        can-apply?       (subscribe [:application/can-apply?])]
    (fn []
      (when (and @wrapper-sections @can-apply?)
        (into [:div.application__banner-wrapper-sections-content]
              (mapv wrapper-section @wrapper-sections))))))

(defn banner [] [:div.application__banner-container
                 [:div.application__top-banner-container
                  [:div.application-top-banner logo [status-controls]]]
                 [:div.application__banner-wrapper-sections [wrapper-sections]]])
