(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [ataru.translations.texts :as translations]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-valinnan-tila-selection []
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

(defn- kevyt-valinta-julkaisun-tila-checkmark []
  [kevyt-valinta-checkmark :unchecked])

(defn- kevyt-valinta-julkaisun-tila-selection []
  (let [application-key     @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        julkaisun-tila      @(re-frame/subscribe [:virkailija-kevyt-valinta/julkaisun-tila application-key])
        lang                @(re-frame/subscribe [:editor/virkailija-lang])
        julkaisun-tila-i18n (-> translations/kevyt-valinta-julkaisun-tila-translations
                                julkaisun-tila
                                lang)]
    [:span.application-handling__kevyt-valinta-value julkaisun-tila-i18n]))

(defn- kevyt-valinta-row [checkmark-component
                          label
                          selection-component]
  [:div.application-handling__kevyt-valinta-row
   [checkmark-component]
   [:div.application-handling__kevyt-valinta-label
    [:span label]
    [:div.application-handling__kevyt-valinta-hr]]
   [selection-component]])

(defn- review-type-label [review-type lang]
  (->> review-states/hakukohde-review-types
       (transduce (comp (filter (fn [[kw]]
                                  (= kw review-type)))
                        (map (fn [[_ label-i18n]]
                               label-i18n))
                        (map lang))
                  conj)
       (first)))

(defn- kevyt-valinta-review-type-label [review-type lang]
  (get-in review-states/kevyt-valinta-hakukohde-review-types
          [review-type lang]
          (str review-type)))

(defn- kevyt-valinta-valinnan-tila-row []
  (let [lang                @(re-frame/subscribe [:editor/virkailija-lang])
        valinnan-tila-label (review-type-label :selection-state lang)]
    [:<>
     [kevyt-valinta-row
      kevyt-valinta-valinnan-tila-checkmark
      valinnan-tila-label
      kevyt-valinta-valinnan-tila-selection]]))

(defn- kevyt-valinta-julkaisun-tila-row []
  (let [lang                 @(re-frame/subscribe [:editor/virkailija-lang])
        julkaisun-tila-label (kevyt-valinta-review-type-label :kevyt-valinta/julkaisun-tila lang)]
    [kevyt-valinta-row
     kevyt-valinta-julkaisun-tila-checkmark
     julkaisun-tila-label
     kevyt-valinta-julkaisun-tila-selection]))

(defn kevyt-valinta []
  [:div.application-handling__kevyt-valinta
   [kevyt-valinta-valinnan-tila-row]
   [kevyt-valinta-julkaisun-tila-row]])
