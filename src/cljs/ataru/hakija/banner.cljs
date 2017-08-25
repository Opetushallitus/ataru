(ns ataru.hakija.banner
  (:require [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.ratom :refer [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]))

(def logo-image
  [:div.logo])

(def logo-text
  [:span.logo-text "Opintopolku.fi"])

(def logo [:div.logo-elements logo-image logo-text])

(defn- form-field-type [form-fields key]
  (->> form-fields
       (filter (comp (partial = key) keyword :id))
       (map :fieldType)
       (first)))

(defn invalid-field-status []
  (let [show-details        (r/atom false)
        toggle-show-details #(do (reset! show-details (not @show-details)) nil)
        lang                (subscribe [:application/form-language])
        default-lang        (subscribe [:application/default-language])
        form-fields         (reaction (util/flatten-form-fields @(subscribe [:state-query [:form :content]])))]
    (fn [valid-status]
      (when (seq (:invalid-fields valid-status))
        [:div.application__invalid-field-status
         [:span.application__invalid-field-status-title
          {:on-click toggle-show-details}
          (case @lang
            :fi "Tarkista "
            :en "Check "
            :sv "Kontrollera ")
          [:b (count (:invalid-fields valid-status))]
          (case @lang
            :fi " tietoa"
            :en " answers"
            :sv " uppgifter")]
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
             [:submitting _] [:div.application__sent-indicator (case @lang
                                                                 :fi "Hakemusta lähetetään"
                                                                 :sv "Ansökan skickas"
                                                                 :en "The application is being sent")]
             [:submitted (_ :guard #(nil? %))]
             [:div.application__sent-indicator.animated.fadeIn (case @lang
                                                                 :fi "Saat vahvistuksen sähköpostiisi"
                                                                 :sv "Du får en bekräftelse till din e-post"
                                                                 :en "Confirmation email will be sent to the email address you've provided")]
             :else nil))))

(defn- edit-text [hakija-secret
                  virkailija-secret
                  hakija-edit-text
                  virkailija-edit-text
                  hakija-new-text]
  (cond (some? hakija-secret)
        hakija-edit-text

        (some? virkailija-secret)
        virkailija-edit-text

        :else
        hakija-new-text))

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
                         [:span.application__sent-placeholder-text (case @lang
                                                                     :fi (if @virkailija-secret "Muutokset tallennettu" "Hakemus lähetetty")
                                                                     :sv (if @virkailija-secret "Ändringarna har sparats" "Ansökan har skickats")
                                                                     :en (if @virkailija-secret "The modifications have been saved" "The application has been sent"))]]
             :else [:button.application__send-application-button
                    {:disabled (or (not (:valid valid-status))
                                   (contains? #{:submitting :submitted} submit-status)
                                   (and @editing (empty? @values-changed?)))
                     :on-click #(if @editing
                                  (dispatch [:application/edit])
                                  (dispatch [:application/submit]))}
                    (case @lang
                      :fi (edit-text @secret @virkailija-secret "LÄHETÄ MUUTOKSET" "TALLENNA MUUTOKSET" "LÄHETÄ HAKEMUS")
                      :sv (edit-text @secret @virkailija-secret "SCICKA FÖRÄNDRINGAR" "SPARA FÖRÄNDRINGAR" "SKICKA ANSÖKAN")
                      :en (edit-text @secret @virkailija-secret "SEND MODIFICATIONS" "SAVE MODIFICATIONS" "SEND APPLICATION"))]))))

(defn status-controls []
  (let [valid-status         (subscribe [:application/valid-status])
        submit-status        (subscribe [:state-query [:application :submit-status]])
        can-apply?           (subscribe [:application/can-apply?])]
    (when @can-apply?
      [:div.application__status-controls
       [send-button-or-placeholder @valid-status @submit-status]
       [invalid-field-status @valid-status]
       [sent-indicator @submit-status]])))

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
