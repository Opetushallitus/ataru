(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.grades-view
  (:require [re-frame.core :refer [subscribe]]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view :refer [loading-indicator not-found error-loading]]))

(defn- grades-loaded []
  (let [grades (subscribe [:application/grades])
        lang (subscribe [:editor/virkailija-lang])]
    (fn []
      [:<>
       (doall
         (for [grade @grades]
            ^{:key (:key grade)}
            [:div.grade
              [:span.grade__subject (@lang (:label grade))]
              [:span.grade__value (:value grade)
              (map-indexed (fn [idx valinnainen]
                ^{:key (str (:key grade) "_valinnainen_" idx)}
                [:span.grade__value--valinnainen
                (str "(" valinnainen ")")]) (:valinnaiset grade))
              ]
              (when (:lang grade)
              [:span.grade__lang (@lang (:lang grade))])]))])))

(defn grades []
  (let [pohjakoulutus-loading-state @(subscribe [:application/pohjakoulutus-for-valinnat-loading-state])]
    [:div.grades
     [:div.grades__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :grades-header])]]
     [:div.grades__right-panel
      (case pohjakoulutus-loading-state
        :loading [loading-indicator]
        :loaded [grades-loaded]
        :error [error-loading :error-loading-pohjakoulutus]
        :not-found [not-found])]]))