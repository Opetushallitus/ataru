(ns ataru.virkailija.application.excel-download.excel-view
  (:require [ataru.excel-common :refer [form-field-belongs-to-hakukohde
                                        hakemuksen-yleiset-tiedot-fields
                                        kasittelymerkinnat-fields]]
            [ataru.translations.texts :refer [virkailija-texts]]
            [ataru.util :refer [assoc?]]
            [ataru.virkailija.application.excel-download.excel-subs]
            [ataru.virkailija.application.excel-download.excel-handlers]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))


(defn- accordion-heading-id [id] (str "accordion-heading_" id))
(defn- accordion-content-id [id] (str "accordion-content_" id))
(defn- checkbox-name [id] (str "checkbox_" id))

(defn- excel-checkbox-on-change [e]
  (dispatch [:application/excel-request-filter-changed
             (-> e .-target .-value)]))

(defn excel-checkbox [id]
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
      :checked checked?}]))

(defn excel-checkbox-control
  [id title]
  [:span.application-handling__excel-checkbox-control
   [excel-checkbox id title]
   (when title [:label {:for (checkbox-name id)} title])])

(defn use-excel-download-mode-state []
  [(subscribe [:application/excel-download-mode])
   (fn [mode] (dispatch [:application/change-excel-download-mode mode]))])

(defn- classes [& cs] (str/join " " (vec cs)))

(defn- accordion-heading [id title folded? child-ids]
  (let [selected-children-count (subscribe [:application/excel-request-filters-selected-count-by-ids child-ids])
        click-action (if folded?
                       #(dispatch [:editor/unfold id])
                       #(dispatch [:editor/fold id]))
        has-children? (not-empty child-ids)]
    [:h4.application-handling__excel-accordion-heading-wrapper
     (if has-children?
       [excel-checkbox id]
       [excel-checkbox-control id title])
     (when has-children? [:button.application-handling__excel-accordion-header-button
                          {:id (accordion-heading-id id)
                           :type "button"
                           "aria-expanded" (not folded?)
                           "aria-controls" (accordion-content-id id)
                           :on-click click-action}
                          [:span.excel-accordion-heading-text
                           [:span title]
                           [:span (str @selected-children-count "/" (count child-ids) " valittu")]]
                          [:i
                           {:class (classes "zmdi"
                                            (if folded? "zmdi-chevron-down" "zmdi-chevron-up"))}]])]))

(defn excel-accordion
  [id title child-ids content]
  (let [folded? @(subscribe [:editor/folded? id])]
    [:div.application-handling__excel-accordion-group
     ^{:key (str "accordion_" id)}
     [accordion-heading id title folded? child-ids]
     [:div.application-handling__excel-accordion-content
      {:id (accordion-content-id id)
       :role "region"
       :style {:display (when folded? "none")}
       "aria-labelledby" (accordion-heading-id id)}
      ^{:key (accordion-content-id id)} content]]))


(defn question-wrapper? [item] (contains? #{"wrapperElement" "questionGroup"} (:fieldClass item)))
(defn info-element? [item] (contains? #{"infoElement" "modalInfoElement"} (:fieldClass item)))

(defn get-excel-checkbox-filter-defs
  ([form-content form-field-belongs-to parent-id level parent-index-acc]
   (if (empty? form-content)
     nil
     (reduce (fn [acc form-field]
               (let [index-acc (+ parent-index-acc (count acc))
                     children (get-excel-checkbox-filter-defs (:children form-field)
                                                              form-field-belongs-to
                                                              (or parent-id (:id form-field))
                                                              (inc level)
                                                              (inc index-acc))]
                 (if (or (and (question-wrapper? form-field) (empty? children))
                         (info-element? form-field)
                         (:exclude-from-answers form-field))
                   acc
                   (merge acc children (when (and (or (= level 0) (not (question-wrapper? form-field)))
                                                  (form-field-belongs-to form-field))
                                         {(:id form-field) (-> {:id (:id form-field)
                                                                :index index-acc
                                                                :label (:label form-field)
                                                                :checked true}
                                                               (assoc? :parent-id parent-id)
                                                               (assoc? :child-ids (->> children
                                                                                       (map second)
                                                                                       (sort-by :index)
                                                                                       (map :id))))})))))
             {}
             form-content)))
  ([form-content form-field-belongs-to]
   (get-excel-checkbox-filter-defs form-content form-field-belongs-to nil 0 0)))


(def common-fields
  [{:id "hakemuksen-yleiset-tiedot"
    :label (:excel-hakemuksen-yleiset-tiedot virkailija-texts)
    :children (map #(select-keys % [:id :label]) hakemuksen-yleiset-tiedot-fields)}
   {:id "kasittelymerkinnat"
    :label (:excel-kasittelymerkinnat virkailija-texts)
    :children (map #(select-keys % [:id :label]) kasittelymerkinnat-fields)}])




(defn get-label-trans [l lng default]
  (let [label (into {} (filter #(not-empty (second %)) l))]
    (or (get label lng) (get label :fi) (get label :sv) (get label :en) default)))

(defn- excel-valitse-tiedot-content [selected-hakukohde selected-hakukohderyhma]
  (let [form-key @(subscribe [:application/selected-form-key])
        form-content @(subscribe [:state-query [:forms form-key :content]])
        all-hakukohteet (subscribe [:state-query [:hakukohteet]])
        form-field-belongs-to (fn [form-field] (form-field-belongs-to-hakukohde form-field selected-hakukohde selected-hakukohderyhma all-hakukohteet))
        filter-defs (get-excel-checkbox-filter-defs
                     (concat common-fields form-content)
                     form-field-belongs-to)
        top-filters (->> filter-defs
                         (filter #(not (:parent-id (second %))))
                         (map second)
                         (sort-by :index))
        filters-initialized? (subscribe [:application/excel-request-filters-initialized?])]
    (when (not @filters-initialized?)
      (dispatch [:application/excel-request-filters-init filter-defs]))
    (fn []
      [:div.application-handling__excel-tiedot
       [:div.application-handling__excel-request-margins
        (->> top-filters
             (map (fn [section]
                    ^{:key (str (:id section) "_section")}
                    [excel-accordion
                     (:id section)
                     (get-label-trans (:label section) :fi (:id section))
                     (:child-ids section)
                     [:div.application-handling__excel-accordion-checkbox-col
                      (map (fn [child-id]
                             (let [sub-filter (get-in filter-defs [child-id])]
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
        fetching-form-content?     (subscribe [:application/fetching-form-content?])
        fetching-excel? (subscribe [:state-query [:application :excel-request :fetching?]])
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
           (if @fetching-form-content?
             [:div
              {:style {:display "flex"
                       :width "100%"
                       :font-size "40px"
                       :justify-content "center"
                       :margin "50px 0"}}
              [:i.zmdi.zmdi-spinner.spin]]
             (case @excel-download-mode
               "valitse-tiedot" [excel-valitse-tiedot-content selected-hakukohde selected-hakukohderyhma]
               "kirjoita-tunnisteet" [excel-kirjoita-tunnisteet-content]))]
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