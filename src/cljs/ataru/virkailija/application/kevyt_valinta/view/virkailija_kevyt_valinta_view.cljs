(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-valinnan-tila-view :as valinnan-tila]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-julkaisun-tila-view :as julkaisun-tila]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-vastaanotto-tila-view :as vastaanotto-tila]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-ilmoittautumisen-tila-view :as ilmoittautumisen-tila]))

(defn kevyt-valinta []
  [:div.application-handling__kevyt-valinta
   [valinnan-tila/kevyt-valinta-valinnan-tila-row]
   [julkaisun-tila/kevyt-valinta-julkaisun-tila-row]
   [vastaanotto-tila/kevyt-valinta-vastaanotto-tila-row]
   [ilmoittautumisen-tila/kevyt-valinta-ilmoittautumisen-tila-row]])
