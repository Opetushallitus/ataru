(ns ataru.virkailija.application.information-request.virkailija-information-request-view
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn application-send-single-email-to-applicant
  []
  (let [application        @(subscribe [:application/selected-application])
        first-name           (-> application :person :preferred-name)
        last-name           (-> application :person :last-name)
        visible?           (subscribe [:application/single-information-request-popup-visible?])
        subject            (subscribe [:state-query [:application :single-information-request :subject]])
        message            (subscribe [:state-query [:application :single-information-request :message]])
        form-status        (subscribe [:application/single-information-request-form-status])]
    (fn []
      [:span.application-handling__single-information-request-container
       [:a.application-handling__send-message-button.application-handling__button
        {:on-click #(dispatch [:application/set-single-information-request-popup-visibility true])}
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
             {
              :value     @subject
              :maxLength 200
              :on-change #(dispatch [:application/set-single-information-request-subject (-> % .-target .-value)])
              }]]]
          [:div.application-handling__information-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {
             :value     @message
             :on-change #(dispatch [:application/set-single-information-request-message (-> % .-target .-value)])
             }]
           ]
          [:div.application-handling__information-request-row
           [:div.application-handling__information-request-row
            [:label
             [:input
              {:type      "checkbox"
               :on-change (fn [event] (let [checkedNewValue (boolean (-> event .-target .-checked))]
                                        (dispatch [:application/set-send-update-link checkedNewValue])
                                        ))
               }]
             [:span @(subscribe [:editor/virkailija-translation :single-applicant-email])]]
            ]]

          [:div.application-handling__information-request-row
           (case @form-status
             (:disabled :enabled nil)
             [:button.application-handling__send-information-request-button
              {:class    "application-handling__send-information-request-button--enabled"
               :on-click #(dispatch [:application/submit-single-information-request])}
              @(subscribe [:editor/virkailija-translation :single-information-request-send])])]])
       ]
      )))


