(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn email-template-editor
  []
  (when @(subscribe [:state-query [:editor :ui :template-editor-visible?]])
    [:div.virkailija-modal__container
     [:div.virkailija-modal__content
      [:a.virkailija-modal__close-link
       {:on-click #(dispatch [:editor/toggle-email-template-editor])}
       "Sulje"]
      [:div.virkailija-modal__panels
       [:div.virkailija-modal__editor-panel
        [:h3 "Sisältö"]
        [:textarea.virkailija-modal__editor
         {:value     @(subscribe [:state-query [:editor :email-template :content]])
          :on-change #(dispatch [:editor/update-email-template (.-value (.-target %))])}]]
       [:div.virkailija-modal__preview-panel
        [:h3 "Esikatselu"]
        [:div.virkailija-modal__preview
         {:dangerouslySetInnerHTML {:__html @(subscribe [:editor/email-template-preview-html])}}]]]]]))