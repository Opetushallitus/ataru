(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-vastaanotto-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]))

(defn- kevyt-valinta-vastaanotto-tila-selection []
  [common-view/kevyt-valinta-dropdown-selection :kevyt-valinta/vastaanotto-tila])

(defn kevyt-valinta-vastaanotto-tila-row []
  [common-view/kevyt-valinta-row
   :kevyt-valinta/vastaanotto-tila
   [kevyt-valinta-vastaanotto-tila-selection]])
