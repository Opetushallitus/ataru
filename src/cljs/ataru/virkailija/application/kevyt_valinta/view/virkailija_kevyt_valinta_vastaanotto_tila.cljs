(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-vastaanotto-tila
  (:require [ataru.application.review-states :as review-states]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view :as common-view]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame]))

(def ^:private vastaanotto-tilat
  ["EI_VASTAANOTETTU_MAARA_AIKANA"
   "PERUNUT"
   "PERUUTETTU"
   "OTTANUT_VASTAAN_TOISEN_PAIKAN"
   "EHDOLLISESTI_VASTAANOTTANUT"
   "VASTAANOTTANUT_SITOVASTI"
   "KESKEN"])

(defn- vastaanotto-tila-label [vastaanotto-tila lang]
  (-> review-states/vastaanotto-tila-selection-state
      (get vastaanotto-tila)
      lang))

(defn- kevyt-valinta-vastaanotto-tila-selection [hakukohde-oid
                                                 application-key
                                                 vastaanotto-tila-dropdown-state
                                                 vastaanotto-tila
                                                 lang]
  (let [vastaanotto-tilat-i18n (map (fn [vastaanotto-tila]
                                      {:value vastaanotto-tila
                                       :label (vastaanotto-tila-label vastaanotto-tila lang)})
                                    vastaanotto-tilat)
        vastaanotto-tila-i18n  (->> vastaanotto-tilat-i18n
                                    (filter (comp (partial = vastaanotto-tila)
                                                  :value))
                                    (map :label))]
    [common-view/kevyt-valinta-selection
     :kevyt-valinta/vastaanotto-tila
     vastaanotto-tila-dropdown-state
     vastaanotto-tila-i18n
     vastaanotto-tilat-i18n
     (partial common-view/on-kevyt-valinta-property-change
              :kevyt-valinta/vastaanotto-tila
              hakukohde-oid
              application-key)]))

(defn kevyt-valinta-vastaanotto-tila-row [hakukohde-oid
                                          application-key
                                          lang]
  (let [vastaanotto-tila                @(re-frame/subscribe [:virkailija-kevyt-valinta/vastaanotto-tila application-key])
        vastaanotto-tila-label          (translations/kevyt-valinta-review-type-label :kevyt-valinta/vastaanotto-tila lang)
        vastaanotto-tila-dropdown-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-dropdown-state
                                                              :kevyt-valinta/vastaanotto-tila
                                                              application-key])]
    [common-view/kevyt-valinta-row
     vastaanotto-tila-dropdown-state
     [common-view/kevyt-valinta-checkmark :kevyt-valinta/vastaanotto-tila application-key]
     vastaanotto-tila-label
     [kevyt-valinta-vastaanotto-tila-selection
      hakukohde-oid
      application-key
      vastaanotto-tila-dropdown-state
      vastaanotto-tila
      lang]]))
