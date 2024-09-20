(ns ataru.virkailija.application.application-review-view
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as cljs-util]
            [ataru.util :as util]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.application-authorization-subs]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-subs]
            [ataru.virkailija.application.attachments.liitepyynto-information-request-view :as lir]
            [ataru.virkailija.application.attachments.virkailija-attachment-handlers]
            [ataru.virkailija.application.attachments.virkailija-attachment-subs]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-handlers]
            [ataru.virkailija.application.pohjakoulutus-toinen-aste.pohjakoulutus-toinen-aste-subs]
            [ataru.virkailija.application.hyvaksynnan-ehto.view :as hyvaksynnan-ehto]
            [ataru.virkailija.application.kevyt-valinta.view.virkailija-kevyt-valinta-view :as kv]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-handlers]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-subs]
            [ataru.virkailija.application.kevyt-valinta.virkailija-kevyt-valinta-translations :as kvt]
            [ataru.virkailija.application.mass-information-request.virkailija-mass-information-request-handlers]
            [ataru.virkailija.application.information-request.virkailija-information-request-handlers]
            [ataru.virkailija.application.information-request.virkailija-information-request-view :as single-information-request-view]
            [ataru.virkailija.application.view.virkailija-application-icons :as icons]
            [ataru.virkailija.application.view.virkailija-application-names :as names]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.views.virkailija-readonly :as readonly-contents]
            [cljs.core.match :refer-macros [match]]
            [clojure.set :as set]
            [clojure.string :as string]
            [goog.string :as gstring]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe dispatch]]
            [ataru.virkailija.application.payment.payment-view :refer [application-tutu-payment-status application-astu-payment-status]]))

(defn- application-contents [{:keys [form application]} hakukohteet]
  [readonly-contents/readonly-fields form application hakukohteet])

(defn- review-state-selected-row [state-name on-click label multiple-values?]
  (let [editable?                                 (subscribe [:application/review-field-editable? state-name])
        rights-to-edit-reviews-for-selected?      @(subscribe [:application/rights-to-view-reviews-for-selected-hakukohteet?])]
    [:div.application-handling__review-state-row.application-handling__review-state-row--selected
     {:on-click #(when (and @editable? rights-to-edit-reviews-for-selected?) (on-click))
      :class    (if (and @editable? rights-to-edit-reviews-for-selected?)
                  "application-handling__review-state-row--enabled"
                  "application-handling__review-state-row--disabled")}
     (if multiple-values?
       [:span
        [icons/icon-many-checked]
        [:span @(subscribe [:editor/virkailija-translation :multiple-values])]]
       [:span
        [icons/icon-check]
        [:span label]])]))

(defn- review-state-row [state-name current-review-state lang multiple-values? [review-state-id review-state-label]]
  (if (or (= current-review-state review-state-id)
          multiple-values?)
    [review-state-selected-row state-name #() (get review-state-label lang) multiple-values?]
    [:div.application-handling__review-state-row
     {:on-click (fn []
                  (dispatch [:application/update-review-field state-name review-state-id]))}
     [icons/icon-unselected] (get review-state-label lang)]))

(defn- opened-review-state-list [state-name current-state all-states lang multiple-values?]
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
  (let [state           (subscribe [:state-query [:application :review :state]])
        can-edit?       (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
        can-deactivate? (subscribe [:application/can-deactivate-application])]
    (fn []
      (let [active?     (= "active" @state)
            can-change? (and @can-edit? @can-deactivate?)]
        [:div.application-handling__review-deactivate-row
         [:span.application-handling__review-deactivate-label @(subscribe [:editor/virkailija-translation :application-state])]
         [:div.application-handling__review-deactivate-toggle
          [:div.application-handling__review-deactivate-toggle-slider
           {:class    (cond-> ""
                              active? (str " application-handling__review-deactivate-toggle-slider-right")
                              (not active?) (str " application-handling__review-deactivate-toggle-slider-left")
                              (not can-change?) (str " application-handling__review-deactivate-toggle-slider--disabled"))
            :title    (when (and @can-edit?
                                 (not @can-deactivate?))
                        @(subscribe [:editor/virkailija-translation :cannot-deactivate-info]))
            :on-click #(when can-change?
                         (dispatch [:application/set-application-activeness (not active?)]))}
           [:div.application-handling__review-deactivate-toggle-label-left
            @(subscribe [:editor/virkailija-translation :active])]
           [:div.application-handling__review-deactivate-toggle-divider]
           [:div.application-handling__review-deactivate-toggle-label-right
            @(subscribe [:editor/virkailija-translation :passive])]]]]))))

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
            :on-click (when-not disabled? (fn [_] (if @list-opened
                                                    (dispatch [:application/select-review-hakukohde hakukohde-oid])
                                                    (toggle-list-open))))}
           (if selected?
             (if @list-opened
               [icons/icon-multi-check]
               [icons/icon-check])
             [icons/icon-select])
           [names/hakukohde-and-tarjoaja-name hakukohde-oid]])))))

