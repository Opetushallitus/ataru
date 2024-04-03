(ns ataru.virkailija.application.view
  (:require [ataru.cljs-util :as cljs-util]
            [ataru.util :refer [assoc?]]
            [ataru.virkailija.application.application-list.virkailija-application-list-view :as application-list]
            [ataru.virkailija.application.application-review-view :as application-review]
            [ataru.virkailija.application.application-search-control :refer [application-search-control]]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.attachments.attachments-tab-view :refer [attachments-tab-view]]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-subs]
            [ataru.virkailija.application.attachments.virkailija-attachment-handlers]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs]
            [ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-handlers]
            [ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-view :as mass-information-request-view]
            [ataru.virkailija.application.mass-review.virkailija-mass-review-view :as mass-review]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.grades-view :refer [grades]]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-subs]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.valinnat-view :refer [valinnat]]
            [ataru.virkailija.application.view.application-heading :refer [application-heading]]
            [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

(defn- accordion-heading-id [id] (str "accordion-heading_" id))
(defn- accordion-content-id [id] (str "accordion-content_" id))
(defn- checkbox-name [id] (str "checkbox_" id))

(defn excel-checkbox-on-change [e]
  (dispatch [:application/excel-request-filter-changed
             (-> e .-target .-value)]))

(defn excel-checkbox [id]
  (let [checked? @(subscribe [:application/excel-request-filter-value id])]
    [:input
     {:type "checkbox"
      :id (checkbox-name id)
      :value id
      :on-change excel-checkbox-on-change
      :checked (boolean checked?)}]))

(defn excel-checkbox-control
  [id title]
  [:span.application-handling__excel-checkbox-control
   [excel-checkbox id title]
   (when title [:label {:for (checkbox-name id)} title])])

(defn use-excel-download-mode-state []
  [(subscribe [:application/excel-download-mode])
   (fn [mode] (dispatch [:application/change-excel-download-mode mode]))])

(defn- classes [& cs] (str/join " " (vec cs)))

(defn- accordion-heading [id title folded?]
  (let [click-action (if folded?
                       #(dispatch [:editor/unfold id])
                       #(dispatch [:editor/fold id]))]
    [:h4.application-handling__excel-accordion-heading-wrapper
     [excel-checkbox id]
     [:button.application-handling__excel-accordion-header-button
      {:id (accordion-heading-id id)
       :type "button"
       "aria-expanded" (not folded?)
       "aria-controls" (accordion-content-id id)
       :on-click click-action}
      [:span.excel-accordion-heading-text title]

      [:i
       {:class (classes "zmdi"
                        (if folded? "zmdi-chevron-down" "zmdi-chevron-up"))}]]]))

(defn excel-accordion
  [id title content]
  (let [folded? @(subscribe [:editor/folded? id])]
    [:div.application-handling__excel-accordion-group
     ^{:key (str "accordion_" id)}
     [accordion-heading id title folded?]
     [:div.application-handling__excel-accordion-content
      {:id (accordion-content-id id)
       :role "region"
       :style {:display (when folded? "none")}
       "aria-labelledby" (accordion-heading-id id)}
      ^{:key (accordion-content-id id)} content]]))


(defn get-form-checkbox-filters
  ([form-content parent-id level parent-index-acc]
   (if (empty? form-content)
     nil
     (reduce (fn [acc item]
               (let [index-acc (+ parent-index-acc (count acc))
                     children (get-form-checkbox-filters (:children item) (or parent-id (:id item)) (inc level) (inc index-acc))]
                 (if (or (= (:fieldClass item) "infoElement")
                         (and (= (:fieldClass item) "wrapperElement") (empty? children))
                         (:exclude-from-answers item)
                         (contains?  #{"hakukohteet" "lupatiedot"} (:id item))
                         (= (:module item) "person-info"))
                   acc
                   (merge acc children (when (or (= level 0) (not-any? #(= (:fieldClass item) %) #{"questionGroup" "wrapperElement"}))
                                         {(:id item) (-> {:id (:id item)
                                                          :index index-acc
                                                          :label (:label item)}
                                                         (assoc? :parent-id parent-id)
                                                         (assoc? :child-ids (->> children
                                                                                 (map second)
                                                                                 (sort-by :index)
                                                                                 (map :id))))})))))
             {}
             form-content)))
  ([form-content]
   (get-form-checkbox-filters form-content nil 0 0)))

(defn get-label-trans [l lng default]
  (let [label (into {} (filter #(not-empty (second %)) l))]
    (or (get label lng) (get label :fi) (get label :sv) (get label :en) default)))

(defn init-excel-checkbox-filters [form-content]
  (let [filter-defs (get-form-checkbox-filters form-content)]
    (dispatch [:application/excel-request-filters-init filter-defs])
    filter-defs))

(defn- excel-valitse-tiedot-content []
  (let [form-key @(subscribe [:application/selected-form-key])
        form-content @(subscribe [:state-query [:forms form-key :content]])
        filters (init-excel-checkbox-filters form-content)
        top-filters (->> filters
                         (filter #(not (:parent-id (second %))))
                         (map second)
                         (sort-by :index))]
    (fn []
      [:div.application-handling__excel-tiedot
       [:div.application-handling__excel-request-margins
        (->> top-filters
             (map (fn [section]
                    ^{:key (str (:id section) "_section")}
                    [excel-accordion (:id section) (get-label-trans (:label section) :fi (:id section))
                     [:div.application-handling__excel-accordion-checkbox-col
                      (map (fn [child-id]
                             (let [sub-filter (get-in filters [child-id])]
                               ^{:key (str child-id "_checkbox")}
                               [excel-checkbox-control
                                child-id
                                (get-label-trans (:label sub-filter) :fi (:id sub-filter))]))
                           (:child-ids section))]])))]])))

(defn- excel-kirjoita-tunnisteet-content
  []
  (let [included-ids (subscribe [:state-query [:application :excel-request :included-ids]])]
    [:div.application-handling__excel-request-margins
     [:div
      [:div.application-handling__excel-tunnisteet-heading @(subscribe [:editor/virkailija-translation :excel-included-ids])]]
     [:div
      [:textarea
       {:class (classes "application-handling__information-request-message-area"
                        "application-handling__information-request-message-area--large")
        :value       (or @included-ids "")
        :placeholder @(subscribe [:editor/virkailija-translation :excel-include-all-placeholder])
        :on-change   #(dispatch [:application/set-excel-request-included-ids (-> % .-target .-value)])}]]]))

(defn excel-download-link
  [_ _ _]
  (let [visible?     (subscribe [:state-query [:application :excel-request :visible?]])
        fetching-applications?     (subscribe [:application/fetching-applications?])
        fetching-excel? (subscribe [:state-query [:application :excel-request :fetching?]])
        excel-error (subscribe [:state-query [:application :excel-request :error]])
        [excel-download-mode set-excel-download-mode] (use-excel-download-mode-state)]
    (fn [selected-hakukohde selected-hakukohderyhma filename]
      [:span.application-handling__excel-request-container
       [:a
        {:class (classes "application-handling__excel-download-link"
                         "editor-form__control-button"
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--variable-width")
         :on-click #(dispatch [:application/set-excel-popup-visibility true])}
        @(subscribe [:editor/virkailija-translation :load-excel])]
       (when @visible?
         [:div.application-handling__excel-request-popup
          [:div.application-handling__excel-request-margins
           [:div.application-handling__mass-edit-review-states-title-container
            [:h4.application-handling__mass-edit-review-states-title
             @(subscribe [:editor/virkailija-translation :excel-request])]
            [:button.virkailija-close-button
             {:type "button"
              :on-click #(dispatch [:application/set-excel-popup-visibility false])}
             [:i.zmdi.zmdi-close]]]
           [:div.application-handling__excel-download-mode-radiogroup
            [:span.application-handling__excel-download-mode-radio-control
             [:input
              {:type      "radio"
               :value     "valitse-tiedot"
               :checked   (= @excel-download-mode "valitse-tiedot")
               :name      "download-mode"
               :on-change (fn [] (set-excel-download-mode "valitse-tiedot"))}]
             [:label {:on-click (fn [] (set-excel-download-mode "valitse-tiedot"))} "Valitse excelin tiedot"]]
            [:span.application-handling__excel-download-mode-radio-control
             [:input
              {:type      "radio"
               :value     "kirjoita-tunnisteet"
               :checked   (= @excel-download-mode "kirjoita-tunnisteet")
               :name      "download-mode"
               :on-change (fn [] (set-excel-download-mode "kirjoita-tunnisteet"))}]
             [:label {:on-click (fn [] (set-excel-download-mode "kirjoita-tunnisteet"))} "Kirjoita tunnisteet"]]]]
          [:div
           (if @fetching-applications?
             [:i.zmdi.zmdi-spinner.spin]
             (case @excel-download-mode
               "valitse-tiedot" [excel-valitse-tiedot-content]
               "kirjoita-tunnisteet" [excel-kirjoita-tunnisteet-content]))]
          (when @excel-error [:span "Tapahtui virhe"])
          [:div.application-handling__excel-request-actions
           [:button.application-handling__excel-request-button
            {:disabled (or @fetching-applications? @fetching-excel?)
             :type "button"
             :on-click (fn [e]
                         (.preventDefault e)
                         (dispatch [:application/start-excel-download
                                    (-> {:filename filename}
                                        (assoc? :selected-hakukohde selected-hakukohde)
                                        (assoc? :selected-hakukohderyhma selected-hakukohderyhma))]))}
            [:span
             @(subscribe [:editor/virkailija-translation :load-excel])]
            (when (or @fetching-applications? @fetching-excel?)
              [:i.zmdi.zmdi-spinner.spin])]
           (let [some-excel-filters-selected? @(subscribe [:application/excel-request-filters-some-selected?])]
             (when (= @excel-download-mode "valitse-tiedot")
               [:button.application-handling__excel-toggle-all-button
                {:on-click (fn [] (dispatch [:application/excel-request-filters-set-all (if some-excel-filters-selected? false true)]))}
                (if some-excel-filters-selected? "Poista valinta" "Valitse kaikki")]))]])])))

(defn- closed-row
  [on-click label]
  [:button.application-handling__hakukohde-rajaus-toggle-button
   {:on-click on-click}
   (or label [:i.zmdi.zmdi-spinner.spin])])

(defn haku-applications-heading
  [_]
  (let [list-opened (r/atom false)
        open-list   #(reset! list-opened true)
        close-list  #(reset! list-opened false)
        opinto-ohjaaja (subscribe [:editor/opinto-ohjaaja?])
        toisen-asteen-yhteishaku? @(subscribe [:application/toisen-asteen-yhteishaku-selected?])]
    (fn [[haku-oid
          selected-hakukohde-oid
          selected-hakukohderyhma-oid
          hakukohteet
          hakukohderyhmat]]
      [:div.application-handling__header-haku-and-hakukohde
       [:div.application-handling__header-haku
        (if-let [haku-name @(subscribe [:application/haku-name haku-oid])]
          haku-name
          [:i.zmdi.zmdi-spinner.spin])]
       (when (not @opinto-ohjaaja)
         (closed-row (if @list-opened close-list open-list)
                     (cond (some? selected-hakukohde-oid)
                           @(subscribe [:application/hakukohde-name
                                        selected-hakukohde-oid])
                           (some? selected-hakukohderyhma-oid)
                           @(subscribe [:application/hakukohderyhma-name
                                        selected-hakukohderyhma-oid])
                           :else
                           @(subscribe [:editor/virkailija-translation :all-hakukohteet]))))
       (when @list-opened
         [h-and-h/popup
          [h-and-h/search-input
           {:id                       haku-oid
            :haut                     [{:oid         haku-oid
                                        :hakukohteet hakukohteet}]
            :hakukohderyhmat          hakukohderyhmat
            :hakukohde-selected?      #(= selected-hakukohde-oid %)
            :hakukohderyhma-selected? #(= selected-hakukohderyhma-oid %)
            :only-hakukohteet?        toisen-asteen-yhteishaku?}]
          nil
          [h-and-h/search-listing
           {:id                         haku-oid
            :haut                       [{:oid         haku-oid
                                          :hakukohteet hakukohteet}]
            :hakukohderyhmat            hakukohderyhmat
            :hakukohde-selected?        #(= selected-hakukohde-oid %)
            :hakukohderyhma-selected?   #(= selected-hakukohderyhma-oid %)
            :on-hakukohde-select        #(do (close-list)
                                             (dispatch [:application/navigate
                                                        (str "/lomake-editori/applications/hakukohde/" %)]))
            :on-hakukohde-unselect      #(do (close-list)
                                             (dispatch [:application/navigate
                                                        (str "/lomake-editori/applications/haku/" haku-oid)]))
            :on-hakukohderyhma-select   #(do (close-list)
                                             (dispatch [:application/navigate
                                                        (str "/lomake-editori/applications/haku/"
                                                             haku-oid
                                                             "/hakukohderyhma/"
                                                             %)]))
            :on-hakukohderyhma-unselect #(do (close-list)
                                             (dispatch [:application/navigate
                                                        (str "/lomake-editori/applications/haku/" haku-oid)]))
            :only-hakukohteet?          toisen-asteen-yhteishaku?}]
          close-list])])))

(defn selected-applications-heading
  [haku-data list-heading]
  (if haku-data
    [haku-applications-heading haku-data]
    [:div.application-handling__header-haku list-heading]))

(defn- application-information-request-contains-modification-link []
  (let [checked?           (subscribe [:application/is-mass-information-link-checkbox-set?])]
    [:div.application-handling__information-request-row
     [:label
      [:input
       {:type      "checkbox"
        :data-test-id "mass-send-update-link"
        :checked   @checked?
        :on-change (fn [event] (let [checkedNewValue (boolean (-> event .-target .-checked))]
                                 (dispatch [:application/set-mass-send-update-link checkedNewValue])))}]
      [:span @(subscribe [:editor/virkailija-translation :send-update-link])]]]))

(defn haku-heading
  []
  (let [show-mass-update-link? (subscribe [:application/show-mass-update-link?])
        show-mass-review-notes-link? (subscribe [:application/show-mass-review-notes-link?])
        show-excel-link?       (subscribe [:application/show-excel-link?])
        rajaus-hakukohteella   (subscribe [:application/rajaus-hakukohteella-value])
        applications-count     (subscribe [:application/loaded-applications-count])
        header                 (subscribe [:application/list-heading])
        haku-header            (subscribe [:application/list-heading-data-for-haku])]
    [:div.application-handling__header
     [selected-applications-heading @haku-header @header]
     [:div.application-handling__form-controls-container
      (when (pos? @applications-count)
        [mass-information-request-view/mass-information-request-link application-information-request-contains-modification-link])
      (when @show-mass-update-link?
        [mass-review/mass-update-applications-link])
      (when @show-mass-review-notes-link?
        [mass-review/mass-review-notes-applications-link])
      (when @show-excel-link?
        (let [selected-hakukohde      (or @rajaus-hakukohteella (second @haku-header))
              selected-hakukohderyhma (when (nil? selected-hakukohde) (nth @haku-header 2))]
          [excel-download-link selected-hakukohde selected-hakukohderyhma @header]))]]))

(defn- select-application
  ([application-key selected-hakukohde-oid]
   (select-application application-key selected-hakukohde-oid nil))
  ([application-key selected-hakukohde-oid with-newest-form?]
   (cljs-util/update-url-with-query-params {:application-key application-key})
   (dispatch [:application/select-application application-key selected-hakukohde-oid with-newest-form?])))

(def end-of-list-tolerance-px 2)
(defn create-application-paging-scroll-handler
  []
  (fn [_]
    (when-let [end-of-list-element (.getElementById js/document "application-handling__end-of-list-element")]
      (let [element-offset  (-> end-of-list-element .getBoundingClientRect .-bottom)
            viewport-offset (.-innerHeight js/window)]
        ; Sometimes element is fraction of a pixel below the viewport even if page is scrolled to the bottom.
        (when (< element-offset (+ viewport-offset end-of-list-tolerance-px))
          (.click end-of-list-element))))))

(defn- application-tab []
  (let [toisen-asteen-yhteishaku? (subscribe [:application/toisen-asteen-yhteishaku-selected?])
        has-right-to-valinnat-tab? (subscribe [:application/has-right-to-valinnat-tab?])
        selected-application-tab (subscribe [:application/selected-application-tab])]
    (fn []
      [:<>
       (when @toisen-asteen-yhteishaku?
         [:div.application__tabs
          [:button
           {:on-click #(dispatch [:application/select-application-tab "application"])
            :disabled (= "application" @selected-application-tab)}
           @(subscribe [:editor/virkailija-translation :application])]
          [:button
           {:on-click #(dispatch [:application/select-application-tab "attachments"])
            :disabled (= "attachments" @selected-application-tab)}
           @(subscribe [:editor/virkailija-translation :attachments])]
          [:button
           {:on-click #(dispatch [:application/select-application-tab "grades"])
            :disabled (= "grades" @selected-application-tab)}
           @(subscribe [:editor/virkailija-translation :grades])]
          (when @has-right-to-valinnat-tab?
            [:button
             {:on-click #(dispatch [:application/select-application-tab "valinnat"])
              :disabled (= "valinnat" @selected-application-tab)}
             @(subscribe [:editor/virkailija-translation :valinnat])])])
       (cond
         (or (not @toisen-asteen-yhteishaku?) (= "application" @selected-application-tab))
         [application-review/application-review-area]

         (= "attachments" @selected-application-tab)
         [attachments-tab-view]

         (= "grades" @selected-application-tab)
         [grades]

         (and
          @has-right-to-valinnat-tab?
          (= "valinnat" @selected-application-tab))
         [valinnat])])))

(defn application []
  (let [search-control-all-page   (subscribe [:application/search-control-all-page-view?])
        loaded-count              (subscribe [:application/loaded-applications-count])
        applications              (subscribe [:application/applications-to-render])
        has-more?                 (subscribe [:application/has-more-applications?])
        loading?                  (subscribe [:application/fetching-applications?])
        user-allowed-fetching?    (subscribe [:application/user-allowed-fetching?])
        expanded                  (subscribe [:state-query [:application :application-list-expanded?]])]
    (fn []
      [:div
       [:div.application-handling__overview
        [application-search-control]
        (when (not @search-control-all-page)
          [:div.application-handling__bottom-wrapper.select_application_list
           [haku-heading]
           [application-list/application-list-header @loaded-count]
           (when (not-empty @applications)
             [application-list/application-list-contents @applications select-application])
           (if (empty? @applications)
             (if @loading?
               [:div.application-handling__list-loading-indicator
                [:i.zmdi.zmdi-spinner]]
               (when-not @user-allowed-fetching?
                 [:div.application-handling__list-loading-indicator
                  [:button.application-handling__show-results-button
                   {:data-test-id "show-results"
                    :on-click #(dispatch [:application/reload-applications true])}
                   @(subscribe [:editor/virkailija-translation :show-results])]]))
             (when (and @expanded (or @has-more? (< (count @applications) @loaded-count)))
               [:div#application-handling__end-of-list-element
                {:on-click #(dispatch [:application/show-more-applications (count @applications)])}
                [:i.application-handling__end-of-list-element-spinner.zmdi.zmdi-spinner.spin]]))])]
       (when (and (not @search-control-all-page) (not @expanded))
         [:div.application-handling__review-area-container
          [:div.application-handling__detail-container
           [application-heading]
           [application-tab]]])])))

