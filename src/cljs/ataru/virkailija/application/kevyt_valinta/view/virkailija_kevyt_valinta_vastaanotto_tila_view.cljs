(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-vastaanotto-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-vastaanotto-tila-selection [hakukohde-oid
                                                 application-key
                                                 vastaanotto-tila-selection-state
                                                 vastaanotto-tila
                                                 lang]
  (let [vastaanotto-tilat      @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                     :kevyt-valinta/vastaanotto-tila
                                                     application-key])
        vastaanotto-tilat-i18n (map (fn [vastaanotto-tila]
                                      {:value vastaanotto-tila
                                       :label (translations/kevyt-valinta-selection-label :kevyt-valinta/vastaanotto-tila
                                                                                          vastaanotto-tila
                                                                                          lang)})
                                    vastaanotto-tilat)
        vastaanotto-tila-i18n  (->> vastaanotto-tilat-i18n
                                    (filter (comp (partial = vastaanotto-tila)
                                                  :value))
                                    (map :label)
                                    (first))]
    [common-view/kevyt-valinta-dropdown-selection
     :kevyt-valinta/vastaanotto-tila
     vastaanotto-tila-selection-state
     vastaanotto-tila-i18n
     vastaanotto-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/vastaanotto-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-vastaanotto-tila-row [hakukohde-oid
                                          application-key
                                          lang]
  (let [vastaanotto-tila                 @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                               :kevyt-valinta/vastaanotto-tila
                                                               application-key])
        vastaanotto-tila-label           (translations/kevyt-valinta-review-type-label :kevyt-valinta/vastaanotto-tila lang)
        vastaanotto-tila-selection-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                               :kevyt-valinta/vastaanotto-tila
                                                               application-key])]
    [common-view/kevyt-valinta-row
     vastaanotto-tila-selection-state
     [common-view/kevyt-valinta-checkmark :kevyt-valinta/vastaanotto-tila application-key]
     vastaanotto-tila-label
     [kevyt-valinta-vastaanotto-tila-selection
      hakukohde-oid
      application-key
      vastaanotto-tila-selection-state
      vastaanotto-tila
      lang]]))
