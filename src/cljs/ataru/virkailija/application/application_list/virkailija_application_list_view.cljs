(ns ataru.virkailija.application.application-list.virkailija-application-list-view
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.virkailija.application.view.virkailija-application-names :as names]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as kevyt-valinta-i18n]
            [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [ataru.constants :as constants]
            [ataru.virkailija.application.application-list.hakukohde-filters-view :refer [hakukohde-state-filter-controls]]
            [ataru.virkailija.application.application-list.filters-view :refer [application-filters]]))

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
             tutu-form? review-states/application-hakukohde-processing-states-tutu
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
