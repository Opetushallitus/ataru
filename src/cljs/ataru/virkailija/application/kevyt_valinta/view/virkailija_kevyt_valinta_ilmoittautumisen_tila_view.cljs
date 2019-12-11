(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-ilmoittautumisen-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.application.review-states :as review-states]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(def ^:private ilmoittautumisen-tilat
  ["EI_TEHTY"
   "LASNA_KOKO_LUKUVUOSI"
   "POISSA_KOKO_LUKUVUOSI"
   "EI_ILMOITTAUTUNUT"
   "LASNA_SYKSY"
   "POISSA_SYKSY"
   "LASNA"
   "POISSA"])

(defn- ilmoittautumisen-tila-label [ilmoittautumisen-tila lang]
  (-> review-states/ilmoittautumisen-tila-selection-state
      (get ilmoittautumisen-tila)
      lang))

(defn- kevyt-valinta-ilmoittautumisen-tila-selection [hakukohde-oid
                                                      application-key
                                                      ilmoittautumisen-tila-selection-state
                                                      ilmoittautumisen-tila
                                                      lang]
  (let [ilmoittautumisen-tilat-i18n (map (fn [ilmoittautumisen-tila]
                                           {:value ilmoittautumisen-tila
                                            :label (ilmoittautumisen-tila-label ilmoittautumisen-tila lang)})
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

