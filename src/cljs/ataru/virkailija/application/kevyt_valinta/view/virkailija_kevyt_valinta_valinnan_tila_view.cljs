(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-valinnan-tila-view
  (:require [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(def ^:private valinnan-tilat
  ["HYLATTY" "PERUUNTUNUT" "VARASIJALTA_HYVAKSYTTY" "HYVAKSYTTY" "PERUNUT" "PERUUTETTU" "VARALLA" "KESKEN"])

(defn- kevyt-valinta-valinnan-tila-selection [hakukohde-oid
                                              application-key
                                              valinnan-tila-dropdown-state
                                              valinnan-tila
                                              lang]
  (let [valinnan-tilat-i18n (map (fn [valinnan-tila]
                                   {:value valinnan-tila
                                    :label (translations/valinnan-tila-label valinnan-tila lang)})
                                 valinnan-tilat)
        valinnan-tila-i18n  (->> valinnan-tilat-i18n
                                 (filter (comp (partial = valinnan-tila)
                                               :value))
                                 (map :label))]
    [common-view/kevyt-valinta-selection
     :kevyt-valinta/valinnan-tila
     valinnan-tila-dropdown-state
     valinnan-tila-i18n
     valinnan-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/valinnan-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-valinnan-tila-row [hakukohde-oid
                                       application-key
                                       lang]
  (let [valinnan-tila-label          (translations/review-type-label :selection-state lang)
        valinnan-tila                @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        valinnan-tila-dropdown-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-dropdown-state
                                                           :kevyt-valinta/valinnan-tila
                                                           application-key])]
    [:<>
     [common-view/kevyt-valinta-row
      valinnan-tila-dropdown-state
      [common-view/kevyt-valinta-checkmark :kevyt-valinta/valinnan-tila application-key]
      valinnan-tila-label
      [kevyt-valinta-valinnan-tila-selection
       hakukohde-oid
       application-key
       valinnan-tila-dropdown-state
       valinnan-tila
       lang]]]))
