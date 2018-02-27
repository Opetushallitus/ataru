(ns ataru.virkailija.views.template-editor
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn email-template-editor
  []
  (when @(subscribe [:state-query [:editor :ui :template-editor-visible?]])
    [:div.application-handling__application-version-history-container
     [:div.application-handling__application-version-history
      [:a.application-handling__close-version-history
       {:on-click #(dispatch [:editor/toggle-email-template-editor])}
       "Sulje"]
      [:div]]]))