(defn- application-hakukohde-selection
  []
  (let [application-hakukohde-oids (subscribe [:state-query [:application :selected-application-and-form :application :hakukohde]])
        list-opened                (r/atom false)
        toggle-list-open           #(swap! list-opened not)]
    (fn []
      (let [hakukohde-count (count @application-hakukohde-oids)
            disabled?       (or (= 1 hakukohde-count) (not @(subscribe [:application/review-field-editable? :hakukohde])))]
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
                               @(subscribe [:editor/virkailija-translation :hakukohteet])
                               hakukohde-count)
               (when-not (= 1 hakukohde-count)
                 (if @list-opened
                   [:i.zmdi.zmdi-chevron-up.application-handling__review-state-selected-icon]
                   [:i.zmdi.zmdi-chevron-down.application-handling__review-state-selected-icon]))]]
             (map (fn [oid]
                    [opened-review-hakukohde-list-row
                     toggle-list-open
                     list-opened oid
                     disabled?]) @application-hakukohde-oids))])))))

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
                                        @(subscribe [:editor/virkailija-translation :unknown-virkailija])))
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
        lang                (subscribe [:editor/virkailija-lang])
        details-folded?     (r/atom true)
        start-removing-note (fn []
                              (reset! removing? true)
                              (js/setTimeout #(reset! removing? false) 1200))]
    (fn [_]
      [:div.application-handling__review-note
       (when @animated?
         {:class "animated fadeIn"})
       [:div.application-handling__review-note-summary-row
        [:span.application-handling__review-note-summary-text
         {:on-click #(swap! details-folded? not)}
         (if @details-folded?
           [:i.zmdi.zmdi-chevron-up]
           [:i.zmdi.zmdi-chevron-down])
         (str " " @created-time " " @name)]
        [:div.application-handling__review-note-remove-link
         {:class    (when @remove-disabled? "application-handling__review-note-remove-link--disabled")
          :on-click #(when-not @remove-disabled?
                       (if @removing?
                         (remove-note)
                         (start-removing-note)))}
         (if @removing?
           @(subscribe [:editor/virkailija-translation :confirm-delete])
           [:i.zmdi.zmdi-close])]]
       (when-not @details-folded?
         [:div.application-handling__review-note-details-row
          (when-let [name @hakukohde-name]
            [:div name])
          [:ul.application-handling__review-note-organizations-list
           (doall
             (for [org  (:virkailija-organizations @note)
                   :let [oid (:oid org)]]
               ^{:key oid}
               [:li
                [:a
                 {:key    oid
                  :href   (str "/organisaatio-service/lomake/" oid)
                  :target "_blank"}
                 (util/non-blank-val (:name org) [@lang :fi :sv :en])]]))]])
       [:div.application-handling__review-note-content
        (when (:hakukohde @note)
          {:data-tooltip (str @(subscribe [:editor/virkailija-translation :eligibility-explanation])
                              (when (not= "form" (:hakukohde @note))
                                (gstring/format " %s %s"
                                                @(subscribe [:editor/virkailija-translation :for-hakukohde])
                                                @hakukohde-name)))})
        @notes]])))

