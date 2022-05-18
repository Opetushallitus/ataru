(ns ataru.virkailija.application.pohjakoulutus-toinen-aste.valinnat-view
  (:require [re-frame.core :refer [subscribe]]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-view :refer [loading-indicator not-found error-loading]]
            [ataru.cljs-util :refer [to-finnish-number]]))

(defn- pisteet [lang hakukohde-oid nimet? pisteet]
  [:<>
    [:div.tulos
     [:p.tulos__sub-header (if nimet?
                                @(subscribe [:editor/virkailija-translation :scores])
                                "")]]
    (for [piste pisteet]
      ^{:key (str hakukohde-oid "-" (:tunniste piste))}
      [:div.tulos
        (if nimet?
          [:span.tulos__subject (lang (:nimi piste))]
          (if (:localize-arvo piste)
            [:span.tulos__value @(subscribe [:editor/virkailija-translation (keyword (:arvo piste))])]
            [:span.tulos__value (to-finnish-number (:arvo piste))]))])])

(defn- valinnat-loaded []
  (let [valinnat (subscribe [:application/application-valinnat])
        lang (subscribe [:editor/virkailija-lang])]
    (fn []
      [:<>
       (doall
         (map-indexed (fn [idx hakukohde]
           ^{:key (:oid hakukohde)}
           [:div.valinnat__tulokset-container
             [:div.valinnat__hakukohde
              [:span (str (+ idx 1) ". " (:name hakukohde))]]
            [:div.valinnat__tulokset__nimet
              (when (not (nil? (:kokonaispisteet hakukohde)))
                [:div.tulos
                 [:span.tulos__subject @(subscribe [:editor/virkailija-translation :valinnan-kokonaispisteet])]])
               [:div.tulos
                [:span.tulos__subject @(subscribe [:editor/virkailija-translation :sijoittelun-tulos])]]
               [:div.tulos
                [:span.tulos__subject @(subscribe [:editor/virkailija-translation :vastaanottotieto])]]
               [:div.tulos
                [:span.tulos__subject @(subscribe [:editor/virkailija-translation :ilmoittautumistila])]]
              (when (> (count (:pisteet hakukohde)) 0)
                [pisteet @lang (:oid hakukohde) true (:pisteet hakukohde)])]
            [:div.valinnat__tulokset__arvot
              (when (not (nil? (:kokonaispisteet hakukohde)))
                [:div.tulos
                  [:span.tulos__value (:kokonaispisteet hakukohde)]])
              [:div.tulos
                [:span.tulos__value @(subscribe [:editor/virkailija-translation (keyword (:valintatila hakukohde))])]]
              [:div.tulos
                [:span.tulos__value @(subscribe [:editor/virkailija-translation (keyword (:vastaanottotila hakukohde))])]]
              [:div.tulos
                [:span.tulos__value @(subscribe [:editor/virkailija-translation (keyword (:ilmoittautumistila hakukohde))])]]
              (when (> (count (:pisteet hakukohde)) 0)
                [pisteet @lang (:oid hakukohde) false (:pisteet hakukohde)])]
            [:div.valinnat__divider]]) @valinnat))])))

(defn valinnat []
  (let [valinnat-loading-state @(subscribe [:application/application-valinnat-loading-state])]
    [:div.valinnat
     [:div.valinnat__left-panel
      [:h2 @(subscribe [:editor/virkailija-translation :valinnat])]]
     [:div.valinnat__right-panel
      (case valinnat-loading-state
        :loading [loading-indicator]
        :loaded [valinnat-loaded]
        :error [error-loading :error-loading-valinnat]
        :not-found [not-found])]]))