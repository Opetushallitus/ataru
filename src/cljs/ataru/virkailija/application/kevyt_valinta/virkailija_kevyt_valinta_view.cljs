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

(def ^:private checked-valinnan-tilat
  #{"HYLATTY" "PERUUNTUNUT" "VARASIJALTA_HYVAKSYTTY" "HYVAKSYTTY" "PERUNUT" "PERUUTETTU"})

(defn- kevyt-valinta-checkmark [kind]
  (let [checkmark-class (case kind
                          :checked
                          "application-handling__kevyt-valinta-checkmark--checked"

                          :unchecked
                          "application-handling__kevyt-valinta-checkmark--unchecked"

                          :grayed-out
                          "application-handling__kevyt-valinta-checkmark--grayed-out")
        show-checkmark? (= kind :checked)]
    [:div.application-handling__kevyt-valinta-checkmark
     {:class checkmark-class}
     (when show-checkmark?
       [:i.zmdi.zmdi-check.application-handling__kevyt-valinta-checkmark--bold])]))

(defn- kevyt-valinta-valinnan-tila-checkmark []
  (let [application-key @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        valinnan-tila   @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        kind            (cond (checked-valinnan-tilat valinnan-tila)
                              :checked

                              (= "VARALLA" valinnan-tila)
                              :unchecked

                              :else
                              :grayed-out)]
    [kevyt-valinta-checkmark kind]))

(defn- kevyt-valinta-selection-state-row []
  (let [lang                  @(re-frame/subscribe [:editor/virkailija-lang])
        selection-state-label (->> review-states/hakukohde-review-types
                                   (filter (fn [[kw]]
                                             (= kw :selection-state)))
                                   (map (fn [[_ label-i18n]]
                                          label-i18n))
                                   (map lang))]
    [:div.application-handling__kevyt-valinta-row
     [kevyt-valinta-valinnan-tila-checkmark]
     [:div.application-handling__kevyt-valinta-label
      [:span selection-state-label]
      [:div.application-handling__kevyt-valinta-hr]]
     [kevyt-valinta-valinnan-tila-label]]))

(defn kevyt-valinta []
  [:div.application-handling__kevyt-valinta
   [kevyt-valinta-selection-state-row]])
