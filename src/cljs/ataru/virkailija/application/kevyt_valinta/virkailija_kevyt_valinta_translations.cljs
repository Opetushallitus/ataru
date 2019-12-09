(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations
  (:require [ataru.application.review-states :as review-states]))

(defn review-type-label [review-type lang]
  (->> review-states/hakukohde-review-types
       (transduce (comp (filter (fn [[kw]]
                                  (= kw review-type)))
                        (map (fn [[_ label-i18n]]
                               label-i18n))
                        (map lang))
                  conj)
       (first)))

(defn valinnan-tila-label [valinnan-tila lang]
  (-> review-states/kevyt-valinta-selection-state
      (get valinnan-tila)
      lang))

(defn kevyt-valinta-review-type-label [review-type lang]
  (get-in review-states/kevyt-valinta-hakukohde-review-types
          [review-type lang]
          (str review-type)))
