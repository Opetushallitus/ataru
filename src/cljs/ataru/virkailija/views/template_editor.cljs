(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn email-template-editor
  []
  (let [visible? @(subscribe [:state-query [:editor :ui :template-editor-visible?]])
        {:keys [body subject content from]} @(subscribe [:state-query [:editor :email-template]])]
    (when visible?
      [:div.virkailija-modal__container
       [:div.virkailija-modal__content
        [:a.virkailija-modal__close-link
         {:on-click #(dispatch [:editor/toggle-email-template-editor])}
         "Sulje"]
        [:div.virkailija-modal__panels
         [:div.virkailija-modal__editor-panel
          [:h3 "Sisältö"]
          [:textarea.virkailija-modal__editor
           {:value     content
            :on-change #(dispatch [:editor/update-email-template (.-value (.-target %))])}]]
         [:div.virkailija-modal__preview-panel
          [:h3 "Esikatselu"]
          [:div "Lähettäjä:" [:span from]]
          [:div "Otsikko:" [:span subject]]
          [:iframe.virkailija-modal__preview
           {:srcDoc body}]
          [:div.virkailija-modal__buttons-container
           [:button.virkailija-modal__button
            {:on-click (fn [_]
                         (dispatch [:editor/toggle-email-template-editor])
                         (dispatch [:editor/update-email-template nil]))}
            "Peruuta"]
           [:button.virkailija-modal__button
            {:on-click #(dispatch [:editor/save-email-template])}
            "Talleta"]]]]]])))