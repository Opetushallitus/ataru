(ns ataru.virkailija.application.view
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as cljs-util :refer [get-virkailija-translation]]
            [ataru.translations.texts :refer [virkailija-texts state-translations general-texts]]
            [ataru.util :as util]
            [ataru.virkailija.application.application-search-control :refer [application-search-control]]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
            [ataru.virkailija.views.modal :as modal]
            [ataru.virkailija.views.virkailija-readonly :as readonly-contents]
            [ataru.virkailija.virkailija-ajax :as ajax]
            [cljs-time.format :as f]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [medley.core :refer [find-first]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- icon-check []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
  [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]])

(defn- icon-many-checked []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
   [:i.zmdi.zmdi-check-all.zmdi-hc-stack-1x]])

(defn- icon-unselected []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-hc-stack-1x]])

(defn- icon-multi-check []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-square-o.zmdi-hc-stack-1x]
    [:i.zmdi.zmdi-check.zmdi-hc-stack-1x]])

(defn- icon-select []
  [:span.application-handling__review-state-selected-icon.zmdi-hc-stack.zmdi-hc-lg
    [:i.zmdi.zmdi-square-o.zmdi-hc-stack-1x]])

(defn excel-download-link
  [_ _ _]
  (let [visible?     (subscribe [:state-query [:application :excel-request :visible?]])
        included-ids (subscribe [:state-query [:application :excel-request :included-ids]])
        applications (subscribe [:state-query [:application :applications]])
        loading?     (subscribe [:state-query [:application :fetching-applications?]])]
    (fn [selected-hakukohde selected-hakukohderyhma filename]
      [:span.application-handling__excel-request-container
       [:a.application-handling__excel-download-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
        {:on-click #(dispatch [:application/set-excel-popup-visibility true])}
        (get-virkailija-translation :load-excel)]
       (when @visible?
         [:div.application-handling__popup__excel.application-handling__excel-request-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            (get-virkailija-translation :excel-request)]
           [:button.virkailija-close-button
            {:on-click #(dispatch [:application/set-excel-popup-visibility false])}
            [:i.zmdi.zmdi-close]]]
          [:div.application-handling__excel-request-row
           [:div.application-handling__excel-request-heading (get-virkailija-translation :excel-included-ids)]]
          [:div.application-handling__excel-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value       (or @included-ids "")
             :placeholder (get-virkailija-translation :excel-include-all-placeholder)
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
                     :name  "skip-answers"
                     :value "false"}]
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
             :on-click (fn [e]
                         (.submit (.getElementById js/document "excel-download-link")))}
            [:span
             (str (get-virkailija-translation :load-excel)
                  (when @loading? " "))
             (when @loading?
               [:i.zmdi.zmdi-spinner.spin])]]]])])))

(defn- selected-or-default-mass-review-state
  [selected all]
  (if @selected
    @selected
    (or
      (ffirst (filter (comp pos? second) all))
      (ffirst all))))

