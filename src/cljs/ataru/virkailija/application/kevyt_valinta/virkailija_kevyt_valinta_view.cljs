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
                                         ongoing-request-property]
  (let [dropdown-open?     @(re-frame/subscribe [:state-query [:application :kevyt-valinta kevyt-valinta-dropdown-id :open?]])
        dropdown-disabled? ongoing-request-property
        show-loader?       (= ongoing-request-property kevyt-valinta-dropdown-id)]
    [:div.application-handling__kevyt-valinta-dropdown-container
     [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item
      (if dropdown-disabled?
        {:class "application-handling__kevyt-valinta-dropdown--disabled"}
        {:on-click (fn toggle-kevyt-valinta-selection-dropdown []
                     (re-frame/dispatch [:virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown kevyt-valinta-dropdown-id]))})
      [:span.application-handling__kevyt-valinta-dropdown-value kevyt-valinta-dropdown-value]
      (when show-loader?
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
  (let [ongoing-request-property @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])]
    (if (and (= kevyt-valinta-property-state :checked)
             (or (not ongoing-request-property)
                 (not= ongoing-request-property kevyt-valinta-dropdown-id)))
      [:span.application-handling__kevyt-valinta-value kevyt-valinta-dropdown-value]
      [kevyt-valinta-selection-dropdown
       kevyt-valinta-dropdown-id
       kevyt-valinta-dropdown-value
       kevyt-valinta-dropdown-values
       kevyt-valinta-on-dropdown-value-change
       ongoing-request-property])))

(defn- kevyt-valinta-checkmark [kevyt-valinta-property-state]
  (let [ongoing-request-property @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
        checkmark-class          (match [kevyt-valinta-property-state ongoing-request-property]
                                        [:checked (_ :guard (comp not nil?))]
                                        "application-handling__kevyt-valinta-checkmark--unchecked"

                                        [:checked _]
                                        "application-handling__kevyt-valinta-checkmark--checked"

                                        [:unchecked _]
                                        "application-handling__kevyt-valinta-checkmark--unchecked"

                                        [:grayed-out _]
                                        "application-handling__kevyt-valinta-checkmark--grayed-out")
        show-checkmark?          (and (= kevyt-valinta-property-state :checked)
                                      (not ongoing-request-property))]
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

(def ^:private valinnan-tilat
  (conj checked-valinnan-tilat "VARALLA"))

(defn- valinnan-tila-label [valinnan-tila lang]
  (-> review-states/kevyt-valinta-selection-state
      (get valinnan-tila)
      lang))

(defn- on-kevyt-valinta-property-change [kevyt-valinta-property
                                         hakukohde-oid
                                         application-key
                                         new-kevyt-valinta-property-value]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-kevyt-valinta-property
                      kevyt-valinta-property
                      hakukohde-oid
                      application-key
                      new-kevyt-valinta-property-value]))

(defn- kevyt-valinta-valinnan-tila-selection [hakukohde-oid
                                              application-key
                                              valinnan-tila-property-state
                                              valinnan-tila
                                              lang]
  (let [valinnan-tilat-i18n (map (fn [valinnan-tila]
                                   {:value valinnan-tila
                                    :label (valinnan-tila-label valinnan-tila lang)})
                                 valinnan-tilat)
        valinnan-tila-i18n  (->> valinnan-tilat-i18n
                                 (filter (comp (partial = valinnan-tila)
                                               :value))
                                 (map :label))]
    [kevyt-valinta-selection
     :kevyt-valinta/valinnan-tila
     valinnan-tila-property-state
     valinnan-tila-i18n
     valinnan-tilat-i18n
     (partial on-kevyt-valinta-property-change
              :kevyt-valinta/valinnan-tila
              hakukohde-oid
              application-key)]))

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

(def ^:private julkaisun-tilat
  [true false])

(defn- kevyt-valinta-julkaisun-tila-selection [hakukohde-oid
                                               application-key
                                               julkaisun-tila
                                               julkaisun-tila-property-state
                                               lang]
  (let [julkaisun-tilat-i18n (map (fn [julkaisun-tila]
                                    (let [translation-key (case julkaisun-tila
                                                            true :kevyt-valinta/julkaistu-hakijalle
                                                            false :kevyt-valinta/ei-julkaistu)]
                                      {:value julkaisun-tila
                                       :label (-> translations/kevyt-valinta-julkaisun-tila-translations
                                                  translation-key
                                                  lang)}))
                                  julkaisun-tilat)
        julkaisun-tila-i18n  (->> julkaisun-tilat-i18n
                                  (filter (comp (partial = julkaisun-tila)
                                                :value))
                                  (map :label))]
    [kevyt-valinta-selection
     :kevyt-valinta/julkaisun-tila
     julkaisun-tila-property-state
     julkaisun-tila-i18n
     julkaisun-tilat-i18n
     (partial on-kevyt-valinta-property-change
              :kevyt-valinta/julkaisun-tila
              hakukohde-oid
              application-key)]))

(defn- kevyt-valinta-julkaisun-tila-row [hakukohde-oid
                                         application-key
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
      hakukohde-oid
      application-key
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
      hakukohde-oid
      application-key
      lang]]))
