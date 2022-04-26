(ns ataru.virkailija.application.grades.grades-view
  (:require [re-frame.core :refer [subscribe]]))

(defn grades []
  (let [grades (subscribe [:application/grades])
        lang (subscribe [:editor/virkailija-lang])]
    (fn []
      [:div.grades
       [:div.grades__left-panel
        [:h2 @(subscribe [:editor/virkailija-translation :grades-header])]]
       [:div.grades__right-panel
       (for [grade @grades]
         [:div.grade
          [:span.grade__subject (@lang (:label grade))]
          [:span.grade__value (:value grade)
          (for [valinnainen (:valinnaiset grade)]
            [:span.grade__value--valinnainen (str "(" valinnainen ")")])
           ]
          (when (:lang grade)
            [:span.grade__lang (@lang (:lang grade))])]
         )]])))