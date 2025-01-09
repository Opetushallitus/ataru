(ns ataru.virkailija.application.application-list.filters-view
  (:require [ataru.application.review-states :as review-states]
            [ataru.translations.texts :refer [general-texts]]
            [ataru.util :as util]
            [ataru.virkailija.dropdown :as dropdown]
            [ataru.virkailija.question-search.handlers :as qsh]
            [ataru.virkailija.question-search.view :as question-search]
            [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
            [clojure.string :as string]
            [goog.string :as gstring]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]))

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
                               [:pohjakoulutus_amp @(subscribe [:editor/virkailija-translation :pohjakoulutus_amp])]
                               [:pohjakoulutus_amt @(subscribe [:editor/virkailija-translation :pohjakoulutus_amt])]
                               [:pohjakoulutus_amv @(subscribe [:editor/virkailija-translation :pohjakoulutus_amv])]
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

(defn- valpas-link
  [organization-oid]
  (let [url (.url js/window "valpas.hakutilanne" (or organization-oid ""))]
    [:div.application-handling__filter-group.application-handling__filter-group__valpas-link
     [:span
      @(subscribe [:editor/virkailija-translation :valpas-hakutilanne-link-text-1])
      [:a
       {:id "valpas-hakutilanne-link"
        :href url
        :target "blank"}
       @(subscribe [:editor/virkailija-translation :valpas-hakutilanne-link-text-2])]]]))

(defn- harkinnanvaraisuus-filter
  []
  (let [filters-checkboxes     (subscribe [:state-query [:application :filters-checkboxes]])]
    [:div.application-handling__filter-group.application-handling__filter-group__harkinnanvaraiset
     [:div.application-handling__filter-group-heading
      @(subscribe [:editor/virkailija-translation :harkinnanvaraisuus])]
     [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :only-harkinnanvaraiset]) :harkinnanvaraisuus :only-harkinnanvaraiset]]))

