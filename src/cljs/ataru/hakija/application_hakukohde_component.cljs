(ns ataru.hakija.application-hakukohde-component
  (:require
    [clojure.string]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [ataru.application-common.application-field-common :refer [scroll-to-anchor]]
    [ataru.util :as util]
    [ataru.translations.translation-util :as translations]
    [reagent.core :as r]
    [ataru.hakija.application-hakukohde-2nd-component :as hakukohde-2nd]
    [ataru.application-common.accessibility-util :as a11y]))

(def ^:private nav-sel
  ".application__search-hit-hakukohde-row--select-button, .application__search-hit-hakukohde-row[aria-selected='true']")

(defn hilighted-text->span [idx {:keys [text hilight]}]
  [(if hilight
     :span.application__search-hit-hakukohde-row--hilight-span.application__search-hit-hakukohde-row--hilight-span__hilight
     :span.application__search-hit-hakukohde-row--hilight-span)
   {:key (str "hilight-" idx)}
   text])

(defn hilight-text [text hilight-text]
  (if (some? text)
    (map-indexed hilighted-text->span (util/match-text text (clojure.string/split hilight-text #"\s+") false))
    [:span ""]))

(defn- hakukohde-remove-event-handler [e]
  (dispatch [:application/hakukohde-remove-selection (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-select-event-handler [e]
  (dispatch [:application/hakukohde-add-selection
             (.getAttribute (.-target e) "data-hakukohde-oid")]))

(defn- hakukohde-search-toggle-event-handler [_]
  (dispatch [:application/hakukohde-search-toggle]))

(defn- selected-hakukohde-row-remove
  [hakukohde-oid disabled?]
  (let [lang           @(subscribe [:application/form-language])
        hakukohde-name @(subscribe [:application/hakukohde-label hakukohde-oid])]
    [:button.application__selected-hakukohde-row--remove
     {:data-hakukohde-oid hakukohde-oid
      :disabled           disabled?
      :tab-index          (when (not disabled?) 0)
      :aria-label         (str (translations/get-hakija-translation :remove lang) " " hakukohde-name)
      :on-click           (when (not disabled?)
                            hakukohde-remove-event-handler)}
     (translations/get-hakija-translation :remove lang)]))

(defn- selected-hakukohde-increase-priority
  [hakukohde-oid priority-number disabled?]
  (let [increase-disabled? (or disabled? (= priority-number 1))
        lang               @(subscribe [:application/form-language])]
    [:span.application__selected-hakukohde-row--priority-increase
     (if increase-disabled?
       {:class "disabled"}
       {:disabled   (when disabled? "disabled")
        :tab-index  0
        :role       "button"
        :aria-label (translations/get-hakija-translation :increase-priority lang)
        :on-key-up #(when (a11y/is-enter-or-space? %)
                     (dispatch [:application/change-hakukohde-priority hakukohde-oid -1]))
        :on-click   #(dispatch [:application/change-hakukohde-priority hakukohde-oid -1])})]))

(defn- selected-hakukohde-decrease-priority
  [hakukohde-oid priority-number disabled?]
  (let [selected-hakukohteet @(subscribe [:application/selected-hakukohteet])
        decrease-disabled?   (or disabled? (= priority-number (count selected-hakukohteet)))
        lang                 @(subscribe [:application/form-language])]
    [:span.application__selected-hakukohde-row--priority-decrease
     (if decrease-disabled?
       {:class "disabled"}
       {:disabled   (when disabled? "disabled")
        :tab-index  0
        :role       "button"
        :aria-label (translations/get-hakija-translation :decrease-priority lang)
        :on-key-up #(when (a11y/is-enter-or-space? %)
                     (dispatch [:application/change-hakukohde-priority hakukohde-oid 1]))
        :on-click   #(dispatch [:application/change-hakukohde-priority hakukohde-oid 1])})]))

(defn- prioritize-hakukohde-buttons
  [hakukohde-oid disabled?]
  (let [priority-number @(subscribe [:application/hakukohde-priority-number hakukohde-oid])]
    [:div.application__selected-hakukohde-row--priority-changer
     [selected-hakukohde-increase-priority hakukohde-oid priority-number disabled?]
     priority-number
     [selected-hakukohde-decrease-priority hakukohde-oid priority-number disabled?]]))

(defn- offending-priorization [should-be-higher should-be-lower]
  (let [lang @(subscribe [:application/form-language])]
    [:div.application__selected-hakukohde-row--offending-priorization
     [:i.zmdi.zmdi-alert-circle]
     [:div
      [:div.application__selected-hakukohde-row--offending-priorization-heading
       (translations/get-hakija-translation :application-priorization-invalid lang)]
      [:div
       (first (translations/get-hakija-translation :should-be-higher-priorization-than lang))
       [:em (str "\"" @(subscribe [:application/hakukohde-label should-be-higher]) "\"")]
       (last (translations/get-hakija-translation :should-be-higher-priorization-than lang))
       [:em (str "\"" @(subscribe [:application/hakukohde-label should-be-lower]) "\"")]]]]))

(defn- selected-hakukohde-row
  [hakukohde-oid]
  (let [deleting?                          @(subscribe [:application/hakukohde-deleting? hakukohde-oid])
        prioritize-hakukohteet?            @(subscribe [:application/prioritize-hakukohteet?])
        haku-editable?                     @(subscribe [:application/hakukohteet-editable?])
        hakukohde-editable?                @(subscribe [:application/hakukohde-editable? hakukohde-oid])
        [should-be-lower should-be-higher] @(subscribe [:application/hakukohde-offending-priorization? hakukohde-oid])
        rajaavat-hakukohteet               @(subscribe [:application/rajaavat-hakukohteet hakukohde-oid])
        lang                               @(subscribe [:application/form-language])
        virkailija?                        @(subscribe [:application/virkailija?])
        archived?                          @(subscribe [:application/hakukohde-archived? hakukohde-oid])]
    [:div.application__selected-hakukohde-row.animated
     {:class (if deleting? "fadeOut" "fadeIn")}
     (when prioritize-hakukohteet?
       [prioritize-hakukohde-buttons hakukohde-oid (not hakukohde-editable?)])
     [:div.application__selected-hakukohde-row--content
      [:div.application__hakukohde-header
       (when (and virkailija? archived?)
         [:i.material-icons-outlined.arkistoitu
          {:title (translations/get-hakija-translation :archived lang)}
          "archive"])
       @(subscribe [:application/hakukohde-label hakukohde-oid])]
      [:div.application__hakukohde-description
       @(subscribe [:application/hakukohde-description hakukohde-oid])]
      (when (not hakukohde-editable?)
        [:div.application__hakukohde-application-period-ended
         [:i.zmdi.zmdi-lock]
         (translations/get-hakija-translation :not-editable-application-period-ended lang)])
      (when (not-empty rajaavat-hakukohteet)
        [:div.application__search-hit-hakukohde-row--limit-reached
         {:aria-live "polite"}
         [:h3.application__search-hit-hakukohde-row--limit-reached-heading
          (translations/get-hakija-translation :application-limit-reached-in-hakukohderyhma lang)]
         [:ul
          (doall (for [hakukohde rajaavat-hakukohteet]
                   ^{:key (str "limitting-hakukohde-" (:oid hakukohde))}
                   [:li.application__search-hit-hakukohde-row--limitting-hakukohde
                    @(subscribe [:application/hakukohde-label (:oid hakukohde)])]))]])
      (if (seq should-be-higher)
        (offending-priorization (first should-be-higher) hakukohde-oid)
        (when (seq should-be-lower)
          (offending-priorization hakukohde-oid (first should-be-lower))))]
     [:div.application__selected-hakukohde-row--buttons
      (cond (and haku-editable? hakukohde-editable?)
            [selected-hakukohde-row-remove hakukohde-oid false]
            (not hakukohde-editable?)
            [selected-hakukohde-row-remove hakukohde-oid true]
            haku-editable?
            [selected-hakukohde-row-remove hakukohde-oid false])]]))

(defn- search-hit-hakukohde-row
  [hakukohde-oid]
  (let [hakukohde-selected?  @(subscribe [:application/hakukohde-selected? hakukohde-oid])
        search-term          @(subscribe [:application/hakukohde-query])
        aria-header-id       (str "hakukohde-search-hit-header-" hakukohde-oid)
        aria-description-id  (str "hakukohde-search-hit-description-" hakukohde-oid)
        aria-limit-id        (str "hakukohde-search-hit-limit-" hakukohde-oid)
        hakukohde-editable?  @(subscribe [:application/hakukohde-editable? hakukohde-oid])
        hakukohteet-full?    @(subscribe [:application/hakukohteet-full? hakukohde-oid])
        rajaavat-hakukohteet @(subscribe [:application/rajaavat-hakukohteet hakukohde-oid])
        lang                 @(subscribe [:application/form-language])
        limit-reached?       (not-empty rajaavat-hakukohteet)
        disabled?            (boolean (or hakukohteet-full? (not hakukohde-editable?) limit-reached?))]
    [:div.application__search-hit-hakukohde-row
     {:role          "option"
      :aria-selected (if hakukohde-selected? "true" "false")
      :tab-index (when hakukohde-selected? 0)}
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       (hilight-text @(subscribe [:application/hakukohde-label hakukohde-oid]) search-term)]
      [:div.application__hakukohde-description
       {:id aria-description-id}
       (hilight-text @(subscribe [:application/hakukohde-description hakukohde-oid]) search-term)]
      (when (not hakukohde-editable?)
        [:div.application__hakukohde-application-period-ended
         [:i.zmdi.zmdi-lock]
         (translations/get-hakija-translation :not-selectable-application-period-ended lang)])
      (when (and (not hakukohde-selected?) limit-reached?)
        [:div.application__search-hit-hakukohde-row--limit-reached
         {:id        aria-limit-id
          :aria-live "polite"}
         [:h3.application__search-hit-hakukohde-row--limit-reached-heading
          (translations/get-hakija-translation :application-limit-reached-in-hakukohderyhma lang)]
         [:ul
          (doall (for [hakukohde rajaavat-hakukohteet]
                   ^{:key (str "limitting-hakukohde-" (:oid hakukohde))}
                   [:li.application__search-hit-hakukohde-row--limitting-hakukohde
                    @(subscribe [:application/hakukohde-label (:oid hakukohde)])]))]])]
     [:div.application__search-hit-hakukohde-row--buttons
      (if hakukohde-selected?
        [:i.zmdi.zmdi-check.zmdi-hc-2x.application__search-hit-hakukohde-row--selected-check
         {:aria-hidden "true"}]
        [:button.application__search-hit-hakukohde-row--select-button
         {:data-hakukohde-oid hakukohde-oid
          :on-click           (when-not disabled?
                               (fn [e]
                                 (let [btn     (.-currentTarget e)
                                       results (.closest btn ".application__hakukohde-selection-search-results")
                                       buttons (when results
                                                 (array-seq (.querySelectorAll results nav-sel)))
                                       idx     (when buttons
                                                 (count (take-while #(not= % btn) buttons)))]
                                   (dispatch-sync [:application/hakukohde-add-selection hakukohde-oid])
                                   (when (and results buttons (pos? (count buttons)))
                                     (r/after-render
                                       (fn []
                                         (let [new-buttons (array-seq (.querySelectorAll results nav-sel))
                                               n           (count new-buttons)]
                                           (when (pos? n)
                                             (.focus (nth new-buttons (min idx (dec n))))))))))))
          :aria-disabled      disabled?
          :aria-labelledby    aria-header-id
          :aria-describedby   (if limit-reached?
                                (str aria-description-id " " aria-limit-id)
                                aria-description-id)}
         (translations/get-hakija-translation :add lang)])]]))

(defn- find-button-idx [container]
  (let [buttons (array-seq (.querySelectorAll container nav-sel))
        active  (.-activeElement js/document)]
    [buttons (count (take-while #(not= % active) buttons))]))

(defn- hakukohde-selection-search
  []
  (let [hakukohde-hits         (subscribe [:application/hakukohde-hits])
        prioritize-hakukohteet (subscribe [:application/prioritize-hakukohteet?])
        search-input           (r/atom @(subscribe [:application/hakukohde-query]))
        lang                   (subscribe [:application/form-language])
        container-ref          (atom nil)
        focus-input            (fn []
                                 (when-let [c @container-ref]
                                   (when-let [inp (.querySelector c ".application__form-text-input-in-box")]
                                     (.focus inp))))
        on-escape              (fn []
                                 (let [parent (some-> @container-ref .-parentElement)]
                                   (dispatch [:application/hakukohde-search-toggle])
                                   (r/after-render
                                     (fn []
                                       (when parent
                                         (when-let [btn (.querySelector parent ".application__hakukohde-selection-open-search")]
                                           (.focus btn)))))))
        on-input-keydown       (fn [e]
                                 (case (.-key e)
                                   "ArrowDown" (do (.preventDefault e)
                                                   (when-let [c @container-ref]
                                                     (when-let [btn (.querySelector c nav-sel)]
                                                       (.focus btn))))
                                   "Escape"    (on-escape)
                                   nil))
        on-results-keydown     (fn [e]
                                 (let [key (.-key e)]
                                   (cond
                                     (#{"ArrowDown" "ArrowUp"} key)
                                     (do (.preventDefault e)
                                         (when-let [c @container-ref]
                                           (let [[buttons idx] (find-button-idx c)
                                                 n             (count buttons)]
                                             (cond
                                               (= key "ArrowDown")
                                               (if (< idx (dec n))
                                                 (.focus (nth buttons (inc idx)))
                                                 (focus-input))
                                               (= key "ArrowUp")
                                               (if (pos? idx)
                                                 (.focus (nth buttons (dec idx)))
                                                 (focus-input))))))
                                     (= key "Escape")
                                     (on-escape))))]
    (fn []
      [:div.application__hakukohde-selection
       {:ref #(reset! container-ref %)}
       [:div.application__hakukohde-selection-search-arrow-up
        {:class (when @prioritize-hakukohteet
                  "application__hakukohde-selection-search-arrow-up--prioritized")}]
       [:div.application__hakukohde-selection-search-container
        [:div.application__hakukohde-selection-search-close-button
         [:a {:aria-label (translations/get-hakija-translation :close-application-options @lang)
              :tab-index 0
              :on-key-up #(when (a11y/is-enter-or-space? %)
                            (hakukohde-search-toggle-event-handler %))
              :on-click hakukohde-search-toggle-event-handler}
          [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
        [:div.application__hakukohde-selection-search-input.application__form-text-input-box
         [:input.application__form-text-input-in-box
          {:on-change        #(do (reset! search-input (.-value (.-target %)))
                                  (dispatch [:application/hakukohde-query-change search-input]))
           :on-key-down      on-input-keydown
           :title            (translations/get-hakija-translation :search-application-options @lang)
           :placeholder      (translations/get-hakija-translation :search-application-options @lang)
           :value            @search-input
           :role             "combobox"
           :aria-haspopup    "listbox"
           :aria-expanded    "true"
           :aria-controls    "hakukohde-search-listbox"
           :aria-autocomplete "list"}]
         (when (not (empty? @search-input))
           [:div.application__form-clear-text-input-in-box
            [:a
             {:on-click #(do (reset! search-input "")
                             (dispatch [:application/hakukohde-query-change search-input]))}
             [:i.zmdi.zmdi-close]]])]
        [:p#hakukohde-search-results-label.visually-hidden
         (translations/get-hakija-translation :hakukohde-search-results @lang)]
        [:div.application__hakukohde-selection-search-results
         {:on-key-down     on-results-keydown
          :role            "listbox"
          :id              "hakukohde-search-listbox"
          :aria-labelledby "hakukohde-search-results-label"}
         (if (and
               (empty? @hakukohde-hits)
               (not (clojure.string/blank? @search-input)))
           [:div.application__hakukohde-selection-search-no-hits
            {:role "status"}
            (translations/get-hakija-translation :no-hakukohde-search-hits @lang)]
           (for [hakukohde-oid @hakukohde-hits]
             ^{:key (str "found-hakukohde-row-" hakukohde-oid)}
             [search-hit-hakukohde-row hakukohde-oid]))]
        (when @(subscribe [:application/show-more-hakukohdes?])
          [:div.application__show_more_hakukohdes_container
           [:span.application__show_more_hakukohdes
            {:tab-index 0
             :role "button"
             :on-click #(dispatch [:application/show-more-hakukohdes])}
            (translations/get-hakija-translation :show-more @lang)]])]])))

(defn- hakukohde-selection-header
  [field-descriptor]
  [:div.application__wrapper-heading
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [scroll-to-anchor field-descriptor]])

(defn- select-new-hakukohde-row []
  (let [lang @(subscribe [:application/form-language])]
    (when @(subscribe [:application/hakukohteet-editable?])
      (if @(subscribe [:application/hakukohteet-full?])
        (let [max-hakukohteet @(subscribe [:application/max-hakukohteet])]
          [:span.application__hakukohde-max-selected
           (translations/get-hakija-translation :applications_at_most lang max-hakukohteet)])
        [:div.application__hakukohde-selection-open-search-wrapper
         [:a.application__hakukohde-selection-open-search
          {:tab-index 0
           :on-key-up #(when (a11y/is-enter-or-space? %)
                         (hakukohde-search-toggle-event-handler %))
           :on-click hakukohde-search-toggle-event-handler}
          (translations/get-hakija-translation :add-application-option lang)]]))))

(defn hakukohteet
  [field-descriptor _]
  [:div.application__wrapper-element
   [hakukohde-selection-header field-descriptor]
   [:div.application__wrapper-contents.application__hakukohde-contents-wrapper
    [:div.application__form-field
     [:div.application__hakukohde-selected-list
      {:on-key-down (fn [e]
                      (let [key (.-key e)]
                        (when (#{"ArrowDown" "ArrowUp"} key)
                          (.preventDefault e)
                          (let [container (.-currentTarget e)
                                buttons   (array-seq (.querySelectorAll container ".application__selected-hakukohde-row--remove:not([disabled])"))
                                active    (.-activeElement js/document)
                                idx       (count (take-while #(not= % active) buttons))
                                n         (count buttons)]
                            (cond
                              (= key "ArrowDown") (when (< idx (dec n))
                                                    (.focus (nth buttons (inc idx))))
                              (= key "ArrowUp")   (when (pos? idx)
                                                    (.focus (nth buttons (dec idx)))))))))}
      (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
        ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
        [selected-hakukohde-row hakukohde-oid])]
     [select-new-hakukohde-row]
     (when @(subscribe [:application/show-hakukohde-search])
       [hakukohde-selection-search])]]])

(defn hakukohteet-picker
  [field-descriptor idx]
  (let [toisen-asteen-yhteishaku @(subscribe [:application/toisen-asteen-yhteishaku?])]
    (if toisen-asteen-yhteishaku
      [hakukohde-2nd/hakukohteet field-descriptor idx]
      [hakukohteet field-descriptor idx])))