(defn- review-state-comment
  [_]
  (fn [state-name]
    (let [current-hakukohteet @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
          note-state-path     (if (and (seq current-hakukohteet)
                                       (empty? (rest current-hakukohteet)))
                                (first current-hakukohteet)
                                "multiple-selected")
          review-note         @(subscribe [:state-query [:application :notes note-state-path state-name]])
          selected-notes-idx  @(subscribe [:application/review-note-indexes-on-eligibility])
          latest-note         (if-let [idx (first selected-notes-idx)]
                                @(subscribe [:state-query [:application :review-notes idx :notes]])
                                "")
          button-disabled?    (or (string/blank? review-note)
                                  (= review-note latest-note))]
      [:div.application-handling__review-state-comment-container
       [:textarea.application-handling__review-note-input
        {:value       review-note
         :placeholder @(subscribe [:editor/virkailija-translation :rejection-reason])
         :on-change   (fn [event]
                        (let [note (.. event -target -value)]
                          (dispatch [:state-update #(assoc-in % [:application :notes note-state-path state-name] note)])))}]
       [:button.application-handling__review-note-submit-button
        {:type     "button"
         :on-click (fn [_]
                     (dispatch [:state-update #(assoc-in % [:application :notes note-state-path state-name] "")])
                     (dispatch [:application/add-review-notes review-note state-name]))
         :disabled button-disabled?
         :class    (if button-disabled?
                     "application-handling__review-note-submit-button--disabled"
                     "application-handling__review-note-submit-button--enabled")}
        @(subscribe [:editor/virkailija-translation :add])]
       [:div.application-handling__review-state-comment--notes
        (map (fn [idx]
               ^{:key (str "application-review-note-" idx)}
               [application-review-note idx])
             selected-notes-idx)]])))

(defn- application-review-header [label kw]
  (let [eligibility-automatically-checked?        (subscribe [:application/eligibility-automatically-checked?])
        payment-obligation-automatically-checked? (subscribe [:application/payment-obligation-automatically-checked?])
        lang                                      (subscribe [:editor/virkailija-lang])]
    [:div.application-handling__review-header
     {:class (str "application-handling__review-header--" (name kw))}
     [:span (util/non-blank-val label [@lang :fi :sv :en])]
     (cond (and (= :eligibility-state kw)
                @eligibility-automatically-checked?)
       [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
        {:title @(subscribe [:editor/virkailija-translation :eligibility-set-automatically])}]
       (and (= :payment-obligation kw)
            @payment-obligation-automatically-checked?)
       [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
        {:title @(subscribe
                  [:editor/virkailija-translation :payment-obligation-set-automatically])}])]))

(defn review-state-list [kw states]
  (let [current-hakukohteet                       (subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        review-states-for-hakukohteet             (set (doall (map (fn [oid]
                                                                     @(subscribe [:state-query [:application :review :hakukohde-reviews (keyword oid) kw]]))
                                                                   @current-hakukohteet)))
        multiple-values?                          (< 1 (count review-states-for-hakukohteet))
        review-state-for-current                  (when-not multiple-values? (first review-states-for-hakukohteet))
        list-click                                (partial toggle-review-list-visibility kw)
        list-opened                               (subscribe [:state-query [:application :ui/review kw]])
        lang                                      (subscribe [:editor/virkailija-lang])]
    [:div.application-handling__review-state-list-container
     (if @list-opened
       (into
        [:div.application-handling__review-state-list.application-handling__review-state-list--opened
         {:on-click list-click}]
        (opened-review-state-list kw review-state-for-current states @lang multiple-values?))
       [:div.application-handling__review-state-list
        [review-state-selected-row
         kw
         list-click
         (application-states/get-review-state-label-by-name
          states
          (or review-state-for-current (ffirst states))
          @lang)
         multiple-values?]])
     (when
       (and (= :eligibility-state kw)
            (= "uneligible" review-state-for-current))
       [review-state-comment kw])]))

(defn- show-processing-state
  [processing-state-kw]
  (let [only-opinto-ohjaaja       @(subscribe [:editor/all-organizations-have-only-opinto-ohjaaja-rights?])
        toisen-asteen-yhteishaku? @(subscribe [:application/toisen-asteen-yhteishaku-selected?])
        rights-to-view-reviews?   @(subscribe [:application/rights-to-view-reviews-for-selected-hakukohteet?])]
    ;; 2. asteen yhteishaussa opoilta piilotetaan käsittely- ja valintatiedot
    ;; jos on sekä opo että käsittelijä, piilotetaan jos valittuna muu kuin oma hakukohde
    (if (or (= :processing-state processing-state-kw)
            (= :selection-state processing-state-kw))
      (not
       (and
        toisen-asteen-yhteishaku?
        (or only-opinto-ohjaaja
            (not rights-to-view-reviews?))))
      true)))

(defn- application-hakukohde-review-input
  [label kw states]
  (let [settings-visible?                         (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?                            (subscribe [:application/review-state-setting-enabled? kw])]
    (fn [_ _ _]
      (when (or @settings-visible? @input-visible?)
        (when (show-processing-state kw)
          [:div.application-handling__review-state-container
           {:class (str "application-handling__review-state-container-" (name kw))}
           (when @settings-visible?
             [review-settings-checkbox kw])
           [application-review-header label kw]
           [review-state-list kw states]])))))

(defn- ehdollisesti-hyvaksyttavissa
  []
  (let [hakukohde-oids  @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        application-key @(subscribe [:state-query [:application :selected-key]])]
    [:div.application-handling__hyvaksynnan-ehto-container
     [:div.application-handling__hyvaksynnan-ehto-container__right-column
      [hyvaksynnan-ehto/hyvaksynnan-ehto application-key hakukohde-oids]]]))

(defn- application-hakukohde-review-inputs
  [review-types application-key]
  (let [show-ehdollisesti-hyvaksyttavissa? @(subscribe [:hyvaksynnan-ehto/show?])
        show-kevyt-valinta?                @(subscribe [:virkailija-kevyt-valinta/show-kevyt-valinta? application-key])
        show-selection-state-dropdown?     @(subscribe [:virkailija-kevyt-valinta/show-selection-state-dropdown?])
        hakukohde-review-input-components  (->> review-types
                                                (filter (fn [[kw]]
                                                          (or (not= kw :selection-state)
                                                              show-selection-state-dropdown?)))
                                                (map (fn [[kw label states]]
                                                       [application-hakukohde-review-input label kw states]))
                                                (into [:div.application-handling__review-hakukohde-inputs]))]
    (cond-> hakukohde-review-input-components
            show-ehdollisesti-hyvaksyttavissa?
            (conj [ehdollisesti-hyvaksyttavissa])
            show-kevyt-valinta?
            (conj [kv/kevyt-valinta]))))

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
      [:span {:data-tooltip name} (str "(" initials ")")])))

(defn- update-event [caption event]
  [[:span caption "| " [:a
                        {:on-click (fn [e]
                                     (.stopPropagation e)
                                     (dispatch [:application/open-application-version-history event]))}
                        @(subscribe [:editor/virkailija-translation :compare])]]
   [:ul.application-handling__event-row-update-list
    (for [[key field] @(subscribe [:application/changes-made-for-event (:id event)])]
      [:li
       {:on-click (fn [e]
                    (.stopPropagation e)
                    (dispatch [:application/highlight-field key]))
        :key      (str "event-list-row-for-" (:id event) "-" key)}
       [:a (:label field)]])]])

(defn- event-organizations-list
  [event lang]
  (when (some? (:virkailija-organizations event))
    [:ul.application-handling__event-row-organization-list
     (doall
       (for [org  (:virkailija-organizations event)
             :let [oid (:oid org)]]
         ^{:key oid}
         [:li
          [:a
           {:key    oid
            :href   (str "/organisaatio-service/lomake/" oid)
            :target "_blank"}
           (util/non-blank-val (:name org) [lang :fi :sv :en])]]))]))

(defn- event-content [event lang & _]
  (match event
         {:event-type "review-state-change"}
         (let [label (application-states/get-review-state-label-by-name
                       review-states/application-review-states
                       (:new-review-state event)
                       lang)]
           [[:span label " " (or (virkailija-initials-span event)
                                 @(subscribe [:editor/virkailija-translation :unknown]))]
            nil])

         {:event-type "updated-by-applicant"}
         (update-event
           (gstring/format "%s %d %s"
                           @(subscribe [:editor/virkailija-translation :from-applicant])
                           (count @(subscribe [:application/changes-made-for-event (:id event)]))
                           @(subscribe [:editor/virkailija-translation :changes]))
           event)

         {:event-type "updated-by-virkailija"}
         (update-event
           [:span
            (or (virkailija-initials-span event) @(subscribe [:editor/virkailija-translation :unknown]))
            (gstring/format " %s %d %s"
                            @(subscribe [:editor/virkailija-translation :did])
                            (count @(subscribe [:application/changes-made-for-event (:id event)]))
                            @(subscribe [:editor/virkailija-translation :changes]))]
           event)

         {:event-type "received-from-applicant"}
         [[:span @(subscribe [:editor/virkailija-translation :application-received])]
          nil]

         {:event-type "received-from-virkailija"}
         [[:span
           (virkailija-initials-span event)
           " "
           @(subscribe [:editor/virkailija-translation :submitted-application])]
          nil]

         {:event-type "hakukohde-review-state-change"}
         [[:span
           (->> review-states/hakukohde-review-types
                (filter #(= (keyword (:review-key event)) (first %)))
                (first)
                (second)
                ((fn [label] (util/non-blank-val label [lang :fi :sv :en]))))
           ": "
           (application-states/get-review-state-label-by-name
             (->> review-states/hakukohde-review-types
                  (map last)
                  (apply concat)
                  (distinct))
             (:new-review-state event)
             lang)
           " "
           (virkailija-initials-span event)]
          (let [hakukohde (:hakukohde event)
                org-list  (event-organizations-list event lang)]
            (when (or (not= "form" hakukohde)
                      (some? org-list))
              [:div
               (when (not= "form" (:hakukohde event))
                 [:span @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])])
               org-list]))]

         {:event-type "eligibility-state-automatically-changed"}
         [[:span
           @(subscribe [:editor/virkailija-translation :eligibility])
           ": "
           (some #(when (= (:new-review-state event) (first %))
                    (get (second %) lang))
                 review-states/application-hakukohde-eligibility-states)
           [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
            {:title @(subscribe [:editor/virkailija-translation :eligibility-set-automatically])}]]
          [:span @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])]]

         {:event-type "payment-obligation-automatically-changed"}
         [[:span
           @(subscribe [:editor/virkailija-translation :payment-obligation])
           ": "
           (some #(when (= (:new-review-state event) (first %))
                    (get (second %) lang))
                 review-states/application-payment-obligation-states)
           [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
            {:title @(subscribe [:editor/virkailija-translation :payment-obligation-set-automatically])}]]
          [:span @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])]]

         {:event-type "attachment-review-state-change"}
         [[:span
           (gstring/format "%s: %s "
                           @(subscribe [:editor/virkailija-translation :attachment])
                           (application-states/get-review-state-label-by-name
                             review-states/attachment-hakukohde-review-types
                             (:new-review-state event)
                             lang))
           (virkailija-initials-span event)]
          nil]

         {:event-type "modification-link-sent"}
         [[:span @(subscribe [:editor/virkailija-translation :confirmation-sent])]
          nil]

         {:event-type "field-deadline-set"}
         [[:span (str @(subscribe [:editor/virkailija-translation :liitepyynto-deadline-set]) " ")
           (virkailija-initials-span event)]
          [:div
           [:div
            [:span (str @(subscribe [:editor/virkailija-translation :attachment]) ": ")]
            [:span @(subscribe [:application/field-label (:review-key event)])]]
           [:div
            [:span (str @(subscribe [:editor/virkailija-translation :liitepyynto-deadline-date]) ": ")]
            [:span (temporal/time->short-str (:new-review-state event))]]
           (event-organizations-list event lang)]]

         {:event-type "field-deadline-unset"}
         [[:span (str @(subscribe [:editor/virkailija-translation :liitepyynto-deadline-unset]) " ")
           (virkailija-initials-span event)]
          [:div
           [:div
            [:span (str @(subscribe [:editor/virkailija-translation :attachment]) ": ")]
            [:span @(subscribe [:application/field-label (:review-key event)])]]
           (event-organizations-list event lang)]]

         {:event-type "ehto-hakukohteessa-set"}
         [[:span @(subscribe [:editor/virkailija-translation :ehdollisesti-hyvaksyttavissa])]
          [:div
           [:div @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])]
           [:div.application-handling__event-row--ehto-hakukohteessa
            (util/non-blank-val (:ehto event) [lang :fi :sv :en])]]]

         {:event-type "ehto-hakukohteessa-unset"}
         [[:span @(subscribe [:editor/virkailija-translation :ei-ehdollisesti-hyvaksyttavissa])]
          [:div
           [:div
            [:span @(subscribe [:application/hakukohde-and-tarjoaja-name (:hakukohde event)])]]]]

         {:event-type "kevyt-valinta-valinnan-tila-change" :valinnan-tila valinnan-tila}
         (let [translation-key              (kvt/kevyt-valinta-value-translation-key
                                              :kevyt-valinta/valinnan-tila
                                              valinnan-tila)
               event-text                   @(subscribe [:editor/virkailija-translation
                                                         translation-key
                                                         valinnan-tila])]
           [[:span event-text]])

         {:event-type "person-found-matching"}
         [[:span @(subscribe [:editor/virkailija-translation :person-found-matching])]
          nil]

         {:event-type "person-dob-or-gender-conflict"}
         [[:span @(subscribe [:editor/virkailija-translation :person-dob-or-gender-conflict])]
          nil]

         {:subject _ :message _ :message-type message-type}
         [[:span
           (case message-type
             "mass-information-request" @(subscribe [:editor/virkailija-translation :mass-information-request-sent])
             "single-information-request" @(subscribe [:editor/virkailija-translation :single-information-request-sent])
             "information-request" @(subscribe [:editor/virkailija-translation :information-request-sent])
             )
           " "
           (virkailija-initials-span event)]
          [:div.application-handling__event-row--message
           [:span.application-handling__event-row--message-subject
            (:subject event)]
           [:span.application-handling__event-row--message-body
            (:message event)]]]

         :else
         [[:span @(subscribe [:editor/virkailija-translation :unknown])]
          nil]))

