(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.valinnat-view
  (:require [re-frame.core :refer [subscribe]]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view :refer [loading-indicator not-found error-loading]]))

(defn- pisteet [lang hakukohde-oid pisteet]
  [:<>
    [:div.grade @(subscribe [:editor/virkailija-translation :scores])]
      (for [piste pisteet]
        ^{:key (str hakukohde-oid "-" (:tunniste piste))}
        [:div.grade
          [:span.grade__subject (lang (:nimi piste))]
          (if (:localize-arvo piste)
            [:span.grade__value @(subscribe [:editor/virkailija-translation (keyword (:arvo piste))])]
            [:span.grade__value (:arvo piste)])])])

(defn- valinnat-loaded []
  (let [valinnat (subscribe [:application/application-valinnat])
        lang (subscribe [:editor/virkailija-lang])]
    (fn []
      [:<>
       (doall
         (map-indexed (fn [idx hakukohde]
           ^{:key (:oid hakukohde)}
           [:<>
             [:div.grade
              [:span.grade__subject (str (+ idx 1) ". " (:name hakukohde))]]
            (when (not (nil? (:kokonaispisteet hakukohde)))
              [:div.grade
               [:span.grade__subject "Valinnan kokonaispisteet"]
               [:span.grade__value (:kokonaispisteet hakukohde)]])
             [:div.grade
              [:span.grade__subject "Sijoittelun tulos"]
              [:span.grade__value @(subscribe [:editor/virkailija-translation (keyword (:valintatila hakukohde))])]]
             [:div.grade
              [:span.grade__subject "Vastaanottotieto"]
              [:span.grade__value @(subscribe [:editor/virkailija-translation (keyword (:vastaanottotila hakukohde))])]]
             [:div.grade
              [:span.grade__subject "Ilmoittautumistila"]
              [:span.grade__value @(subscribe [:editor/virkailija-translation (keyword (:ilmoittautumistila hakukohde))])]]
            (when (> (count (:pisteet hakukohde)) 0)
              [pisteet @lang (:oid hakukohde) (:pisteet hakukohde)])
            [:hr]]) @valinnat))])))

(defn valinnat []
  (let [valinnat-loading-state @(subscribe [:application/application-valinnat-loading-state])]
    [:div.grades
     [:div.grades__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :valinnat])]]
     [:div.grades__right-panel
      (case valinnat-loading-state
        :loading [loading-indicator]
        :loaded [valinnat-loaded]
        :error [error-loading :error-loading-valinnat]
        :not-found [not-found])]]))