(defn- school-and-class-filters
  []
  (let [opinto-ohjaaja-or-admin?   (subscribe [:editor/opinto-ohjaaja-or-admin?])
        only-opinto-ohjaaja?       (subscribe [:editor/all-organizations-have-only-opinto-ohjaaja-rights?])
        schools                    (subscribe [:application/schools-of-departure])
        filtered-schools           (subscribe [:application/schools-of-departure-filtered])
        selected-school            (subscribe [:application/pending-selected-school])
        classes-of-selected-school (subscribe [:application/classes-of-selected-school])
        pending-classes-of-school  (subscribe [:application/pending-classes-of-school])
        get-school-name            (fn [school]
                                     (some #(-> (:name school) %) [:fi :sv :en]))
        selected-school-name       (fn [school orgs]
                                     (->> orgs
                                          (filter #(= school (:oid %)))
                                          (first)
                                          (get-school-name)))
        set-school-filter          (fn [org]
                                     (dispatch [:application/set-school-filter (:oid org)]))]
    (fn []
      [:div.application-handling__popup-column.application-handling__popup-column--large
       [:div.application-handling__filter-group--other-application-information
        [:div.application-handling__filter-group-heading
         @(subscribe [:editor/virkailija-translation :other-application-info])]
        [:div.application-handling__filter-group.school-filter-group
         (when @opinto-ohjaaja-or-admin?
           [:div.application-handling__filter-group-title
            @(subscribe [:editor/virkailija-translation :applicants-school-of-departure])])
         (when @opinto-ohjaaja-or-admin?
           [:div
            (if (not @selected-school)
              [:input
               {:type        "text"
                :id          "school-search"
                :placeholder @(subscribe [:editor/virkailija-translation :search-placeholder])
                :on-change   (fn [event]
                               (let [value (-> event .-target .-value)]
                                 (dispatch [:editor/filter-organizations-for-school-of-departure value])))}]
              [:div.school-filter__selected-filter
               [:span
                {:title (selected-school-name @selected-school @filtered-schools)
                 :id    "selected-school"}
                (selected-school-name @selected-school @filtered-schools)]
               (when (not (and @only-opinto-ohjaaja?
                               (= (count @schools) 1)))
                 [:button.virkailija-close-button.application-handling__filters-popup-close-button
                  {:id       "remove-selected-school-button"
                   :on-click (fn [_]
                               (dispatch [:application/remove-selected-school-pending])
                               (dispatch [:editor/clear-filter-organizations-for-school-of-departure]))}
                  [:i.zmdi.zmdi-close]])])
            (when (and (not @selected-school)
                       (> (count @filtered-schools) 0))
              [:div.school-filter__options
               {:tab-index -1}
               (for [org @filtered-schools]
                 [:div.school-filter__option
                  {:on-click #(set-school-filter org)
                   :on-key-up (fn [event]
                                (when (= 13 (.-keyCode event))
                                  (set-school-filter org)))
                   :key (:oid org)
                   :id (str "school-filter-option-" (:oid org))}
                  [:span
                   {:title (get-school-name org)
                    :tab-index 0}
                   (get-school-name org)]])])])]
        [:div.application-handling__filter-group.class-filter-group
         (when @opinto-ohjaaja-or-admin?
           [:div.application-handling__filter-group-title
            @(subscribe [:editor/virkailija-translation :applicants-classes])])
         (when @opinto-ohjaaja-or-admin?
           (let [classes-options  (map (fn [luokka]
                                         (let [checked            (boolean (some #(= luokka %) @pending-classes-of-school))
                                               on-change-argument [luokka checked]]
                                           [checked luokka on-change-argument]))
                                       @classes-of-selected-school)
                 classes-label      (string/join ", " @pending-classes-of-school)
                 classes-on-change  (fn [[luokka checked]]
                                      (dispatch [:application/set-pending-classes-of-school luokka (not checked)]))]
             [dropdown/multi-option
              classes-label
              classes-options
              classes-on-change]))]
        (when @opinto-ohjaaja-or-admin?
          [valpas-link @selected-school])
        [harkinnanvaraisuus-filter]]])))

(defn application-filters
  []
  (let [filters-checkboxes                        (subscribe [:state-query [:application :filters-checkboxes]])
        applications-count                        (subscribe [:application/loaded-applications-count])
        fetching?                                 (subscribe [:application/fetching-applications?])
        enabled-filter-count                      (subscribe [:application/enabled-filter-count])
        review-settings                           (subscribe [:state-query [:application :review-settings :config]])
        kk-application-payment-required?          (subscribe [:application/kk-application-payment-haku-selected?])
        selected-hakukohde-oid                    (subscribe [:state-query [:application :selected-hakukohde]])
        show-eligibility-set-automatically-filter (subscribe [:application/show-eligibility-set-automatically-filter])
        has-base-education-answers                (subscribe [:application/applications-have-base-education-answers])
        show-ensisijaisesti?                      (subscribe [:application/show-ensisijaisesti?])
        show-rajaa-hakukohteella?                 (subscribe [:application/show-rajaa-hakukohteella?])
        filters-changed?                          (subscribe [:application/filters-changed?])
        form-key                                  (subscribe [:application/selected-form-key-for-search])
        filter-questions                          (subscribe [:application/filter-questions])
        tutu-form?                                (subscribe [:payment/tutu-form? @form-key])
        astu-form?                                (subscribe [:payment/astu-form? @form-key])
        opinto-ohjaaja-or-admin?                  (subscribe [:editor/opinto-ohjaaja-or-admin?])
        opo-and-hak-pal-paakayttaja?               (subscribe [:editor/all-organizations-have-opinto-ohjaaja-and-hakemuspalvelun-paakayttaja-rights?])
        question-search-id                        :filters-attachment-search
        filters-visible                           (r/atom false)
        rajaava-hakukohde-opened?                 (r/atom false)
        lang                                      (subscribe [:editor/virkailija-lang])
        toisen-asteen-yhteishaku-selected?        (subscribe [:application/toisen-asteen-yhteishaku-selected?])]
    (fn []
      (let [filters-to-include (if @kk-application-payment-required?
                                 #{:language-requirement :degree-requirement :eligibility-state :payment-obligation :kk-application-payment}
                                 #{:language-requirement :degree-requirement :eligibility-state :payment-obligation})]
        [:span.application-handling__filters
         [:a
          {:id       "open-application-filters"
           :on-click #(do
                        (when (and @opinto-ohjaaja-or-admin? @toisen-asteen-yhteishaku-selected?)
                          (if @opo-and-hak-pal-paakayttaja?
                            (dispatch [:application/do-organization-query-for-schools-of-departure-without-lahtokoulu ""])
                            (dispatch [:application/do-organization-query-for-schools-of-departure ""])))
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
               [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :active-status-passive]) :active-status :passive]]
              [:div.application-handling__filter-group
               [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :only-edited-hakutoiveet])]
               [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :only-edited-hakutoiveet-edited]) :only-edited-hakutoiveet :edited]
               [application-filter-checkbox filters-checkboxes @(subscribe [:editor/virkailija-translation :only-edited-hakutoiveet-unedited]) :only-edited-hakutoiveet :unedited]]]
             (when (not @toisen-asteen-yhteishaku-selected?)
               [:div.application-handling__popup-column
                [:div.application-handling__filter-group
                 [:h3.application-handling__filter-group-heading @(subscribe [:editor/virkailija-translation :handling-notes])]
                 (when (some? @selected-hakukohde-oid)
                   [:div.application-handling__filter-hakukohde-name
                    @(subscribe [:application/hakukohde-name @selected-hakukohde-oid])])
                 (->> (cond
                        @tutu-form? review-states/hakukohde-review-types-tutu
                        @astu-form? review-states/hakukohde-review-types-astu
                        @kk-application-payment-required? review-states/hakukohde-review-types-kk-application-payment
                        :else review-states/hakukohde-review-types-normal)
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
                      :no]]])]])
             (when @toisen-asteen-yhteishaku-selected?
               [school-and-class-filters])
             (when (and @has-base-education-answers (not @toisen-asteen-yhteishaku-selected?))
               [:div.application-handling__popup-column.application-handling__popup-column--large
                [application-base-education-filters filters-checkboxes @lang]])]
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
                               {:on-click #(dispatch [:application/remove-question-filter (get @(subscribe [:application/form-fields-by-id @form-key]) (keyword field-id))])}
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
                      (dispatch [:application/add-question-filter @form-key %]))]])]
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
              @(subscribe [:editor/virkailija-translation :filters-cancel-button])]]])]))))