(defn- event-row
  [_]
  (let [show-details?    (r/atom false)
        lang             (subscribe [:editor/virkailija-lang])
        korkeakouluhaku? (subscribe [:virkailija-kevyt-valinta/korkeakouluhaku?])]
    (fn [event]
      (let [[caption details] (event-content event @lang :korkeakouluhaku? @korkeakouluhaku?)]
        [:div.application-handling__event-row
         [:div.application-handling__event-row-header
          {:on-click #(swap! show-details? not)
           :class    (when (some? details) "application-handling__event-row-header--clickable")}
          [:div.application-handling__event-row-fold-container
           (when (some? details)
             (if @show-details?
               [:i.zmdi.zmdi-chevron-up]
               [:i.zmdi.zmdi-chevron-down]))]
          [:div.application-handling__event-timestamp
           (temporal/time->short-str (or (:time event) (:created-time event)))]
          caption]
         (when (and @show-details? (some? details))
           [:div.application-handling__event-row-details
            details])]))))

(defn- application-review-events []
  (let [application-key @(subscribe [:state-query [:application :selected-application-and-form :application :key]])]
    [:div.application-handling__event-list
     [:div.application-handling__review-header @(subscribe [:editor/virkailija-translation :events])]
     (doall
       (map-indexed
         (fn [i event]
           ^{:key (str "event-row-for-" i)}
           [event-row event])
         @(subscribe [:application/events-and-information-requests application-key])))]))

