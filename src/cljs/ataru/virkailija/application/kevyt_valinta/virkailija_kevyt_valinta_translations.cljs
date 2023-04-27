(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations
  (:require [ataru.application.review-states :as review-states]))

(defn kevyt-valinta-value-translation-key
  ([kevyt-valinta-property kevyt-valinta-property-value] (kevyt-valinta-value-translation-key kevyt-valinta-property kevyt-valinta-property-value true))
  ([kevyt-valinta-property kevyt-valinta-property-value kk-haku?]
  (let [i18n-mapping (case kevyt-valinta-property
                       :kevyt-valinta/valinnan-tila review-states/valinnan-tila-translation-key-mapping
                       :kevyt-valinta/julkaisun-tila review-states/julkaisun-tila-translation-key-mapping
                       :kevyt-valinta/vastaanotto-tila (review-states/get-vastaanotto-tila-translation-key-mapping kk-haku?)
                       :kevyt-valinta/ilmoittautumisen-tila review-states/ilmoittautumisen-tila-translation-key-mapping)]
    (get i18n-mapping kevyt-valinta-property-value))))

(defn kevyt-valinta-label-translation-key [kevyt-valinta-property]
  (case kevyt-valinta-property
    :kevyt-valinta/valinnan-tila :valinta
    :kevyt-valinta/julkaisun-tila :julkaisu
    :kevyt-valinta/vastaanotto-tila :vastaanotto
    :kevyt-valinta/ilmoittautumisen-tila :ilmoittautuminen))