(defn- review-state-label
  [state-name]
  (get (->> review-states/application-hakukohde-processing-states
            (filter #(= (first %) state-name))
            first
            second)
       @(subscribe [:editor/virkailija-lang])))

(defn- review-label-with-count
  [label count]
  (str label
       (when (< 0 count)
         (str " (" count ")"))))

(defn- selected-or-default-mass-review-state-label
  [selected all review-state-counts]
  (let [name  (selected-or-default-mass-review-state selected all)
        label (review-state-label name)
        count (get review-state-counts name)]
    (review-label-with-count label count)))

(defn- mass-review-state-selected-row
  [on-click label]
  [:div.application-handling__review-state-row.application-handling__review-state-row--mass-update.application-handling__review-state-row--selected
   {:on-click on-click}
   [icon-check] label])

(defn- mass-review-state-row
  [current-review-state states review-state-counts disable-empty-rows? state]
  (let [review-state-count (get review-state-counts state 0)
        review-state-label (review-state-label state)
        label-with-count   (review-label-with-count review-state-label review-state-count)
        on-click           #(reset! current-review-state state)
        disabled?          (and disable-empty-rows? (zero? review-state-count))]
    (if (= (selected-or-default-mass-review-state current-review-state states) state)
      (mass-review-state-selected-row #() label-with-count)
      [:div.application-handling__review-state-row.application-handling__review-state-row--mass-update
       {:on-click (when-not disabled? on-click)
        :class    (when disabled? "application-handling__review-state-row--disabled")}
       [icon-unselected] label-with-count])))

(defn- opened-mass-review-state-list
  [current-state states review-state-counts disable-empty-rows?]
  (mapv (partial mass-review-state-row current-state states review-state-counts disable-empty-rows?) (map first states)))

(defn- mass-update-applications-link
  []
  (let [visible?                   (subscribe [:state-query [:application :mass-update :visible?]])
        from-list-open?            (r/atom false)
        to-list-open?              (r/atom false)
        submit-button-state        (r/atom :submit)
        selected-from-review-state (r/atom nil)
        selected-to-review-state   (r/atom nil)
        haku-header                (subscribe [:application/list-heading-data-for-haku])
        review-state-counts        (subscribe [:state-query [:application :review-state-counts]])
        loading?                   (subscribe [:state-query [:application :fetching-applications?]])
        all-states                 (reduce (fn [acc [state _]]
                                             (assoc acc state 0))
                                           {}
                                           review-states/application-hakukohde-processing-states)]
    (fn []
      (let [from-states (merge all-states @review-state-counts)]
        [:span.application-handling__mass-edit-review-states-container
         [:a.application-handling__mass-edit-review-states-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
          {:on-click #(dispatch [:application/set-mass-update-popup-visibility true])}
          (get-virkailija-translation :mass-edit)]
         (when @visible?
           [:div.application-handling__mass-edit-review-states-popup.application-handling__popup
            [:div.application-handling__mass-edit-review-states-title-container
             [:h4.application-handling__mass-edit-review-states-title
              (get-virkailija-translation :mass-edit)]
             [:button.virkailija-close-button
              {:on-click #(dispatch [:application/set-mass-update-popup-visibility false])}
              [:i.zmdi.zmdi-close]]]
            (when-let [[haku-oid hakukohde-oid _ _ _] @haku-header]
              [:p
               @(subscribe [:application/haku-name haku-oid])
               (when hakukohde-oid
                 (str ", " @(subscribe [:application/hakukohde-name hakukohde-oid])))])
            [:h4.application-handling__mass-edit-review-states-heading (get-virkailija-translation :from-state)]

            (if @from-list-open?
              (into [:div.application-handling__review-state-list.application-handling__review-state-list--opened
                     {:on-click #(swap! from-list-open? not)}]
                    (opened-mass-review-state-list selected-from-review-state from-states @review-state-counts true))
              (mass-review-state-selected-row
               (fn []
                 (swap! from-list-open? not)
                 (reset! submit-button-state :submit))
               (selected-or-default-mass-review-state-label selected-from-review-state from-states @review-state-counts)))

            [:h4.application-handling__mass-edit-review-states-heading (get-virkailija-translation :to-state)]

            (if @to-list-open?
              (into [:div.application-handling__review-state-list.application-handling__review-state-list--opened
                     {:on-click #(when (-> from-states (keys) (count) (pos?)) (swap! to-list-open? not))}]
                    (opened-mass-review-state-list selected-to-review-state all-states @review-state-counts false))
              (mass-review-state-selected-row
               (fn []
                 (swap! to-list-open? not)
                 (reset! submit-button-state :submit))
               (selected-or-default-mass-review-state-label selected-to-review-state all-states @review-state-counts)))

            (case @submit-button-state
              :submit
              (let [button-disabled? (or (= (selected-or-default-mass-review-state selected-from-review-state from-states)
                                            (selected-or-default-mass-review-state selected-to-review-state all-states))
                                         @loading?)]
                [:a.application-handling__link-button.application-handling__mass-edit-review-states-submit-button
                 {:on-click #(when-not button-disabled? (reset! submit-button-state :confirm))
                  :disabled button-disabled?}
                 [:span
                  (str (get-virkailija-translation :change)
                       (when @loading? " "))
                  (when @loading?
                    [:i.zmdi.zmdi-spinner.spin])]])

              :confirm
              [:a.application-handling__link-button.application-handling__mass-edit-review-states-submit-button--confirm
               {:on-click (fn []
                            (let [from-state-name (selected-or-default-mass-review-state selected-from-review-state from-states)
                                  to-state-name   (selected-or-default-mass-review-state selected-to-review-state all-states)]
                              (dispatch [:application/mass-update-application-reviews
                                         from-state-name
                                         to-state-name])
                              (dispatch [:application/set-mass-update-popup-visibility false])
                              (reset! selected-from-review-state nil)
                              (reset! selected-to-review-state nil)
                              (reset! from-list-open? false)
                              (reset! to-list-open? false)))}
               (get-virkailija-translation :confirm-change)])])]))))

(declare application-information-request-contains-modification-link)

(defn- mass-information-request-link
  []
  (let [visible?           (subscribe [:application/mass-information-request-popup-visible?])
        subject            (subscribe [:state-query [:application :mass-information-request :subject]])
        message            (subscribe [:state-query [:application :mass-information-request :message]])
        form-status        (subscribe [:application/mass-information-request-form-status])
        applications-count (subscribe [:application/loaded-applications-count])
        button-enabled?    (subscribe [:application/mass-information-request-button-enabled?])]
    (fn []
      [:span.application-handling__mass-information-request-container
       [:a.application-handling__mass-information-request-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
        {:on-click #(dispatch [:application/set-mass-information-request-popup-visibility true])}
        (get-virkailija-translation :mass-information-request)]
       (when @visible?
         [:div.application-handling__popup.application-handling__mass-information-request-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            (get-virkailija-translation :mass-information-request)]
           [:button.virkailija-close-button
            {:on-click #(dispatch [:application/set-mass-information-request-popup-visibility false])}
            [:i.zmdi.zmdi-close]]]
          [:p (get-virkailija-translation :mass-information-request-email-n-recipients @applications-count)]
          [:div.application-handling__information-request-row
           [:div.application-handling__information-request-info-heading (get-virkailija-translation :mass-information-request-subject)]
           [:div.application-handling__information-request-text-input-container
            [:input.application-handling__information-request-text-input
             {:value     @subject
              :maxLength 200
              :on-change #(dispatch [:application/set-mass-information-request-subject (-> % .-target .-value)])}]]]
          [:div.application-handling__information-request-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value     @message
             :on-change #(dispatch [:application/set-mass-information-request-message (-> % .-target .-value)])}]]
          [application-information-request-contains-modification-link]
          [:div.application-handling__information-request-row
           (case @form-status
             (:disabled :enabled nil)
             [:button.application-handling__send-information-request-button
              {:disabled (not @button-enabled?)
               :class    (if @button-enabled?
                           "application-handling__send-information-request-button--enabled"
                           "application-handling__send-information-request-button--disabled")
               :on-click #(dispatch [:application/confirm-mass-information-request])}
              (get-virkailija-translation :mass-information-request-send)]

             :loading-applications
             [:button.application-handling__send-information-request-button.application-handling__send-information-request-button--disabled
              {:disabled true}
              [:span (str (get-virkailija-translation :mass-information-request-send) " ")
               [:i.zmdi.zmdi-spinner.spin]]]

             :confirm
             [:button.application-handling__send-information-request-button.application-handling__send-information-request-button--confirm
              {:on-click #(dispatch [:application/submit-mass-information-request])}
              (get-virkailija-translation :mass-information-request-confirm-n-messages @applications-count)]

             :submitting
             [:div.application-handling__information-request-status
              [:i.zmdi.zmdi-hc-lg.zmdi-spinner.spin.application-handling__information-request-status-icon]
              (get-virkailija-translation :mass-information-request-sending-messages)]

             :submitted
             [:div.application-handling__information-request-status
              [:i.zmdi.zmdi-hc-lg.zmdi-check-circle.application-handling__information-request-status-icon.application-handling__information-request-status-icon--sent]
              (get-virkailija-translation :mass-information-request-messages-sent)])]])])))

(defn- closed-row
  [on-click label]
  [:button.application-handling__hakukohde-rajaus-toggle-button
   {:on-click on-click}
   (or label [:i.zmdi.zmdi-spinner.spin])])

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
     [:span (get-virkailija-translation :ensisijaisesti)]]))

(defn haku-applications-heading
  [_]
  (let [list-opened (r/atom false)
        open-list   #(reset! list-opened true)
        close-list  #(reset! list-opened false)]
    (fn [[haku-oid
          selected-hakukohde-oid
          selected-hakukohderyhma-oid
          hakukohteet
          hakukohderyhmat]]
      (let [hakukohde-oids      (map :oid hakukohteet)
            hakukohderyhma-oids (map :oid hakukohderyhmat)]
        [:div.application-handling__header-haku-and-hakukohde
         [:div.application-handling__header-haku
          (if-let [haku-name @(subscribe [:application/haku-name haku-oid])]
            haku-name
            [:i.zmdi.zmdi-spinner.spin])]
         (closed-row (if @list-opened close-list open-list)
                     (cond (some? selected-hakukohde-oid)
                           @(subscribe [:application/hakukohde-name
                                        selected-hakukohde-oid])
                           (some? selected-hakukohderyhma-oid)
                           @(subscribe [:application/hakukohderyhma-name
                                        selected-hakukohderyhma-oid])
                           :else
                           (get-virkailija-translation :all-hakukohteet)))
         (when @list-opened
           [h-and-h/popup
            [h-and-h/search-input
             {:id                       haku-oid
              :haut                     [{:oid         haku-oid
                                          :name        (get virkailija-texts :hakukohteet)
                                          :hakukohteet hakukohteet}]
              :hakukohderyhmat          hakukohderyhmat
              :hakukohde-selected?      #(= selected-hakukohde-oid %)
              :hakukohderyhma-selected? #(= selected-hakukohderyhma-oid %)}]
            [h-and-h/search-listing
             {:id                       haku-oid
              :haut                     [{:oid         haku-oid
                                          :name        (get virkailija-texts :hakukohteet)
                                          :hakukohteet hakukohteet}]
              :hakukohderyhmat          hakukohderyhmat
              :hakukohde-selected?      #(= selected-hakukohde-oid %)
              :hakukohderyhma-selected? #(= selected-hakukohderyhma-oid %)
              :on-hakukohde-select      #(do (close-list)
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
                                                          (str "/lomake-editori/applications/haku/" haku-oid)]))}]
            close-list])]))))

(defn selected-applications-heading
  [haku-data list-heading]
  (if haku-data
    [haku-applications-heading haku-data]
    [:div.application-handling__header-haku list-heading]))

(defn haku-heading
  []
  (let [show-mass-update-link? (subscribe [:application/show-mass-update-link?])
        show-excel-link?       (subscribe [:application/show-excel-link?])
        applications-count     (subscribe [:application/loaded-applications-count])
        header                 (subscribe [:application/list-heading])
        haku-header            (subscribe [:application/list-heading-data-for-haku])]
    [:div.application-handling__header
     [selected-applications-heading @haku-header @header]
     [:div.editor-form__form-controls-container
      (when (pos? @applications-count)
        [mass-information-request-link])
      (when @show-mass-update-link?
        [mass-update-applications-link])
      (when @show-excel-link?
        [excel-download-link (second @haku-header) (nth @haku-header 2) @header])]]))

(defn- select-application
  ([application-key selected-hakukohde-oid]
   (select-application application-key selected-hakukohde-oid nil))
  ([application-key selected-hakukohde-oid with-newest-form?]
   (cljs-util/update-url-with-query-params {:application-key application-key})
   (dispatch [:application/select-application application-key selected-hakukohde-oid with-newest-form?])))

(defn hakukohde-review-state
  [hakukohde-reviews hakukohde-oid requirement]
  (:state (first (get hakukohde-reviews [hakukohde-oid requirement]))))

(defn- hakukohde-and-tarjoaja-name [hakukohde-oid]
  (if-let [hakukohde-and-tarjoaja-name @(subscribe [:application/hakukohde-and-tarjoaja-name
                                                    hakukohde-oid])]
    [:span hakukohde-and-tarjoaja-name]
    [:i.zmdi.zmdi-spinner.spin]))

(defn- attachment-state-counts [states]
  [:span.application-handling__list-row--attachment-states
   (when (< 0 (:checked states))
     [:span.application-handling_list-row-attachment-state-counts.checked (:checked states)])
   (when (< 0 (:unchecked states))
     [:span.application-handling_list-row-attachment-state-counts.unchecked (:unchecked states)])])

(defn applications-hakukohde-rows
  [review-settings application filtered-hakukohde attachment-states]
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
                selection-state        (hakukohde-review-state application-hakukohde-reviews hakukohde-oid "selection-state")
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
                [hakukohde-and-tarjoaja-name hakukohde-oid]])
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
                 (get-virkailija-translation :unprocessed))
               (when show-state-email-icon?
                 [:i.zmdi.zmdi-email.application-handling__list-row-email-icon])]]
             (when (:selection-state review-settings true)
               [:span.application-handling__hakukohde-selection-cell
                [:span.application-handling__hakukohde-selection.application-handling__count-tag
                 [:span.application-handling__state-label
                  {:class (str "application-handling__state-label--" (or selection-state "incomplete"))}]
                 (or
                   (application-states/get-review-state-label-by-name
                     review-states/application-hakukohde-selection-states
                     selection-state
                     @lang)
                   (get-virkailija-translation :incomplete))]])]))
        application-hakukohde-oids))))

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

(defn application-list-row [application selected?]
  (let [selected-time-column    (subscribe [:state-query [:application :selected-time-column]])
        day-date-time           (-> (get application @selected-time-column)
                                    (t/str->googdate)
                                    (t/time->str)
                                    (clojure.string/split #"\s"))
        day                     (first day-date-time)
        date-time               (->> day-date-time (rest) (clojure.string/join " "))
        applicant               (str (-> application :person :last-name) ", " (-> application :person :preferred-name))
        review-settings         (subscribe [:state-query [:application :review-settings :config]])
        filtered-hakukohde      (subscribe [:state-query [:application :selected-hakukohde]])
        attachment-states       (application-attachment-states application)
        form-attachment-states  (:form attachment-states)]
    [:div.application-handling__list-row
     {:on-click #(select-application (:key application) @filtered-hakukohde)
      :class    (clojure.string/join " " [(when selected?
                                            "application-handling__list-row--selected")
                                          (when (= "inactivated" (:state application))
                                            "application-handling__list-row--inactivated")])
      :id       (str "application-list-row-" (:key application))}
     [:div.application-handling__list-row-person-info
      [:span.application-handling__list-row--application-applicant
       (or applicant [:span.application-handling__list-row--applicant-unknown (get-virkailija-translation :unknown)])]
      [:span.application-handling__list-row--application-time
       [:span.application-handling__list-row--time-day day]
       [:span date-time]]
      (when (:attachment-handling @review-settings true)
        [attachment-state-counts form-attachment-states])
      [:span.application-handling__list-row--state]
      (when (:selection-state @review-settings true)
        [:span.application-handling__hakukohde-selection-cell])]
     [applications-hakukohde-rows @review-settings application @filtered-hakukohde attachment-states]]))

(defn application-list-contents [applications]
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
                                       [application-list-row application selected?])))})))

