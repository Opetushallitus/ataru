(ns ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-common-view
  (:require [ataru.application-common.loaders.ellipsis-loader :as el]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as i18n-mapping]
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
        (doall (map (fn [{value :value label :label}]
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
                    kevyt-valinta-dropdown-values))])]))

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
        application-key                        @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-property-values          @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                                     kevyt-valinta-property
                                                                     application-key])
        kevyt-valinta-dropdown-values          (map (fn [kevyt-valinta-property-value]
                                                      {:value kevyt-valinta-property-value
                                                       :label (let [translation-key (i18n-mapping/kevyt-valinta-value-translation-key
                                                                                      kevyt-valinta-property
                                                                                      kevyt-valinta-property-value)]
                                                                @(re-frame/subscribe [:editor/virkailija-translation translation-key]))})
                                                    kevyt-valinta-property-values)
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid                          (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]]))
        kevyt-valinta-property-value           @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                                     kevyt-valinta-property
                                                                     application-key
                                                                     hakukohde-oid])
        kevyt-valinta-dropdown-label           (->> kevyt-valinta-dropdown-values
                                                    (filter (comp (partial = kevyt-valinta-property-value)
                                                                  :value))
                                                    (map :label)
                                                    (first))
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

(defn kevyt-valinta-slider-toggle-selection [kevyt-valinta-property]
  (let [lang                                        (re-frame/subscribe [:editor/virkailija-lang])
        ongoing-request-property                    (re-frame/subscribe [:virkailija-kevyt-valinta/ongoing-request-property])
        application-key                             (re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-slider-toggle-state           (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-selection-state
                                                                                    kevyt-valinta-property
                                                                                    @application-key]))
        slider-toggle-disabled?                     (reaction (or (some? @ongoing-request-property)
                                                                  (= @kevyt-valinta-slider-toggle-state :checked)))
        show-loader?                                (reaction (= @ongoing-request-property kevyt-valinta-property))
        kevyt-valinta-property-values               (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/allowed-kevyt-valinta-property-values
                                                                                    kevyt-valinta-property
                                                                                    @application-key]))
        kevyt-valinta-slider-toggle-values          (reaction (map (fn [kevyt-valinta-property-value]
                                                                     {:value kevyt-valinta-property-value
                                                                      :label (let [translation-key (i18n-mapping/kevyt-valinta-value-translation-key
                                                                                                     kevyt-valinta-property
                                                                                                     kevyt-valinta-property-value)]
                                                                               @(re-frame/subscribe [:editor/virkailija-translation translation-key]))})
                                                                   @kevyt-valinta-property-values))
        ;; kevytvalinta näytetään ainoastaan, kun yksi hakukohde valittuna, ks. :virkailija-kevyt-valinta/show-kevyt-valinta?
        hakukohde-oid                               (reaction (first @(re-frame/subscribe [:state-query [:application :selected-review-hakukohde-oids]])))
        kevyt-valinta-slider-toggle-value           (reaction @(re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                                                    kevyt-valinta-property
                                                                                    @application-key
                                                                                    @hakukohde-oid]))
        kevyt-valinta-slider-toggle-label           (reaction (->> @kevyt-valinta-slider-toggle-values
                                                                   (filter (comp (partial = @kevyt-valinta-slider-toggle-value)
                                                                                 :value))
                                                                   (map :label)
                                                                   (first)))
        kevyt-valinta-on-slider-toggle-value-change (reaction (partial on-kevyt-valinta-property-change
                                                                       kevyt-valinta-property
                                                                       @hakukohde-oid
                                                                       @application-key))
        force-show-slider-toggle?                   (reagent/atom nil)
        kevyt-valinta-dropdowns-open?               (re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-dropdowns-open?])
        kevyt-valinta-write-rights?                 (re-frame/subscribe [:virkailija-kevyt-valinta/kevyt-valinta-write-rights?])
        slider-toggle-info-visible?                 (reaction (and @kevyt-valinta-write-rights?
                                                                   (if (some? @force-show-slider-toggle?)
                                                                     @force-show-slider-toggle?
                                                                     (not @kevyt-valinta-slider-toggle-value))
                                                                   (not @kevyt-valinta-dropdowns-open?)))]
    (fn [_]
      [:div.application-handling__kevyt-valinta-slider-toggle-container
       (when @slider-toggle-disabled?
         {:class "application-handling__kevyt-valinta-slider-toggle-container--disabled"})
       [:div.application-handling__kevyt-valinta-slider-toggle-label
        [:span @kevyt-valinta-slider-toggle-label]]
       [:div.application-handling__kevyt-valinta-slider-toggle
        {:class    (cond-> ""
                           @slider-toggle-disabled?
                           (str " application-handling__kevyt-valinta-slider-toggle--disabled")

                           (match [@kevyt-valinta-slider-toggle-state @ongoing-request-property]
                                  [:unchecked (_ :guard nil?)]
                                  true

                                  [:unchecked (_ :guard #(not= % kevyt-valinta-property))]
                                  true

                                  [:checked (_ :guard #(not= % kevyt-valinta-property))]
                                  true

                                  :else
                                  false)
                           (str " application-handling__kevyt-valinta-slider-toggle--enabled")

                           @kevyt-valinta-slider-toggle-value
                           (str " application-handling__kevyt-valinta-slider-toggle--checked"))
         :on-click (when-not @slider-toggle-disabled?
                     (fn []
                       (let [new-value (not @kevyt-valinta-slider-toggle-value)]
                         (reset! force-show-slider-toggle? nil)
                         (@kevyt-valinta-on-slider-toggle-value-change new-value))))}
        [:div.application-handling__kevyt-valinta-slider-toggle-toggle-indicator]]
       (when-not @show-loader?
         [:<>
          [:a
           {:on-click (fn []
                        (swap! force-show-slider-toggle? (fnil not @slider-toggle-info-visible?)))}
           [:div.application-handling__kevyt-valinta-slider-toggle-info-container
            (when @kevyt-valinta-write-rights?
              [:i.zmdi.zmdi-info.application-handling__kevyt-valinta-slider-toggle-info-symbol])
            (when @slider-toggle-info-visible?
              [:div.application-handling__kevyt-valinta-slider-toggle-info-indicator.animated.fadeIn])]
           (when @slider-toggle-info-visible?
             [:div.application-handling__kevyt-valinta-slider-toggle-info.animated.fadeIn
              [:span.application-handling__kevyt-valinta-slider-toggle-info-text "Julkaisun jälkeen valintatieto näkyy hakijalle Oma opintopolku -palvelussa."]
              [:span.application-handling__kevyt-valinta-slider-toggle-info-text "Hyväksytyille hakijoille lähetetään myös sähköposti klo 8.00 tai 20.00"]])]])
       (when @show-loader?
         [el/ellipsis-loader])])))

(defn kevyt-valinta-row [kevyt-valinta-property selection-component]
  (let [application-key               @(re-frame/subscribe [:state-query [:application :selected-application-and-form :application :key]])
        kevyt-valinta-label           (let [translation-key (i18n-mapping/kevyt-valinta-label-translation-key kevyt-valinta-property)]
                                        @(re-frame/subscribe [:editor/virkailija-translation translation-key]))
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
