(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.valinnat-view
  (:require [re-frame.core :refer [subscribe]]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view :refer [loading-indicator not-found error-loading]]))

(defn- pisteet
  [pisteet]
  (doall
    (for [piste pisteet]
      ^{:key (:name piste)}
      [:div.grade
       [:span.grade__subject ]]
      )))

(defn- grades-loaded []
  (let [valinnat (subscribe [:application/application-valinnat])]
    (fn []
      [:<>
       (doall
         (for [hakukohde @valinnat]
           ^{:key (:oid hakukohde)}
           [:<>
             [:div.grade
              [:span.grade__subject (:name hakukohde)]]
             [:div.grade
              [:span.grade__subject "Sijoittelun tulos"]
              [:span.grade__value (:valintatila hakukohde)]]
             [:div.grade
              [:span.grade__subject "Vastaanottotieto"]
              [:span.grade__value (:vastaanottotila hakukohde)]]
             [:div.grade
              [:span.grade__subject "Ilmoittautumistila"]
              [:span.grade__value (:ilmoittautumistila hakukohde)]]
            [:hr]
            [pisteet (:pisteet hakukohde)]
            ]))])))

(defn valinnat []
  (let [valinnat-loading-state @(subscribe [:application/application-valinnat-loading-state])]
    [:div.grades
     [:div.grades__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :valinnat])]]
     [:div.grades__right-panel
      (case valinnat-loading-state
        :loading [loading-indicator]
        :loaded [grades-loaded]
        :error [error-loading :error-loading-pohjakoulutus]
        :not-found [not-found])]]))