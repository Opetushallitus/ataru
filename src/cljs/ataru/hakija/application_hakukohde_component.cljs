(ns ataru.hakija.application-hakukohde-component
  (:require
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ataru.application-common.application-field-common :refer [scroll-to-anchor]]))

(defn index-of [s val from-index]
  (clojure.string/index-of (clojure.string/lower-case s)
                           (clojure.string/lower-case val)
                           from-index))

(defn- should-search? [search-term]
  (> (count search-term) 1))

(defn match-text [text search-term]
  (if-not (should-search? search-term)
    [{:text text :hilight false}]
    (loop [res           []
           current-index 0]
      (let [match-index (index-of text search-term current-index)]
        (cond
          (nil? match-index)
          (conj res {:text    (subs text current-index)
                     :hilight false})

          (< current-index match-index)
          (recur (conj res
                       {:text    (subs text current-index match-index)
                        :hilight false}
                       {:text    (subs text match-index (+ (count search-term) match-index))
                        :hilight true})
                 (+ match-index (count search-term)))

          :else
          (recur (conj res {:text    (subs text current-index (+ (count search-term) current-index))
                            :hilight true})
                 (+ current-index (count search-term))))))))

(defn hilighted-text->span [idx {:keys [text hilight]}]
  (let [key (str "hilight-" idx)]
    [:span
     (cond-> {:key key}
             (true? hilight)
             (assoc :class "application__hakukohde-row--search-hit-highlight"))
     text]))

(defn hilight-text [text hilight-text]
  (if (some? text)
    (map-indexed hilighted-text->span (match-text text hilight-text))
    [:span ""]))

(defn- hakukohde-remove-event-handler [e]
  (js/setTimeout ; Do this with timeout since phantomrunner is bad with `on animationend|transitionend`
    #(dispatch [:application/hakukohde-remove-selection (.getAttribute (.-target e) "data-hakukohde-oid")])
    500)
  (set! (.-className parent) (str (.-className parent) " fadeOut")))

(defn- hakukohde-select-event-handler [e]
  (dispatch [:application/hakukohde-add-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-query-change-event-handler [e]
  (dispatch [:application/hakukohde-query-change (.-value (.-target e))]))

(defn- hakukohde-query-clear-event-handler [_]
  (dispatch [:application/hakukohde-query-change ""]))

(defn- hakukohde-search-toggle-event-handler [_]
  (dispatch [:application/hakukohde-search-toggle]))

(defn- selected-hakukohde-row-remove
  [hakukohde-oid]
  [:div.application__hakukohde-row-button-container
   [:a.application__hakukohde-remove-link
    {:on-click           hakukohde-remove-event-handler
     :data-hakukohde-oid hakukohde-oid
     :role               "button"}
    @(subscribe [:application/get-i18n-text
                 {:fi "Poista"
                  :sv "Ta bort"
                  :en "Remove"}])]])

(defn- selected-hakukohde-row
  [hakukohde-oid]
  [:div.application__hakukohde-row.application__hakukohde-row--selected.animated.fadeIn
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
  (let [hakukohde-selected? @(subscribe [:application/hakukohde-selected? hakukohde-oid])
        search-term         @(subscribe [:application/hakukohde-query])
        aria-header-id      (str "hakukohde-search-hit-header-" hakukohde-oid)
        aria-description-id (str "hakukohde-search-hit-description-" hakukohde-oid)]
    [:div.application__hakukohde-row.application__hakukohde-row--search-hit
     {:class         (when hakukohde-selected? "application__hakukohde-row--search-hit-selected")
      :aria-selected hakukohde-selected?}
     [:div.application__hakukohde-row-text-container
      [:div.application__hakukohde-selected-row-header
       {:id aria-header-id}
       (hilight-text @(subscribe [:application/hakukohde-label hakukohde-oid]) search-term)]
      [:div.application__hakukohde-selected-row-description
       {:id aria-description-id}
       (hilight-text @(subscribe [:application/hakukohde-description hakukohde-oid]) search-term)]]
     [:div.application__hakukohde-row-button-container
      (if hakukohde-selected?
        [:i.application__hakukohde-selected-check.zmdi.zmdi-check.zmdi-hc-2x]
        (if @(subscribe [:application/hakukohteet-full?])
          [:a.application__hakukohde-select-button.application__hakukohde-select-button--disabled
           {:role             "button"
            :aria-labelledby  aria-header-id
            :aria-describedby aria-description-id
            :aria-disabled    true}
           @(subscribe [:application/get-i18n-text
                        {:fi "Lisää"
                         :sv "Lägg till"
                         :en "Add"}])]
          [:a.application__hakukohde-select-button
           {:on-click           hakukohde-select-event-handler
            :role               "button"
            :data-hakukohde-oid hakukohde-oid
            :aria-labelledby    aria-header-id
            :aria-describedby   aria-description-id}
           @(subscribe [:application/get-i18n-text
                        {:fi "Lisää"
                         :sv "Lägg till"
                         :en "Add"}])]))]]))

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
         :title @(subscribe [:application/get-i18n-text
                             {:fi "Etsi tämän haun koulutuksia"
                              :sv "Sök ansökningsmål i denna ansökan"
                              :en "Search for application options"}])
         :placeholder @(subscribe [:application/get-i18n-text
                                   {:fi "Etsi tämän haun koulutuksia"
                                    :sv "Sök ansökningsmål i denna ansökan"
                                    :en "Search for application options"}])
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
                     {:fi "Lisää hakukohde"
                      :sv "Lägg till ansökningsmål"
                      :en "Add application option"}])]
       (when-let [max-hakukohteet @(subscribe [:application/max-hakukohteet])]
         [:span.application__hakukohde-selection-max-label (str "(max. " max-hakukohteet ")")])
       (when @(subscribe [:application/show-hakukohde-search])
         [hakukohde-selection-search])])]])
