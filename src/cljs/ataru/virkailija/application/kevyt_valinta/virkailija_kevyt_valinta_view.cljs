(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [ataru.translations.texts :as translations]
            [re-frame.core :as re-frame]))

(defn- kevyt-valinta-selection [label]
  [:span.application-handling__kevyt-valinta-value label])

(defn- kevyt-valinta-valinnan-tila-selection [valinnan-tila lang]
  (let [valinnan-tila-i18n (-> review-states/kevyt-valinta-selection-state
                               (get valinnan-tila)
                               lang)]
    [kevyt-valinta-selection valinnan-tila-i18n]))

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

(defn- kevyt-valinta-valinnan-tila-checkmark [kind]
  [kevyt-valinta-checkmark kind])

(defn- kevyt-valinta-julkaisun-tila-checkmark [kind]
  [kevyt-valinta-checkmark kind])

                   :else
                   :grayed-out)]
    [kevyt-valinta-checkmark kind]))

(defn- kevyt-valinta-julkaisun-tila-checkmark []
  [kevyt-valinta-checkmark :unchecked])

(defn- kevyt-valinta-julkaisun-tila-selection [application-key lang]
  (let [julkaisun-tila      @(re-frame/subscribe [:virkailija-kevyt-valinta/julkaisun-tila application-key])
        julkaisun-tila-i18n (-> translations/kevyt-valinta-julkaisun-tila-translations
                                julkaisun-tila
                                lang)]
    [kevyt-valinta-selection julkaisun-tila-i18n]))

(defn- kevyt-valinta-row [checkmark-component
                          label
                          selection-component]
  [:div.application-handling__kevyt-valinta-row
   checkmark-component
   [:div.application-handling__kevyt-valinta-label
    [:span label]
    [:div.application-handling__kevyt-valinta-hr]]
   selection-component])

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

(defn- kevyt-valinta-valinnan-tila-row [application-key lang]
  (let [valinnan-tila       @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        valinnan-tila-label (review-type-label :selection-state lang)
        kind                (cond (checked-valinnan-tilat valinnan-tila)
                                  :checked

                                  (= "VARALLA" valinnan-tila)
                                  :unchecked

                                  :else
                                  :grayed-out)]
    [:<>
     [kevyt-valinta-row
      [kevyt-valinta-valinnan-tila-checkmark kind]
      valinnan-tila-label
      [kevyt-valinta-valinnan-tila-selection valinnan-tila lang]]]))

(defn- kevyt-valinta-julkaisun-tila-row [application-key lang]
  (let [julkaisun-tila-label (kevyt-valinta-review-type-label :kevyt-valinta/julkaisun-tila lang)
        kind                 :checked]
    [kevyt-valinta-row
     [kevyt-valinta-julkaisun-tila-checkmark kind]
     julkaisun-tila-label
     [kevyt-valinta-julkaisun-tila-selection application-key lang]]))

(defn kevyt-valinta []
  (let [application-key @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        lang            @(re-frame/subscribe [:editor/virkailija-lang])]
    [:div.application-handling__kevyt-valinta
     [kevyt-valinta-valinnan-tila-row application-key lang]
     [kevyt-valinta-julkaisun-tila-row application-key lang]]))
