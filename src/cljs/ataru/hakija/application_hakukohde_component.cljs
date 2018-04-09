(ns ataru.hakija.application-hakukohde-component
  (:require
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ataru.application-common.application-field-common :refer [scroll-to-anchor]]
    [ataru.cljs-util :refer [get-translation]]))

(defn- should-search? [search-term]
  (> (count search-term) 1))

(defn text-matches
  [text search-terms]
  (let [text         (clojure.string/lower-case text)
        search-terms (->> search-terms
                          (filter should-search?)
                          (map clojure.string/lower-case))]
    (apply concat
           (for [search-term search-terms]
             (loop [res           []
                    current-index 0]
               (let [match-index (clojure.string/index-of text search-term current-index)
                     match-end   (when match-index (+ match-index (count search-term)))]
                 (if (nil? match-index)
                   res
                   (recur
                     (conj res [match-index match-end])
                     match-end))))))))

(defn- combine-overlapping-matches
  [text-matches]
  (reduce
    (fn [acc [match-start match-end]]
      (let [[previous-start previous-end] (last acc)]
        (if (and previous-start (<= match-start previous-end))
          (conj (vec (butlast acc)) [previous-start match-end])
          (conj acc [match-start match-end]))))
    []
    (sort text-matches)))

(defn match-text [text search-terms]
  (if (or (empty? search-terms)
          (every? false? (map should-search? search-terms)))
    [{:text text :hilight false}]
    (let [highlights (-> text
                         (text-matches search-terms)
                         (combine-overlapping-matches))]
      (loop [res           []
             current-index 0
             [[match-begin match-end] & rest-highlights] highlights]
        (cond
          (nil? match-begin)
          (if (= current-index (count text))
            res
            (conj res {:text    (subs text current-index)
                       :hilight false}))

          (< current-index match-begin)
          (recur (conj res
                       {:text    (subs text current-index match-begin)
                        :hilight false}
                       {:text    (subs text match-begin match-end)
                        :hilight true})
                 match-end
                 rest-highlights)

          :else
          (recur (conj res
                       {:text    (subs text current-index match-end)
                        :hilight true})
                 match-end
                 rest-highlights))))))

(defn hilighted-text->span [idx {:keys [text hilight]}]
  (let [key (str "hilight-" idx)]
    [:span
     (cond-> {:key key}
             (true? hilight)
             (assoc :class "application__hakukohde-row--search-hit-highlight"))
     text]))

