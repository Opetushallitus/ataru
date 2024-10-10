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
            [re-frame.core :refer [subscribe dispatch]]
            [ataru.constants :as constants]))

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

(defn- hakemuksen-vastaanoton-tila-sarake [{:keys [application-key
                                                hakukohde-oid]}]
  (let [kevyt-valinta-enabled-for-application-and-hakukohde? @(subscribe [:virkailija-kevyt-valinta/kevyt-valinta-enabled-for-application-and-hakukohde?
                                                                          application-key
                                                                          hakukohde-oid])
        kevyt-valinta-property-value                         (when kevyt-valinta-enabled-for-application-and-hakukohde?
                                                               @(subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                                            :kevyt-valinta/vastaanotto-tila
                                                                            application-key
                                                                            hakukohde-oid]))
        korkeakouluhaku? @(subscribe [:virkailija-kevyt-valinta-filter/korkeakouluhaku?])]
    [:span.application-handling__hakukohde-vastaanotto-cell
     {:data-test-id "list-hakukohde-vastaanotto-state"}
     [:span.application-handling__hakukohde-selection.application-handling__application-list-view-cell
      (if kevyt-valinta-enabled-for-application-and-hakukohde?
        (let [kevyt-valinta-property-exists? (some? @(subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key hakukohde-oid]))
              translation-key              (kevyt-valinta-i18n/kevyt-valinta-value-translation-key
                                             :kevyt-valinta/vastaanotto-tila
                                             kevyt-valinta-property-value
                                             korkeakouluhaku?)]
          [:<>
           @(subscribe [:editor/virkailija-translation translation-key])
           [:i.zmdi.zmdi-info.application-handling__vastaanoton-tila-info-ikoni
            {:class (if kevyt-valinta-property-exists?
                      "application-handling__valinnan-tila-info-ikoni--tiedot-valinnoista"
                      "application-handling__valinnan-tila-info-ikoni--tiedot-valinnoista-lataamatta")
             :title (if kevyt-valinta-property-exists?
                      @(subscribe [:editor/virkailija-translation :valinnan-tila-ladattu-valinnoista])
                      @(subscribe [:editor/virkailija-translation :valinnan-tila-ladataan-valinnoista]))}]])
        @(subscribe [:editor/virkailija-translation :incomplete]))]]))

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
                                                                          hakukohde-oid])
        kevyt-valinta-property-value                         (when kevyt-valinta-enabled-for-application-and-hakukohde?
                                                               @(subscribe [:virkailija-kevyt-valinta/kevyt-valinta-property-value
                                                                            :kevyt-valinta/valinnan-tila
                                                                            application-key
                                                                            hakukohde-oid]))]
    [:span.application-handling__hakukohde-selection-cell
     {:data-test-id "list-hakukohde-selection-state"}
     [:span.application-handling__hakukohde-selection.application-handling__application-list-view-cell
      (let [css-modifier-prefix (cond (and kevyt-valinta-enabled-for-application-and-hakukohde?
                                           (= "KESKEN" kevyt-valinta-property-value))
                                      "incomplete"

                                      kevyt-valinta-enabled-for-application-and-hakukohde?
                                      "processed"

                                      :else
                                      (or selection-state "incomplete"))]
        [:span.application-handling__state-label
         {:class (str "application-handling__state-label--" css-modifier-prefix)}])
      (if kevyt-valinta-enabled-for-application-and-hakukohde?
        (let [kevyt-valinta-property-exists? (some? @(subscribe [:virkailija-kevyt-valinta/valinnan-tulos-for-application application-key hakukohde-oid]))
              translation-key              (kevyt-valinta-i18n/kevyt-valinta-value-translation-key
                                             :kevyt-valinta/valinnan-tila
                                             kevyt-valinta-property-value)]
          [:<>
           @(subscribe [:editor/virkailija-translation translation-key])
           [:i.zmdi.zmdi-info.application-handling__valinnan-tila-info-ikoni
            {:class (if kevyt-valinta-property-exists?
                      "application-handling__valinnan-tila-info-ikoni--tiedot-valinnoista"
                      "application-handling__valinnan-tila-info-ikoni--tiedot-valinnoista-lataamatta")
             :title (if kevyt-valinta-property-exists?
                      @(subscribe [:editor/virkailija-translation :valinnan-tila-ladattu-valinnoista])
                      @(subscribe [:editor/virkailija-translation :valinnan-tila-ladataan-valinnoista]))}]])
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
        selected-hakukohde-oids       (subscribe [:application/hakukohde-oids-from-selected-hakukohde-or-hakukohderyhma])
        review-states-visible?        (subscribe [:application/review-states-visible?])]
    (into
      [:div.application-handling__list-row-hakukohteet-wrapper
       {:class (when direct-form-application? "application-handling__application-hakukohde-cell--form")}]
      (map
        (fn [hakukohde-oid]
          (let [processing-state       (hakukohde-review-state application-hakukohde-reviews hakukohde-oid "processing-state")
                show-state-email-icon? (and
                                         (< 0 (:new-application-modifications application))
                                         (= "information-request" processing-state))
                hakukohde-attachment-states ((keyword hakukohde-oid) attachment-states)
                rights-to-review-states-for-hakukohde @(subscribe [:application/rights-to-view-review-states-for-hakukohde? hakukohde-oid])]
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
              (when (and @review-states-visible?
                         rights-to-review-states-for-hakukohde)
                [:span.application-handling__hakukohde-state.application-handling__application-list-view-cell
                 {:data-test-id "list-hakukohde-handling-state"}
                 [:span.application-handling__state-label
                  {:class (str "application-handling__state-label--" (or processing-state "unprocessed"))}]
                 (or
                  (application-states/get-review-state-label-by-name
                   review-states/application-hakukohde-processing-states
                   processing-state
                   @lang)
                  @(subscribe [:editor/virkailija-translation :unprocessed]))
                 (when show-state-email-icon?
                   [:i.zmdi.zmdi-email.application-handling__list-row-email-icon])])]
             (when (:selection-state review-settings true)
               (if (and @review-states-visible?
                        rights-to-review-states-for-hakukohde)
                 [hakemuksen-valinnan-tila-sarake
                  {:application-key               (:key application)
                   :hakukohde-oid                 hakukohde-oid
                   :application-hakukohde-reviews application-hakukohde-reviews
                   :lang                          @lang}]
                 [:span.application-handling__hakukohde-selection-cell]))

             (when (:vastaanotto-state review-settings true)
               (if (and @review-states-visible?
                        rights-to-review-states-for-hakukohde)
                 [hakemuksen-vastaanoton-tila-sarake
                  {:application-key (:key application)
                   :hakukohde-oid   hakukohde-oid}]
                 [:span.application-handling__hakukohde-selection-cell]))]))
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
        form-attachment-states  (:form attachment-states)
        haku-name               (subscribe [:application/haku-name (:haku application)])
        haku-heading-data       (subscribe [:application/list-heading-data-for-haku])]
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
       [:span.application-handling__list-row--applicant-details (or (-> application :person :ssn) (-> application :person :dob))]
       (cond
         (= (:tunnistautuminen application) constants/auth-type-strong)
         [:span.application-handling__list-row--tunnistautunut-icon
          [:img.logo-suomi-fi
           {:title @(subscribe [:editor/virkailija-translation :ht-hakenut-vahvasti-tunnistautuneena])
            :src "/lomake-editori/images/suomifi_16x16.svg"}]]
         ;(= (:tunnistautuminen application) "eidas")
         ;[:span.application-handling__list-row--tunnistautunut-icon
         ; [:img.logo-suomi-fi
         ;  {:title @(subscribe [:editor/virkailija-translation :ht-eidas-tunnistautunut])
         ;   :src "/lomake-editori/images/suomifi_16x16.svg"}]]
         )]
      [:span.application-handling__list-row--application-time
       [:span.application-handling__list-row--time-day day]
       [:span date-time]]
      (when (:attachment-handling @review-settings true)
        [attachment-state-counts form-attachment-states])
      [:span.application-handling__list-row--state]
      (when (:selection-state @review-settings true)
        [:span.application-handling__hakukohde-selection-cell])
      (when (:vastaanotto-state @review-settings true)
        [:span.application-handling__hakukohde-selection-cell])]
     ; Only show haku information per application row when there is no single haku selected in the list.
     (when (not @haku-heading-data)
       [:div.application-handling__list-row-haku-info
        [:span.application-handling__list-row--haku-name @haku-name]])
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