(defn- toggle-state-filter!
  [hakukohde-filters states filter-kw filter-id selected?]
  (let [new-filter (if selected?
                     (remove #(= filter-id %) hakukohde-filters)
                     (conj hakukohde-filters filter-id))]
    (cljs-util/update-url-with-query-params
      {filter-kw (clojure.string/join ","
                                      (cljs-util/get-unselected-review-states
                                        new-filter
                                        states))})
    (dispatch [:state-update #(assoc-in % [:application filter-kw] new-filter)])
    (dispatch [:application/reload-applications])))

(defn hakukohde-state-filter-controls
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
                                                         (clojure.string/join "," (map first states))
                                                         nil)})
                                          (dispatch [:state-update #(assoc-in % [:application filter-kw]
                                                                              (if all-filters-selected?
                                                                                []
                                                                                (map first states)))])
                                          (dispatch [:application/reload-applications]))}]
                    [:span (get-virkailija-translation :all)]]]]
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

(defn application-list-basic-column-header [column-id heading]
  (let [application-sort (subscribe [:state-query [:application :sort]])]
    (fn [column-id heading]
      [:span.application-handling__basic-list-basic-column-header
       {:on-click #(dispatch [:application/update-sort column-id])}
       heading
       (when (= column-id (:order-by @application-sort))
         (if (= "desc" (:order @application-sort))
           [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow]
           [:i.zmdi.zmdi-chevron-up.application-handling__sort-arrow]))])))

(defn created-time-column-header []
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
           (get-virkailija-translation :last-modified)
           (get-virkailija-translation :submitted-at))]
        " |"
        [:i.zmdi
         {:on-click #(dispatch [:application/update-sort @selected-time-column])
          :class    (if (= "desc" (:order @application-sort))
                      "zmdi-chevron-down application-handling__sort-arrow"
                      "zmdi-chevron-up application-handling__sort-arrow")}]]])))

(defn- application-filter-checkbox
  [filters label lang kw state]
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
     [:span (if (some? lang)
              (util/non-blank-val label [lang :fi :sv :en])
              label)]]))

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
         (application-filter-checkbox filters checkbox-label lang kw state))
       states))])

(defn- application-base-education-filters
  [filters-checkboxes]
  (let [checkboxes            [[:pohjakoulutus_yo (get-virkailija-translation :pohjakoulutus_yo)]
                               [:pohjakoulutus_lk (get-virkailija-translation :pohjakoulutus_lk)]
                               [:pohjakoulutus_yo_kansainvalinen_suomessa (get-virkailija-translation :pohjakoulutus_yo_kansainvalinen_suomessa)]
                               [:pohjakoulutus_yo_ammatillinen (get-virkailija-translation :pohjakoulutus_yo_ammatillinen)]
                               [:pohjakoulutus_am (get-virkailija-translation :pohjakoulutus_am)]
                               [:pohjakoulutus_amt (get-virkailija-translation :pohjakoulutus_amt)]
                               [:pohjakoulutus_kk (get-virkailija-translation :pohjakoulutus_kk)]
                               [:pohjakoulutus_yo_ulkomainen (get-virkailija-translation :pohjakoulutus_yo_ulkomainen)]
                               [:pohjakoulutus_kk_ulk (get-virkailija-translation :pohjakoulutus_kk_ulk)]
                               [:pohjakoulutus_ulk (get-virkailija-translation :pohjakoulutus_ulk)]
                               [:pohjakoulutus_avoin (get-virkailija-translation :pohjakoulutus_avoin)]
                               [:pohjakoulutus_muu (get-virkailija-translation :pohjakoulutus_muu)]]
        all-filters-selected? (subscribe [:application/all-pohjakoulutus-filters-selected?])]
    (fn []
      [:div.application-handling__filter-group
       [:h3.application-handling__filter-group-heading (get-virkailija-translation :base-education)]
       [:label.application-handling__filter-checkbox-label.application-handling__filter-checkbox-label--all
        {:key   (str "application-filter-pohjakoulutus-any")
         :class (when @all-filters-selected? "application-handling__filter-checkbox-label--checked")}
        [:input.application-handling__filter-checkbox
         {:type      "checkbox"
          :checked   @all-filters-selected?
          :on-change #(dispatch [:application/toggle-all-pohjakoulutus-filters @all-filters-selected?])}]
        [:span "Kaikki"]]
       (->> checkboxes
            (map (fn [[id label]] (application-filter-checkbox filters-checkboxes label nil :base-education id)))
            (doall))])))

(defn- select-rajaava-hakukohde [opened?]
  (let [ryhman-ensisijainen-hakukohde @(subscribe [:state-query [:application :rajaus-hakukohteella-value]])]
    [:div.application-handling__ensisijaisesti-hakukohteeseen
     [:button.application-handling__ensisijaisesti-hakukohteeseen-popup-button
      {:on-click #(swap! opened? not)}
      (if (nil? ryhman-ensisijainen-hakukohde)
        (get-virkailija-translation :all-hakukohteet)
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

(defn- application-filters
  []
  (let [filters                    (subscribe [:state-query [:application :filters]])
        filters-checkboxes         (subscribe [:state-query [:application :filters-checkboxes]])
        applications-count         (subscribe [:application/loaded-applications-count])
        has-more?                  (subscribe [:application/has-more-applications?])
        enabled-filter-count       (subscribe [:application/enabled-filter-count])
        review-settings            (subscribe [:state-query [:application :review-settings :config]])
        selected-hakukohde-oid     (subscribe [:state-query [:application :selected-hakukohde]])
        has-base-education-answers (subscribe [:application/applications-have-base-education-answers])
        show-ensisijaisesti?       (subscribe [:application/show-ensisijaisesti?])
        show-rajaa-hakukohteella?  (subscribe [:application/show-rajaa-hakukohteella?])
        filters-changed?           (subscribe [:application/filters-changed?])
        filters-visible            (r/atom false)
        rajaava-hakukohde-opened?  (r/atom false)
        filters-to-include         #{:language-requirement :degree-requirement :eligibility-state :payment-obligation}
        lang                       (subscribe [:editor/virkailija-lang])]
    (fn []
      [:span.application-handling__filters
       [:a
        {:on-click #(do
                      (dispatch [:application/undo-filters])
                      (swap! filters-visible not))}
        [:span
         (gstring/format "%s (%d"
                         (get-virkailija-translation :filter-applications)
                         @applications-count)]
        (when @has-more?
          [:span "+ "
           [:i.zmdi.zmdi-spinner.spin]])
        [:span ")"]]
       (when (pos? @enabled-filter-count)
         [:span
          [:span.application-handling__filters-count-separator "|"]
          [:a
           {:on-click #(dispatch [:application/remove-filters])}
           (get-virkailija-translation :remove-filters)
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
               [:h3.application-handling__filter-group-heading (get-virkailija-translation :ensisijaisuus)]
               [ensisijaisesti]
               (when @show-rajaa-hakukohteella?
                 [select-rajaava-hakukohde rajaava-hakukohde-opened?])])
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading (get-virkailija-translation :ssn)]
             [application-filter-checkbox filters-checkboxes (:without-ssn virkailija-texts) @lang :only-ssn :without-ssn]
             [application-filter-checkbox filters-checkboxes (:with-ssn virkailija-texts) @lang :only-ssn :with-ssn]]
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading (get-virkailija-translation :identifying)]
             [application-filter-checkbox filters-checkboxes (:unidentified virkailija-texts) @lang :only-identified :unidentified]
             [application-filter-checkbox filters-checkboxes (:identified virkailija-texts) @lang :only-identified :identified]]
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading (get-virkailija-translation :active-status)]
             [application-filter-checkbox filters-checkboxes (:active-status-active virkailija-texts) @lang :active-status :active]
             [application-filter-checkbox filters-checkboxes (:active-status-passive virkailija-texts) @lang :active-status :passive]]]
           [:div.application-handling__popup-column
            [:div.application-handling__filter-group
             [:h3.application-handling__filter-group-heading (get-virkailija-translation :handling-notes)]
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
             [:div.application-handling__filter-group
              [:div.application-handling__filter-group-title
               (util/non-blank-val (:eligibility-set-automatically virkailija-texts)
                                   [@lang :fi :sv :en])]
              [:div.application-handling__filter-group-checkboxes
               [application-filter-checkbox filters-checkboxes (:yes general-texts) @lang :eligibility-set-automatically :yes]
               [application-filter-checkbox filters-checkboxes (:no general-texts) @lang :eligibility-set-automatically :no]]]]]
           (when @has-base-education-answers
             [:div.application-handling__popup-column.application-handling__popup-column--large
              [application-base-education-filters filters-checkboxes @lang]])]
          [:div.application-handling__filters-popup-apply-button-container
           [:a.editor-form__control-button.editor-form__control-button--variable-width
            {:class    (if @filters-changed?
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--disabled")
             :on-click (fn [_]
                         (reset! filters-visible false)
                         (dispatch [:application/apply-filters]))}
            (get-virkailija-translation :filters-apply-button)]
           [:a.editor-form__control-button.editor-form__control-button--variable-width
            {:class    (if @filters-changed?
                         "editor-form__control-button--enabled"
                         "editor-form__control-button--disabled")
             :on-click #(dispatch [:application/undo-filters])}
            (get-virkailija-translation :filters-cancel-button)]]])])))

(defn- application-list-header [applications]
  (let [review-settings (subscribe [:state-query [:application :review-settings :config]])]
    [:div.application-handling__list-header.application-handling__list-row
     [:span.application-handling__list-row--applicant
      [application-list-basic-column-header
       "applicant-name"
       (get-virkailija-translation :applicant)]
      [application-filters]]
     [created-time-column-header]
     (when (:attachment-handling @review-settings true)
       [:span.application-handling__list-row--attachment-state
        [hakukohde-state-filter-controls
         :attachment-state-filter
         (get-virkailija-translation :attachments)
         review-states/attachment-hakukohde-review-types-with-no-requirements
         (subscribe [:state-query [:application :attachment-state-counts]])]])
     [:span.application-handling__list-row--state
      [hakukohde-state-filter-controls
       :processing-state-filter
       (get-virkailija-translation :processing-state)
       review-states/application-hakukohde-processing-states
       (subscribe [:state-query [:application :review-state-counts]])]]
     (when (:selection-state @review-settings true)
       [:span.application-handling__list-row--selection
        [hakukohde-state-filter-controls
         :selection-state-filter
         (get-virkailija-translation :selection)
         review-states/application-hakukohde-selection-states
         (subscribe [:state-query [:application :selection-state-counts]])]])]))

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn review-state-selected-row [on-click label multiple-values?]
  (let [settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
        enabled?          (and (not @settings-visible?) @can-edit?)]
    [:div.application-handling__review-state-row.application-handling__review-state-row--selected
     {:on-click #(when enabled? (on-click))
      :class    (if enabled?
                  "application-handling__review-state-row--enabled"
                  "application-handling__review-state-row--disabled")}
     (if multiple-values?
       [:span
        [icon-many-checked]
        [:span (get-virkailija-translation :multiple-values)]
        ]
       [:span
        [icon-check]
        [:span label]
        ])]))