(defn hilight-text [text hilight-text]
  (if (some? text)
    (map-indexed hilighted-text->span (match-text text (clojure.string/split hilight-text #"\s+")))
    [:span ""]))

(defn- hakukohde-remove-event-handler [e]
  (dispatch [:application/hakukohde-remove-selection (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-select-event-handler [e]
  (dispatch [:application/hakukohde-add-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-query-change-event-handler [e]
  (dispatch [:application/hakukohde-query-change (.-value (.-target e))]))

(defn- hakukohde-query-clear-event-handler [_]
  (dispatch [:application/hakukohde-query-change ""]))

(defn- hakukohde-search-toggle-event-handler [_]
  (dispatch [:application/hakukohde-search-toggle]))

(defn- selected-hakukohde-disabled-row-remove [hakukohde-oid]
       [:div.application__hakukohde-row-button-container
        {:disabled "disabled"}
        [:a.application__hakukohde-remove-link
         {:data-hakukohde-oid hakukohde-oid
          :role               "button"}
         (get-translation :remove)]])

(defn- selected-hakukohde-row-remove
  [hakukohde-oid]
  [:div.application__hakukohde-row-button-container
   [:a.application__hakukohde-remove-link
    {:on-click           hakukohde-remove-event-handler
     :data-hakukohde-oid hakukohde-oid
     :role               "button"}
    (get-translation :remove)]])

(defn- selected-hakukohde-increase-priority
  [hakukohde-oid priority-number disabled?]
 (let [increase-disabled? (= priority-number 1)]
  [:span.application__hakukohde-priority-changer.increase
     {:disabled (when disabled? "disabled")
      :class    (when increase-disabled? "disabled")
      :on-click (when-not increase-disabled?
                  #(dispatch [:application/change-hakukohde-priority hakukohde-oid -1]))}]))

(defn- selected-hakukohde-decrease-priority
  [hakukohde-oid priority-number disabled?]
  (let [selected-hakukohteet @(subscribe [:application/selected-hakukohteet])
        decrease-disabled? (= priority-number (count selected-hakukohteet))]
    [:span.application__hakukohde-priority-changer.decrease
     {:disabled (when disabled? "disabled")
      :class    (when decrease-disabled? "disabled")
      :on-click (when-not decrease-disabled?
                  #(dispatch [:application/change-hakukohde-priority hakukohde-oid 1]))}]))

(defn- prioritize-hakukohde-buttons
  [hakukohde-oid disabled?]
  (let [priority-number @(subscribe [:application/hakukohde-priority-number hakukohde-oid])]
    [:div.application__hakukohde-row-priority-container
     [selected-hakukohde-increase-priority hakukohde-oid priority-number disabled?]
     priority-number
     [selected-hakukohde-decrease-priority hakukohde-oid priority-number disabled?]]))

(defn- selected-hakukohde-row
  [hakukohde-oid]
  (let [deleting? @(subscribe [:application/hakukohde-deleting? hakukohde-oid])
        prioritize-hakukohteet? @(subscribe [:application/prioritize-hakukohteet?])
        haku-editable? @(subscribe [:application/hakukohteet-editable?])
        hakukohde-editable? @(subscribe [:application/hakukohde-hakuaika-on? hakukohde-oid])]
    [:div.application__hakukohde-row.application__hakukohde-row--selected.animated
     {:class (if deleting?
               "fadeOut"
               "fadeIn")}
     (when prioritize-hakukohteet?
       [prioritize-hakukohde-buttons hakukohde-oid (not hakukohde-editable?)])
     [:div.application__hakukohde-row-text-container.application__hakukohde-row-text-container--selected
      [:div.application__hakukohde-selected-row-header
       @(subscribe [:application/hakukohde-label hakukohde-oid])]
      [:div.application__hakukohde-selected-row-description
       @(subscribe [:application/hakukohde-description hakukohde-oid])]
      (when (not hakukohde-editable?)
        [:div.application__hakukohde-selected-row-description
         [:span.application__hakukohde-sub-header-dates
          [:i.application__hakukohde-selected-check.zmdi.zmdi-lock]
          (get-translation :not-editable-application-period-ended)]])]
     (cond (and haku-editable? hakukohde-editable?) [selected-hakukohde-row-remove hakukohde-oid]
           (not hakukohde-editable?) [selected-hakukohde-disabled-row-remove hakukohde-oid]
           haku-editable? [selected-hakukohde-row-remove hakukohde-oid])]))

(defn- search-hit-hakukohde-row
  [hakukohde-oid]
  (let [hakukohde-selected? @(subscribe [:application/hakukohde-selected? hakukohde-oid])
        search-term         @(subscribe [:application/hakukohde-query])
        aria-header-id      (str "hakukohde-search-hit-header-" hakukohde-oid)
        aria-description-id (str "hakukohde-search-hit-description-" hakukohde-oid)
        hakukohde-hakuaika-on? @(subscribe [:application/hakukohde-hakuaika-on? hakukohde-oid])
        hakukohteet-full?   @(subscribe [:application/hakukohteet-full?])]
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
      (cond
        (not hakukohde-hakuaika-on?) [:div.application__hakukohde-row-additional-text-container
                                      {:aria-labelledby  aria-header-id
                                       :aria-describedby aria-description-id
                                       :aria-disabled    true}
                                      (get-translation :not-selectable-application-period-ended)]
        :else [:div.application__hakukohde-row-button-container
               (cond
                 hakukohde-selected? [:i.application__hakukohde-selected-check.zmdi.zmdi-check.zmdi-hc-2x]
                 hakukohteet-full? [:a.application__hakukohde-select-button.application__hakukohde-select-button--disabled
                                    {:role             "button"
                                     :aria-labelledby  aria-header-id
                                     :aria-describedby aria-description-id
                                     :aria-disabled    true}
                                    (get-translation :add)]
                 :else [:a.application__hakukohde-select-button
                        {:on-click           hakukohde-select-event-handler
                         :role               "button"
                         :data-hakukohde-oid hakukohde-oid
                         :aria-labelledby    aria-header-id
                         :aria-describedby   aria-description-id}
                        (get-translation :add)])])]))

(defn- hakukohde-selection-search
  []
  (let [hakukohde-query @(subscribe [:application/hakukohde-query])
        hakukohde-hits @(subscribe [:application/hakukohde-hits])]
    [:div.application__hakukohde-selection
     [:div.application__hakukohde-selection-search-arrow-up
      {:class (when @(subscribe [:application/prioritize-hakukohteet?])
                "application__hakukohde-selection-search-arrow-up--prioritized")}]
     [:div.application__hakukohde-selection-search-container
      [:div.application__hakukohde-selection-search-close-button
       [:a {:on-click hakukohde-search-toggle-event-handler}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
      [:div.application__hakukohde-selection-search-input.application__form-text-input-box
       [:input.application__form-text-input-in-box
        {:on-change   hakukohde-query-change-event-handler
         :title (get-translation :search-application-options)
         :placeholder (get-translation :search-application-options)
         :value       hakukohde-query}]
       (when (not (empty? hakukohde-query))
         [:div.application__form-clear-text-input-in-box
          [:a
           {:on-click hakukohde-query-clear-event-handler}
           [:i.zmdi.zmdi-close]]])]
      [:div.application__hakukohde-selection-search-results
       (if (and
             (empty? hakukohde-hits)
             (not (clojure.string/blank? hakukohde-query)))
         [:div.application__hakukohde-selection-search-no-hits (get-translation :no-hakukohde-search-hits)]
         (for [hakukohde-oid hakukohde-hits]
           ^{:key (str "found-hakukohde-row-" hakukohde-oid)}
           [search-hit-hakukohde-row hakukohde-oid]))]
      (when @(subscribe [:application/show-more-hakukohdes?])
        [:div.application__show_more_hakukohdes_container
         [:span.application__show_more_hakukohdes
          {:on-click #(dispatch [:application/show-more-hakukohdes])}
          (get-translation :show-more)]])]]))

(defn- hakukohde-selection-header
  [field-descriptor]
  [:div.application__wrapper-heading
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [scroll-to-anchor field-descriptor]])

(defn- select-new-hakukohde-row []
  (when @(subscribe [:application/hakukohteet-editable?])
    [:a.application__hakukohde-selection-open-search
     (if @(subscribe [:application/hakukohteet-full?])
       {:class "application__hakukohde-selection-open-search--disabled"}
       {:on-click hakukohde-search-toggle-event-handler})
     (get-translation :add-application-option)
     (when-let [max-hakukohteet @(subscribe [:application/max-hakukohteet])]
       (str " " (get-translation :applications_at_most max-hakukohteet)))]))

(defn hakukohteet
  [field-descriptor]
  [:div.application__wrapper-element.application__wrapper-element--border
   [hakukohde-selection-header field-descriptor]
   [:div.application__wrapper-contents.application__wrapper-contents--hakukohde
    [:div.application__hakukohde-selected-list
     (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
       ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
       [selected-hakukohde-row hakukohde-oid])]
    [select-new-hakukohde-row]
    (when @(subscribe [:application/show-hakukohde-search])
      [hakukohde-selection-search])]])
