(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view
  (:require [ataru.application-common.loaders.ellipsis-loader :as el]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]))

(defn on-kevyt-valinta-property-change [kevyt-valinta-property
                                        hakukohde-oid
                                        application-key
                                        new-kevyt-valinta-property-value]
  (re-frame/dispatch [:virkailija-kevyt-valinta/change-kevyt-valinta-property
                      kevyt-valinta-property
                      hakukohde-oid
                      application-key
                      new-kevyt-valinta-property-value]))

(defn- kevyt-valinta-selection-dropdown [kevyt-valinta-property
                                         kevyt-valinta-dropdown-value
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
      [:span.application-handling__kevyt-valinta-dropdown-value kevyt-valinta-dropdown-value]
      (when show-loader?
        [el/ellipsis-loader])
      [:i.zmdi.application-handling__kevyt-valinta-dropdown-chevron.zmdi-chevron-up
       {:class (when dropdown-open?
                 "application-handling__kevyt-valinta-dropdown-chevron-open")}]]
     (when dropdown-open?
       [:div.application-handling__kevyt-valinta-dropdown.application-handling__kevyt-valinta-dropdown-item-list.animated.fadeIn
        (map (fn [{value :value label :label}]
               ^{:key (str (name kevyt-valinta-property) "-" value)}
               [:div.application-handling__kevyt-valinta-dropdown-item
                {:on-click (fn []
                             (kevyt-valinta-on-dropdown-value-change value))}
                [:span label]])
             kevyt-valinta-dropdown-values)])]))

(defn kevyt-valinta-checkmark [kevyt-valinta-property application-key]
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

(defn kevyt-valinta-selection [kevyt-valinta-property
                               kevyt-valinta-dropdown-state
                               kevyt-valinta-dropdown-value
                               kevyt-valinta-dropdown-values
                               kevyt-valinta-on-dropdown-value-change]
  (let [ongoing-request-property @(re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])]
    (if (and (= kevyt-valinta-dropdown-state :checked)
             (or (not ongoing-request-property)
                 (not= ongoing-request-property kevyt-valinta-property)))
      [:span.application-handling__kevyt-valinta-value kevyt-valinta-dropdown-value]
      [kevyt-valinta-selection-dropdown
       kevyt-valinta-property
       kevyt-valinta-dropdown-value
       kevyt-valinta-dropdown-values
       kevyt-valinta-on-dropdown-value-change
       ongoing-request-property])))

(defn kevyt-valinta-row [kevyt-valinta-dropdown-state
                         checkmark-component
                         label
                         selection-component]
  (if (= kevyt-valinta-dropdown-state :grayed-out)
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
