(ns ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-view
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn mass-information-request-link
  [_]
  (let [visible?           (subscribe [:application/mass-information-request-popup-visible?])
        subject            (subscribe [:state-query [:application :mass-information-request :subject]])
        message            (subscribe [:state-query [:application :mass-information-request :message]])
        form-status        (subscribe [:application/mass-information-request-form-status])
        applications-count (subscribe [:application/loaded-applications-count])
        button-enabled?    (subscribe [:application/mass-information-request-button-enabled?])]
    (fn [application-information-request-contains-modification-link]
      [:span.application-handling__mass-information-request-container
       [:a.application-handling__mass-information-request-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
        {:on-click #(dispatch [:application/set-mass-information-request-popup-visibility true])}
        @(subscribe [:editor/virkailija-translation :mass-information-request])]
       (when @visible?
         [:div.application-handling__popup.application-handling__mass-information-request-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            @(subscribe [:editor/virkailija-translation :mass-information-request])]
           [:button.virkailija-close-button
            {:on-click #(dispatch [:application/set-mass-information-request-popup-visibility false])}
            [:i.zmdi.zmdi-close]]]
          [:p @(subscribe [:editor/virkailija-translation :mass-information-request-email-n-recipients @applications-count])]
          [:div.application-handling__information-request-row
           [:div.application-handling__information-request-info-heading @(subscribe [:editor/virkailija-translation :mass-information-request-subject])]
           [:div.application-handling__information-request-text-input-container
            [:input.application-handling__information-request-text-input
             {:value     @subject
              :maxLength 200
              :on-change #(dispatch [:application/set-mass-information-request-subject (-> % .-target .-value)])}]]]
          [:div.application-handling__information-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value     @message
             :on-change #(dispatch [:application/set-mass-information-request-message (-> % .-target .-value)])}]]
          [application-information-request-contains-modification-link]
          [:div.application-handling__information-request-row
           (case @form-status
             (:disabled :enabled nil)
             [:button.application-handling__send-information-request-button
              {:disabled (not @button-enabled?)
               :class    (if @button-enabled?
                           "application-handling__send-information-request-button--enabled"
                           "application-handling__send-information-request-button--disabled")
               :on-click #(dispatch [:application/confirm-mass-information-request])}
              @(subscribe [:editor/virkailija-translation :mass-information-request-send])]

             :loading-applications
             [:button.application-handling__send-information-request-button.application-handling__send-information-request-button--disabled
              {:disabled true}
              [:span (str @(subscribe [:editor/virkailija-translation :mass-information-request-send]) " ")
               [:i.zmdi.zmdi-spinner.spin]]]

             :confirm
             [:button.application-handling__send-information-request-button.application-handling__send-information-request-button--confirm
              {:on-click #(dispatch [:application/submit-mass-information-request])}
              @(subscribe [:editor/virkailija-translation :mass-information-request-confirm-n-messages @applications-count])]

             :submitting
             [:div.application-handling__information-request-status
              [:i.zmdi.zmdi-hc-lg.zmdi-spinner.spin.application-handling__information-request-status-icon]
              @(subscribe [:editor/virkailija-translation :mass-information-request-sending-messages])]

             :submitted
             [:div.application-handling__information-request-status
              [:i.zmdi.zmdi-hc-lg.zmdi-check-circle.application-handling__information-request-status-icon.application-handling__information-request-status-icon--sent]
              @(subscribe [:editor/virkailija-translation :mass-information-request-messages-sent])])]])])))
