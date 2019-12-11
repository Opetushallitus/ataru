(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-julkaisun-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [ataru.translations.texts :as texts]
            [re-frame.core :as re-frame]))

(def ^:private julkaisun-tilat
  [true false])

(defn- kevyt-valinta-julkaisun-tila-selection [hakukohde-oid
                                               application-key
                                               julkaisun-tila
                                               julkaisun-tila-selection-state
                                               lang]
  (let [julkaisun-tilat-i18n (map (fn [julkaisun-tila]
                                    (let [translation-key (case julkaisun-tila
                                                            true :kevyt-valinta/julkaistu-hakijalle
                                                            false :kevyt-valinta/ei-julkaistu)]
                                      {:value julkaisun-tila
                                       :label (-> texts/kevyt-valinta-julkaisun-tila-translations
                                                  translation-key
                                                  lang)}))
                                  julkaisun-tilat)
        julkaisun-tila-i18n  (->> julkaisun-tilat-i18n
                                  (filter (comp (partial = julkaisun-tila)
                                                :value))
                                  (map :label)
                                  (first))]
    [common-view/kevyt-valinta-checkbox-selection
     :kevyt-valinta/julkaisun-tila
     julkaisun-tila-selection-state
     julkaisun-tila-i18n
     julkaisun-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/julkaisun-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-julkaisun-tila-row [hakukohde-oid
                                        application-key
                                        lang]
  (let [julkaisun-tila                 @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                             :kevyt-valinta/julkaisun-tila
                                                             application-key])
        julkaisun-tila-label           (translations/kevyt-valinta-review-type-label :kevyt-valinta/julkaisun-tila lang)
        julkaisun-tila-selection-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                             :kevyt-valinta/julkaisun-tila
                                                             application-key])]
    [common-view/kevyt-valinta-row
     julkaisun-tila-selection-state
     [common-view/kevyt-valinta-checkmark :kevyt-valinta/julkaisun-tila application-key]
     julkaisun-tila-label
     [kevyt-valinta-julkaisun-tila-selection
      hakukohde-oid
      application-key
      julkaisun-tila
      julkaisun-tila-selection-state
      lang]]))
