(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-julkaisun-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]))

(defn- kevyt-valinta-julkaisun-tila-selection []
  [common-view/kevyt-valinta-slider-toggle-selection :kevyt-valinta/julkaisun-tila])

(defn kevyt-valinta-julkaisun-tila-row []
  [common-view/kevyt-valinta-row
   :kevyt-valinta/julkaisun-tila
   [kevyt-valinta-julkaisun-tila-selection]])
