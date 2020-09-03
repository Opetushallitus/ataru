(ns ataru.virkailija.application.application-list.virkailija-application-list-view
  (:require [ataru.application.application-states :as application-states]
              [ataru.application.review-states :as review-states]
              [ataru.cljs-util :as cljs-util]
              [ataru.translations.texts :refer [general-texts]]
              [ataru.util :as util]
              [ataru.virkailija.application.view.virkailija-application-names :as names]
              [ataru.virkailija.dropdown :as dropdown]
              [ataru.virkailija.question-search.handlers :as qsh]
              [ataru.virkailija.question-search.view :as question-search]
              [ataru.virkailija.temporal :as temporal]
              [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
              [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as kevyt-valinta-i18n]
              [clojure.string :as string]
              [goog.string :as gstring]
              [reagent.core :as r]
              [re-frame.core :refer [subscribe dispatch]]))

(defn- application-list-basic-column-header [_ _]
  (let [application-sort (subscribe [:state-query [:application :sort]])]
    (fn [column-id heading]
      [:span.application-handling__basic-list-basic-column-header
       {:on-click #(dispatch [:application/update-sort column-id])}
       heading
       (when (= column-id (:order-by @application-sort))
         (if (= "desc" (:order @application-sort))
           [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow]
           [:i.zmdi.zmdi-chevron-up.application-handling__sort-arrow]))])))

(defn- created-time-column-header []
  (let [application-sort     (subscribe [:state-query [:application :sort]])
        selected-time-column (subscribe [:state-query [:application :selected-time-column]])]
    (fn []
      [:span
       {:class (if (= "created-time" @selected-time-column)
                 "application-handling__list-row--created-time"
                 "application-handling__list-row--submitted")}
       [:span.application-handling__basic-list-basic-column-header
        [:span.application-handling__created-time-column-header
         {:on-click #(dispatch [:application/toggle-shown-time-column])}
         (if (= "created-time" @selected-time-column)
           @(subscribe [:editor/virkailija-translation :last-modified])
           @(subscribe [:editor/virkailija-translation :submitted-at]))]
        " |"
        [:i.zmdi
         {:on-click #(dispatch [:application/update-sort @selected-time-column])
          :class    (if (= "desc" (:order @application-sort))
                      "zmdi-chevron-down application-handling__sort-arrow"
                      "zmdi-chevron-up application-handling__sort-arrow")}]]])))

(defn- application-attachment-states
  [application]
  (let [attachment-reviews (->> application
                                :application-attachment-reviews
                                (group-by (comp keyword :hakukohde)))
        hakukohteet        (conj (map keyword (:hakukohde application)) :form)]
    (reduce (fn [states-by-hakukohde hakukohde]
              (let [hakukohde-attachment-reviews (->> attachment-reviews hakukohde (map :state))
                    checked-attachments          (count (filter #(= "checked" %) hakukohde-attachment-reviews))
                    hakukohde-attachments        (count hakukohde-attachment-reviews)]
                (assoc states-by-hakukohde hakukohde
                       {:checked   checked-attachments
                        :unchecked (- hakukohde-attachments checked-attachments)})))
            {}
            hakukohteet)))

(defn- attachment-state-counts [states]
  [:span.application-handling__list-row--attachment-states
   (when (< 0 (:checked states))
     [:span.application-handling_list-row-attachment-state-counts.checked (:checked states)])
   (when (< 0 (:unchecked states))
     [:span.application-handling_list-row-attachment-state-counts.unchecked (:unchecked states)])])

(defn- hakukohde-review-state
  [hakukohde-reviews hakukohde-oid requirement]
  (:state (first (get hakukohde-reviews [hakukohde-oid requirement]))))

(defn- hakemuksen-valinnan-tila-sarake [{:keys [application-key
                                                application-hakukohde-reviews
                                                hakukohde-oid
                                                lang]}]
  (let [selection-state                                      (hakukohde-review-state
                                                               application-hakukohde-reviews
                                                               hakukohde-oid
                                                               "selection-state")
        kevyt-valinta-enabled-for-application-and-hakukohde? @(subscribe [:virkailija-kevyt-valinta/kevyt-valinta-enabled-for-application-and-hakukohde?
                                                                          application-key
                                                                          hakukohde-oid])]
    [:span.application-handling__hakukohde-selection-cell
     [:span.application-handling__hakukohde-selection.application-handling__count-tag
      [:span.application-handling__state-label
       {:class (str "application-handling__state-label--" (or selection-state "incomplete"))}]
      (if kevyt-valinta-enabled-for-application-and-hakukohde?
        (let [kevyt-valinta-property-value @(subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                        :kevyt-valinta/valinnan-tila
                                                        application-key
                                                        hakukohde-oid])
              translation-key              (kevyt-valinta-i18n/kevyt-valinta-value-translation-key
                                             :kevyt-valinta/valinnan-tila
                                             kevyt-valinta-property-value)]
          @(subscribe [:editor/virkailija-translation translation-key]))
        (or
          (application-states/get-review-state-label-by-name
            review-states/application-hakukohde-selection-states
            selection-state
            lang)
          @(subscribe [:editor/virkailija-translation :incomplete])))]]))

(defn- applications-hakukohde-rows
  [review-settings
   application
   filtered-hakukohde
   attachment-states
   select-application]
  (let [direct-form-application?      (empty? (:hakukohde application))
        application-hakukohde-oids    (if direct-form-application?
                                        ["form"]
                                        (:hakukohde application))
        application-hakukohde-reviews (group-by #(vector (:hakukohde %) (:requirement %))
                                                (:application-hakukohde-reviews application))
        lang                          (subscribe [:editor/virkailija-lang])
        selected-hakukohde-oids       (subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])]
    (into
      [:div.application-handling__list-row-hakukohteet-wrapper
       {:class (when direct-form-application? "application-handling__application-hakukohde-cell--form")}]
      (map
        (fn [hakukohde-oid]
          (let [processing-state       (hakukohde-review-state application-hakukohde-reviews hakukohde-oid "processing-state")
                show-state-email-icon? (and
                                         (< 0 (:new-application-modifications application))
                                         (= "information-request" processing-state))
                hakukohde-attachment-states ((keyword hakukohde-oid) attachment-states)]
            [:div.application-handling__list-row-hakukohde
             {:class (when (and (not direct-form-application?)
                                (some? @selected-hakukohde-oids)
                                (not (contains? @selected-hakukohde-oids hakukohde-oid)))
                       "application-handling__list-row-hakukohde--not-in-selection")}
             (when (not direct-form-application?)
               [:span.application-handling__application-hakukohde-cell
                {:class    (when @(subscribe [:application/hakukohde-selected-for-review? hakukohde-oid])
                             "application-handling__application-hakukohde-cell--selected")
                 :on-click (fn [evt]
                             (.preventDefault evt)
                             (.stopPropagation evt)
                             (select-application (:key application) (or filtered-hakukohde
                                                                        hakukohde-oid)))}
                [names/hakukohde-and-tarjoaja-name hakukohde-oid]])
             [:span.application-handling__application-hl
              {:class (when direct-form-application? "application-handling__application-hl--direct-form")}]
             (when (and (not= "form" hakukohde-oid)
                        (:attachment-handling review-settings true))
               [attachment-state-counts hakukohde-attachment-states])
             [:span.application-handling__hakukohde-state-cell
              [:span.application-handling__hakukohde-state.application-handling__count-tag
               [:span.application-handling__state-label
                {:class (str "application-handling__state-label--" (or processing-state "unprocessed"))}]
               (or
                 (application-states/get-review-state-label-by-name
                   review-states/application-hakukohde-processing-states
                   processing-state
                   @lang)
                 @(subscribe [:editor/virkailija-translation :unprocessed]))
               (when show-state-email-icon?
                 [:i.zmdi.zmdi-email.application-handling__list-row-email-icon])]]
             (when (:selection-state review-settings true)
               [hakemuksen-valinnan-tila-sarake {:application-key               (:key application)
                                                 :hakukohde-oid                 hakukohde-oid
                                                 :application-hakukohde-reviews application-hakukohde-reviews
                                                 :lang                          @lang}])]))
        application-hakukohde-oids))))

(defn- application-list-row [application selected? select-application]
  (let [selected-time-column    (subscribe [:state-query [:application :selected-time-column]])
        day-date-time           (-> (get application (keyword @selected-time-column))
                                    (temporal/str->googdate)
                                    (temporal/time->str)
                                    (string/split #"\s"))
        day                     (first day-date-time)
        date-time               (->> day-date-time (rest) (string/join " "))
        applicant               (str (-> application :person :last-name) ", " (-> application :person :preferred-name))
        review-settings         (subscribe [:state-query [:application :review-settings :config]])
        filtered-hakukohde      (subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])
        attachment-states       (application-attachment-states application)
        form-attachment-states  (:form attachment-states)]
    [:div.application-handling__list-row
     {:on-click #(select-application (:key application) @filtered-hakukohde false)
      :class    (string/join " " [(when selected?
                                            "application-handling__list-row--selected")
                                          (when (= "inactivated" (:state application))
                                            "application-handling__list-row--inactivated")])
      :id       (str "application-list-row-" (:key application))}
     [:div.application-handling__list-row-person-info
      [:span.application-handling__list-row--application-applicant
       [:span.application-handling__list-row--applicant-name (or applicant [:span.application-handling__list-row--applicant-unknown
                                                                            @(subscribe [:editor/virkailija-translation :unknown])])]
       [:span.application-handling__list-row--applicant-details (or (-> application :person :ssn) (-> application :person :dob))]]
      [:span.application-handling__list-row--application-time
       [:span.application-handling__list-row--time-day day]
       [:span date-time]]
      (when (:attachment-handling @review-settings true)
        [attachment-state-counts form-attachment-states])
      [:span.application-handling__list-row--state]
      (when (:selection-state @review-settings true)
        [:span.application-handling__hakukohde-selection-cell])]
     [applications-hakukohde-rows @review-settings application @filtered-hakukohde attachment-states select-application]]))

(defn application-list-contents [applications select-application]
  (let [selected-key (subscribe [:state-query [:application :selected-key]])
        expanded?    (subscribe [:state-query [:application :application-list-expanded?]])
        on-update    #(when (and @expanded? (not-empty applications))
                        (dispatch [:application/scroll-list-to-selected-or-previously-closed-application]))]
    (r/create-class
      {:component-did-update on-update
       :component-did-mount  on-update
       :reagent-render       (fn [applications]
                               (into [:div.application-handling__list
                                      {:class (str (when (= true @expanded?) "application-handling__list--expanded")
                                                   (when (> (count applications) 0) " animated fadeIn"))
                                       :id    "application-handling-list"}]
                                     (for [application applications
                                           :let [selected? (= @selected-key (:key application))]]
                                       [application-list-row application selected? select-application])))})))

(defn- toggle-state-filter!
  [hakukohde-filters states filter-kw filter-id selected?]
  (let [new-filter (if selected?
                     (remove #(= filter-id %) hakukohde-filters)
                     (conj hakukohde-filters filter-id))]
    (cljs-util/update-url-with-query-params
      {filter-kw (string/join ","
                                      (cljs-util/get-unselected-review-states
                                        new-filter
                                        states))})
    (dispatch [:state-update #(assoc-in % [:application filter-kw] new-filter)])
    (dispatch [:application/reload-applications])))

(defn- hakukohde-state-filter-controls
  [filter-kw title states state-counts-sub]
  (let [filter-sub           (subscribe [:state-query [:application filter-kw]])
        filter-opened        (r/atom false)
        toggle-filter-opened #(swap! filter-opened not)
        get-state-count      (fn [counts state-id] (or (get counts state-id) 0))
        lang                 (subscribe [:editor/virkailija-lang])
        has-more?            (subscribe [:application/has-more-applications?])]
    (fn []
      (let [all-filters-selected? (= (count @filter-sub)
                                     (count states))]
        [:span.application-handling__filter-state.application-handling__filter-state--application-state
         [:a.application-handling__basic-list-basic-column-header
          {:on-click toggle-filter-opened}
          title
          [:i.zmdi.zmdi-assignment-check.application-handling__filter-state-link-icon
           {:class (when-not all-filters-selected? "application-handling__filter-state-link-icon--enabled")}]]
         (when @filter-opened
           (into [:div.application-handling__filter-state-selection
                  [:div.application-handling__filter-state-selection-close-button-container
                   [:button.virkailija-close-button.application-handling__filter-state-selection-close-button
                    {:on-click #(reset! filter-opened false)}
                    [:i.zmdi.zmdi-close]]]
                  [:div.application-handling__filter-state-selection-row.application-handling__filter-state-selection-row--all
                   {:class (when all-filters-selected? "application-handling__filter-state-selected-row")}
                   [:label
                    [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                             :type      "checkbox"
                             :checked   all-filters-selected?
                             :on-change (fn [_]
                                          (cljs-util/update-url-with-query-params
                                            {filter-kw (if all-filters-selected?
                                                         (string/join "," (map first states))
                                                         nil)})
                                          (dispatch [:state-update #(assoc-in % [:application filter-kw]
                                                                              (if all-filters-selected?
                                                                                []
                                                                                (map first states)))])
                                          (dispatch [:application/reload-applications]))}]
                    [:span @(subscribe [:editor/virkailija-translation :all])]]]]
                 (mapv
                   (fn [[review-state-id review-state-label]]
                     (let [filter-selected? (contains? (set @filter-sub) review-state-id)]
                       [:div.application-handling__filter-state-selection-row
                        {:class (if filter-selected? "application-handling__filter-state-selected-row" "")}
                        [:label
                         [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                  :type      "checkbox"
                                  :checked   filter-selected?
                                  :on-change #(toggle-state-filter! @filter-sub states filter-kw review-state-id filter-selected?)}]
                         [:span (str (get review-state-label @lang)
                                     (when state-counts-sub
                                       (str " ("
                                            (get-state-count @state-counts-sub review-state-id)
                                            (when @has-more? "+")
                                            ")")))]]]))
                   states)))]))))

(defn- select-rajaava-hakukohde [opened?]
  (let [ryhman-ensisijainen-hakukohde @(subscribe [:state-query [:application :rajaus-hakukohteella-value]])]
    [:div.application-handling__ensisijaisesti-hakukohteeseen
     [:button.application-handling__ensisijaisesti-hakukohteeseen-popup-button
      {:on-click #(swap! opened? not)}
      (if (nil? ryhman-ensisijainen-hakukohde)
        @(subscribe [:editor/virkailija-translation :all-hakukohteet])
        (or @(subscribe [:application/hakukohde-name ryhman-ensisijainen-hakukohde])
            [:i.zmdi.zmdi-spinner.spin]))]
     (when @opened?
       (let [close                         #(reset! opened? false)
             [haku-oid hakukohderyhma-oid] @(subscribe [:state-query [:application :selected-hakukohderyhma]])
             ryhman-hakukohteet            @(subscribe [:application/selected-hakukohderyhma-hakukohteet])]
         [h-and-h/popup
          [h-and-h/search-input
           {:id                       (str haku-oid "-" hakukohderyhma-oid)
            :haut                     [{:oid         haku-oid
                                        :hakukohteet ryhman-hakukohteet}]
            :hakukohderyhmat          []
            :hakukohde-selected?      #(= ryhman-ensisijainen-hakukohde %)
            :hakukohderyhma-selected? (constantly false)}]
          nil
          [h-and-h/search-listing
           {:id                         (str haku-oid "-" hakukohderyhma-oid)
            :haut                       [{:oid         haku-oid
                                          :hakukohteet ryhman-hakukohteet}]
            :hakukohderyhmat            []
            :hakukohde-selected?        #(= ryhman-ensisijainen-hakukohde %)
            :hakukohderyhma-selected?   (constantly false)
            :on-hakukohde-select        #(do (close)
                                             (dispatch [:application/set-rajaus-hakukohteella %]))
            :on-hakukohde-unselect      #(do (close)
                                             (dispatch [:application/set-rajaus-hakukohteella nil]))
            :on-hakukohderyhma-select   (fn [])
            :on-hakukohderyhma-unselect (fn [])}]
          close]))]))

(defn- ensisijaisesti
  []
  (let [ensisijaisesti? @(subscribe [:application/ensisijaisesti?])]
    [:label.application-handling__filter-checkbox-label
     {:class (when ensisijaisesti? "application-handling__filter-checkbox-label--checked")}
     [:input.application-handling__filter-checkbox
      {:type      "checkbox"
       :checked   ensisijaisesti?
       :on-change #(dispatch [:application/set-ensisijaisesti
                              (not ensisijaisesti?)])}]
     [:span @(subscribe [:editor/virkailija-translation :ensisijaisesti])]]))

(defn- application-filter-checkbox
  [filters label kw state]
  (let [kw       (keyword kw)
        state    (keyword state)
        checked? (boolean (get-in @filters [kw state]))]
    [:label.application-handling__filter-checkbox-label
     {:key   (str "application-filter-" (name kw) "-" (name state))
      :class (when checked? "application-handling__filter-checkbox-label--checked")}
     [:input.application-handling__filter-checkbox
      {:type      "checkbox"
       :checked   checked?
       :on-change #(dispatch [:application/toggle-filter kw state])}]
     [:span label]]))

(defn- review-type-filter
  [filters lang [kw group-label states]]
  [:div.application-handling__filter-group
   {:key (str "application-filter-group-" kw)}
   [:div.application-handling__filter-group-title
    (util/non-blank-val group-label [lang :fi :sv :en])]
   (into
     [:div.application-handling__filter-group-checkboxes]
     (map
       (fn [[state checkbox-label]]
         (application-filter-checkbox filters
                                      (lang checkbox-label)
                                      kw
                                      state))
       states))])

(defn- application-base-education-filters
  [filters-checkboxes]
  (let [checkboxes            [[:pohjakoulutus_yo @(subscribe [:editor/virkailija-translation :pohjakoulutus_yo])]
                               [:pohjakoulutus_lk @(subscribe [:editor/virkailija-translation :pohjakoulutus_lk])]
                               [:pohjakoulutus_yo_kansainvalinen_suomessa @(subscribe [:editor/virkailija-translation :pohjakoulutus_yo_kansainvalinen_suomessa])]
                               [:pohjakoulutus_yo_ammatillinen @(subscribe [:editor/virkailija-translation :pohjakoulutus_yo_ammatillinen])]
                               [:pohjakoulutus_am @(subscribe [:editor/virkailija-translation :pohjakoulutus_am])]
                               [:pohjakoulutus_amt @(subscribe [:editor/virkailija-translation :pohjakoulutus_amt])]
                               [:pohjakoulutus_kk @(subscribe [:editor/virkailija-translation :pohjakoulutus_kk])]
                               [:pohjakoulutus_yo_ulkomainen @(subscribe [:editor/virkailija-translation :pohjakoulutus_yo_ulkomainen])]
                               [:pohjakoulutus_kk_ulk @(subscribe [:editor/virkailija-translation :pohjakoulutus_kk_ulk])]
                               [:pohjakoulutus_ulk @(subscribe [:editor/virkailija-translation :pohjakoulutus_ulk])]
                               [:pohjakoulutus_avoin @(subscribe [:editor/virkailija-translation :pohjakoulutus_avoin])]
                               [:pohjakoulutus_muu @(subscribe [:editor/virkailija-translation :pohjakoulutus_muu])]]
        all-filters-selected? (subscribe [:application/all-pohjakoulutus-filters-selected?])]
    [:div.application-handling__filter-group
     [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :base-education])]
     [:label.application-handling__filter-checkbox-label.application-handling__filter-checkbox-label--all
      {:key   (str "application-filter-pohjakoulutus-any")
       :class (when @all-filters-selected? "application-handling__filter-checkbox-label--checked")}
      [:input.application-handling__filter-checkbox
       {:type      "checkbox"
        :checked   @all-filters-selected?
        :on-change #(dispatch [:application/toggle-all-pohjakoulutus-filters @all-filters-selected?])}]
      [:span "Kaikki"]]
     (->> checkboxes
          (map (fn [[id label]] (application-filter-checkbox filters-checkboxes label :base-education id)))
          (doall))]))

(defn- filter-attachment-state-dropdown
  [field-id]
  (let [lang             @(subscribe [:editor/virkailija-lang])
        states           @(subscribe [:application/filter-attachment-review-states field-id])
        options          (map (fn [[state label]]
                                (let [checked? (get states state false)]
                                  [checked?
                                   (util/non-blank-val label [lang :fi :sv :en])
                                   [state checked?]]))
                              review-states/attachment-hakukohde-review-types)
        selected-options (filter first options)]
    [:div.application-handling__filters-attachment-attachments__dropdown
     [dropdown/multi-option
      (cond (seq (rest selected-options))
            @(subscribe [:editor/virkailija-translation :states-selected])
            (seq selected-options)
            (str @(subscribe [:editor/virkailija-translation :state])
                 ": "
                 (second (first selected-options)))
            :else
            @(subscribe [:editor/virkailija-translation :filter-by-state]))
      options
      (fn [[state checked?]]
        (dispatch [:application/set-filter-attachment-state field-id state (not checked?)]))]]))

(defn- filter-question-answer-dropdown
  [field-id]
  (let [form-key         @(subscribe [:application/selected-form-key])
        filtering        @(subscribe [:application/filter-question-answers-filtering-options field-id])
        field-options    @(subscribe [:application/form-field-options-labels form-key field-id])
        options          (mapv (fn [{:keys [value label]}]
                                 (let [checked? (get filtering value false)]
                                   [checked?
                                    label
                                    [value checked?]]))
                               field-options)
        selected-options (filter first options)]
    [:div.application-handling__filters-attachment-attachments__dropdown
     [dropdown/multi-option
      (cond (seq (rest selected-options))
            @(subscribe [:editor/virkailija-translation :question-answers-selected])
            (seq selected-options)
            (str @(subscribe [:editor/virkailija-translation :question-answer])
                 ": "
                 (second (first selected-options)))
            :else
            @(subscribe [:editor/virkailija-translation :filter-by-question-answer]))
      options
      (fn [[option-value checked?]]
        (dispatch [:application/set-question-answer-filtering-options field-id option-value (not checked?)]))]]))

(defn- question-filter-dropdown
  [form-key field-id]
  (let [field @(subscribe [:application/form-field form-key field-id])]
    (if (= (:fieldType field) "attachment")
      [filter-attachment-state-dropdown field-id]
      [filter-question-answer-dropdown field-id])))

(defn- form-fields-by-id []
  (let [form-key (subscribe [:application/selected-form-key])]
    (subscribe [:application/form-fields-by-id @form-key])))

(defn- application-filters
  []
  (let [filters-checkboxes                        (subscribe [:state-query [:application :filters-checkboxes]])
        applications-count                        (subscribe [:application/loaded-applications-count])
        fetching?                                 (subscribe [:application/fetching-applications?])
        enabled-filter-count                      (subscribe [:application/enabled-filter-count])
        review-settings                           (subscribe [:state-query [:application :review-settings :config]])
        selected-hakukohde-oid                    (subscribe [:state-query [:application :selected-hakukohde]])
        show-eligibility-set-automatically-filter (subscribe [:application/show-eligibility-set-automatically-filter])
        has-base-education-answers                (subscribe [:application/applications-have-base-education-answers])
        show-ensisijaisesti?                      (subscribe [:application/show-ensisijaisesti?])
        show-rajaa-hakukohteella?                 (subscribe [:application/show-rajaa-hakukohteella?])
        filters-changed?                          (subscribe [:application/filters-changed?])
        form-key                                  (subscribe [:application/selected-form-key])
        filter-questions                          (subscribe [:application/filter-questions])
        question-search-id                        :filters-attachment-search
        filters-visible                           (r/atom false)
        rajaava-hakukohde-opened?                 (r/atom false)
        filters-to-include                        #{:language-requirement :degree-requirement :eligibility-state :payment-obligation}
        lang                                      (subscribe [:editor/virkailija-lang])]
    (fn []
      [:span.application-handling__filters
       [:a
        {:on-click #(do
                      (dispatch [:application/undo-filters])
                      (swap! filters-visible not))}
        [:span
         (gstring/format "%s (%d"
                         @(subscribe [:editor/virkailija-translation :filter-applications])
                         @applications-count)]
        (when @fetching?
          [:span "+ "
           [:i.zmdi.zmdi-spinner.spin]])
        [:span ")"]]
       (when (pos? @enabled-filter-count)
         [:span
          [:span.application-handling__filters-count-separator "|"]
          [:a
           {:on-click #(dispatch [:application/remove-filters])}
           @(subscribe [:editor/virkailija-translation :remove-filters])
           " (" @enabled-filter-count ")"]])
       (when @filters-visible
         [:div.application-handling__filters-popup
          [:div.application-handling__filters-popup-close-button-container
           [:button.virkailija-close-button.application-handling__filters-popup-close-button
            {:on-click #(reset! filters-visible false)}
            [:i.zmdi.zmdi-close]]]
          [:div.application-handling__filters-popup-content-container
           [:div.application-handling__popup-column
            (when @show-ensisijaisesti?
              [:div.application-handling__filter-group
               [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :ensisijaisuus])]
               [ensisijaisesti]
               (when @show-rajaa-hakukohteella?
                 [select-rajaava-hakukohde rajaava-hakukohde-opened?])])
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :ssn])]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :without-ssn]) :only-ssn :without-ssn]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :with-ssn]) :only-ssn :with-ssn]]
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :identifying])]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :unidentified]) :only-identified :unidentified]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :identified]) :only-identified :identified]]
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :active-status])]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :active-status-active]) :active-status :active]
             [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :active-status-passive]) :active-status :passive]]]
           [:div.application-handling__popup-column
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :handling-notes])]
             (when (some? @selected-hakukohde-oid)
               [:div.application-handling__filter-hakukohde-name
                @(subscribe [:application/hakukohde-name @selected-hakukohde-oid])])
             (->> review-states/hakukohde-review-types
                  (filter (fn [[kw _ _]]
                            (and
                             (contains? filters-to-include kw)
                             (-> @review-settings (get kw) (false?) (not)))))
                  (map (partial review-type-filter filters-checkboxes @lang))
                  (doall))
             (when @show-eligibility-set-automatically-filter
               [:div.application-handling__filter-group
                [:div.application-handling__filter-group-title
                 @(subscribe [:editor/virkailija-translation :eligibility-set-automatically])]
                [:div.application-handling__filter-group-checkboxes
                 [application-filter-checkbox
                  filters-checkboxes
                  (-> general-texts :yes (get @lang))
                  :eligibility-set-automatically
                  :yes]
                 [application-filter-checkbox
                  filters-checkboxes
                  (-> general-texts :no (get @lang))
                  :eligibility-set-automatically
                  :no]]])]]
           (when @has-base-education-answers
             [:div.application-handling__popup-column.application-handling__popup-column--large
              [application-base-education-filters filters-checkboxes @lang]])]
          (when (some? @form-key)
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :submitted-content-search-label])]
             [:div.application-handling__filters-attachment-search-input
              [question-search/search-input
               @form-key
               question-search-id
               @(subscribe [:editor/virkailija-translation :submitted-content-search-placeholder])
               (not (empty? @filter-questions))
               (fn [db form-key]
                 (every-pred (qsh/field-type-filter-predicate ["attachment"
                                                               "dropdown"
                                                               "multipleChoice"
                                                               "singleChoice"])
                             (qsh/belongs-to-selected-filter-predicate db form-key)))]]
             (if (seq @filter-questions)
               [:div.application-handling__filters-attachment-attachments
                (into [:ul.application-handling__filters-attachment-attachments__list]
                      (map (fn [[field-id _]]
                             [:li.application-handling__filters-attachment-attachments__list-item
                              [:button.application-handling__filters-attachment-attachments__remove-button
                               {:on-click #(dispatch [:application/remove-question-filter (get @(form-fields-by-id) (keyword field-id))])}
                               [:i.zmdi.zmdi-close]]
                              [:span.application-handling__filters-attachment-attachments__label
                               @(subscribe [:application/form-field-label @form-key field-id])]
                              [question-filter-dropdown @form-key field-id]])
                           @filter-questions))]
               [:div.application-handling__filters-attachment-search-results
                [question-search/search-results
                 @form-key
                 question-search-id
                 #(do (dispatch [:question-search/clear-search-input @form-key question-search-id])
                      (dispatch [:application/add-question-filter %]))]])])
          [:div.application-handling__filters-popup-apply-button-container
           [:a.editor-form__control-button.editor-form__control-button--variable-width
            {:class    (if @filters-changed?
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--disabled")
             :on-click (fn [_]
                         (reset! filters-visible false)
                         (dispatch [:application/apply-filters]))}
            @(subscribe [:editor/virkailija-translation :filters-apply-button])]
           [:a.editor-form__control-button.editor-form__control-button--variable-width
            {:class    (if @filters-changed?
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--disabled")
             :on-click #(dispatch [:application/undo-filters])}
            @(subscribe [:editor/virkailija-translation :filters-cancel-button])]]])])))

