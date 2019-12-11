(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-valinnan-tila-view :as valinnan-tila]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-julkaisun-tila-view :as julkaisun-tila]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-vastaanotto-tila-view :as vastaanotto-tila]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn kevyt-valinta []
  (let [application-key @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid   (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]]))
        lang            @(re-frame/subscribe [:editor/virkailija-lang])]
    [:div.application-handling__kevyt-valinta
     [valinnan-tila/kevyt-valinta-valinnan-tila-row
      hakukohde-oid
      application-key
      lang]
     [julkaisun-tila/kevyt-valinta-julkaisun-tila-row
      hakukohde-oid
      application-key
      lang]
     [vastaanotto-tila/kevyt-valinta-vastaanotto-tila-row
      hakukohde-oid
      application-key
      lang]]))
