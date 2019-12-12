(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations
  (:require [ataru.application.review-states :as review-states]))

(defn kevyt-valinta-selection-label [kevyt-valinta-property
                                     kevyt-valinta-property-value
                                     lang]
  (let [valinta-tulos-service->translation-mapping (case kevyt-valinta-property
                                                     :kevyt-valinta/valinnan-tila review-states/valinnan-tila-selection-state
                                                     :kevyt-valinta/julkaisun-tila review-states/julkaisun-tila-selection-state
                                                     :kevyt-valinta/vastaanotto-tila review-states/vastaanotto-tila-selection-state
                                                     :kevyt-valinta/ilmoittautumisen-tila review-states/ilmoittautumisen-tila-selection-state)]
    (-> valinta-tulos-service->translation-mapping
        (get kevyt-valinta-property-value)
        lang)))

(defn kevyt-valinta-review-type-label [review-type lang]
  (get-in review-states/kevyt-valinta-hakukohde-review-types
          [review-type lang]
          (str review-type)))
