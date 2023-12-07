(ns ataru.virkailija.application.information-request.virkailija-information-request-view
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn application-send-single-email-to-applicant
  []
  (let [application        @(subscribe [:application/selected-application])
        first-name           (-> application :person :preferred-name)
        last-name           (-> application :person :last-name)
        visible?           (subscribe [:application/single-information-request-popup-visible?])
        checked?           (subscribe [:application/is-single-information-link-checkbox-set?])
        subject            (subscribe [:state-query [:application :single-information-request :subject]])
        message            (subscribe [:state-query [:application :single-information-request :message]])
        form-status        (subscribe [:application/single-information-request-form-status])
        enabled?           (subscribe [:application/single-email-sending-enabled?])
        button-enabled?    (subscribe [:application/single-information-request-button-enabled?])]
    (fn []
      [:span.application-handling__single-information-request-container
       [:a.application-handling__send-message-button.application-handling__button
        {:on-click (when enabled? #(dispatch [:application/set-single-information-request-popup-visibility true]))
         :class (when (not enabled?) "application-handling__button--disabled")}
        @(subscribe [:editor/virkailija-translation :send-email-to-applicant])]
       (when @visible?
         [:div.application-handling__popup.application-handling__-information-request-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            @(subscribe [:editor/virkailija-translation :single-information-request])]
            [:button.virkailija-close-button
             {:on-click #(dispatch [:application/set-single-information-request-popup-visibility false])}
             [:i.zmdi.zmdi-close]]]
            [:p @(subscribe [:editor/virkailija-translation :single-information-request-email-applicant (str last-name ", " first-name)])]
          [:div.application-handling__information-request-row
           [:div.application-handling__information-request-info-heading @(subscribe [:editor/virkailija-translation :single-information-request-subject])]
           [:div.application-handling__information-request-text-input-container
            [:input.application-handling__information-request-text-input
             {:value     @subject
              :class (when (> (count @subject) 120) "application-handling__information-request-text-input--invalid")
              :on-change #(dispatch [:application/set-single-information-request-subject (-> % .-target .-value)])}]
            (when (> (count @subject) 120) 
              [:div.application-handling__information-request-text-input--invalid 
               @(subscribe [:editor/virkailija-translation :single-information-request-vaidation-error-message])])]]
          [:div.application-handling__information-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value     @message
             :on-change #(dispatch [:application/set-single-information-request-message (-> % .-target .-value)])
             }]
           ]
          [:div.application-handling__information-request-row
           [:div.application-handling__information-request-row
            [:label
             [:input
              {:type      "checkbox"
               :checked   @checked?
               :on-change (fn [event] (let [checkedNewValue (boolean (-> event .-target .-checked))]
                                        (dispatch [:application/set-send-update-link checkedNewValue])))}]
             [:span @(subscribe [:editor/virkailija-translation :send-update-link])]]]]
          [:div.application-handling__information-request-row
           (case @form-status
             (:disabled :enabled nil)
             [:button.application-handling__send-information-request-button
              {:disabled (not @button-enabled?)
               :class    (if @button-enabled?
                           "application-handling__send-information-request-button--enabled"
                           "application-handling__send-information-request-button--disabled")
               :on-click #(dispatch [:application/submit-single-information-request])}
              @(subscribe [:editor/virkailija-translation :single-information-request-send])]

             :submitted
             [:div.application-handling__information-request-status
              [:i.zmdi.zmdi-hc-lg.zmdi-check-circle.application-handling__information-request-status-icon.application-handling__information-request-status-icon--sent]
              @(subscribe [:editor/virkailija-translation :single-information-request-message-sent])]
             )]])
       ]
      )))


