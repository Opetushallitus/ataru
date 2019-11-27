(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-valinnan-tila-label []
  (let [application-key    @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        valinnan-tila      @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        lang               @(re-frame/subscribe [:editor/virkailija-lang])
        valinnan-tila-i18n (-> review-states/kevyt-valinta-selection-state
                               (get valinnan-tila)
                               lang)]
    [:span.application-handling__kevyt-valinta-value valinnan-tila-i18n]))

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
     [kevyt-valinta-valinnan-tila-label]]))

(defn kevyt-valinta []
  [:div.application-handling__kevyt-valinta
   [kevyt-valinta-selection-state-row]])
