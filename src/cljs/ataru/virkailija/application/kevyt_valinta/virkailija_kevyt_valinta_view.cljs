(ns ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-view
  (:require [ataru.application.review-states :as review-states]
            [ataru.application-common.loaders.ellipsis-loader :as el]
            [ataru.translations.texts :as translations]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- kevyt-valinta-selection-dropdown [kevyt-valinta-dropdown-id
                                         kevyt-valinta-dropdown-value
                                         kevyt-valinta-dropdown-values
                                         kevyt-valinta-on-dropdown-value-change
                                         ongoing-request?]
  (let [dropdown-open? @(re-frame/subscribe [:state-query [:application :kevyt-valinta kevyt-valinta-dropdown-id :open?]])]
    [:div.application-handling__kevyt-valinta-dropdown-container
     [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item
      (if ongoing-request?
        {:class "application-handling__kevyt-valinta-dropdown--disabled"}
        {:on-click (fn toggle-kevyt-valinta-selection-dropdown []
                     (re-frame/dispatch [:virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown kevyt-valinta-dropdown-id]))})
      [:span.application-handling__kevyt-valinta-dropdown-value kevyt-valinta-dropdown-value]
      (when ongoing-request?
        [el/ellipsis-loader])
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
                                kevyt-valinta-property-state
                                kevyt-valinta-dropdown-value
                                kevyt-valinta-dropdown-values
                                kevyt-valinta-on-dropdown-value-change]
  (let [ongoing-request? @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request?])]
    (if (and (= kevyt-valinta-property-state :checked)
             (not ongoing-request?))
      [:span.application-handling__kevyt-valinta-value kevyt-valinta-dropdown-value]
      [kevyt-valinta-selection-dropdown
       kevyt-valinta-dropdown-id
       kevyt-valinta-dropdown-value
       kevyt-valinta-dropdown-values
       kevyt-valinta-on-dropdown-value-change
       ongoing-request?])))

(defn- kevyt-valinta-checkmark [kevyt-valinta-property-state]
  (let [checkmark-class (case kevyt-valinta-property-state
                          :checked
                          "application-handling__kevyt-valinta-checkmark--checked"

                          :unchecked
                          "application-handling__kevyt-valinta-checkmark--unchecked"

                          :grayed-out
                          "application-handling__kevyt-valinta-checkmark--grayed-out")
        show-checkmark? (= kevyt-valinta-property-state :checked)]
    [:div.application-handling__kevyt-valinta-checkmark
     {:class checkmark-class}
     (when show-checkmark?
       [:i.zmdi.zmdi-check.application-handling__kevyt-valinta-checkmark--bold])]))

(defn- kevyt-valinta-row [kevyt-valinta-property-state
                          checkmark-component
                          label
                          selection-component]
  (if (= kevyt-valinta-property-state :grayed-out)
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

(defn- on-valinnan-tila-change [hakukohde-oid
                                application-key
                                new-valinnan-tila]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-valinnan-tila
                      hakukohde-oid
                      application-key
                      new-valinnan-tila]))

(defn- kevyt-valinta-valinnan-tila-selection [hakukohde-oid
                                              application-key
                                              valinnan-tila-property-state
                                              valinnan-tila
                                              lang]
  (let [valinnan-tila-i18n  (valinnan-tila-label valinnan-tila lang)
        valinnan-tilat      (conj checked-valinnan-tilat "VARALLA")
        valinnan-tilat-i18n (map (fn [valinnan-tila]
                                   {:value valinnan-tila
                                    :label (valinnan-tila-label valinnan-tila lang)})
                                 valinnan-tilat)]
    [kevyt-valinta-selection
     :kevyt-valinta/valinnan-tila
     valinnan-tila-property-state
     valinnan-tila-i18n
     valinnan-tilat-i18n
     (partial on-valinnan-tila-change hakukohde-oid application-key)]))

(defn- kevyt-valinta-valinnan-tila-row [hakukohde-oid
                                        application-key
                                        lang]
  (let [valinnan-tila-label          (review-type-label :selection-state lang)
        valinnan-tila                @(re-frame/subscribe [:virkailija-kevyt-valinta/valinnan-tila application-key])
        valinnan-tila-property-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                           :kevyt-valinta/valinnan-tila
                                                           application-key])]
    [:<>
     [kevyt-valinta-row
      valinnan-tila-property-state
      [kevyt-valinta-checkmark valinnan-tila-property-state]
      valinnan-tila-label
      [kevyt-valinta-valinnan-tila-selection
       hakukohde-oid
       application-key
       valinnan-tila-property-state
       valinnan-tila
       lang]]]))

(defn- on-julkaisun-tila-change [new-julkaisun-tila]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-julkaisun-tila new-julkaisun-tila]))

(defn- kevyt-valinta-julkaisun-tila-selection [julkaisun-tila
                                               julkaisun-tila-property-state
                                               lang]
  (let [julkaisun-tila-i18n (-> translations/kevyt-valinta-julkaisun-tila-translations
                                julkaisun-tila
                                lang)]
    [kevyt-valinta-selection
     :kevyt-valinta/julkaisun-tila
     julkaisun-tila-property-state
     julkaisun-tila-i18n
     [{:value julkaisun-tila :label julkaisun-tila-i18n}]
     on-julkaisun-tila-change]))

(defn- kevyt-valinta-julkaisun-tila-row [application-key
                                         lang]
  (let [julkaisun-tila                @(re-frame/subscribe [:virkailija-kevyt-valinta/julkaisun-tila application-key])
        julkaisun-tila-label          (kevyt-valinta-review-type-label :kevyt-valinta/julkaisun-tila lang)
        julkaisun-tila-property-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                            :kevyt-valinta/julkaisun-tila
                                                            application-key])]
    [kevyt-valinta-row
     julkaisun-tila-property-state
     [kevyt-valinta-checkmark julkaisun-tila-property-state]
     julkaisun-tila-label
     [kevyt-valinta-julkaisun-tila-selection
      julkaisun-tila
      julkaisun-tila-property-state
      lang]]))

(defn kevyt-valinta []
  (let [application-key @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid   (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]]))
        lang            @(re-frame/subscribe [:editor/virkailija-lang])]
    [:div.application-handling__kevyt-valinta
     [kevyt-valinta-valinnan-tila-row
      hakukohde-oid
      application-key
      lang]
     [kevyt-valinta-julkaisun-tila-row
      application-key
      lang]]))
