(ns ataru.virkailija.application.excel-download.excel-view
  (:require [ataru.cljs-util :refer [classes]]
            [ataru.util :refer [assoc? from-multi-lang]]
            [ataru.virkailija.application.excel-download.excel-handlers]
            [ataru.virkailija.application.excel-download.excel-subs]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

(defn- accordion-heading-id [id] (str "accordion-heading_" id))

(defn- accordion-content-id [id] (str "accordion-content_" id))

(defn- checkbox-name [id] (str "checkbox_" id))

(defn- excel-checkbox-on-change [e]
  (dispatch [:application/excel-request-filter-changed
             (-> e .-target .-value)]))

(defn- excel-checkbox [id]
  (let [ref (atom nil)
        checked? @(subscribe [:application/excel-request-filter-value id])
        indeterminate? @(subscribe [:application/excel-request-filter-indeterminate? id])]
    (r/after-render (fn []
                      (when @ref
                        (set! (.-indeterminate @ref) indeterminate?))))
    [:input
     {:key (str id "_" indeterminate?)
      :type "checkbox"
      :id (checkbox-name id)
      :ref #(reset! ref %)
      :value id
      :on-change excel-checkbox-on-change
      :checked (boolean checked?)}]))

(defn- excel-checkbox-control
  [id label]
  [:span.application-handling__excel-checkbox-control
   [excel-checkbox id label]
   (when label [:label {:for (checkbox-name id)} label])])

(defn- accordion-heading [id title open? child-ids]
  (let [has-children? (not-empty child-ids)]
    [:h4.application-handling__excel-accordion-heading-wrapper
     {:id (accordion-heading-id id)}
     [excel-checkbox-control id title]
     (when has-children? (let [selected-children-count (subscribe [:application/excel-request-filters-selected-count-by-ids child-ids])
                               click-action #(dispatch [:application/excel-request-toggle-accordion-open id])]
                           [:button.application-handling__excel-accordion-header-button
                            {:type "button"
                             "aria-expanded" open?
                             "aria-controls" (accordion-content-id id)
                             :on-click click-action}
                            [:span.excel-accordion-heading-text
                             (str @selected-children-count "/" (count child-ids) " " @(subscribe [:editor/virkailija-translation :valittu]))]
                            [:i
                             {:class (classes "zmdi"
                                              (if open? "zmdi-chevron-up" "zmdi-chevron-down"))}]]))]))

(defn- excel-accordion
  [id title child-ids content]
  (let [open? @(subscribe [:application/excel-request-accordion-open? id])
        has-children? (not-empty child-ids)]
    [:div.application-handling__excel-accordion-group
     ^{:key (str "accordion_" id)}
     [accordion-heading id title open? child-ids]
     (when has-children? [:div.application-handling__excel-accordion-content
                          {:id (accordion-content-id id)
                           :role "region"
                           :style {:display (when (not open?) "none")}
                           "aria-labelledby" (accordion-heading-id id)}
                          ^{:key (accordion-content-id id)} content])]))

(defn- get-filter-trans [filter-def lng]
  (or (from-multi-lang (:label filter-def) lng) (:id filter-def)))

(defn- excel-valitse-tiedot-content [selected-hakukohde selected-hakukohderyhma]
  (let [form-key (subscribe [:application/selected-form-key])
        filters-initializing? @(subscribe [:application/excel-request-filters-initializing?])
        filters-need-initialization? @(subscribe [:application/excel-request-filters-need-initialization? @form-key selected-hakukohde selected-hakukohderyhma])]
    (when filters-need-initialization?
      (dispatch [:application/excel-request-filters-init @form-key selected-hakukohde selected-hakukohderyhma]))
    (if filters-initializing?
      [:div.application-handling_excel-spinner
       [:i.zmdi.zmdi-spinner.spin]]
      [:div.application-handling__excel-tiedot
       [:div.application-handling__excel-request-margins
        (let [filter-defs @(subscribe [:application/excel-request-filters])]
          (->> filter-defs
               (filter #(not (:parent-id (second %))))
               (vals)
               (sort-by :index)
               (map (fn [section]
                      ^{:key (str (:id section) "_section")}
                      [excel-accordion
                       (:id section)
                       (get-filter-trans section :fi)
                       (:child-ids section)
                       [:div.application-handling__excel-accordion-checkbox-col
                        (map (fn [child-id]
                               (let [sub-filter (get-in filter-defs [child-id])]
                                 ^{:key (str child-id "_checkbox")}
                                 [excel-checkbox-control
                                  child-id
                                  (get-filter-trans sub-filter :fi)]))
                             (:child-ids section))]]))))]])))

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

(defn- excel-download-mode-radio [id excel-download-mode set-excel-download-mode]
  [:span.application-handling__excel-download-mode-radio-control
   [:input
    {:type      "radio"
     :id        (str id "-input")
     :value     id
     :checked   (= @excel-download-mode id)
     :name      "download-mode"
     :on-change (fn [] (set-excel-download-mode id))}]
   [:label {:for (str id "-input")} @(subscribe [:editor/virkailija-translation (keyword (str "excel-mode-" id))])]])

(defn excel-download-link
  [_ _ _]
  (let [visible?     (subscribe [:state-query [:application :excel-request :visible?]])
        fetching-applications?     (subscribe [:application/fetching-applications?])
        fetching-excel? (subscribe [:state-query [:application :excel-request :fetching?]])
        excel-export-mode (subscribe [:application/excel-download-mode])
        set-excel-export-mode #(dispatch [:application/change-excel-download-mode %])]
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
            [excel-download-mode-radio "ids-only" excel-export-mode set-excel-export-mode]
            [excel-download-mode-radio "with-defaults" excel-export-mode set-excel-export-mode]]]
          [:div
           (case @excel-export-mode
             "ids-only" [excel-valitse-tiedot-content selected-hakukohde selected-hakukohderyhma]
             "with-defaults" [excel-kirjoita-tunnisteet-content])]
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
             (when (= @excel-export-mode "ids-only")
               [:button.application-handling__excel-toggle-all-button
                {:on-click (fn [] (dispatch [:application/excel-request-filters-set-all (if some-excel-filters-selected? false true)]))}
                (if some-excel-filters-selected?
                  @(subscribe [:editor/virkailija-translation :excel-poista-valinnat])
                  @(subscribe [:editor/virkailija-translation :excel-valitse-kaikki]))]))]])])))