(defn- application-review-note-input []
  (let [input-value               (subscribe [:state-query [:application :review-comment]])
        review-notes              (subscribe [:state-query [:application :review-notes]])
        only-selected-hakukohteet (subscribe [:state-query [:application :only-selected-hakukohteet]])
        editable?                 (subscribe [:application/review-field-editable? :notes])
        button-enabled?           (reaction (and (-> @input-value string/blank? not)
                                                 (every? (comp not :animated?) @review-notes)))]
    (fn []
      [:div.application-handling__review-row.application-handling__review-row--notes-row
       [:textarea.application-handling__review-note-input
        {:type      "text"
         :value     @input-value
         :disabled  (not @editable?)
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
                     (if @only-selected-hakukohteet
                       (dispatch [:application/add-review-notes @input-value nil])
                       (dispatch [:application/add-review-note @input-value nil])))}
        @(subscribe [:editor/virkailija-translation :add])]])))

(defn application-review-notes []
  (let [notes-for-selected        (subscribe [:application/review-note-indexes-excluding-eligibility-for-selected-hakukohteet])
        selected-review-hakukohde (subscribe [:state-query [:application :selected-review-hakukohde-oids]])
        only-selected-hakukohteet (subscribe [:state-query [:application :only-selected-hakukohteet]])
        editable?                 (subscribe [:application/review-field-editable? :notes])
        rights-to-edit-reviews-for-selected?   (subscribe [:application/rights-to-edit-reviews-for-selected-hakukohteet?])]
    (fn []
      [:div.application-handling__review-row--nocolumn
       [:div.application-handling__review-header
        {:class (when (not @editable?) "application-handling__review-header--disabled")}
        @(subscribe [:editor/virkailija-translation :notes])
        (when (< 0 (count @selected-review-hakukohde))
          [:div.application-handling__review-filters
           [:input.application-handling__review-checkbox
            {:id        "application-handling__review-checkbox--only-selected-hakukohteet"
             :type      "checkbox"
             :value     "only-selected"
             :disabled  (not @rights-to-edit-reviews-for-selected?)
             :checked   @only-selected-hakukohteet
             :on-change #(dispatch [:application/toggle-only-selected-hakukohteet])}]
           [:label
            {:for "application-handling__review-checkbox--only-selected-hakukohteet"}
            @(subscribe [:editor/virkailija-translation :only-selected-hakukohteet])]])]
       [application-review-note-input]
       (->> @notes-for-selected
            (map (fn [idx]
                   ^{:key (str "application-review-note-" idx)}
                   [application-review-note idx])))])))

(defn- score->number
  [score]
  (let [maybe-number (js/Number (string/replace score #"," "."))]
    (cond
      (string/blank? score) nil
      ; NaN:
      (not= maybe-number maybe-number) nil
      :else maybe-number)))

(defn- valid-display-score?
  [score]
  (or
    (string/blank? score)
    (if (re-matches #"^[0-9]+[,.]$" score)
      (some? (score->number (apply str (butlast score))))
      (some? (score->number score)))))

(defn application-review-inputs []
  (let [score             (subscribe [:state-query [:application :review :score]])
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?    (subscribe [:application/review-state-setting-enabled? :score])
        editable?         (subscribe [:application/review-field-editable? :score])
        display-value     (r/atom (string/replace (str @score) #"\." ","))]
    (fn []
      [:div.application-handling__review-inputs
       (when (or @settings-visible? @input-visible?)
         [:div.application-handling__review-row
          (when @settings-visible?
            [review-settings-checkbox :score])
          [:div.application-handling__review-header.application-handling__review-header--points
           {:class (when (not @editable?) "application-handling__review-header--disabled")}
           @(subscribe [:editor/virkailija-translation :points])]
          [:input.application-handling__score-input
           {:type      "text"
            :value     @display-value
            :disabled  (not @editable?)
            :on-change (when-not @settings-visible?
                         (fn [evt]
                           (let [new-value (-> evt .-target .-value)]
                             (when (valid-display-score? new-value)
                               (reset! display-value new-value))
                             (if (string/blank? new-value)
                               (dispatch [:application/update-review-field :score nil]) ;jotta saadan pistekenttä myös tyhjennettyä
                               (when-let [number (score->number new-value)]
                                (dispatch [:application/update-review-field :score number]))))))}]])])))

(defn- application-modify-link [superuser?]
  (let [application-key   (subscribe [:state-query [:application :selected-key]])
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    [:a.application-handling__link-button.application-handling__button
     {:href   (when (and (not @settings-visible?) @can-edit?)
                (str "/lomake-editori/api/applications/" @application-key (if superuser?
                                                                            "/rewrite-modify"
                                                                            "/modify")))
      :class  (when (or @settings-visible? (not @can-edit?))
                "application-handling__button--disabled")
      :target "_blank"}
     @(subscribe [:editor/virkailija-translation (if superuser?
                                                   :edit-application-with-rewrite
                                                   :edit-application)])]))

(defn- application-information-request-recipient []
  (let [email (subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading @(subscribe [:editor/virkailija-translation :receiver])]
     [:div @email]]))

(defn- application-information-request-subject []
  (let [subject (subscribe [:state-query [:application :information-request :subject]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading @(subscribe [:editor/virkailija-translation :mass-information-request-subject])]
     [:div.application-handling__information-request-text-input-container
      [:input.application-handling__information-request-text-input
       {:value     @subject
        :maxLength 120
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
                                  @(subscribe [:editor/virkailija-translation :sending-information-request])
                                  @(subscribe [:editor/virkailija-translation :send-information-request])))]
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
     @(subscribe [:editor/virkailija-translation :send-information-request-to-applicant])
     (when (nil? @request-state)
       [:i.zmdi.zmdi-close-circle.application-handling__information-request-close-button
        {:on-click #(dispatch [:application/set-information-request-window-visibility false])}])]))

(defn- application-information-request-contains-modification-link []
  [:div.application-handling__information-request-row
   [:p.application-handling__information-request-contains-modification-link
    @(subscribe [:editor/virkailija-translation :edit-link-sent-automatically])]])

(defn- application-information-request-submitted []
  [:div.application-handling__information-request-row.application-handling__information-request-row--checkmark-container
   [:div.application-handling__information-request-submitted-loader]
   [:div.application-handling__information-request-submitted-checkmark]
   [:span.application-handling__information-request-submitted-text @(subscribe [:editor/virkailija-translation :information-request-sent])]])

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
          @(subscribe [:editor/virkailija-translation :send-information-request-to-applicant])]]))))

(defn- application-resend-modify-link []
  (let [recipient          (subscribe [:state-query [:application :selected-application-and-form
                                                     :application :answers :email :value]])
        first-guardian     (subscribe [:state-query [:application :selected-application-and-form
                                                     :application :answers :guardian-email :value]])
        second-guardian    (subscribe [:state-query [:application :selected-application-and-form
                                                     :application :answers :guardian-email-secondary :value]])
        recipients         (remove string/blank? (flatten [@recipient @first-guardian @second-guardian]))
        enabled?           (subscribe [:application/resend-modify-application-link-enabled?])
        settings-visible?  (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?          (subscribe [:state-query [:application :selected-application-and-form
                                                     :application :can-edit?]])]
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
     (if (> (count recipients) 1)
       [:div @(subscribe [:editor/virkailija-translation :send-confirmation-email-to-applicant-and-guardian])]
       [:div @(subscribe [:editor/virkailija-translation :send-confirmation-email-to-applicant])])
     [:div.application-handling__resend-modify-application-link-email-text (string/join ", " recipients)]]))


(defn- application-resend-modify-link-confirmation []
  (let [state (subscribe [:state-query [:application :modify-application-link :state]])]
    (when @state
      [:div.application-handling__resend-modify-link-confirmation.application-handling__button.animated.fadeIn
       {:class (when (= @state :disappearing) "animated fadeOut")}
       [:div.application-handling__resend-modify-link-confirmation-indicator]
       @(subscribe [:editor/virkailija-translation :send-edit-link-to-applicant])])))

(defn- attachment-review-row [_ _ _]
  (let [list-opened (r/atom false)]
    (fn [selected-attachment-keys all-similar-attachments lang]
      (let [all-reviews          (map first all-similar-attachments)
            all-states           (set (map :state all-reviews))
            multiple-values?     (seq (rest all-states))
            review               (first all-reviews)
            selected-hakukohteet (map second all-similar-attachments)
            attachment-key       (-> review :key keyword)
            files                (filter identity (-> review :values flatten))
            selected-state       (or (when multiple-values?
                                       "multiple-values")
                                     (:state review)
                                     "not-checked")
            application-key      @(subscribe [:state-query [:application :selected-key]])
            can-edit?            (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
            virkailija-lang      (subscribe [:editor/virkailija-lang])
            review-types         (if multiple-values?
                                   review-states/attachment-hakukohde-review-types-with-multiple-values
                                   review-states/attachment-hakukohde-review-types)]
        [:div.application__attachment-review-row
         [:div.application__attachment-review-info-row
          [:input.application-handling__attachment-download-checkbox
           {:type      "checkbox"
            :checked   (every? #(contains? selected-attachment-keys (:key %)) files)
            :on-change (fn [_]
                         (let [attachment-keys-of-liitepyynto           (->> files
                                                                             (map :key)
                                                                             (set))
                               attachments-with-inconsistent-visibility (set/difference attachment-keys-of-liitepyynto
                                                                                        selected-attachment-keys)
                               attachments-to-toggle                    (if (-> attachments-with-inconsistent-visibility
                                                                                (count)
                                                                                (= 0))
                                                                          attachment-keys-of-liitepyynto
                                                                          attachments-with-inconsistent-visibility)]
                           (dispatch [:virkailija-attachments/toggle-attachment-selection attachments-to-toggle])))}]
          [:p.application__attachment-review-row-label (some #(-> review :label % not-empty) [lang :fi :sv :en])]
          (if @list-opened
            [:div.application-handling__review-state-list
             (doall
               (for [[state labels]
                     review-types]
                 (let [label (get labels @virkailija-lang)]
                   [:div.application-handling__review-state-row
                    {:class    (when (= state selected-state) "application-handling__review-state-row--selected application-handling__review-state-row--enabled")
                     :on-click (if (= state selected-state)
                                 #(swap! list-opened not)
                                 (fn []
                                   (swap! list-opened not)
                                   (doall (map #(dispatch [:application/update-attachment-review attachment-key % state]) selected-hakukohteet))))
                     :key      (str attachment-key label)}
                    (if (= state selected-state) [icons/icon-check]
                                                 [icons/icon-unselected]) label])))]
            [:div.application-handling__review-state-row.application-handling__review-state-row--selected
             {:class    (if @can-edit?
                          "application-handling__review-state-row--enabled"
                          "application-handling__review-state-row--disabled")
              :on-click #(when @can-edit? (swap! list-opened not))}
             [icons/icon-check]
             (application-states/get-review-state-label-by-name review-types selected-state @virkailija-lang)])]
         [:div.application__attachment-review-deadline-row
          (when (or (= "incomplete-attachment" selected-state)
                    @(subscribe [:liitepyynto-information-request/deadline-visible?
                                 application-key
                                 attachment-key]))
            [:div.application__attachment-review-deadline-row__send-toggle
             [lir/send-toggle-lable application-key attachment-key]
             [:div.application__attachment-review-deadline-row__send-toggle-input
              [lir/send-toggle application-key attachment-key]]])
          (when @(subscribe [:liitepyynto-information-request/deadline-visible?
                             application-key
                             attachment-key])
            [:div.application__attachment-review-deadline-row__deadline
             [lir/deadline-date-label application-key attachment-key]
             [:div.application__attachment-review-deadline-row__deadline-date
              [lir/deadline-date-input application-key attachment-key]]
             [lir/deadline-time-label application-key attachment-key]
             [:div.application__attachment-review-deadline-row__deadline-time
              [lir/deadline-time-input application-key attachment-key]]])
          (when @(subscribe [:liitepyynto-information-requets/deadline-error? application-key attachment-key])
            [:div.application__attachment-review-deadline-row__deadline-error
             [lir/deadline-error]])]
         [:ul.application__attachment-review-row-attachments
          (for [attachment-file files
                :let [text (str (:filename attachment-file) " (" (util/size-bytes->str (:size attachment-file)) ")")]]
            ^{:key (str "attachment-file-" attachment-file)}
            [:li
             (if (and (= (:virus-scan-status attachment-file) "done")
                      (contains? selected-attachment-keys (:key attachment-file)))
               [:a
                {:on-click #(dispatch [:virkailija-attachments/open-attachment-skimming (:key attachment-file)])}
                text]
               text)])]]))))

(defn- attachment-review-area [reviews lang]
  (let [all-keys                 (->> (vals reviews)
                                      (mapcat #(-> % ffirst :values flatten))
                                      (keep :key)
                                      set)
        selected-attachment-keys (->> all-keys
                                      (filter (fn [attachment-key]
                                                @(subscribe [:virkailija-attachments/attachment-selected? attachment-key])))
                                      (set))]
    [:div.application-handling__attachment-review-container.animated
     {:class (str (if @(subscribe [:state-query [:application :show-attachment-reviews?]])
                    " fadeInRight"
                    " fadeOutRight"))}
     (when (not-empty reviews)
       [:div
        [:div.application-handling__attachment-review-header
         [:div
          (gstring/format "%s %s (%d)"
                          (if (= "form" (second (first (vals reviews))))
                            @(subscribe [:editor/virkailija-translation :of-form])
                            @(subscribe [:editor/virkailija-translation :of-hakukohde]))
                          (.toLowerCase @(subscribe [:editor/virkailija-translation :attachments]))
                          (count (keys reviews)))]]
        [:div.application__attachment-review-row
         [:div.application__attachment-review-info-row
          [:input.application-handling__attachment-download-checkbox
           {:type      "checkbox"
            :checked   (= all-keys selected-attachment-keys)
            :on-change (fn [_]
                         (let [attachments-to-toggle (if (or (empty? selected-attachment-keys)
                                                             (= all-keys selected-attachment-keys))
                                                       all-keys
                                                       selected-attachment-keys)]
                           (dispatch [:virkailija-attachments/toggle-attachment-selection attachments-to-toggle])))}]
          [:p.application__attachment-review-row-label
           @(subscribe [:editor/virkailija-translation :select-all])]
          [:div.application-handling__excel-request-row
           [:form#attachment-download-link
            {:action "/lomake-editori/api/files/zip"
             :method "POST"}
            [:input {:type  "hidden"
                     :name  "keys"
                     :value (.stringify js/JSON (clj->js selected-attachment-keys))}]
            (when-let [csrf-token (cljs-util/csrf-token)]
              [:input {:type  "hidden"
                       :name  "CSRF"
                       :value csrf-token}])]
           [:button.application-handling__download-attachments-button
            {:disabled (empty? selected-attachment-keys)
             :on-click (fn [_]
                         (.submit (.getElementById js/document "attachment-download-link")))}
            @(subscribe [:editor/virkailija-translation :load-attachments])]]]]
        (doall (for [all-similar-attachments (vals reviews)]
                 ^{:key (:key (ffirst all-similar-attachments))}
                 [attachment-review-row selected-attachment-keys all-similar-attachments lang]))])]))

(defn- application-review []
  (let [settings-visible        (subscribe [:state-query [:application :review-settings :visible?]])
        superuser?              (subscribe [:state-query [:editor :user-info :superuser?]])]
    (r/create-class
      {:component-did-mount
       (fn []
         (dispatch [:virkailija-attachments/restore-attachment-view-scroll-position]))
       :reagent-render
       (fn []
         (let [selected-review-hakukohde        @(subscribe [:state-query [:application :selected-review-hakukohde-oids]])
               tutu-form?                       @(subscribe [:payment/tutu-form-selected?])
               astu-form?                       @(subscribe [:payment/astu-form-selected?])
               application-key                  @(subscribe [:state-query [:application :review :application-key]])
               payments                         @(subscribe [:payment/payments application-key])
               attachment-reviews-for-hakukohde (->> @(subscribe [:virkailija-attachments/liitepyynnot-for-selected-hakukohteet])
                                                     (map (fn [liitepyynto]
                                                            [liitepyynto (:hakukohde-oid liitepyynto)]))
                                                     (group-by (comp :key first)))
               lang (subscribe [:application/lang])
               rights-to-view-reviews? @(subscribe [:application/rights-to-view-reviews-for-selected-hakukohteet?])
               opinto-ohjaaja (subscribe [:editor/opinto-ohjaaja?])
               only-opinto-ohjaaja @(subscribe [:editor/all-organizations-have-only-opinto-ohjaaja-rights?])
               toisen-asteen-yhteishaku? @(subscribe [:application/toisen-asteen-yhteishaku-selected?])
               show-attachment-review? @(subscribe [:state-query [:application :show-attachment-reviews?]])]
           [:div.application-handling__review-outer
            [:a.application-handling__review-area-settings-link
             {:on-click (fn [event]
                          (.preventDefault event)
                          (dispatch [:application/toggle-review-area-settings-visibility]))}
             [:i.application-handling__review-area-settings-button.zmdi.zmdi-settings]]
            [:div.application-handling__review-settings
             {:style (when-not @settings-visible
                       {:visibility "hidden"})}
             [:div.application-handling__review-settings-indicator-outer
              [:div.application-handling__review-settings-indicator-inner]]
             [:div.application-handling__review-settings-header
              [:i.zmdi.zmdi-account.application-handling__review-settings-header-icon]
              [:span.application-handling__review-settings-header-text
               @(subscribe [:editor/virkailija-translation :settings])]]]
            [:div.application-handling__review
             (when show-attachment-review?
               [attachment-review-area attachment-reviews-for-hakukohde @lang])
             [:div.application-handling__review-outer-container
              [application-hakukohde-selection]
              (when (not-empty selected-review-hakukohde)
                ;; 2. asteen yhteishaussa piilotetaan käsittely jos valittuna hakukohde johon ei oikeuksia
                (if (or (not toisen-asteen-yhteishaku?)
                        rights-to-view-reviews?
                        @opinto-ohjaaja)
                  [:div
                   ;; 2. asteen yhteishaussa näytetään ilmoitus jos opo+käsittelijä ja valittuna hakukohde johon ei oikeuksia
                   (when (and (not rights-to-view-reviews?)
                              (not only-opinto-ohjaaja))
                     [:div.application-handling__review-row
                      [:span.hakukohde-review-rights-alert
                       @(subscribe [:editor/virkailija-translation :selected-hakukohde-no-rights])]])
                   (when (not-empty attachment-reviews-for-hakukohde)
                     [:div.application-handling__attachment-review-toggle-container
                      (when @settings-visible
                        [review-settings-checkbox :attachment-handling])
                      [:span.application-handling__attachment-review-toggle-container-link
                       {:on-click (fn []
                                    (when-not @settings-visible
                                      (dispatch
                                        [:state-update
                                         #(assoc-in % [:application :show-attachment-reviews?] (not show-attachment-review?))])))}
                       [:span.application-handling__attachment-review-toggle
                        (if show-attachment-review?
                          [:span [:i.zmdi.zmdi-chevron-right] [:i.zmdi.zmdi-chevron-right]]
                          [:span [:i.zmdi.zmdi-chevron-left] [:i.zmdi.zmdi-chevron-left]])]
                       (gstring/format "%s (%d)"
                                       @(subscribe [:editor/virkailija-translation :attachments])
                                       (count (keys attachment-reviews-for-hakukohde)))]])
                   [application-hakukohde-review-inputs
                    (cond
                      tutu-form? review-states/hakukohde-review-types
                      astu-form? review-states/hakukohde-review-types-astu
                      :else review-states/hakukohde-review-types-normal)
                    application-key]]
                  [:div.application-handling__review-row
                   [:span.hakukohde-review-rights-alert
                    @(subscribe [:editor/virkailija-translation :selected-hakukohde-no-rights])]]))
              (cond
                tutu-form? [application-tutu-payment-status payments]
                astu-form? [application-astu-payment-status payments])
              (when @(subscribe [:application/show-info-request-ui?])
                [application-information-request])
              [application-review-inputs]
              [application-review-notes]

              [application-modify-link false]
              (when @superuser?
                [application-modify-link true])
              [single-information-request-view/application-send-single-email-to-applicant]
              [application-resend-modify-link]
              [application-resend-modify-link-confirmation]
              [application-deactivate-toggle]
              [application-review-events]]]]))})))

(defn application-review-area []
  (let [selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        application-loading (subscribe [:state-query [:application :loading?]])
        hakukohteet (subscribe [:state-query [:hakukohteet]])]
    (fn []
      (if @application-loading
        [:div.application-handling__application-loading-indicator
         [:div.application-handling__application-loading-indicator-spin
          [:i.zmdi.zmdi-spinner.spin]]]
        [:div.application-handling__review-area
         [:div.application-handling__application-contents
          [application-contents @selected-application-and-form @hakukohteet]]
         [:span#application-handling__review-position-canary]
         [application-review]]))))
