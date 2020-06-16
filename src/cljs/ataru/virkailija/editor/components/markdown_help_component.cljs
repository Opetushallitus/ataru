(ns ataru.virkailija.editor.components.markdown-help-component
  (:require [re-frame.core :refer [subscribe]]))

(defn markdown-help []
  [:div.editor-form__markdown-help
   [:div
    [:div.editor-form__markdown-help-arrow-left]
    [:div.editor-form__markdown-help-content
     [:span @(subscribe [:editor/virkailija-translation :md-help-title])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-bold])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-cursive])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-link])]
     [:br]
     [:a {:href          "https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet"
          :target        "_blank"
          :on-mouse-down (fn [evt]
                           (let [url (.getAttribute (-> evt .-target) "href")]
                             (.open js/window url "_blank")))}
      @(subscribe [:editor/virkailija-translation :md-help-more])]]]])

