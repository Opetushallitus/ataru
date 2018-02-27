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
      [:div]]]))