(defn application-list-header [_]
  (let [review-settings (subscribe [:state-query [:application :review-settings :config]])]
    [:div.application-handling__list-header.application-handling__list-row
     [:span.application-handling__list-row--applicant
      [application-list-basic-column-header
       "applicant-name"
       @(subscribe [:editor/virkailija-translation :applicant])]
      [application-filters]]
     [created-time-column-header]
     (when (:attachment-handling @review-settings true)
       [:span.application-handling__list-row--attachment-state
        [hakukohde-state-filter-controls
         :attachment-state-filter
         @(subscribe [:editor/virkailija-translation :attachments])
         review-states/attachment-hakukohde-review-types-with-no-requirements
         (subscribe [:state-query [:application :attachment-state-counts]])]])
     [:span.application-handling__list-row--state
      [hakukohde-state-filter-controls
       :processing-state-filter
       @(subscribe [:editor/virkailija-translation :processing-state])
       review-states/application-hakukohde-processing-states
       (subscribe [:state-query [:application :review-state-counts]])]]
     (when (:selection-state @review-settings true)
       [:span.application-handling__list-row--selection
        [hakukohde-state-filter-controls
         :selection-state-filter
         @(subscribe [:editor/virkailija-translation :selection])
         review-states/application-hakukohde-selection-states
         (subscribe [:state-query [:application :selection-state-counts]])]])]))
