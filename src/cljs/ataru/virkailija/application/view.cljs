(ns ataru.virkailija.application.view
  (:require [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.application.application-list.virkailija-application-list-view :as application-list]
            [ataru.virkailija.application.application-review-view :as application-review]
            [ataru.virkailija.application.application-search-control :refer [application-search-control]]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.attachments.attachments-tab-view :refer [attachments-tab-view]]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-subs]
            [ataru.virkailija.application.attachments.virkailija-attachment-handlers]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs]
            [ataru.virkailija.application.excel-download.excel-view :refer [excel-download-link]]
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
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

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

