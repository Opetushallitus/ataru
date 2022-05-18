(ns ataru.virkailija.application.view
  (:require [ataru.cljs-util :as cljs-util]
            [ataru.virkailija.application.application-list.virkailija-application-list-view :as application-list]
            [ataru.virkailija.application.application-search-control :refer [application-search-control]]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-subs]
            [ataru.virkailija.application.attachments.virkailija-attachment-handlers]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-subs]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs]
            [ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-handlers]
            [ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-view :as mass-information-request-view]
            [ataru.virkailija.application.mass-review.virkailija-mass-review-view :as mass-review]
            [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
            [ataru.virkailija.application.view.application-heading :refer [application-heading]]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            [ataru.virkailija.application.application-review-view :as application-review]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.grades-view :refer [grades]]))

(defn excel-download-link
  [_ _ _]
  (let [visible?     (subscribe [:state-query [:application :excel-request :visible?]])
        included-ids (subscribe [:state-query [:application :excel-request :included-ids]])
        applications (subscribe [:state-query [:application :applications]])
        loading?     (subscribe [:application/fetching-applications?])]
    (fn [selected-hakukohde selected-hakukohderyhma filename]
      [:span.application-handling__excel-request-container
       [:a.application-handling__excel-download-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
        {:on-click #(dispatch [:application/set-excel-popup-visibility true])}
        @(subscribe [:editor/virkailija-translation :load-excel])]
       (when @visible?
         [:div.application-handling__popup__excel.application-handling__excel-request-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            @(subscribe [:editor/virkailija-translation :excel-request])]
           [:button.virkailija-close-button
            {:on-click #(dispatch [:application/set-excel-popup-visibility false])}
            [:i.zmdi.zmdi-close]]]
          [:div.application-handling__excel-request-row
           [:div.application-handling__excel-request-heading @(subscribe [:editor/virkailija-translation :excel-included-ids])]]
          [:div.application-handling__excel-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value       (or @included-ids "")
             :placeholder @(subscribe [:editor/virkailija-translation :excel-include-all-placeholder])
             :on-change   #(dispatch [:application/set-excel-request-included-ids (-> % .-target .-value)])}]]
          [:div.application-handling__excel-request-row
           [:form#excel-download-link
            {:action "/lomake-editori/api/applications/excel"
             :method "POST"}
            [:input {:type  "hidden"
                     :name  "application-keys"
                     :value (.stringify js/JSON (clj->js (map :key @applications)))}]
            [:input {:type  "hidden"
                     :name  "filename"
                     :value filename}]
            [:input {:type  "hidden"
                     :name  "included-ids"
                     :value (or @included-ids "")}]
            (when-let [csrf-token (cljs-util/csrf-token)]
              [:input {:type  "hidden"
                       :name  "CSRF"
                       :value csrf-token}])
            (when selected-hakukohde
              [:input {:type  "hidden"
                       :name  "selected-hakukohde"
                       :value selected-hakukohde}])
            (when selected-hakukohderyhma
              [:input {:type  "hidden"
                       :name  "selected-hakukohderyhma"
                       :value selected-hakukohderyhma}])]
           [:button.application-handling__excel-request-button
            {:disabled @loading?
             :on-click (fn [_]
                         (.submit (.getElementById js/document "excel-download-link")))}
            [:span
             (str @(subscribe [:editor/virkailija-translation :load-excel])
                  (when @loading? " "))
             (when @loading?
               [:i.zmdi.zmdi-spinner.spin])]]]])])))

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
  [:div.application-handling__information-request-row
   [:p.application-handling__information-request-contains-modification-link
    @(subscribe [:editor/virkailija-translation :edit-link-sent-automatically])]])

(defn haku-heading
  []
  (let [show-mass-update-link? (subscribe [:application/show-mass-update-link?])
        show-excel-link?       (subscribe [:application/show-excel-link?])
        rajaus-hakukohteella   (subscribe [:application/rajaus-hakukohteella-value])
        applications-count     (subscribe [:application/loaded-applications-count])
        header                 (subscribe [:application/list-heading])
        haku-header            (subscribe [:application/list-heading-data-for-haku])]
    [:div.application-handling__header
     [selected-applications-heading @haku-header @header]
     [:div.editor-form__form-controls-container
      (when (pos? @applications-count)
        [mass-information-request-view/mass-information-request-link application-information-request-contains-modification-link])
      (when @show-mass-update-link?
        [mass-review/mass-update-applications-link])
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

(defn create-application-paging-scroll-handler
  []
  (fn [_]
    (when-let [end-of-list-element (.getElementById js/document "application-handling__end-of-list-element")]
      (let [element-offset  (-> end-of-list-element .getBoundingClientRect .-bottom)
            viewport-offset (.-innerHeight js/window)]
        (when (< element-offset viewport-offset)
          (.click end-of-list-element))))))

(defn- application-tab []
  (let [toisen-asteen-yhteishaku? (subscribe [:application/toisen-asteen-yhteishaku-selected?])
        grades-tab-selected? (subscribe [:application/tab-accomplishments-selected?])]
    (fn []
      [:<>
       (when @toisen-asteen-yhteishaku?
         [:div.application__tabs
          [:button
           {:on-click #(dispatch [:application/select-application-tab "application"])
            :disabled (not @grades-tab-selected?)}
           @(subscribe [:editor/virkailija-translation :application])]
          [:button
           {:on-click #(dispatch [:application/select-application-tab "accomplishments"])
            :disabled @grades-tab-selected?}
           @(subscribe [:editor/virkailija-translation :grades])]])
       (cond
         (or (not @toisen-asteen-yhteishaku?) (not @grades-tab-selected?))
         [application-review/application-review-area]

         @grades-tab-selected?
         [grades])])))

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

