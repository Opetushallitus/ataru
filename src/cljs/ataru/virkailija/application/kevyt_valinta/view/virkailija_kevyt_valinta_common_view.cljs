(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view
  (:require [ataru.application-common.loaders.ellipsis-loader :as el]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as translations]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame])
  (:require-macros [cljs.core.match :refer [match]]
                   [reagent.ratom :refer [reaction]]))

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
                  {:on-click (fn []
                               (if current-value?
                                 (re-frame/dispatch [:virkailija-kevyt-valinta/toggle-kevyt-valinta-dropdown kevyt-valinta-property])
                                 (kevyt-valinta-on-dropdown-value-change value)))}
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
        kevyt-valinta-property-value           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
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
  (let [lang                                   (re-frame/subscribe [:editor/virkailija-lang])
        ongoing-request-property               (re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
        application-key                        (re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-checkbox-state           (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                                               kevyt-valinta-property
                                                                               @application-key]))
        checkbox-disabled?                     (reaction (or @ongoing-request-property
                                                             (= @kevyt-valinta-checkbox-state :checked)))
        show-loader?                           (reaction (= @ongoing-request-property kevyt-valinta-property))
        kevyt-valinta-property-values          (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                                               kevyt-valinta-property
                                                                               @application-key]))
        kevyt-valinta-checkbox-values          (reaction (map (fn [kevyt-valinta-property-value]
                                                                {:value kevyt-valinta-property-value
                                                                 :label (translations/kevyt-valinta-selection-label kevyt-valinta-property
                                                                                                                    kevyt-valinta-property-value
                                                                                                                    @lang)})
                                                              @kevyt-valinta-property-values))
        kevyt-valinta-checkbox-value           (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                                               kevyt-valinta-property
                                                                               @application-key]))
        kevyt-valinta-checkbox-label           (reaction (->> @kevyt-valinta-checkbox-values
                                                              (filter (comp (partial = @kevyt-valinta-checkbox-value)
                                                                            :value))
                                                              (map :label)
                                                              (first)))
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid                          (reaction (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])))

        kevyt-valinta-on-checkbox-value-change (reaction (partial on-kevyt-valinta-property-change
                                                                  kevyt-valinta-property
                                                                  @hakukohde-oid
                                                                  @application-key))
        force-show-checkbox?                   (reagent/atom nil)
        kevyt-valinta-dropdowns-open?          (re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-dropdowns-open?])
        kevyt-valinta-write-rights?            (re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-write-rights?])
        checkbox-info-visible?                 (reaction (and @kevyt-valinta-write-rights?
                                                              (if (some? @force-show-checkbox?)
                                                                @force-show-checkbox?
                                                                (not @kevyt-valinta-checkbox-value))
                                                              (not @kevyt-valinta-dropdowns-open?)))]
    (fn [_]
      [:div.application-handling__kevyt-valinta-checkbox-container
       (when @checkbox-disabled?
         {:class "application-handling__kevyt-valinta-checkbox-container--disabled"})
       [:div.application-handling__kevyt-valinta-checkbox
        {:class    (as-> "" classes
                         (str classes (if @checkbox-disabled?
                                        " application-handling__kevyt-valinta-checkbox--disabled"
                                        " application-handling__kevyt-valinta-checkbox--enabled"))

                         (cond-> classes
                                 @kevyt-valinta-checkbox-value
                                 (str " application-handling__kevyt-valinta-checkbox--checked")))
         :on-click (when-not @checkbox-disabled?
                     (fn []
                       (let [new-value (not @kevyt-valinta-checkbox-value)]
                         (reset! force-show-checkbox? nil)
                         (@kevyt-valinta-on-checkbox-value-change new-value))))}
        (when @kevyt-valinta-checkbox-value
          [:i.zmdi.zmdi-check.application-handling__kevyt-valinta-checkbox-checkmark])]
       [:div.application-handling__kevyt-valinta-checkbox-label
        [:span @kevyt-valinta-checkbox-label]
        (when-not @show-loader?
          [:<>
           [:a
            {:on-click (fn []
                         (swap! force-show-checkbox? (fnil not @checkbox-info-visible?)))}
            [:div.application-handling__kevyt-valinta-checkbox-info-container
             (when @kevyt-valinta-write-rights?
               [:i.zmdi.zmdi-info.application-handling__kevyt-valinta-checkbox-info-symbol])
             (when @checkbox-info-visible?
               [:div.application-handling__kevyt-valinta-checkbox-info-indicator.animated.fadeIn])]
            (when @checkbox-info-visible?
              [:div.application-handling__kevyt-valinta-checkbox-info.animated.fadeIn
               [:span.application-handling__kevyt-valinta-checkbox-info-text "Julkaisun jälkeen valintatieto näkyy hakijalle Oma opintopolku -palvelussa."]
               [:span.application-handling__kevyt-valinta-checkbox-info-text "Hyväksytyille hakijoille lähetetään myös sähköposti klo 8.00 tai 20.00"]])]])]
       (when @show-loader?
         [el/ellipsis-loader])])))

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
      [:span
       (when selection-grayed-out?
         {:class "application-handling__kevyt-valinta-label--disabled"})
       kevyt-valinta-label]
      (when-not selection-grayed-out?
        [:div.application-handling__kevyt-valinta-hr])]
     (when-not selection-grayed-out?
       selection-component)]))
