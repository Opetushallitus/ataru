(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [ataru.translations.texts :as translations]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- kevyt-valinta-selection-dropdown [kevyt-valinta-dropdown-id
                                         kevyt-valinta-dropdown-value
                                         kevyt-valinta-dropdown-values
                                         kevyt-valinta-on-dropdown-value-change]
  (let [dropdown-open? @(re-frame/subscribe [:state-query [:application :kevyt-valinta kevyt-valinta-dropdown-id :open?]])]
    [:div.application-handling__kevyt-valinta-dropdown-container
     [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item
      {:on-click (fn toggle-kevyt-valinta-selection-dropdown []
                   (re-frame/dispatch [:virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown kevyt-valinta-dropdown-id]))}
      [:span kevyt-valinta-dropdown-value]
      [:i.zmdi.application-handling__kevyt-valinta-dropdown-chevron.zmdi-chevron-up
       {:class (when dropdown-open?
                 "application-handling__kevyt-valinta-dropdown-chevron-open")}]]
     (when dropdown-open?
       [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item-list.animated.fadeIn
        (map (fn [{value :value label :label}]
               ^{:key (str (name kevyt-valinta-dropdown-id) "-" value)}
               [:div.application-handling__kevyt-valinta-dropdown-item
                {:on-click (fn []
                             (kevyt-valinta-on-dropdown-value-change value))}
                [:span label]])
             kevyt-valinta-dropdown-values)])]))

(defn- kevyt-valinta-selection [kevyt-valinta-dropdown-id
                                kind
                                kevyt-valinta-dropdown-value
                                kevyt-valinta-dropdown-values
                                kevyt-valinta-on-dropdown-value-change]
  (if (= kind :checked)
    [:span.application-handling__kevyt-valinta-value kevyt-valinta-dropdown-value]
    [kevyt-valinta-selection-dropdown
     kevyt-valinta-dropdown-id
     kevyt-valinta-dropdown-value
     kevyt-valinta-dropdown-values
     kevyt-valinta-on-dropdown-value-change]))

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

(defn- kevyt-valinta-row [kind
                          checkmark-component
                          label
                          selection-component]
  (if (= kind :grayed-out)
    [:div.application-handling__kevyt-valinta-row
     checkmark-component
     [:div.application-handling__kevyt-valinta-label
      [:span label]]]
    [:div.application-handling__kevyt-valinta-row
     checkmark-component
     [:div.application-handling__kevyt-valinta-label
      [:span label]
      [:div.application-handling__kevyt-valinta-hr]]
     selection-component]))

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

(def ^:private checked-valinnan-tilat
  ["HYLATTY" "PERUUNTUNUT" "VARASIJALTA_HYVAKSYTTY" "HYVAKSYTTY" "PERUNUT" "PERUUTETTU"])

(defn- valinnan-tila-label [valinnan-tila lang]
  (-> review-states/kevyt-valinta-selection-state
      (get valinnan-tila)
      lang))

(defn- kevyt-valinta-valinnan-tila-checkmark [kind]
  [kevyt-valinta-checkmark kind])

(defn- on-valinnan-tila-change [new-valinnan-tila]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-valinnan-tila new-valinnan-tila]))

(defn- kevyt-valinta-valinnan-tila-selection [kind valinnan-tila lang]
  (let [valinnan-tila-i18n  (valinnan-tila-label valinnan-tila lang)
        valinnan-tilat      (conj checked-valinnan-tilat "VARALLA")
        valinnan-tilat-i18n (map (fn [valinnan-tila]
                                   {:value valinnan-tila
                                    :label (valinnan-tila-label valinnan-tila lang)})
                                 valinnan-tilat)]
    [kevyt-valinta-selection
     :kevyt-valinta/valinnan-tila
     kind
     valinnan-tila-i18n
     valinnan-tilat-i18n
     on-valinnan-tila-change]))

(defn- kevyt-valinta-valinnan-tila-row [valinnan-tila
                                        valinnan-tila-kind
                                        lang]
  (let [valinnan-tila-label (review-type-label :selection-state lang)]
    [:<>
     [kevyt-valinta-row
      valinnan-tila-kind
      [kevyt-valinta-valinnan-tila-checkmark valinnan-tila-kind]
      valinnan-tila-label
      [kevyt-valinta-valinnan-tila-selection valinnan-tila-kind valinnan-tila lang]]]))

(defn- kevyt-valinta-julkaisun-tila-checkmark [julkaisun-tila-kind]
  [kevyt-valinta-checkmark julkaisun-tila-kind])

(defn- on-julkaisun-tila-change [new-julkaisun-tila]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-julkaisun-tila new-julkaisun-tila]))

(defn- kevyt-valinta-julkaisun-tila-selection [julkaisun-tila
                                               julkaisun-tila-kind
                                               lang]
  (let [julkaisun-tila-i18n (-> translations/kevyt-valinta-julkaisun-tila-translations
                                julkaisun-tila
                                lang)]
    [kevyt-valinta-selection
     :kevyt-valinta/julkaisun-tila
     julkaisun-tila-kind
     julkaisun-tila-i18n
     [{:value julkaisun-tila :label julkaisun-tila-i18n}]
     on-julkaisun-tila-change]))

(defn- kevyt-valinta-julkaisun-tila-row [application-key
                                         valinnan-tila-kind
                                         lang]
  (let [julkaisun-tila       @(re-frame/subscribe [:virkailija-kevyt-valinta/julkaisun-tila application-key])
        julkaisun-tila-label (kevyt-valinta-review-type-label :kevyt-valinta/julkaisun-tila lang)
        julkaisun-tila-kind  (match [valinnan-tila-kind julkaisun-tila]
                                    [(_ :guard (partial not= :checked)) _]
                                    :grayed-out

                                    :else
                                    :unchecked)]
    [kevyt-valinta-row
     julkaisun-tila-kind
     [kevyt-valinta-julkaisun-tila-checkmark julkaisun-tila-kind]
     julkaisun-tila-label
     [kevyt-valinta-julkaisun-tila-selection julkaisun-tila julkaisun-tila-kind lang]]))

(defn kevyt-valinta []
  (let [application-key    @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        lang               @(re-frame/subscribe [:editor/virkailija-lang])
        valinnan-tila      @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        valinnan-tila-kind (cond (some (partial = valinnan-tila) checked-valinnan-tilat)
                                 :checked

                                 (= "VARALLA" valinnan-tila)
                                 :unchecked

                                 :else
                                 :grayed-out)]
    [:div.application-handling__kevyt-valinta
     [kevyt-valinta-valinnan-tila-row valinnan-tila valinnan-tila-kind lang]
     [kevyt-valinta-julkaisun-tila-row application-key valinnan-tila-kind lang]]))
