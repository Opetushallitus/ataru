(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-valinnan-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]))

(defn- kevyt-valinta-valinnan-tila-selection []
  [common-view/kevyt-valinta-dropdown-selection :kevyt-valinta/valinnan-tila])

(defn kevyt-valinta-valinnan-tila-row []
  [common-view/kevyt-valinta-row
   :kevyt-valinta/valinnan-tila
   [kevyt-valinta-valinnan-tila-selection]])