(defn- hakukohde-state-filter-controls-title
  [{:keys [title on-click all-filters-selected?]}]
  [:a.application-handling__basic-list-basic-column-header
   {:on-click on-click}
   title
   [:i.zmdi.zmdi-assignment-check.application-handling__filter-state-link-icon
    {:class (when-not all-filters-selected? "application-handling__filter-state-link-icon--enabled")}]])

(defn- hakukohde-state-filter-controls
  []
  (let [filter-opened        (r/atom false)
        toggle-filter-opened #(swap! filter-opened not)
        get-state-count      (fn [counts state-id] (or (get counts state-id) 0))]
    (fn [{:keys [title
                 states
                 state-counts-subs
                 filter-titles]}]
      (let [lang                  @(subscribe [:editor/virkailija-lang])
            has-more?             @(subscribe [:application/has-more-applications?])
            kk? @(subscribe [:virkailija-kevyt-valinta-filter/korkeakouluhaku?])
            all-filters-selected? (->> (keys states)
                                       (map (fn [filter-kw]
                                              [filter-kw @(subscribe [:state-query [:application filter-kw]])]))
                                       (every? (fn [[filter-kw filter-sub]]
                                                 (= (if (and (not kk?)
                                                             (= :kevyt-valinta-vastaanotto-state-filter filter-kw)) ;one less vastaanotto state for non-kk
                                                      (count (filter #(not= "EHDOLLISESTI_VASTAANOTTANUT" %) filter-sub))
                                                      (count filter-sub))
                                                    (-> states filter-kw count)))))
            all-counts-zero?      (->> (keys states)
                                       (every? (fn [filter-kw]
                                                 (let [state-counts-sub (some-> state-counts-subs filter-kw)]
                                                   (or (not state-counts-sub)
                                                       (every? (fn [[review-state-id]]
                                                                  (= 0 (get-state-count state-counts-sub review-state-id)))
                                                                  (filter-kw states)))))))]
        [:div.application-handling__filter-state.application-handling__filter-state--application-state
         [hakukohde-state-filter-controls-title
          {:title                 title
           :on-click              toggle-filter-opened
           :all-filters-selected? all-filters-selected?}]
         (when @filter-opened
           [:div.application-handling__filter-state-selection
            (->> (keys states)
                 (filter (fn [filter-kw]
                           (let [state-counts-sub (some-> state-counts-subs filter-kw)]
                             (or (not state-counts-sub)
                                 (some (fn [[review-state-id]]
                                         (or all-counts-zero?
                                             (< 0 (get-state-count state-counts-sub review-state-id))))
                                       (filter-kw states))))))
                 (map (fn [filter-kw]
                        (let [filter-sub                     @(subscribe [:state-query [:application filter-kw]])
                              all-filters-of-state-selected? (= (if (and (not kk?)
                                                                         (= :kevyt-valinta-vastaanotto-state-filter filter-kw)) ;one less vastaanotto state for non-kk
                                                                  (count (filter #(not= "EHDOLLISESTI_VASTAANOTTANUT" %) filter-sub))
                                                                  (count filter-sub))
                                                                (-> states filter-kw count))
                              state-counts-sub               (some-> state-counts-subs filter-kw)]
                          (into ^{:key (str "filter-state-column-" filter-kw)}
                                [:div.application-handling__filter-state-selection-column
                                 (when-let [translation-key (filter-kw filter-titles)]
                                   [:div.application-handling__filter-state-selection-row-header
                                    @(subscribe [:editor/virkailija-translation translation-key])])
                                 [:div.application-handling__filter-state-selection-row.application-handling__filter-state-selection-row--all
                                  {:class (when all-filters-of-state-selected? "application-handling__filter-state-selected-row")}
                                  [:label
                                   [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                            :type      "checkbox"
                                            :checked   all-filters-of-state-selected?
                                            :on-change (fn [_]
                                                         (cljs-util/update-url-with-query-params
                                                           {filter-kw (if all-filters-of-state-selected?
                                                                        (string/join "," (->> states filter-kw (map first)))
                                                                        nil)})
                                                         (dispatch [:state-update #(assoc-in % [:application filter-kw]
                                                                                             (if all-filters-of-state-selected?
                                                                                               []
                                                                                               (->> states filter-kw (map first))))])
                                                         (dispatch [:application/reload-applications]))}]
                                   [:span @(subscribe [:editor/virkailija-translation :all])]]]]
                                (mapv
                                  (fn [[review-state-id review-state-label]]
                                    (let [filter-selected? (contains? (set filter-sub) review-state-id)]
                                      [:div.application-handling__filter-state-selection-row
                                       {:class (if filter-selected? "application-handling__filter-state-selected-row" "")}
                                       [:label
                                        [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                                 :type      "checkbox"
                                                 :checked   filter-selected?
                                                 :on-change #(toggle-state-filter! filter-sub (filter-kw states) filter-kw review-state-id filter-selected?)}]
                                        [:span (str (get review-state-label lang)
                                                    (when state-counts-sub
                                                      (str " ("
                                                           (get-state-count state-counts-sub review-state-id)
                                                           (when has-more? "+")
                                                           ")")))]]]))
                                  (filter-kw states))))))
                 doall)
            [:div.application-handling__filter-state-selection-close-button-container
             [:button.virkailija-close-button.application-handling__filter-state-selection-close-button
              {:on-click #(reset! filter-opened false)}
              [:i.zmdi.zmdi-close]]]])]))))

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
        form-key                                  (subscribe [:application/selected-form-key-for-search])
        filter-questions                          (subscribe [:application/filter-questions])
        tutu-form?                                (subscribe [:payment/tutu-form? @form-key])
        astu-form?                                (subscribe [:payment/astu-form? @form-key])
        opinto-ohjaaja-or-admin?                  (subscribe [:editor/opinto-ohjaaja-or-admin?])
        question-search-id                        :filters-attachment-search
        filters-visible                           (r/atom false)
        rajaava-hakukohde-opened?                 (r/atom false)
        filters-to-include                        #{:language-requirement :degree-requirement :eligibility-state :payment-obligation}
        lang                                      (subscribe [:editor/virkailija-lang])
        toisen-asteen-yhteishaku-selected?        (subscribe [:application/toisen-asteen-yhteishaku-selected?])]
    (fn []
      [:span.application-handling__filters
       [:a
        {:id       "open-application-filters"
         :on-click #(do
                      (when (and @opinto-ohjaaja-or-admin? @toisen-asteen-yhteishaku-selected?)
                        (dispatch [:application/do-organization-query-for-schools-of-departure ""]))
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
                      @tutu-form? review-states/hakukohde-review-types
                      @astu-form? review-states/hakukohde-review-types-astu
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
            @(subscribe [:editor/virkailija-translation :filters-cancel-button])]]])])))

