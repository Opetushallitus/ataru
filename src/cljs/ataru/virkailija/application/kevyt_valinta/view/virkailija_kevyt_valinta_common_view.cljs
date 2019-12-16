(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view
  (:require [ataru.application-common.loaders.ellipsis-loader :as el]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn- on-kevyt-valinta-property-change [kevyt-valinta-property
                                         hakukohde-oid
                                         application-key
                                         new-kevyt-valinta-property-value]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-kevyt-valinta-property
                      kevyt-valinta-property
                      hakukohde-oid
                      application-key
                      new-kevyt-valinta-property-value]))

(defn- kevyt-valinta-selection-dropdown [kevyt-valinta-property
                                         kevyt-valinta-dropdown-label
                                         kevyt-valinta-property-value
                                         kevyt-valinta-dropdown-values
                                         kevyt-valinta-on-dropdown-value-change
                                         ongoing-request-property]
  (let [dropdown-open?     @(re-frame/subscribe [:state-query [:application :kevyt-valinta kevyt-valinta-property :open?]])
        dropdown-disabled? ongoing-request-property
        show-loader?       (= ongoing-request-property kevyt-valinta-property)]
    [:div.application-handling__kevyt-valinta-dropdown-container
     [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item
      (if dropdown-disabled?
        {:class "application-handling__kevyt-valinta-dropdown--disabled"}
        {:on-click (fn toggle-kevyt-valinta-selection-dropdown []
                     (re-frame/dispatch [:virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown kevyt-valinta-property]))})
      [:span.application-handling__kevyt-valinta-dropdown-label kevyt-valinta-dropdown-label]
      (when show-loader?
        [el/ellipsis-loader])
      [:i.zmdi.application-handling__kevyt-valinta-dropdown-chevron.zmdi-chevron-up
       {:class (when dropdown-open?
                 "application-handling__kevyt-valinta-dropdown-chevron-open")}]]
     (when dropdown-open?
       [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item-list.animated.fadeIn
        (map (fn [{value :value label :label}]
               (let [current-value? (= value kevyt-valinta-property-value)]
                 ^{:key (str (name kevyt-valinta-property) "-" value)}
                 [:div.application-handling__kevyt-valinta-dropdown-item
                  {:on-click (when-not current-value?
                               (fn []
                                 (kevyt-valinta-on-dropdown-value-change current-value?)))}
                  [:span
                   (when current-value?
                     {:class "application-handling__kevyt-valinta-dropdown-label--selected"})
                   label]]))
             kevyt-valinta-dropdown-values)])]))

(defn- kevyt-valinta-checkmark [kevyt-valinta-property application-key]
  (let [checkmark-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-checkmark-state kevyt-valinta-property application-key])
        checkmark-class (case checkmark-state
                          :checked
                          "application-handling__kevyt-valinta-checkmark--checked"

                          :unchecked
                          "application-handling__kevyt-valinta-checkmark--unchecked"

                          :grayed-out
                          "application-handling__kevyt-valinta-checkmark--grayed-out")
        show-checkmark? (= checkmark-state :checked)]
    [:div.application-handling__kevyt-valinta-checkmark
     {:class checkmark-class}
     (when show-checkmark?
       [:i.zmdi.zmdi-check.application-handling__kevyt-valinta-checkmark--bold])]))

(defn- kevyt-valinta-selection [kevyt-valinta-selection-component
                                kevyt-valinta-property
                                kevyt-valinta-selection-state
                                kevyt-valinta-selection-label
                                kevyt-valinta-selection-values
                                kevyt-valinta-on-selection-value-change]
  (let [ongoing-request-property @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])]
    (if (and (= kevyt-valinta-selection-state :checked)
             (or (not ongoing-request-property)
                 (not= ongoing-request-property kevyt-valinta-property)))
      [:span.application-handling__kevyt-valinta-value kevyt-valinta-selection-label]
      [kevyt-valinta-selection-component
       kevyt-valinta-property
       kevyt-valinta-selection-label
       kevyt-valinta-selection-values
       kevyt-valinta-on-selection-value-change
       ongoing-request-property])))

