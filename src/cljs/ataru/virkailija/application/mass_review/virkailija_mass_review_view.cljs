(ns ataru.virkailija.application.mass-review.virkailija-mass-review-view
  (:require
    [ataru.application.review-states :as review-states]
    [ataru.virkailija.application.view.virkailija-application-icons :as icons]
    [reagent.core :as r]
    [re-frame.core :refer [subscribe dispatch]]))

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
   [icons/icon-check] label])

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
       [icons/icon-unselected] label-with-count])))

(defn- opened-mass-review-state-list
  [current-state states review-state-counts disable-empty-rows?]
  (mapv (partial mass-review-state-row current-state states review-state-counts disable-empty-rows?) (map first states)))

(defn mass-update-applications-link
  []
  (let [visible?                   (subscribe [:state-query [:application :mass-update :visible?]])
        from-list-open?            (r/atom false)
        to-list-open?              (r/atom false)
        submit-button-state        (r/atom :submit)
        selected-from-review-state (r/atom nil)
        selected-to-review-state   (r/atom nil)
        haku-header                (subscribe [:application/list-heading-data-for-haku])
        review-state-counts        (subscribe [:state-query [:application :review-state-counts]])
        loading?                   (subscribe [:application/fetching-applications?])
        tutu-form-visible?         (subscribe [:tutu-payment/tutu-form-selected?])
        processing-states          (if @tutu-form-visible?
                                       review-states/application-hakukohde-processing-states
                                       review-states/application-hakukohde-processing-states-normal)
        all-states                 (reduce (fn [acc [state _]]
                                             (assoc acc state 0))
                                           {}
                                           processing-states)]
    (fn []
      (let [from-states (merge all-states @review-state-counts)]
        [:span.application-handling__mass-edit-review-states-container
         [:a.application-handling__mass-edit-review-states-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
          {:on-click #(dispatch [:application/set-mass-update-popup-visibility true])}
          @(subscribe [:editor/virkailija-translation :mass-edit])]
         (when @visible?
           [:div.application-handling__mass-edit-review-states-popup.application-handling__popup
            [:div.application-handling__mass-edit-review-states-title-container
             [:h4.application-handling__mass-edit-review-states-title
              @(subscribe [:editor/virkailija-translation :mass-edit])]
             [:button.virkailija-close-button
              {:on-click #(dispatch [:application/set-mass-update-popup-visibility false])}
              [:i.zmdi.zmdi-close]]]
            (when-let [[haku-oid hakukohde-oid _ _ _] @haku-header]
              [:p
               @(subscribe [:application/haku-name haku-oid])
               (when hakukohde-oid
                 (str ", " @(subscribe [:application/hakukohde-name hakukohde-oid])))])
            [:h4.application-handling__mass-edit-review-states-heading @(subscribe [:editor/virkailija-translation :from-state])]

            (if @from-list-open?
              (into [:div.application-handling__review-state-list.application-handling__review-state-list--opened
                     {:on-click #(swap! from-list-open? not)}]
                    (opened-mass-review-state-list selected-from-review-state from-states @review-state-counts true))
              (mass-review-state-selected-row
                (fn []
                  (swap! from-list-open? not)
                  (reset! submit-button-state :submit))
                (selected-or-default-mass-review-state-label selected-from-review-state from-states @review-state-counts)))

            [:h4.application-handling__mass-edit-review-states-heading @(subscribe [:editor/virkailija-translation :to-state])]

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
                  (str @(subscribe [:editor/virkailija-translation :change])
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
               @(subscribe [:editor/virkailija-translation :confirm-change])])])]))))

(defn mass-review-notes-applications-link
  []
  (let [visible?            (subscribe [:state-query [:application :mass-review-notes :visible?]])
        mass-review-note    (subscribe [:state-query [:application :mass-review-notes :review-notes]])
        applications-count  (subscribe [:application/loaded-applications-count])
        form-status        (subscribe [:application/mass-review-notes-form-status])
        button-enabled?    (subscribe [:application/mass-review-notes-button-enabled?])]
    (fn []
      [:span.application-handling__mass-edit-review-notes-container
       [:a.application-handling__mass-edit-review-notes-link.editor-form__control-button.editor-form__control-button--enabled.editor-form__control-button--variable-width
        {:on-click #(dispatch [:application/set-mass-review-notes-popup-visibility true])}
        @(subscribe [:editor/virkailija-translation :mass-review-notes])]
       (when @visible?
         [:div.application-handling__popup.application-handling__mass-review-notes-popup
          [:div.application-handling__mass-edit-review-states-title-container
           [:h4.application-handling__mass-edit-review-states-title
            @(subscribe [:editor/virkailija-translation :mass-review-notes])]
           [:button.virkailija-close-button
            {:on-click #(dispatch [:application/set-mass-review-notes-popup-visibility false])}
            [:i.zmdi.zmdi-close]]]
          [:p @(subscribe [:editor/virkailija-translation :mass-review-notes-n-applications @applications-count])]
          [:div.application-handling__mass-review-notes-row
           [:div.application-handling__mass-review-notes-heading @(subscribe [:editor/virkailija-translation :mass-review-notes-content])]]
          [:div.application-handling__mass-review-notes-row
           [:textarea.application-handling__information-request-message-area.application-handling__information-request-message-area--large
            {:value     (or @mass-review-note "")
             :on-change #(dispatch [:application/set-mass-review-notes (-> % .-target .-value)])}]]

          (case @form-status
            (:disabled :enabled nil)
            [:button.application-handling__mass-review-notes-button
             (let [enabled? @button-enabled?]
               {:disabled (not enabled?)
                :class    (if enabled?
                            "application-handling__send-information-request-button--enabled"
                            "application-handling__send-information-request-button--disabled")
                :on-click #(dispatch [:application/confirm-mass-review-notes])})
             @(subscribe [:editor/virkailija-translation :submit])]

            :loading-applications
            [:button.application-handling__mass-review-notes-button.application-handling__send-information-request-button--disabled
             {:disabled true}
             [:span (str @(subscribe [:editor/virkailija-translation :mass-information-request-send]) " ")
              [:i.zmdi.zmdi-spinner.spin]]]

            :confirm
            [:button.application-handling__mass-review-notes-button.application-handling__send-information-request-button--confirm
             {:on-click #(dispatch [:application/mass-update-application-review-notes @mass-review-note])}
             @(subscribe [:editor/virkailija-translation :mass-review-notes-confirm-n-applications
                            @applications-count])]

            :submitting
            [:div.application-handling__information-request-status
             [:i.zmdi.zmdi-hc-lg.zmdi-spinner.spin.application-handling__information-request-status-icon]
             @(subscribe [:editor/virkailija-translation :mass-review-notes-saving])]

            :submitted
            [:div.application-handling__information-request-status
             [:i.zmdi.zmdi-hc-lg.zmdi-check-circle.application-handling__information-request-status-icon.application-handling__information-request-status-icon--sent]
             @(subscribe [:editor/virkailija-translation :mass-information-request-messages-sent])])])
])))
