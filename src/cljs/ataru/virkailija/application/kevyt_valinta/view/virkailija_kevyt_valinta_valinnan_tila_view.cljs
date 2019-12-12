(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-valinnan-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-valinnan-tila-selection [hakukohde-oid
                                              application-key
                                              valinnan-tila-selection-state
                                              valinnan-tila
                                              lang]
  (let [valinnan-tilat      @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                  :kevyt-valinta/valinnan-tila
                                                  application-key])
        valinnan-tilat-i18n (map (fn [valinnan-tila]
                                   {:value valinnan-tila
                                    :label (translations/kevyt-valinta-selection-label :kevyt-valinta/valinnan-tila
                                                                                       valinnan-tila
                                                                                       lang)})
                                 valinnan-tilat)
        valinnan-tila-i18n  (->> valinnan-tilat-i18n
                                 (filter (comp (partial = valinnan-tila)
                                               :value))
                                 (map :label)
                                 (first))]
    [common-view/kevyt-valinta-dropdown-selection
     :kevyt-valinta/valinnan-tila
     valinnan-tila-selection-state
     valinnan-tila-i18n
     valinnan-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/valinnan-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-valinnan-tila-row [hakukohde-oid
                                       application-key
                                       lang]
  (let [valinnan-tila-label           (translations/kevyt-valinta-review-type-label :kevyt-valinta/valinnan-tila lang)
        valinnan-tila                 @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                            :kevyt-valinta/valinnan-tila
                                                            application-key])
        valinnan-tila-selection-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                            :kevyt-valinta/valinnan-tila
                                                            application-key])]
    [common-view/kevyt-valinta-row
     valinnan-tila-selection-state
     [common-view/kevyt-valinta-checkmark :kevyt-valinta/valinnan-tila application-key]
     valinnan-tila-label
     [kevyt-valinta-valinnan-tila-selection
      hakukohde-oid
      application-key
      valinnan-tila-selection-state
      valinnan-tila
      lang]]))
