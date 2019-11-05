(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-selection-state-row []
  (let [lang                  @(re-frame/subscribe [:editor/virkailija-lang])
        selection-state-label (->> review-states/hakukohde-review-types
                                   (filter (fn [[kw]]
                                             (= kw :selection-state)))
                                   (map (fn [[_ label-i18n]]
                                          label-i18n))
                                   (map lang))]
    [:div.application-handling__kevyt-valinta-row
     [:div.application-handling__kevyt-valinta-check.application-handling__kevyt-valinta-check--checked
      [:i.zmdi.zmdi-check.application-handling__kevyt-valinta-check--bold]]
     [:div.application-handling__kevyt-valinta-label
      [:span selection-state-label]
      [:div.application-handling__kevyt-valinta-hr]]
     [:span.application-handling__kevyt-valinta-value "Hyväksytty"]]))

(defn kevyt-valinta []
  [:div.application-handling__kevyt-valinta
   [kevyt-valinta-selection-state-row]])
