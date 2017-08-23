(ns ataru.hakija.application-hakukohde-component
  (:require
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ataru.application-common.application-field-common :refer [scroll-to-anchor]]))

(defn- hakukohde-remove-event-handler [e]
  (dispatch [:application/hakukohde-remove-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-select-event-handler [e]
  (dispatch [:application/hakukohde-add-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-query-change-event-handler [e]
  (dispatch [:application/hakukohde-query-change (.-value (.-target e))]))

(defn- hakukohde-query-clear-event-handler [_]
  (dispatch [:application/hakukohde-query-clear]))

(defn- hakukohde-search-toggle-event-handler [_]
  (dispatch [:application/hakukohde-search-toggle]))

(defn- selected-hakukohde-row-remove
  [hakukohde-oid]
  [:div.application__hakukohde-row-button-container
   [:a.application__hakukohde-remove-link
    {:on-click hakukohde-remove-event-handler
     :data-hakukohde-oid hakukohde-oid}
    @(subscribe [:application/get-i18n-text
                 ; TODO localization
                 {:fi "Poista"
                  :sv ""
                  :en ""}])]])

(defn- selected-hakukohde-row
  [hakukohde-oid]
  [:div.application__hakukohde-row.application__hakukohde-row--selected
   [:div.application__hakukohde-row-icon-container
    [:i.zmdi.zmdi-graduation-cap.zmdi-hc-3x]]
   [:div.application__hakukohde-row-text-container.application__hakukohde-row-text-container--selected
    [:div.application__hakukohde-selected-row-header
     @(subscribe [:application/hakukohde-label hakukohde-oid])]
    [:div.application__hakukohde-selected-row-description
     @(subscribe [:application/hakukohde-description hakukohde-oid])]]
   (when @(subscribe [:application/hakukohteet-editable?])
     [selected-hakukohde-row-remove hakukohde-oid])])

(defn- search-hit-hakukohde-row
  [hakukohde-oid]
  (let [hakukohde-selected? @(subscribe [:application/hakukohde-selected? hakukohde-oid])]
    [:div.application__hakukohde-row.application__hakukohde-row--search-hit
     {:class (when hakukohde-selected? "application__hakukohde-row--search-hit-selected")}
     [:div.application__hakukohde-row-text-container
      [:div.application__hakukohde-selected-row-header
       @(subscribe [:application/hakukohde-label hakukohde-oid])]
      [:div.application__hakukohde-selected-row-description
       @(subscribe [:application/hakukohde-description hakukohde-oid])]]
     [:div.application__hakukohde-row-button-container
      (if hakukohde-selected?
        [:i.application__hakukohde-selected-check.zmdi.zmdi-check.zmdi-hc-2x]
        (if @(subscribe [:application/hakukohteet-full?])
          [:a.application__hakukohde-select-button.application__hakukohde-select-button--disabled
           @(subscribe [:application/get-i18n-text
                        ; TODO localization
                        {:fi "Lisää"
                         :sv ""
                         :en ""}])]
          [:a.application__hakukohde-select-button
           {:on-click           hakukohde-select-event-handler
            :data-hakukohde-oid hakukohde-oid}
           @(subscribe [:application/get-i18n-text
                        ; TODO localization
                        {:fi "Lisää"
                         :sv ""
                         :en ""}])]))]]))

(defn- hakukohde-selection-search
  []
  (let [hakukohde-query @(subscribe [:application/hakukohde-query])]
    [:div
     [:div.application__hakukohde-selection-search-arrow-up]
     [:div.application__hakukohde-selection-search-container
      [:div.application__hakukohde-selection-search-close-button
       [:a {:on-click hakukohde-search-toggle-event-handler}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
      [:div.application__hakukohde-selection-search-input.application__form-text-input-box
       [:input.application__form-text-input-in-box
        {:on-change   hakukohde-query-change-event-handler
         :placeholder @(subscribe [:application/get-i18n-text
                                   ; TODO localization
                                   {:fi "Etsi tämän haun koulutuksia"
                                    :sv ""
                                    :en ""}])
         :value       hakukohde-query}]
       (when (not (empty? hakukohde-query))
         [:div.application__form-clear-text-input-in-box
          [:a
           {:on-click hakukohde-query-clear-event-handler}
           [:i.zmdi.zmdi-close]]])]
      [:div.application__hakukohde-selection-search-results
       (for [hakukohde-oid @(subscribe [:application/hakukohde-hits])]
         ^{:key (str "found-hakukohde-row-" hakukohde-oid)}
         [search-hit-hakukohde-row hakukohde-oid])]]]))

(defn- hakukohde-selection-header
  [field-descriptor]
  [:div.application__wrapper-heading.application__wrapper-heading-block
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [scroll-to-anchor field-descriptor]])

(defn hakukohteet
  [field-descriptor]
  [:div.application__wrapper-element.application__wrapper-element-border
   [hakukohde-selection-header field-descriptor]
   [:div.application__hakukohde-selected-list
    (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
      ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
      [selected-hakukohde-row hakukohde-oid])
    (when @(subscribe [:application/hakukohteet-editable?])
      [:div.application__hakukohde-row.application__hakukohde-row--search-toggle
       [:a.application__hakukohde-selection-open-search
        {:on-click hakukohde-search-toggle-event-handler}
        @(subscribe [:application/get-i18n-text
                     ; TODO localization
                     {:fi "Lisää hakukohde"
                      :sv ""
                      :en ""}])]
       (when-let [max-hakukohteet @(subscribe [:application/max-hakukohteet])]
         [:span.application__hakukohde-selection-max-label (str "(max. " max-hakukohteet ")")])
       (when @(subscribe [:application/show-hakukohde-search])
         [hakukohde-selection-search])])]])
