(ns ataru.hakija.application-hakukohde-2nd-component
  (:require [reagent.core :as r]
            [re-frame.core :refer [dispatch subscribe]]
            [ataru.application-common.components.button-component :as btn]
            [ataru.util :as u]
            [ataru.translations.translation-util :as translations]
            [ataru.translations.texts :refer [general-texts]]
            [ataru.application-common.accessibility-util :as a11y]))


(defn- koulutustyyppi-filter-row [koulutustyyppi-name is-selected on-change-fn]
  [:div.application__koulutustyypit-filter-row
   {:on-mouse-down #(.preventDefault %)}
   [:input {:id            (str koulutustyyppi-name "-checkbox")
            :aria-label    koulutustyyppi-name
            :type          "checkbox"
            :on-change     on-change-fn
            :checked       is-selected
            :on-mouse-down #(.preventDefault %)}]
   [:span {:on-mouse-down #(.preventDefault %)
           :on-click      on-change-fn
           :aria-hidden   true}
    koulutustyyppi-name]])

(defn- koulutustyyppi-btn [label is-open? on-click-fn]
  [:div
   [btn/button {:label    label
                :on-click on-click-fn
                :id       "koulutustyyppi-btn"}]
   (if is-open?
     [:i.zmdi.zmdi-caret-up]
     [:i.zmdi.zmdi-caret-down])])

(defn- koulutustyypit-filter [idx]
  (let [is-open (r/atom false)
        koulutustyypit (subscribe [:application/koulutustyypit])
        koulutustyypit-filters (subscribe [:application/active-koulutustyyppi-filters idx])
        lang (subscribe [:application/form-language])]
    (fn []
      (let [koulutustyypit-filters' @koulutustyypit-filters
            label (str
                    (translations/get-hakija-translation :filter-by-koulutustyyppi @lang)
                    (when (not-empty koulutustyypit-filters')
                      (str " (" (count koulutustyypit-filters') ")")))
            on-click-fn #(swap! is-open not)]
        [:div.application__hakukohde-2nd-row__hakukohde-koulutustyyppi
         [koulutustyyppi-btn label @is-open on-click-fn]
         (when @is-open
           [:div.application__koulutustyypit-filter-wrapper
            [:btn/button.a-button
             {:on-click   on-click-fn
              :id         "koulutustyyppi-close-btn"
              :aria-label (get (:close general-texts) @lang)}
              ^{:key :i.zmdi.zmdi-close} [:i.zmdi.zmdi-close]]
            (for [{uri :uri :as koulutustyyppi} @koulutustyypit]
              (let [is-selected (boolean (koulutustyypit-filters' uri))
                    on-select #(dispatch [:application/toggle-koulutustyyppi-filter idx uri])
                    label (u/non-blank-val (:label koulutustyyppi) [@lang :fi :sv :en])]
                ^{:key uri}
                [koulutustyyppi-filter-row label is-selected on-select]))])]))))

(defn hide-toast-after-delay []
  (dispatch [:application/hide-hakukohde-toast]))

(defn- start-toast-hide-timer []
  (js/setTimeout hide-toast-after-delay 3500))

(defn- search-hit-hakukohde-row
  [hakukohde-oid idx]
  (let [aria-header-id (str "hakukohde-search-hit-header-" hakukohde-oid)
        select-fn #(do
                     (dispatch [:application/hakukohde-query-process (atom "") idx])
                     (dispatch [:application/set-active-hakukohde-search nil])
                     (dispatch [:application/hakukohde-add-selection-2nd hakukohde-oid idx])
                     (dispatch [:application/show-hakukohde-toast @(subscribe [:application/hakukohde-label hakukohde-oid])])
                     (.focus (.getElementById js/document "valitut-hakukohteet") #js {:focusVisible true})
                     (start-toast-hide-timer)
                    )]
    [:div.application__search-hit-hakukohde-row-2nd
     {:on-mouse-down #(.preventDefault %)
      :tab-index 0
      :role "button"
      :on-key-up #(when (a11y/is-enter-or-space? %)
                       (select-fn))
      :on-click select-fn}
     [:div.application__search-hit-hakukohde-row--content
      [:div.application__hakukohde-header
       {:id aria-header-id}
       [:span.application__search-hit-hakukohde-row-2nd-span
        ^{:key :i.zmdi.zmdi-chevron-right} [:i.zmdi.zmdi-chevron-right]]
        @(subscribe [:application/hakukohde-label hakukohde-oid])]]]))
(defn- hakukohde-selection [idx]
  (let [search-input (r/atom "")
        hakukohde-hits (subscribe [:application/koulutustyyppi-filtered-hakukohde-hits idx])
        active-hakukohde-selection (subscribe [:application/active-hakukohde-search])
        lang (subscribe [:application/form-language])]
    (fn []
      [:div.application__hakukohde-2nd-row__hakukohde
       [:input.application__form-text-input-in-box
        {:auto-focus (> idx 0)
         :on-change   #(do (reset! search-input (.-value (.-target %)))
                           (dispatch [:application/hakukohde-query-change search-input idx])
                           (dispatch [:application/set-active-hakukohde-search idx])
                        )
         :placeholder (translations/get-hakija-translation :search-application-options-or-education @lang)
         :value       @search-input}]
       (when (= idx @active-hakukohde-selection)
         [:div.application__hakukohde-2nd-row__hakukohde-hits
          (for [hakukohde-oid @hakukohde-hits]
            ^{:key hakukohde-oid}
            [search-hit-hakukohde-row hakukohde-oid idx])])])))

(defn- remove-hakukohde [idx hakukohde-oid hakukohteet-count]
  (let [cannot-remove? (and (nil? hakukohde-oid) (zero? hakukohteet-count))
        lang @(subscribe [:application/form-language])
        label (translations/get-hakija-translation :remove lang)
        on-click-fn #(when-not cannot-remove? (dispatch [:application/hakukohde-remove-by-idx idx]))]
    [:div.application__hakukohde-2nd-row__selected-button-wrapper
     [btn/button {:label    label
                  :on-click on-click-fn}
      ^{:key :i.zmdi.zmdi-delete} [:i.zmdi.zmdi-delete]]]))

(defn- clear-hakukohde [idx]
  (let [lang @(subscribe [:application/form-language])
        on-click-fn #(dispatch [:application/hakukohde-clear-selection idx])]
    [:div.application__hakukohde-2nd-row__selected-button-wrapper
     [btn/button {:label    (translations/get-hakija-translation :clear lang)
                  :on-click on-click-fn}
      ^{:key :i.zmdi.zmdi-close-circle-o} [:i.zmdi.zmdi-close-circle-o]]]))

(defn- hakukohde-details [hakukohde-oid]
  (let [lang @(subscribe [:application/form-language])
        url @(subscribe [:application/hakukohde-konfo-url-by-oid hakukohde-oid])]
    [:div.application__hakukohde-2nd-row__selected-button-wrapper
       [:a {:href url :target :blank}
        [:i.zmdi.zmdi-open-in-new]
        (translations/get-hakija-translation :read-more lang)]]))

(defn- selected-hakukohde [idx hakukohde-oid]
  (let [editable? (subscribe [:application/hakukohteet-editable?])
        virkailija? (subscribe [:application/virkailija?])
        archived? (subscribe [:application/hakukohde-archived? hakukohde-oid])
        lang @(subscribe [:application/form-language])]
    [:div.application__hakukohde-2nd-row__selected-hakukohde-row
     [:div.application-hakukohde-2nd-row__name-wrapper
      {:tab-index 0}
      [:span
        (when (and @virkailija? @archived?)
          [:i.material-icons-outlined.arkistoitu
            {:title (translations/get-hakija-translation :archived lang)}
          "archive"])
       [:span.application__search-hit-hakukohde-row-2nd-span
        ^{:key :i.zmdi.zmdi-chevron-right} [:i.zmdi.zmdi-chevron-right]]
        @(subscribe [:application/hakukohde-name-label-by-oid hakukohde-oid])]
      [:span.application__search-hit-hakukohde-row-2nd-span
       ^{:key :i.zmdi.zmdi-chevron-right} [:i.zmdi.zmdi-chevron-right]]
      @(subscribe [:application/hakukohde-tarjoaja-name-label-by-oid hakukohde-oid])]
     [hakukohde-details hakukohde-oid]
     (when @editable?
       [clear-hakukohde idx])
     (when @editable?
       [remove-hakukohde idx])]))

(defn- hakukohde-priority [idx hakukohde-oid hakukohteet-count]
  (let [editable? (subscribe [:application/hakukohteet-editable?])
        increase-disabled (or (not @editable?) (= idx 0))
        decrease-disabled (or (not @editable?) (= idx (max 0 (dec hakukohteet-count))))
        change-priority-fn (fn [acc] (dispatch [:application/change-hakukohde-priority hakukohde-oid acc idx]))
        lang @(subscribe [:application/form-language])]
    [:div.application__hakukohde-2nd-row__hakukohde-order
     [:span
      [:i.zmdi.zmdi-caret-up.zmdi-hc-2x
       (if increase-disabled
         {:class "application__hakukohde-2nd-row__hakukohde-change-order-hidden"}
         {:tab-index 0
          :role "button"
          :aria-label (translations/get-hakija-translation :increase-priority lang)
          :on-key-up #(when (a11y/is-enter-or-space? %)
                        (change-priority-fn -1))
          :on-click #(change-priority-fn -1)})]]
     [:span (inc idx)]
     [:span
      [:i.zmdi.zmdi-caret-down.zmdi-hc-2x
       (if decrease-disabled
         {:class "application__hakukohde-2nd-row__hakukohde-change-order-hidden"}
         {:tab-index 0
          :role "button"
          :aria-label (translations/get-hakija-translation :decrease-priority lang)
          :on-key-up #(when (a11y/is-enter-or-space? %)
                        (change-priority-fn 1))
          :on-click #(change-priority-fn 1 )})]]]))

(defn- select-hakukohde [idx hakukohde-oid hakukohteet-count]
  [:div
   [remove-hakukohde idx hakukohde-oid hakukohteet-count]
   [:div.application__hakukohde-2nd-select
    [koulutustyypit-filter idx]
    [hakukohde-selection idx]]])

(defn- hakukohde-row [idx hakukohde-oid hakukohteet-count]
  [:div.application__hakukohde-2nd-row
   [hakukohde-priority idx hakukohde-oid hakukohteet-count]
   [:div.application__hakukohde-2nd-row__right
    (if hakukohde-oid
      [selected-hakukohde idx hakukohde-oid hakukohteet-count]
      [select-hakukohde idx hakukohde-oid hakukohteet-count])]])

(defn- hakukohde-max-amount-reached-message [max-hakukohteet]
  (let [lang @(subscribe [:application/form-language])
        [label1 label2] (translations/get-hakija-translation :select-max-n-application-options lang)]
    [:span.application__hakukohde-2nd-max-amount-reached
     [:i.zmdi.zmdi-info-outline]
     (str label1 max-hakukohteet label2)]))

(defn- hakukohde-max-amount-message [remaining-hakukohteet]
  (let [lang @(subscribe [:application/form-language])
        [n-label1 n-label2] (translations/get-hakija-translation :select-still-n-application-options lang)
        single-label (translations/get-hakija-translation :select-still-1-application-option lang)
        label (cond
                (= 1 remaining-hakukohteet) single-label
                (< 1 remaining-hakukohteet) (str n-label1 remaining-hakukohteet n-label2))]
    (when (int? remaining-hakukohteet)
      [:div.application__hakukohde-2nd-max-amount-msg 
       {:tab-index 0}
       label])))

(defn- lisaa-hakukohde-button [remaining-hakukohteet]
  (let [lang @(subscribe [:application/form-language])]
    [:<>
     [hakukohde-max-amount-message remaining-hakukohteet]
     [:button.application_add-hakukohde-row-button
      {:on-click #(dispatch [:application/add-empty-hakukohde-selection])}
      [:i.zmdi.zmdi-plus]
      (translations/get-hakija-translation :add-application-option lang)]]))

(defn- add-hakukohde-row [hakukohteet-full? max-hakukohteet hakukohteet-count]
  (let [remaining-hakukohteet (when (and (int? max-hakukohteet) (int? hakukohteet-count))
                                (- max-hakukohteet (max 1 hakukohteet-count)))]
    (cond
      (and hakukohteet-full? (int? max-hakukohteet)) [hakukohde-max-amount-reached-message max-hakukohteet]
      (not hakukohteet-full?) [lisaa-hakukohde-button remaining-hakukohteet]
      :else nil)))

(defn hakukohteet
  [_ _]
  (let [selected-hakukohteet (subscribe [:application/selected-hakukohteet])
        hakukohteet-count (count @selected-hakukohteet)
        hakukohteet-full? (subscribe [:application/hakukohteet-full?])
        max-hakukohteet (subscribe [:application/max-hakukohteet])
        editable? (subscribe [:application/hakukohteet-editable?])
        lang @(subscribe [:application/form-language])
        toast (subscribe [:application/hakukohde-lisatty-toast])
        {:keys [visible message]} @toast]
    [:div.application__wrapper-element
     [:div.application__wrapper-contents.application__hakukohde-2nd-contents-wrapper
      [:div.application__toast-hakukohde-lisatty
       {:role "alert"
       :aria-live "polite"
       :class  (if visible "show-valitut-hakukohteet" "hide-valitut-hakukohteet")}
       (str (translations/get-hakija-translation :application-study-program-added lang) message)]
      [:div.application__form-field
       [:div.application__hakukohde-selected-list
        {:id "valitut-hakukohteet"
         :tab-index 0
         :aria-label (str (translations/get-hakija-translation :application-selected-study-programms lang)  (str hakukohteet-count))}
        (for [idx (range (max hakukohteet-count 1))]
          (let [hakukohde-oid (nth @selected-hakukohteet idx nil)
                hakukohde-oid (when (not= "" hakukohde-oid) hakukohde-oid)]
            ^{:key (str "hakukohde-row-" idx)} [hakukohde-row idx hakukohde-oid hakukohteet-count]))]
       (when @editable?
         [add-hakukohde-row @hakukohteet-full? @max-hakukohteet hakukohteet-count])]]]))