(defn application-list-header [_]
  (let [review-settings (subscribe [:state-query [:application :review-settings :config]])
        form-key        @(subscribe [:application/selected-form-key])
        tutu-form?       @(subscribe [:payment/tutu-form? form-key])
        astu-form?       @(subscribe [:payment/astu-form? form-key])
        korkeakouluhaku? @(subscribe [:virkailija-kevyt-valinta-filter/korkeakouluhaku?])
        review-states-visible? (subscribe [:application/review-states-visible?])]
    [:div.application-handling__list-header.application-handling__list-row
     [:span.application-handling__list-row--applicant
      [application-list-basic-column-header
       "applicant-name"
       @(subscribe [:editor/virkailija-translation :applicant])]
      [application-filters]]
     [created-time-column-header]
     (when (:attachment-handling @review-settings true)
       [:div.application-handling__list-row--attachment-state
        [hakukohde-state-filter-controls
         {:title
          @(subscribe [:editor/virkailija-translation :attachments])
          :states
          {:attachment-state-filter
           review-states/attachment-hakukohde-review-types-with-no-requirements}
          :state-counts-subs
          {:attachment-state-filter
           @(subscribe [:state-query [:application :attachment-state-counts]])}}]])
     [:div.application-handling__list-row--state
      (when @review-states-visible?
        {:data-test-id "processing-state-filter"}
        [hakukohde-state-filter-controls
         {:title
          @(subscribe [:editor/virkailija-translation :processing-state])
          :states
          {:processing-state-filter
           (cond
             tutu-form? review-states/application-hakukohde-processing-states
             astu-form? review-states/application-hakukohde-processing-states-astu
             :else review-states/application-hakukohde-processing-states-normal)}
          :state-counts-subs
          {:processing-state-filter
           @(subscribe [:state-query [:application :review-state-counts]])}}])]
     (when (:selection-state @review-settings true)
       [:div.application-handling__list-row--selection
        (when @review-states-visible?
          {:data-test-id "selection-state-filter"}
          [hakukohde-state-filter-controls
           {:title
            @(subscribe [:editor/virkailija-translation :selection])
            :filter-titles
            {:selection-state-filter
             :valintakasittelymerkinta
             :kevyt-valinta-selection-state-filter
             :valinnan-tila}
            :states
            {:selection-state-filter
             review-states/application-hakukohde-selection-states
             :kevyt-valinta-selection-state-filter
             review-states/kevyt-valinta-valinnan-tila-selection-states}
            :state-counts-subs
            {:selection-state-filter
             @(subscribe [:state-query [:application :selection-state-counts]])
             :kevyt-valinta-selection-state-filter
             @(subscribe [:state-query [:application :kevyt-valinta-selection-state-counts]])}}])])
     (when (:selection-state @review-settings true)
       [:div.application-handling__list-row--vastaanotto
        (when @review-states-visible?
          {:data-test-id "vastaanotto-state-filter"}
          [hakukohde-state-filter-controls
           {:kk? korkeakouluhaku?
            :title
            @(subscribe [:editor/virkailija-translation :vastaanotto])
            :filter-titles
            {:kevyt-valinta-vastaanotto-state-filter
             :vastaanotto}
            :states
            {:kevyt-valinta-vastaanotto-state-filter
             (review-states/kevyt-valinta-vastaanoton-tila-selection-states korkeakouluhaku?)}
            :state-counts-subs
            {:kevyt-valinta-vastaanotto-state-filter
             @(subscribe [:state-query [:application :kevyt-valinta-vastaanotto-state-counts]])}}])])]))