(defn kevyt-valinta-dropdown-selection [kevyt-valinta-property]
  (let [ongoing-request-property               @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
        lang                                   @(re-frame/subscribe [:editor/virkailija-lang])
        application-key                        @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-property-values          @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                                     kevyt-valinta-property
                                                                     application-key])
        kevyt-valinta-dropdown-values          (map (fn [kevyt-valinta-property-value]
                                                      {:value kevyt-valinta-property-value
                                                       :label (translations/kevyt-valinta-selection-label kevyt-valinta-property
                                                                                                          kevyt-valinta-property-value
                                                                                                          lang)})
                                                    kevyt-valinta-property-values)
        kevyt-valinta-property-value           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                                     kevyt-valinta-property
                                                                     application-key])
        kevyt-valinta-dropdown-label           (->> kevyt-valinta-dropdown-values
                                                    (filter (comp (partial = kevyt-valinta-property-value)
                                                                  :value))
                                                    (map :label)
                                                    (first))
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid                          (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]]))
        kevyt-valinta-on-dropdown-value-change (partial on-kevyt-valinta-property-change
                                                        kevyt-valinta-property
                                                        hakukohde-oid
                                                        application-key)
        kevyt-valinta-dropdown-state           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                                     kevyt-valinta-property
                                                                     application-key])]
    (if (and (= kevyt-valinta-dropdown-state :checked)
             (or (not ongoing-request-property)
                 (not= ongoing-request-property kevyt-valinta-property)))
      [:span.application-handling__kevyt-valinta-value kevyt-valinta-dropdown-label]
      [kevyt-valinta-selection-dropdown
       kevyt-valinta-property
       kevyt-valinta-dropdown-label
       kevyt-valinta-property-value
       kevyt-valinta-dropdown-values
       kevyt-valinta-on-dropdown-value-change
       ongoing-request-property])))

(defn kevyt-valinta-checkbox-selection [kevyt-valinta-property]
  (let [lang                                   @(re-frame/subscribe [:editor/virkailija-lang])
        ongoing-request-property               @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
        application-key                        @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-checkbox-state           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                                     kevyt-valinta-property
                                                                     application-key])
        checkbox-disabled?                     (or ongoing-request-property
                                                   (= kevyt-valinta-checkbox-state :checked))
        show-loader?                           (= ongoing-request-property kevyt-valinta-property)
        kevyt-valinta-property-values          @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                                     kevyt-valinta-property
                                                                     application-key])
        kevyt-valinta-checkbox-values          (map (fn [kevyt-valinta-property-value]
                                                      {:value kevyt-valinta-property-value
                                                       :label (translations/kevyt-valinta-selection-label kevyt-valinta-property
                                                                                                          kevyt-valinta-property-value
                                                                                                          lang)})
                                                    kevyt-valinta-property-values)
        kevyt-valinta-checkbox-state           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-state
                                                                     kevyt-valinta-property
                                                                     application-key])
        kevyt-valinta-checkbox-label           (->> kevyt-valinta-checkbox-values
                                                    (filter (comp (partial = kevyt-valinta-checkbox-state)
                                                                  :value))
                                                    (map :label)
                                                    (first))
        checkbox-checked?                      (->> kevyt-valinta-checkbox-values
                                                    (some (fn [{value :value label :label}]
                                                            (and value
                                                                 (= kevyt-valinta-checkbox-label label))))
                                                    (true?))
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid                          (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]]))

        kevyt-valinta-on-checkbox-value-change (partial on-kevyt-valinta-property-change
                                                        kevyt-valinta-property
                                                        hakukohde-oid
                                                        application-key)]
    [:div.application-handling__kevyt-valinta-dropdown-container
     [:input.application-handling__kevyt-valinta-checkbox
      {:type      "checkbox"
       :disabled  checkbox-disabled?
       :checked   checkbox-checked?
       :on-change (fn []
                    (let [new-value (not checkbox-checked?)]
                      (kevyt-valinta-on-checkbox-value-change new-value)))}]
     (when show-loader?
       [el/ellipsis-loader])]))

(defn kevyt-valinta-row [kevyt-valinta-property selection-component]
  (let [lang                          @(re-frame/subscribe [:editor/virkailija-lang])
        application-key               @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-label           (translations/kevyt-valinta-review-type-label kevyt-valinta-property lang)
        kevyt-valinta-selection-state @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                            kevyt-valinta-property
                                                            application-key])
        selection-grayed-out?         (= kevyt-valinta-selection-state :grayed-out)]
    [:div.application-handling__kevyt-valinta-row
     [kevyt-valinta-checkmark kevyt-valinta-property application-key]
     [:div.application-handling__kevyt-valinta-label
      [:span kevyt-valinta-label]
      (when-not selection-grayed-out?
        [:div.application-handling__kevyt-valinta-hr])]
     (when-not selection-grayed-out?
       selection-component)]))
