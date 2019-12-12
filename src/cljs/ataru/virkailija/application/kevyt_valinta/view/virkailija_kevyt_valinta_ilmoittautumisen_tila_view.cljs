(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-ilmoittautumisen-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-ilmoittautumisen-tila-selection [hakukohde-oid
                                                      application-key
                                                      ilmoittautumisen-tila-selection-state
                                                      ilmoittautumisen-tila
                                                      lang]
  (let [ilmoittautumisen-tilat      @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                          :kevyt-valinta/ilmoittautumisen-tila
                                                          application-key])
        ilmoittautumisen-tilat-i18n (map (fn [ilmoittautumisen-tila]
                                           {:value ilmoittautumisen-tila
                                            :label (translations/kevyt-valinta-selection-label :kevyt-valinta/ilmoittautumisen-tila
                                                                                               ilmoittautumisen-tila
                                                                                               lang)})
                                         ilmoittautumisen-tilat)
        ilmoittautumisen-tila-i18n  (->> ilmoittautumisen-tilat-i18n
                                         (filter (comp (partial = ilmoittautumisen-tila)
                                                       :value))
                                         (map :label)
                                         (first))]
    [common-view/kevyt-valinta-dropdown-selection
     :kevyt-valinta/ilmoittautumisen-tila
     ilmoittautumisen-tila-selection-state
     ilmoittautumisen-tila-i18n
     ilmoittautumisen-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/ilmoittautumisen-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-ilmoittautumisen-tila-row [hakukohde-oid
                                               application-key
                                               lang]
  (let [ilmoittautumisen-tila                 @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                                    :kevyt-valinta/ilmoittautumisen-tila
                                                                    application-key])
        ilmoittautumisen-tila-label           (translations/kevyt-valinta-review-type-label :kevyt-valinta/ilmoittautumisen-tila lang)
        ilmoittautumisen-tila-selection-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                                    :kevyt-valinta/ilmoittautumisen-tila
                                                                    application-key])]
    [common-view/kevyt-valinta-row
     ilmoittautumisen-tila-selection-state
     [common-view/kevyt-valinta-checkmark :kevyt-valinta/ilmoittautumisen-tila application-key]
     ilmoittautumisen-tila-label
     [kevyt-valinta-ilmoittautumisen-tila-selection
      hakukohde-oid
      application-key
      ilmoittautumisen-tila-selection-state
      ilmoittautumisen-tila
      lang]]))

