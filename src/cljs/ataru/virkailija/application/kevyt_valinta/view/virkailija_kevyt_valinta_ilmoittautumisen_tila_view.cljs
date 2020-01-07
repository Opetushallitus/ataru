(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-ilmoittautumisen-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]))

(defn- kevyt-valinta-ilmoittautumisen-tila-selection []
  [common-view/kevyt-valinta-dropdown-selection :kevyt-valinta/ilmoittautumisen-tila])

(defn kevyt-valinta-ilmoittautumisen-tila-row []
  [common-view/kevyt-valinta-row
   :kevyt-valinta/ilmoittautumisen-tila
   [kevyt-valinta-ilmoittautumisen-tila-selection]])

