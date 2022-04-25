(ns ataru.virkailija.application.grades.grades-view
  (:require [re-frame.core :refer [subscribe]]))

(defn grades []
  (let [pohjakoulutus (subscribe [:application/pohjakoulutus-for-valinnat])
        lang (subscribe [:editor/virkailija-lang])]
    (fn []
      [:div.grades
       [:div.grades__left-panel
        [:h2 @(subscribe [:editor/virkailija-translation :grades-header])]]
       [:div.grades__right-panel
       (for [arvosana  (:arvosanat @pohjakoulutus)]
         [:div.grade
          [:span.grade__subject (@lang (:label arvosana))]
          [:span " "]
          [:span.grade__value (:value arvosana)]
          (when (:lang arvosana)
            [:span.grade__lang (@lang (:lang arvosana))])]
         )]])))