(defn review-state-row [state-name current-review-state lang multiple-values? [review-state-id review-state-label]]
  (if (or (= current-review-state review-state-id)
          multiple-values?)
    [review-state-selected-row #() (get review-state-label lang) multiple-values?]
    [:div.application-handling__review-state-row
     {:on-click (fn []
                  (let [selected-hakukohde-oids @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])]
                    (dispatch [:application/update-review-field state-name review-state-id])))}
     [icon-unselected] (get review-state-label lang)]))

(defn opened-review-state-list [state-name current-state all-states lang multiple-values?]
  (let [current-state (if (and (not multiple-values?)
                               (not current-state))
                        (ffirst all-states)
                        current-state)
        review-rows (mapv (fn [state] [review-state-row state-name current-state lang false state]) all-states)]
    (if multiple-values?
      (cons [review-state-row state-name current-state lang true nil] review-rows)
      review-rows)))

(defn- toggle-review-list-visibility [list-kwd]
  (dispatch [:application/toggle-review-list-visibility list-kwd]))

(defn- application-deactivate-toggle
  []
  (let [state     (subscribe [:state-query [:application :review :state]])
        can-edit? (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    (fn []
      (let [active? (= "active" @state)]
        [:div.application-handling__review-deactivate-row
         [:span.application-handling__review-deactivate-label (get-virkailija-translation :application-state)]
         [:div.application-handling__review-deactivate-toggle
          [:div.application-handling__review-deactivate-toggle-slider
           {:class    (cond-> ""
                              active? (str " application-handling__review-deactivate-toggle-slider-right")
                              (not active?) (str " application-handling__review-deactivate-toggle-slider-left")
                              (not @can-edit?) (str " application-handling__review-deactivate-toggle-slider--disabled"))
            :on-click #(when @can-edit?
                         (dispatch [:application/set-application-activeness (not active?)]))}
           [:div.application-handling__review-deactivate-toggle-label-left
            (get-virkailija-translation :active)]
           [:div.application-handling__review-deactivate-toggle-divider]
           [:div.application-handling__review-deactivate-toggle-label-right
            (get-virkailija-translation :passive)]]]]))))

(defn- hakukohde-name [hakukohde-oid]
  (if-let [hakukohde-name @(subscribe [:application/hakukohde-name
                                       hakukohde-oid])]
    [:span hakukohde-name]
    [:i.zmdi.zmdi-spinner.spin]))

(defn- opened-review-hakukohde-list-row
  [toggle-list-open list-opened hakukohde-oid disabled?]
  (let [selected-hakukohde-oids (subscribe [:state-query [:application :selected-review-hakukohde-oids]])]
    (fn []
      (let [selected? (contains? (set @selected-hakukohde-oids) hakukohde-oid)]
        (when (or @list-opened
                  selected?)
          [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde
           {:data-hakukohde-oid hakukohde-oid
            :class (when disabled?
                     "application-handling__review-state-row--disabled")
            :on-click (when-not disabled? (fn [event] (if @list-opened
                                              (dispatch [:application/select-review-hakukohde hakukohde-oid])
                                              (toggle-list-open))))}
           (if selected?
             (if @list-opened
               [icon-multi-check]
               [icon-check])
             [icon-select])
           [hakukohde-and-tarjoaja-name hakukohde-oid]])))))

(defn- application-hakukohde-selection
  []
  (let [application-hakukohde-oids (subscribe [:state-query [:application :selected-application-and-form :application :hakukohde]])
        list-opened                (r/atom false)
        toggle-list-open           #(swap! list-opened not)]
    (fn []
      (let [hakukohde-count (count @application-hakukohde-oids)]
        (when (not= 0 hakukohde-count)
          [:div.application-handling__review-state-container.application-handling__review-state-container--columnar
           (into
             [:div.application-handling__review-state-list
              {:class (when (not @list-opened)
                        "application-handling__review-state-list--closed")}
              [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde
               {:on-click (when-not (= 1 hakukohde-count) toggle-list-open)
                :class (when (= 1 hakukohde-count) "application-handling__review-state-row--disabled")}
               (gstring/format "%s (%d)"
                 (get-virkailija-translation :hakukohteet)
                 hakukohde-count)
               (when-not (= 1 hakukohde-count)
                 (if @list-opened
                   [:i.zmdi.zmdi-chevron-up.application-handling__review-state-selected-icon]
                   [:i.zmdi.zmdi-chevron-down.application-handling__review-state-selected-icon]))]]
             (map (fn [oid]
                    [opened-review-hakukohde-list-row
                     toggle-list-open
                     list-opened oid
                     (= 1 hakukohde-count)]) @application-hakukohde-oids))])))))

(defn- review-settings-checkbox [setting-kwd]
  (let [checked?  (subscribe [:application/review-state-setting-enabled? setting-kwd])
        disabled? (subscribe [:application/review-state-setting-disabled? setting-kwd])]
    [:input.application-handling__review-state-setting-checkbox
     {:class     (str "application-handling__review-state-setting-checkbox-" (name setting-kwd))
      :type      "checkbox"
      :checked   @checked?
      :disabled  @disabled?
      :on-change #(dispatch [:application/toggle-review-state-setting setting-kwd])}]))

(defn- application-review-note [note-idx]
  (let [note                (subscribe [:state-query [:application :review-notes note-idx]])
        name                (reaction (if (and (:first-name @note) (:last-name @note))
                                        (str (:first-name @note) " " (:last-name @note))
                                        (get-virkailija-translation :unknown-virkailija)))
        created-time        (reaction (when-let [created-time (:created-time @note)]
                                        (temporal/time->short-str created-time)))
        notes               (reaction (:notes @note))
        animated?           (reaction (:animated? @note))
        remove-disabled?    (reaction (or (-> @note :state some?)
                                          (-> @note :id not)))
        hakukohde-name      (subscribe [:application/hakukohde-name (:hakukohde @note)])
        removing?           (r/atom false)
        remove-note         (fn []
                              (dispatch [:application/remove-review-note note-idx])
                              (reset! removing? false))
        start-removing-note (fn []
                              (reset! removing? true)
                              (js/setTimeout #(reset! removing? false) 1200))]
    (fn [note-idx]
      [:div.application-handling__review-note
       (when @animated?
         {:class "animated fadeIn"})
       [:div.application-handling__review-note-details-row
        [:div [:span (str @name " ")] [:span.application-handling__review-note-details-timestamp (str @created-time)]]
        [:div.application-handling__review-note-remove-link
         {:class    (when @remove-disabled? "application-handling__review-note-remove-link--disabled")
          :on-click #(when-not @remove-disabled?
                       (if @removing?
                         (remove-note)
                         (start-removing-note)))}
         (if @removing?
           (get-virkailija-translation :confirm-delete)
           [:i.zmdi.zmdi-close])]]
       [:div.application-handling__review-note-content
        (when (:hakukohde @note)
          {:data-tooltip (str (get-virkailija-translation :eligibility-explanation)
                              (when (not= "form" (:hakukohde @note))
                                (gstring/format " %s %s"
                                  (get-virkailija-translation :for-hakukohde)
                                  @hakukohde-name)))})
        @notes]])))

(defn- review-state-comment
  [state-name]
  (fn [state-name]
    (let [current-hakukohteet @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
          note-state-path     (if (and (seq current-hakukohteet)
                                       (empty? (rest current-hakukohteet)))
                                (first current-hakukohteet)
                                "multiple-selected")
          review-note         (subscribe [:state-query [:application :notes note-state-path state-name]])
          selected-notes-idx  (subscribe [:application/review-note-indexes-on-eligibility])
          button-enabled?     (and (-> @review-note clojure.string/blank? not)
                                   (or (nil? (first @selected-notes-idx))
                                       (not= @review-note (:notes @(subscribe [:state-query [:application :review-notes
                                                                                             (first @selected-notes-idx)]])))))]
      [:div.application-handling__review-state-selected-container
       [:textarea.application-handling__review-note-input
        {:value       @review-note
         :placeholder (get-virkailija-translation :rejection-reason)
         :on-change   (fn [event]
                        (let [note (.. event -target -value)]
                          (dispatch [:state-update #(assoc-in % [:application :notes note-state-path state-name] note)])))}]
       [:button.application-handling__review-note-submit-button
        {:type     "button"
         :on-click #(dispatch [:application/add-review-notes @review-note state-name])
         :disabled (not button-enabled?)
         :class    (if button-enabled?
                     "application-handling__review-note-submit-button--enabled"
                     "application-handling__review-note-submit-button--disabled")}
        (get-virkailija-translation :add)]
       (->> @selected-notes-idx
            (map (fn [idx]
                   ^{:key (str "application-review-note-" idx)}
                   [application-review-note idx])))])))

(defn- application-hakukohde-review-input
  [label kw states]
  (let [current-hakukohteet                (subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        list-click                         (partial toggle-review-list-visibility kw)
        list-opened                        (subscribe [:state-query [:application :ui/review kw]])
        settings-visible?                  (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?                     (subscribe [:application/review-state-setting-enabled? kw])
        eligibility-automatically-checked? (subscribe [:application/eligibility-automatically-checked?])
        lang                               (subscribe [:editor/virkailija-lang])]
    (fn [_ _ _]
      (when (or @settings-visible? @input-visible?)
        (let [review-states-for-hakukohteet (set (doall (map (fn [oid] @(subscribe [:state-query [:application :review :hakukohde-reviews (keyword oid) kw]]))
                                                          @current-hakukohteet)))
              multiple-values? (< 1 (count review-states-for-hakukohteet))
              review-state-for-current (when-not multiple-values? (first review-states-for-hakukohteet))]
          [:div.application-handling__review-state-container
           {:class (str "application-handling__review-state-container-" (name kw))}
           (when @settings-visible?
             [review-settings-checkbox kw])
           [:div.application-handling__review-header
            {:class (str "application-handling__review-header--" (name kw))}
            [:span (util/non-blank-val label [@lang :fi :sv :en])]
            (when (and (= :eligibility-state kw)
                       @eligibility-automatically-checked?)
              [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
               {:title (get-virkailija-translation :eligibility-set-automatically)}])]
           (if @list-opened
             (into [:div.application-handling__review-state-list.application-handling__review-state-list--opened
                     {:on-click list-click}]
                    (opened-review-state-list kw review-state-for-current states @lang multiple-values?))
             [:div.application-handling__review-state-list
              [review-state-selected-row
               list-click
               (application-states/get-review-state-label-by-name
                states
                (or review-state-for-current (ffirst states))
                @lang)
               multiple-values?]
              (when (and (= :eligibility-state kw)
                         (= "uneligible" review-state-for-current))
                [review-state-comment kw])])])))))

(defn- application-hakukohde-review-inputs
  [review-types]
  (into [:div.application-handling__review-hakukohde-inputs]
        (mapv (fn [[kw label states]]
                [application-hakukohde-review-input label kw states])
              review-types)))

(defn- name-and-initials [{:keys [first-name last-name]}]
  (if (and first-name last-name)
    [(str first-name " " last-name)
     (str (subs first-name 0 1)
          (subs last-name 0 1))]
    [nil nil]))

(defn- virkailija-initials-span
  [event]
  (let [[name initials] (name-and-initials event)]
    (when (and name initials)
      [:span.application-handling__review-state-initials {:data-tooltip name} (str "(" initials ")")])))

(defn- update-event-caption [text-span event show-details?]
  [:span.application-handling__event-caption--inner
   {:on-click #(swap! show-details? not)
    :class    "application-handling__event-caption-modify-event"}
   text-span
   (if @show-details?
     [:i.zmdi.zmdi-chevron-up.application-handling__event-caption-chevron]
     [:i.zmdi.zmdi-chevron-down.application-handling__event-caption-chevron])
   "|"
   [:a.application-handling__event-caption-compare
    {:on-click (fn [e]
                 (.stopPropagation e)
                 (dispatch [:application/open-application-version-history event]))}
    (get-virkailija-translation :compare)]])

(defn event-caption [event show-details? lang]
  (match event
         {:event-type "review-state-change"}
         (let [label (application-states/get-review-state-label-by-name
                      review-states/application-review-states
                      (:new-review-state event)
                      lang)]
           [:span.application-handling__event-caption--inner
            label
            " "
            (or (virkailija-initials-span event)
                (get-virkailija-translation :unknown))])

         {:event-type "updated-by-applicant"}
         (update-event-caption
          [:span (gstring/format "%s %d %s"
                                 (get-virkailija-translation :from-applicant)
                                 (count @(subscribe [:application/changes-made-for-event (:id event)]))
                                 (get-virkailija-translation :changes))]
          event
          show-details?)

         {:event-type "updated-by-virkailija"}
         (update-event-caption
           [:span
            (or (virkailija-initials-span event) (get-virkailija-translation :unknown))
            (gstring/format " %s %d %s"
                            (get-virkailija-translation :did)
                            (count @(subscribe [:application/changes-made-for-event (:id event)]))
                            (get-virkailija-translation :changes))]
          event
          show-details?)

         {:event-type "received-from-applicant"}
         (get-virkailija-translation :application-received)

         {:event-type "received-from-virkailija"}
         [:span.application-handling__event-caption--inner
          (virkailija-initials-span event)
          (str " " (get-virkailija-translation :submitted-application))]

         {:event-type "hakukohde-review-state-change"}
         [:div.application-handling__multi-line-event-caption
          [:span.application-handling__event-caption--inner
           (str (->> review-states/hakukohde-review-types
                     (filter #(= (keyword (:review-key event)) (first %)))
                     (first)
                     (second)
                     ((fn [label] (util/non-blank-val label [lang :fi :sv :en])))) ": "
                (application-states/get-review-state-label-by-name
                 (->> review-states/hakukohde-review-types
                      (map last)
                      (apply concat)
                      (distinct))
                 (:new-review-state event)
                 lang))
           " "
           (virkailija-initials-span event)]
          (when (not= "form" (:hakukohde event))
            [:span.application-handling__event-caption--inner.application-handling__event-caption--extra-info
             @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])])]

         {:event-type "eligibility-state-automatically-changed"}
         [:div.application-handling__multi-line-event-caption
          [:span.application-handling__event-caption--inner
           (str (get-virkailija-translation :eligibility)
                ": "
                (some #(when (= (:new-review-state event) (first %))
                         (get (second %) lang))
                      review-states/application-hakukohde-eligibility-states))
           [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
            {:title (get-virkailija-translation :eligibility-set-automatically)}]]
          [:span.application-handling__event-caption--inner.application-handling__event-caption--extra-info
           @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])]]

         {:event-type "attachment-review-state-change"}
         [:span.application-handling__event-caption--inner
          (gstring/format "%s: %s "
                          (get-virkailija-translation :attachment)
                          (application-states/get-review-state-label-by-name
                            review-states/attachment-hakukohde-review-types
                            (:new-review-state event)
                            lang))
          (virkailija-initials-span event)]

         {:event-type "modification-link-sent"}
         (get-virkailija-translation :confirmation-sent)

         {:subject _ :message _ :message-type message-type}
         [:div.application-handling__multi-line-event-caption
          [:span.application-handling__event-caption--inner.application-handling__event-caption-modify-event
           {:on-click #(swap! show-details? not)}
           (str
            (if (= message-type "mass-information-request")
              (get-virkailija-translation :mass-information-request-sent)
              (get-virkailija-translation :information-request-sent))
            " ")
           (virkailija-initials-span event)
           (if @show-details?
             [:i.zmdi.zmdi-chevron-up.application-handling__event-caption-chevron]
             [:i.zmdi.zmdi-chevron-down.application-handling__event-caption-chevron])]]

         :else (get-virkailija-translation :unknown)))

(defn event-row
  [_]
  (let [show-details? (r/atom false)
        lang          (subscribe [:editor/virkailija-lang])]
    (fn [event]
      [:div.application-handling__event-row
       [:div.application-handling__event-row--header
        [:span.application-handling__event-timestamp
         (t/time->short-str (or (:time event) (:created-time event)))]
        [:div.application-handling__event-caption
         (event-caption event show-details? @lang)]]
       (when @show-details?
         (if (or (= (:event-type event) "updated-by-applicant")
                 (= (:event-type event) "updated-by-virkailija"))
           [:ul.application-handling__event-row-details
            (for [[key field] @(subscribe [:application/changes-made-for-event (:id event)])]
              [:li
               {:on-click (fn [e]
                            (.stopPropagation e)
                            (dispatch [:application/highlight-field key]))
                :key      (str "event-list-row-for-" (:id event) "-" key)}
               [:a (:label field)]])]
           [:div.application-handling__event-row--message
            [:span.application-handling__event-row--message-subject
             (:subject event)]
            [:span.application-handling__event-row--message-body
             (:message event)]]))])))

(defn application-review-events []
  [:div.application-handling__event-list
   [:div.application-handling__review-header (get-virkailija-translation :events)]
   (doall
    (map-indexed
     (fn [i event]
       ^{:key (str "event-row-for-" i)}
       [event-row event])
     @(subscribe [:application/events-and-information-requests])))])

(defn- application-review-note-input []
  (let [input-value     (subscribe [:state-query [:application :review-comment]])
        review-notes    (subscribe [:state-query [:application :review-notes]])
        button-enabled? (reaction (and (-> @input-value clojure.string/blank? not)
                                       (every? (comp not :animated?) @review-notes)))]
    (fn []
      [:div.application-handling__review-row.application-handling__review-row--notes-row
       [:textarea.application-handling__review-note-input
        {:type      "text"
         :value     @input-value
         :on-change (fn [event]
                      (let [review-comment (.. event -target -value)]
                        (dispatch [:application/set-review-comment-value review-comment])))}]
       [:button.application-handling__review-note-submit-button
        {:type     "button"
         :class    (if @button-enabled?
                     "application-handling__review-note-submit-button--enabled"
                     "application-handling__review-note-submit-button--disabled")
         :disabled (not @button-enabled?)
         :on-click (fn [_]
                     (dispatch [:application/add-review-note @input-value nil]))}
        (get-virkailija-translation :add)]])))

(defn application-review-notes []
  (let [notes (subscribe [:application/review-note-indexes-excluding-eligibility])]
    (fn []
      [:div.application-handling__review-row--nocolumn
       [:div.application-handling__review-header (get-virkailija-translation :notes)]
       [application-review-note-input]
       (->> @notes
            (map (fn [idx]
                   ^{:key (str "application-review-note-" idx)}
                   [application-review-note idx])))])))

(defn- score->number
  [score]
  (let [maybe-number (js/Number (clojure.string/replace score #"," "."))]
    (cond
      (clojure.string/blank? score) nil
      ; NaN:
      (not= maybe-number maybe-number) nil
      :else maybe-number)))

(defn- valid-display-score?
  [score]
  (or
    (clojure.string/blank? score)
    (if (re-matches #"^[0-9]+[,.]$" score)
      (some? (score->number (apply str (butlast score))))
      (some? (score->number score)))))

(defn application-review-inputs []
  (let [score             (subscribe [:state-query [:application :review :score]])
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?    (subscribe [:application/review-state-setting-enabled? :score])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
        display-value     (r/atom (clojure.string/replace (str @score) #"\." ","))]
    (fn []
      [:div.application-handling__review-inputs
       (when (or @settings-visible? @input-visible?)
         [:div.application-handling__review-row
          (when @settings-visible?
            [review-settings-checkbox :score])
          [:div.application-handling__review-header.application-handling__review-header--points
           (get-virkailija-translation :points)]
          [:input.application-handling__score-input
           {:type      "text"
            :value     @display-value
            :disabled  (or @settings-visible? (not @can-edit?))
            :on-change (when-not @settings-visible?
                         (fn [evt]
                           (let [new-value (-> evt .-target .-value)]
                             (when (valid-display-score? new-value)
                               (reset! display-value new-value))
                             (when-let [number (score->number new-value)]
                               (dispatch [:application/update-review-field :score number])))))}]])])))

(defn- application-modify-link []
  (let [application-key   (subscribe [:state-query [:application :selected-key]])
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    [:a.application-handling__link-button.application-handling__button
     {:href   (when (and (not @settings-visible?) @can-edit?)
                (str "/lomake-editori/api/applications/" @application-key "/modify"))
      :class  (when (or @settings-visible? (not @can-edit?))
                "application-handling__button--disabled")
      :target "_blank"}
     (get-virkailija-translation :edit-application)]))

(defn- application-information-request-recipient []
  (let [email (subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading (get-virkailija-translation :receiver)]
     [:div @email]]))

(defn- application-information-request-subject []
  (let [subject (subscribe [:state-query [:application :information-request :subject]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading "Aihe:"]
     [:div.application-handling__information-request-text-input-container
      [:input.application-handling__information-request-text-input
       {:value     @subject
        :maxLength 200
        :on-change (fn [event]
                     (let [subject (-> event .-target .-value)]
                       (dispatch [:application/set-information-request-subject subject])))}]]]))

(defn- application-information-request-message []
  (let [message (subscribe [:state-query [:application :information-request :message]])]
    [:div.application-handling__information-request-row
     [:textarea.application-handling__information-request-message-area
      {:value     @message
       :on-change (fn [event]
                    (let [message (-> event .-target .-value)]
                      (dispatch [:application/set-information-request-message message])))}]]))

(defn- application-information-request-submit-button []
  (let [enabled?      (subscribe [:application/information-request-submit-enabled?])
        request-state (subscribe [:state-query [:application :information-request :state]])
        button-text   (reaction (if (= @request-state :submitting)
                                  (get-virkailija-translation :sending-information-request)
                                  (get-virkailija-translation :send-information-request)))]
    (fn []
      [:div.application-handling__information-request-row
       [:button.application-handling__send-information-request-button
        {:type     "button"
         :disabled (not @enabled?)
         :class    (if @enabled?
                     "application-handling__send-information-request-button--enabled"
                     "application-handling__send-information-request-button--disabled")
         :on-click #(dispatch [:application/submit-information-request])}
        @button-text]])))

(defn- application-information-request-header []
  (let [request-state (subscribe [:state-query [:application :information-request :state]])]
    [:div.application-handling__information-request-header
     (get-virkailija-translation :send-information-request-to-applicant)
     (when (nil? @request-state)
       [:i.zmdi.zmdi-close-circle.application-handling__information-request-close-button
        {:on-click #(dispatch [:application/set-information-request-window-visibility false])}])]))

(defn- application-information-request-submitted []
  [:div.application-handling__information-request-row.application-handling__information-request-row--checkmark-container
   [:div.application-handling__information-request-submitted-loader]
   [:div.application-handling__information-request-submitted-checkmark]
   [:span.application-handling__information-request-submitted-text (get-virkailija-translation :information-request-sent)]])

(defn- application-information-request-contains-modification-link []
  [:div.application-handling__information-request-row
   [:p.application-handling__information-request-contains-modification-link
    (get-virkailija-translation :edit-link-sent-automatically)]])

(defn- application-information-request []
  (let [window-visible?      (subscribe [:state-query [:application :information-request :visible?]])
        request-window-open? (reaction (if-some [visible? @window-visible?]
                                         visible?
                                         false))
        request-state        (subscribe [:state-query [:application :information-request :state]])]
    (fn []
      (if @request-window-open?
        (let [container [:div.application-handling__information-request-container]]
          (if (= @request-state :submitted)
            (conj container
                  [application-information-request-submitted])
            (conj container
                  [application-information-request-header]
                  [application-information-request-recipient]
                  [application-information-request-subject]
                  [application-information-request-message]
                  [application-information-request-contains-modification-link]
                  [application-information-request-submit-button])))
        [:div.application-handling__information-request-show-container-link
         [:a
          {:on-click #(dispatch [:application/set-information-request-window-visibility true])}
          (get-virkailija-translation :send-information-request-to-applicant)]]))))

(defn- application-resend-modify-link []
  (let [recipient         (subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])
        enabled?          (subscribe [:application/resend-modify-application-link-enabled?])
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    [:button.application-handling__send-information-request-button.application-handling__button
     {:on-click #(dispatch [:application/resend-modify-application-link])
      :disabled (or (not @enabled?)
                    (not @can-edit?)
                    @settings-visible?)
      :class    (str (if (and @enabled? @can-edit?)
                       "application-handling__send-information-request-button--enabled"
                       "application-handling__send-information-request-button--disabled")
                     (if (or @settings-visible? (not @can-edit?))
                       " application-handling__send-information-request-button--cursor-default"
                       " application-handling__send-information-request-button--cursor-pointer"))}
     [:span (get-virkailija-translation :send-confirmation-email-to-applicant)]
     [:span.application-handling__resend-modify-application-link-email-text @recipient]]))

(defn- application-resend-modify-link-confirmation []
  (let [state (subscribe [:state-query [:application :modify-application-link :state]])]
    (when @state
      [:div.application-handling__resend-modify-link-confirmation.application-handling__button.animated.fadeIn
       {:class (when (= @state :disappearing) "animated fadeOut")}
       [:div.application-handling__resend-modify-link-confirmation-indicator]
       (get-virkailija-translation :send-edit-link-to-applicant)])))

(defn- attachment-review-row [all-similar-attachments lang]
  (let [list-opened (r/atom false)]
    (fn [all-similar-attachments lang]
      (let [all-reviews          (map first all-similar-attachments)
            all-states           (set (map :state all-reviews))
            multiple-values?     (seq (rest all-states))
            review               (first all-reviews)
            selected-hakukohteet (map second all-similar-attachments)
            attachment-key       (-> review :key keyword)
            selected-state       (or (when multiple-values?
                                       "multiple-values")
                                     (:state review)
                                     "not-checked")
            can-edit?            (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
            virkailija-lang      (subscribe [:editor/virkailija-lang])
            review-types         (if multiple-values?
                                   review-states/attachment-hakukohde-review-types-with-multiple-values
                                   review-states/attachment-hakukohde-review-types)]
        [:div.application__attachment-review-row
         [:div.application__attachment-review-row-answer-information
          [:p.application__attachment-review-row-label (some #(-> review :label % not-empty) [lang :fi :sv :en])]
          (for [attachment-file (filter identity (-> review :values flatten))
                :let [text (str (:filename attachment-file) " (" (util/size-bytes->str (:size attachment-file)) ")")]]
            ^{:key (:key (str "attachment-file-" attachment-file))}
            [:div
             (if (= (:virus-scan-status attachment-file) "done")
               [:a {:href (str "/lomake-editori/api/files/content/" (:key attachment-file))}
                text]
               text)])]
         (if @list-opened
           [:div.application-handling__review-state-list
            (doall
              (for [[state labels]
                    review-types]
                (let [label (get labels lang)]
                  [:div.application-handling__review-state-row.application-handling__review-state-row--small
                   {:class    (when (= state selected-state) "application-handling__review-state-row--selected application-handling__review-state-row--enabled")
                    :on-click (if (= state selected-state)
                                #(swap! list-opened not)
                                (fn []
                                  (swap! list-opened not)
                                  (doall (map #(dispatch [:application/update-attachment-review attachment-key % state]) selected-hakukohteet))))
                    :key      (str attachment-key label)}
                   (if (= state selected-state) [icon-check]
                                                [icon-unselected]) label])))]
           [:div.application-handling__review-state-row.application-handling__review-state-row--small.application-handling__review-state-row--selected
            {:class    (if @can-edit?
                         "application-handling__review-state-row--enabled"
                         "application-handling__review-state-row--disabled")
             :on-click #(when @can-edit? (swap! list-opened not))}
            [icon-check]
            (application-states/get-review-state-label-by-name review-types selected-state @virkailija-lang)])]))))

(defn- attachment-review-area [reviews review-positioning lang]
  (fn [reviews review-positioning lang]
    [:div.application-handling__attachment-review-container.animated
     {:class (str (when (= :fixed review-positioning)
                    "application-handling__attachment-review-container-floating")
                  (if @(subscribe [:state-query [:application :show-attachment-reviews?]])
                    " fadeInRight"
                    " fadeOutRight"))}
     (when (not-empty reviews)
       [:div
        [:p.application-handling__attachment-review-header
         (gstring/format "%s %s (%d)"
           (if (= "form" (second (first (vals reviews)))) (get-virkailija-translation :of-form) (get-virkailija-translation :of-hakukohde))
           (.toLowerCase (get-virkailija-translation :attachments))
           (count (keys reviews)))]
        (doall (for [all-similar-attachments (vals reviews)]
                 ^{:key (:key (ffirst all-similar-attachments))}
                 [attachment-review-row all-similar-attachments lang]))])]))

(defn application-review []
  (let [review-positioning      (subscribe [:state-query [:application :review-positioning]])
        settings-visible        (subscribe [:state-query [:application :review-settings :visible?]])
        show-attachment-review? (r/atom false)]
    (fn []
      (let [selected-review-hakukohde        @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
            attachment-reviews-for-hakukohde (group-by #(:key (first %))
                                               (mapcat (fn [oid]
                                                         (map (fn [attachments]
                                                                [attachments oid])
                                                           @(subscribe [:application/get-attachment-reviews-for-selected-hakukohde oid]))) selected-review-hakukohde))
            lang                             (subscribe [:application/lang])]
        [:div.application-handling__review-outer
         {:class (when (= :fixed @review-positioning)
                   "application-handling__review-outer-floating")}
         [:a.application-handling__review-area-settings-link
          {:on-click (fn [event]
                       (.preventDefault event)
                       (dispatch [:application/toggle-review-area-settings-visibility]))}
          [:i.application-handling__review-area-settings-button.zmdi.zmdi-settings]]
         [:div.application-handling__review-settings
          {:style (when-not @settings-visible
                    {:visibility "hidden"})
           :class (when (= :fixed @review-positioning)
                    "application-handling__review-settings-floating")}
          [:div.application-handling__review-settings-indicator-outer
           [:div.application-handling__review-settings-indicator-inner]]
          (when (not= :fixed @review-positioning)
            [:div.application-handling__review-settings-header
             [:i.zmdi.zmdi-account.application-handling__review-settings-header-icon]
             [:span.application-handling__review-settings-header-text (get-virkailija-translation :settings)]])]
         [:div.application-handling__review
          (when @show-attachment-review?
            [attachment-review-area attachment-reviews-for-hakukohde @review-positioning @lang])
          [:div.application-handling__review-outer-container
           [application-hakukohde-selection]
           (when (not-empty selected-review-hakukohde)
             [:div
              (when (not-empty attachment-reviews-for-hakukohde)
                [:div.application-handling__attachment-review-toggle-container
                 (when @settings-visible
                   [review-settings-checkbox :attachment-handling])
                 [:span.application-handling__attachment-review-toggle-container-link
                  {:on-click (fn []
                               (when-not @settings-visible
                                 (let [show? (not @show-attachment-review?)]
                                   (dispatch [:state-update #(assoc-in % [:application :show-attachment-reviews?] show?)])
                                   (if show?
                                     (reset! show-attachment-review? show?)
                                     (js/setTimeout #(reset! show-attachment-review? show?) 500)))))}
                  [:span.application-handling__attachment-review-toggle
                   (if @show-attachment-review?
                     [:span [:i.zmdi.zmdi-chevron-right] [:i.zmdi.zmdi-chevron-right]]
                     [:span [:i.zmdi.zmdi-chevron-left] [:i.zmdi.zmdi-chevron-left]])]
                  (gstring/format "%s (%d)"
                    (get-virkailija-translation :attachments)
                    (count (keys attachment-reviews-for-hakukohde)))]])
              [application-hakukohde-review-inputs review-states/hakukohde-review-types]])
           (when @(subscribe [:application/show-info-request-ui?])
             [application-information-request])
           [application-review-inputs]
           [application-review-notes]
           [application-modify-link]
           [application-resend-modify-link]
           [application-resend-modify-link-confirmation]
           [application-deactivate-toggle]
           [application-review-events]]]]))))

(defn notification [link-params]
  (fn [{:keys [text link-text href on-click]}]
    [:div.application__message-display--details-notification (get-virkailija-translation text)
     [:a.application-handling__form-outdated--button.application-handling__button
      {:href     href
       :target   "_blank"
       :on-click on-click}
      [:span (get-virkailija-translation link-text)]]]))

(defn notifications-display []
  (let [selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        selected-review-hakukohde     (subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        latest-form                   (subscribe [:state-query [:application :latest-form]])
        metadata-not-found            (subscribe [:state-query [:application :metadata-not-found]])]
    (fn []
      (let [application (:application @selected-application-and-form)
            person-oid  (-> application :person :oid)
            yksiloity?  (-> application :person :yksiloity)]
        (when (or @latest-form
                  (nil? person-oid)
                  (not yksiloity?)
                  @metadata-not-found)
          [:div.application__message-display.application__message-display--notification
           [:div.application__message-display--exclamation [:i.zmdi.zmdi-alert-triangle]]
           [:div.application__message-display--details
            (when @latest-form
              [notification {:text      :form-outdated
                             :link-text :show-newest-version
                             :on-click  (fn [evt]
                                          (.preventDefault evt)
                                          (select-application (:key application) @selected-review-hakukohde true))}])
            (cond (nil? person-oid)
                  [:div.individualization
                   [notification {:text :creating-henkilo-failed}]]
                  (not yksiloity?)
                  [:div.individualization
                   [notification {:text      :person-not-individualized
                                  :link-text :individualize-in-henkilopalvelu
                                  :href      (str "/henkilo-ui/oppija/"
                                                  person-oid
                                                  "/duplikaatit?permissionCheckService=ATARU")}]])
            (when @metadata-not-found
              [:div.individualization
               [notification {:text :metadata-not-found}]])]])))))

(defn application-heading [application loading?]
  (let [answers            (:answers application)
        pref-name          (-> application :person :preferred-name)
        last-name          (-> application :person :last-name)
        ssn                (-> application :person :ssn)
        birth-date         (-> application :person :birth-date)
        person-oid         (-> application :person :oid)
        email              (get-in answers [:email :value])
        applications-count (:applications-count application)]
    [:div.application__handling-heading
     [:div.application-handling__review-area-main-heading-container
      (when-not loading?
        [:div.application-handling__review-area-main-heading-person-info
         [:div.application-handling__review-area-main-heading-name-row
          (when pref-name
            [:h2.application-handling__review-area-main-heading
             (str last-name ", " pref-name " — " (or ssn birth-date))])]
         [:div.application-handling__review-area-main-heading-application-oid-row
          [:span (:key application)]]
         [:div.application-handling__review-area-main-heading-person-oid-row
          [:div.application-handling__applicant-links
           (when person-oid
             [:a
              {:href   (str "/henkilo-ui/oppija/"
                            person-oid
                            "?permissionCheckService=ATARU")
               :target "_blank"}
              [:i.zmdi.zmdi-account-circle.application-handling__review-area-main-heading-person-icon]
              [:span.application-handling__review-area-main-heading-person-oid
               (str (get-virkailija-translation :student) " " person-oid)]])
           (when person-oid
             [:a
              {:href   (str "/suoritusrekisteri/#/opiskelijat?henkilo=" person-oid)
               :target "_blank"}
              [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
              [:span.application-handling__review-area-main-heading-person-oid
               (get-virkailija-translation :person-completed-education)]])
           (when (> applications-count 1)
             [:a.application-handling__review-area-main-heading-applications-link
              {:on-click (fn [_]
                           (dispatch [:application/navigate
                                      (str "/lomake-editori/applications/search"
                                           "?term=" (or ssn email))]))}
              [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
              [:span.application-handling__review-area-main-heading-person-oid
               (str (get-virkailija-translation :view-applications) " (" applications-count ")")]])]]
         [notifications-display]])
      (when (not (contains? (:answers application) :hakukohteet))
        [:ul.application-handling__hakukohteet-list
         (for [hakukohde-oid (:hakukohde application)]
           ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
           [:li.application-handling__hakukohteet-list-row
            [:div.application-handling__review-area-hakukohde-heading
             [hakukohde-and-tarjoaja-name hakukohde-oid]]])])]
     [:div.application-handling__navigation
      [:a.application-handling__navigation-link
       {:on-click #(dispatch [:application/navigate-application-list -1])}
       [:i.zmdi.zmdi-chevron-left]
       (str " " (get-virkailija-translation :navigate-applications-back))]
      [:span.application-handling__navigation-link-divider "|"]
      [:a.application-handling__navigation-link
       {:on-click #(dispatch [:application/navigate-application-list 1])}
       (str (get-virkailija-translation :navigate-applications-forward) " ")
       [:i.zmdi.zmdi-chevron-right]]]]))

(defn close-application []
  [:a {:href     "#"
       :on-click (fn [event]
                   (.preventDefault event)
                   (dispatch [:application/close-application]))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn- floating-application-review-placeholder
  "Keeps the content of the application in the same place when review-area starts floating (fixed position)"
  []
  [:div.application-handling__floating-application-review-placeholder])

(defn application-review-area []
  (let [selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        expanded?                     (subscribe [:state-query [:application :application-list-expanded?]])
        review-positioning            (subscribe [:state-query [:application :review-positioning]])
        application-loading           (subscribe [:state-query [:application :loading?]])]
    (fn []
      (let [application (:application @selected-application-and-form)]
        (when-not @expanded?
          [:div.application-handling__detail-container
           [close-application]
           [application-heading application @application-loading]
           (if @application-loading
             [:div.application-handling__application-loading-indicator
              [:div.application-handling__application-loading-indicator-spin
               [:i.zmdi.zmdi-spinner.spin]]]
             [:div.application-handling__review-area
              [:div.application-handling__application-contents
               [application-contents @selected-application-and-form]]
              [:span#application-handling__review-position-canary]
              (when (= :fixed @review-positioning) [floating-application-review-placeholder])
              [application-review]])])))))

(defn create-application-paging-scroll-handler
  []
  (fn [_]
    (when-let [end-of-list-element (.getElementById js/document "application-handling__end-of-list-element")]
      (let [element-offset  (-> end-of-list-element .getBoundingClientRect .-bottom)
            viewport-offset (.-innerHeight js/window)]
        (when (< element-offset viewport-offset)
          (.click end-of-list-element))))))

(defn application []
  (let [search-control-all-page (subscribe [:application/search-control-all-page-view?])
        loaded-count            (subscribe [:application/loaded-applications-count])
        applications            (subscribe [:application/applications-to-render])
        has-more?               (subscribe [:application/has-more-applications?])
        loading?                (subscribe [:state-query [:application :fetching-applications?]])
        expanded                (subscribe [:state-query [:application :application-list-expanded?]])]
    (fn []
      [:div
       [:div.application-handling__overview
        [application-search-control]
        (when (not @search-control-all-page)
          [:div.application-handling__bottom-wrapper.select_application_list
           [haku-heading]
           [application-list-header @loaded-count]
           (when (not-empty @applications)
             [application-list-contents @applications])
           (if (empty? @applications)
             (when @loading?
               [:div.application-handling__list-loading-indicator
                [:i.zmdi.zmdi-spinner]])
             (when (and @expanded (or @has-more? (< (count @applications) @loaded-count)))
               [:div#application-handling__end-of-list-element
                {:on-click #(dispatch [:application/show-more-applications (count @applications)])}
                [:i.application-handling__end-of-list-element-spinner.zmdi.zmdi-spinner.spin]]))])]
       (when (not @search-control-all-page)
         [:div.application-handling__review-area-container
          [application-review-area]])])))

(defn create-review-position-handler []
  (let [review-canary-visible        (atom true)
        positioning-change-threshold 45]
    (fn [_]
      (when-let [canary-element (.getElementById js/document "application-handling__review-position-canary")]
        (if (<= (-> canary-element .getBoundingClientRect .-top) positioning-change-threshold)
          (when @review-canary-visible
            (dispatch [:state-update #(assoc-in % [:application :review-positioning] :fixed)])
            (reset! review-canary-visible false))
          (when-not @review-canary-visible
            (dispatch [:state-update #(assoc-in % [:application :review-positioning] :in-flow)])
            (reset! review-canary-visible true)))))))

(defn application-version-history-header [changes-amount]
  (let [event (subscribe [:application/selected-event])]
    (fn []
      (let [changed-by (if (= (:event-type @event) "updated-by-applicant")
                         (.toLowerCase (get-virkailija-translation :applicant))
                         (str (:first-name @event) " " (:last-name @event)))]
        [:div.application-handling__version-history-header
         [:div.application-handling__version-history-header-text
          (str (get-virkailija-translation :diff-from-changes)
               " "
               (t/time->short-str (or (:time @event) (:created-time @event))))]
         [:div.application-handling__version-history-header-sub-text
          [:span.application-handling__version-history-header-virkailija
           changed-by]
          [:span
           (gstring/format " %s %s %s"
                           (get-virkailija-translation :changed)
                           changes-amount
                           (get-virkailija-translation :answers))]]]))))

(defn- application-version-history-list-value [values]
  [:ol.application-handling__version-history-list-value
   (map-indexed
    (fn [index value]
      ^{:key index}
      [:li.application-handling__version-history-list-value-item
       value])
    values)])

(defn application-version-history-value [value-or-values]
  (cond
    (every? sequential? value-or-values)
    [:ol.application-handling__version-history-question-group-value
     (map-indexed
      (fn [index values]
        ^{:key index}
        [:li.application-handling__version-history-question-group-value-item
         (application-version-history-list-value values)])
      value-or-values)]

    (sequential? value-or-values)
    (application-version-history-list-value value-or-values)

    :else [:span (str value-or-values)]))

(defn- application-version-history-sub-row
  [left right]
  [:div.application-handling__version-history-sub-row
   [:div.application-handling__version-history-sub-row__left
    left]
   [:div.application-handling__version-history-sub-row__right
    right]])

(defn- breadcrumb-label
  [history-item]
  [:span.application-handling__version-history-row-breadcrumb
   (->> (:label history-item)
        (map-indexed (fn [i [label value]]
                       ^{:key (str "breadcrumb-" i "-" label)}
                       [^{:key (str "breadcrumb-" i "-" label "-label")}
                        [:span.application-handling__version-history-row-breadcrumb-label
                         label ": "]
                        (when (some? value)
                          ^{:key (str "breadcrumb-" i "-" label "-value")}
                          [:span.application-handling__version-history-row-breadcrumb-value
                           "\"" value "\" > "])]))
        (mapcat identity))])

(defn application-version-history-row [key history-item]
  ^{:key (str "application-change-history-" key)}
  [:div.application-handling__version-history-row
   [application-version-history-sub-row
    nil
    (breadcrumb-label history-item)]
   [application-version-history-sub-row
    [:span.application-handling__version-history-value-label
     (get-virkailija-translation :diff-removed)]
    [:div.application-handling__version-history-value.application-handling__version-history-value__old
     (application-version-history-value (:old history-item))]]
   [application-version-history-sub-row
    [:span.application-handling__version-history-value-label
     (get-virkailija-translation :diff-added)]
    [:div.application-handling__version-history-value.application-handling__version-history-value__new
     (application-version-history-value (:new history-item))]]])

(defn application-version-changes []
  (let [history-items (subscribe [:application/current-history-items])]
    (when @history-items
      [modal/modal
       #(dispatch [:application/close-application-version-history])
       [:div.application-handling__version-history
        [application-version-history-header (count @history-items)]
        (for [[key item] @history-items]
          ^{:key (str "application-history-row-for-" key)}
          [application-version-history-row key item])]])))
