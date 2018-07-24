(ns ataru.virkailija.application.view
  (:require [ataru.application.application-states :as application-states]
            [ataru.application.review-states :as review-states]
            [ataru.cljs-util :as cljs-util]
            [ataru.util :as util]
            [ataru.virkailija.application.application-search-control :refer [application-search-control]]
            [ataru.virkailija.application.application-subs]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.routes :as routes]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.temporal :as temporal]
            [ataru.virkailija.views.virkailija-readonly :as readonly-contents]
            [ataru.virkailija.virkailija-ajax :as ajax]
            [cljs-time.format :as f]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [medley.core :refer [find-first]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- icon-check []
  [:img.application-handling__review-state-selected-icon
   {:src "/lomake-editori/images/icon_check.png"}])

(defn excel-download-link
  [applications selected-hakukohde filename]
  (when (not-empty applications)
    [:div
     [:form#excel-download-link
      {:action "/lomake-editori/api/applications/excel"
       :method "POST"}
      [:input {:type  "hidden"
               :name  "application-keys"
               :value (clojure.string/join "," (map :key applications))}]
      [:input {:type  "hidden"
               :name  "filename"
               :value filename}]
      [:input {:type  "hidden"
               :name  "skip-answers"
               :value "false"}]
      (when-let [csrf-token (cljs-util/csrf-token)]
        [:input {:type  "hidden"
                 :name  "CSRF"
                 :value csrf-token}])
      (when selected-hakukohde
        [:input {:type  "hidden"
                 :name  "selected-hakukohde"
                 :value selected-hakukohde}])]
     [:a.application-handling__excel-download-link.editor-form__control-button.editor-form__control-button--enabled
      {:on-click (fn [e]
                   (.submit (.getElementById js/document "excel-download-link")))}
      "Lataa Excel"]]))

(defn- count-for-application-state
  [from-states state]
  (get from-states state 0))

(defn- selected-or-default-mass-review-state
  [selected all]
  (if @selected
    @selected
    (or
      (ffirst (filter (comp pos? second) all))
      (ffirst all))))

(defn- review-state-label
  [state-name]
  (second (first (filter #(= (first %) state-name) review-states/application-hakukohde-processing-states))))

(defn- review-label-with-count
  [label count]
  (str label
       (when (< 0 count)
         (str " (" count ")"))))

(defn- selected-or-default-mass-review-state-label
  [selected all]
  (let [name  (selected-or-default-mass-review-state selected all)
        label (review-state-label name)
        count (count-for-application-state all name)]
    (review-label-with-count label count)))

(defn- mass-review-state-selected-row
  [on-click label]
  [:div.application-handling__review-state-row.application-handling__review-state-row--mass-update.application-handling__review-state-selected-row
   {:on-click on-click}
   [icon-check] label])

(defn- mass-review-state-row
  [current-review-state states disable-empty-rows? state]
  (let [review-state-count (count-for-application-state states state)
        review-state-label (review-state-label state)
        label-with-count   (review-label-with-count review-state-label review-state-count)
        on-click           #(reset! current-review-state state)
        disabled?          (and disable-empty-rows? (zero? review-state-count))]
    (if (= (selected-or-default-mass-review-state current-review-state states) state)
      (mass-review-state-selected-row #() label-with-count)
      [:div.application-handling__review-state-row.application-handling__review-state-row--mass-update
       {:on-click (when-not disabled? on-click)
        :class    (when disabled? "application-handling__review-state-row--disabled")}
       label-with-count])))

(defn- opened-mass-review-state-list
  [current-state states disable-empty-rows?]
  (mapv (partial mass-review-state-row current-state states disable-empty-rows?) (map first states)))

(defn- toggle-mass-update-popup-visibility
  [element-visible submit-button-state fn-or-bool]
  (if (boolean? fn-or-bool)
    (reset! element-visible fn-or-bool)
    (swap! element-visible fn-or-bool))
  (when (not @element-visible)
    (reset! submit-button-state :submit)))

(defn- mass-update-applications-link
  []
  (let [element-visible?           (r/atom false)
        from-list-open?            (r/atom false)
        to-list-open?              (r/atom false)
        submit-button-state        (r/atom :submit)
        selected-from-review-state (r/atom nil)
        selected-to-review-state   (r/atom nil)
        massamuokkaus?             (subscribe [:application/massamuutos-enabled?])
        filtered-applications      (subscribe [:application/filtered-applications])
        haku-header                (subscribe [:application/list-heading-data-for-haku])
        all-states                 (reduce (fn [acc [state _]]
                                             (assoc acc state 0))
                                     {}
                                     review-states/application-hakukohde-processing-states)]
    (fn []
      (when-not (empty? @filtered-applications)
        (let [from-states (reduce
                            (fn [acc application]
                              (merge-with
                                +
                                acc
                                (frequencies
                                  (map :state
                                       (filter
                                         #(= "processing-state" (:requirement %))
                                         (application-states/get-all-reviews-for-all-requirements
                                          application
                                          (second @haku-header)))))))
                            all-states
                            @filtered-applications)]
          [:span.application-handling__mass-edit-review-states-container
           (when @massamuokkaus?
             [:a.application-handling__mass-edit-review-states-link.editor-form__control-button.editor-form__control-button--enabled
              {:on-click #(toggle-mass-update-popup-visibility element-visible? submit-button-state not)}
              "Massamuutos"])
           (when @element-visible?
             [:div.application-handling__mass-edit-review-states-popup
              [:div.application-handling__popup-close-button
               {:on-click #(toggle-mass-update-popup-visibility element-visible? submit-button-state false)}
               [:i.zmdi.zmdi-close]]
              [:h4.application-handling__mass-edit-review-states-heading.application-handling__mass-edit-review-states-heading--title "Massamuutos"]
              (when-let [[haku-oid hakukohde-oid _] @haku-header]
                [:p
                 @(subscribe [:application/haku-name haku-oid])
                 (when hakukohde-oid
                   (str ", " @(subscribe [:application/hakukohde-name hakukohde-oid])))])
              [:h4.application-handling__mass-edit-review-states-heading "Tilasta"]

              (if @from-list-open?
                [:div.application-handling__review-state-list-opened-anchor
                 (into [:div.application-handling__review-state-list-opened
                        {:on-click #(swap! from-list-open? not)}]
                       (opened-mass-review-state-list selected-from-review-state from-states true))]
                (mass-review-state-selected-row
                  (fn []
                    (swap! from-list-open? not)
                    (reset! submit-button-state :submit))
                  (selected-or-default-mass-review-state-label selected-from-review-state from-states)))

              [:h4.application-handling__mass-edit-review-states-heading "Muutetaan tilaan"]

              (if @to-list-open?
                [:div.application-handling__review-state-list-opened-anchor
                 (into [:div.application-handling__review-state-list-opened
                        {:on-click #(when (-> from-states (keys) (count) (pos?)) (swap! to-list-open? not))}]
                       (opened-mass-review-state-list selected-to-review-state all-states false))]
                (mass-review-state-selected-row
                  (fn []
                    (swap! to-list-open? not)
                    (reset! submit-button-state :submit))
                  (selected-or-default-mass-review-state-label selected-to-review-state all-states)))

              (case @submit-button-state
                :submit
                (let [button-disabled? (= (selected-or-default-mass-review-state selected-from-review-state from-states)
                                          (selected-or-default-mass-review-state selected-to-review-state all-states))]
                  [:a.application-handling__link-button.application-handling__mass-edit-review-states-submit-button
                   {:on-click #(when-not button-disabled? (reset! submit-button-state :confirm))
                    :disabled button-disabled?}
                   "Muuta"])

                :confirm
                [:a.application-handling__link-button.application-handling__mass-edit-review-states-submit-button--confirm
                 {:on-click (fn []
                              (let [from-state-name              (selected-or-default-mass-review-state selected-from-review-state from-states)
                                    to-state-name                (selected-or-default-mass-review-state selected-to-review-state all-states)
                                    application-keys             (map :key @filtered-applications)]
                                (dispatch [:application/mass-update-application-reviews
                                           application-keys
                                           from-state-name
                                           to-state-name])
                                (reset! selected-from-review-state nil)
                                (reset! selected-to-review-state nil)
                                (toggle-mass-update-popup-visibility element-visible? submit-button-state false)
                                (reset! from-list-open? false)
                                (reset! to-list-open? false)))}
                 "Vahvista muutos"]

                [:div])])])))))

(def all-hakukohteet-label "Kaikki hakukohteet")

(defn- closed-row
  [on-click label]
  [:div.application-handling__dropdown-box-closed
   {:on-click on-click}
   [:i.zmdi.zmdi-chevron-down]
   [:p.application-handling__dropdown-box-closed-label
    (or label [:i.zmdi.zmdi-spinner.spin])]])

(defn- row-component
  [close-list href label description selected?]
  [:a.application-handling__dropdown-box-item
   {:href     href
    :on-click close-list}
   (if selected?
     [:img.application-handling__dropdown-box-item-selected-icon
      {:src "/lomake-editori/images/icon_check.png"}]
     [:span.application-handling__dropdown-box-item-selected-icon])
   (if label
     [:div
      [:span.application-handling__dropdown-box-item--label label]
      [:span.application-handling__dropdown-box-item--description description]]
     [:i.zmdi.zmdi-spinner.spin])])

(defn- hakukohde-row
  [close-list oid selected?]
  (let [name        @(subscribe [:application/hakukohde-name oid])
        description @(subscribe [:application/tarjoaja-name oid])]
    (row-component close-list
                   (str "/lomake-editori/applications/hakukohde/" oid)
                   name
                   description
                   selected?)))

(defn- hakukohderyhma-row
  [close-list haku-oid oid selected?]
  (let [name @(subscribe [:application/hakukohderyhma-name oid])]
    (row-component close-list
                   (str "/lomake-editori/applications/haku/"
                     haku-oid
                     "/hakukohderyhma/"
                     oid)
                   name
                   nil
                   selected?)))

(defn- haku-row
  [close-list oid selected?]
  (row-component close-list
                 (str "/lomake-editori/applications/haku/" oid)
                 all-hakukohteet-label
                 nil
                 selected?))

(defn- ensisijaisesti
  []
  (let [ensisijaisesti? @(subscribe [:application/ensisijaisesti?])]
    [:label.application-handling__ensisijaisesti
     [:input.application-handling__ensisijaisesti-checkbox
      {:type     "checkbox"
       :checked  ensisijaisesti?
       :on-click #(dispatch [:application/navigate-to-ensisijaisesti
                             (not ensisijaisesti?)])}]
     "Hakenut ensisijaisesti"]))

(defn haku-applications-heading
  [_]
  (let [list-opened (r/atom false)
        open-list   #(reset! list-opened true)
        close-list  #(reset! list-opened false)]
    (fn [[haku-oid
          selected-hakukohde-oid
          selected-hakukohderyhma-oid
          hakukohde-oids
          hakukohderyhma-oids]]
      [:div.application-handling__header-haku-and-hakukohde
       [:div.application-handling__header-haku
        (if-let [haku-name @(subscribe [:application/haku-name haku-oid])]
          haku-name
          [:i.zmdi.zmdi-spinner.spin])]
       (when @list-opened
         [:div.application-handling__dropdown-box-opened
          (haku-row close-list
            haku-oid
            (and (nil? selected-hakukohde-oid)
                 (nil? selected-hakukohderyhma-oid)))
          (let [hakukohde-sorted-oids (->> hakukohde-oids
                                           (map (fn [hakukohde-oid]
                                                  [@(subscribe [:application/hakukohde-name hakukohde-oid])
                                                   hakukohde-oid]))
                                           (sort-by first)
                                           (map second))]
            (doall
              (for [hakukohde-oid hakukohde-sorted-oids]
                ^{:key hakukohde-oid}
                [hakukohde-row
                 close-list
                 hakukohde-oid
                 (= hakukohde-oid selected-hakukohde-oid)])))
          (let [hakukohderyhma-sorted-oids (->> hakukohderyhma-oids
                                                (map (fn [hakukohderyhma-oid]
                                                       [@(subscribe [:application/hakukohderyhma-name hakukohderyhma-oid])
                                                        hakukohderyhma-oid]))
                                                (sort-by first)
                                                (map second))]
            (doall
              (for [hakukohderyhma-oid hakukohderyhma-sorted-oids]
                ^{:key hakukohderyhma-oid}
                [hakukohderyhma-row
                 close-list
                 haku-oid
                 hakukohderyhma-oid
                 (= hakukohderyhma-oid selected-hakukohderyhma-oid)])))])
       (closed-row open-list
                   (cond (some? selected-hakukohde-oid)
                         @(subscribe [:application/hakukohde-name
                                      selected-hakukohde-oid])
                         (some? selected-hakukohderyhma-oid)
                         @(subscribe [:application/hakukohderyhma-name
                                      selected-hakukohderyhma-oid])
                         :else
                         all-hakukohteet-label))
       (when @(subscribe [:application/show-ensisijaisesti?])
         [ensisijaisesti])])))

(defn selected-applications-heading
  [haku-data list-heading]
  (if haku-data
    [haku-applications-heading haku-data]
    [:div.application-handling__header-haku list-heading]))

(defn haku-heading
  []
  (let [show-mass-update-link? (subscribe [:application/show-mass-update-link?])
        show-excel-link?       (subscribe [:application/show-excel-link?])
        applications           (subscribe [:application/filtered-applications])
        header                 (subscribe [:application/list-heading])
        haku-header            (subscribe [:application/list-heading-data-for-haku])]
    [:div.application-handling__header
     [selected-applications-heading @haku-header @header]
     [:div.editor-form__form-controls-container
      (when @show-mass-update-link?
        [mass-update-applications-link])
      (when @show-excel-link?
        [excel-download-link @applications (second @haku-header) @header])]]))

(defn- select-application
  [application-key]
  (cljs-util/update-url-with-query-params {:application-key application-key})
  (dispatch [:application/select-application application-key]))

(defn hakukohde-review-state
  [hakukohde-reviews hakukohde-oid requirement]
  (:state (find-first #(and (= (:hakukohde %) hakukohde-oid)
                            (= (:requirement %) requirement))
                      hakukohde-reviews)))

(defn review-label-for-hakukohde
  [reviews states hakukohde-oid requirement]
  (application-states/get-review-state-label-by-name
    states
    (hakukohde-review-state reviews hakukohde-oid requirement)))

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
  [review-settings application selected-hakukohde attachment-states]
  (let [direct-form-application?      (empty? (:hakukohde application))
        application-hakukohde-oids    (if direct-form-application?
                                        ["form"]
                                        (:hakukohde application))
        application-hakukohde-reviews (:application-hakukohde-reviews application)]
    (into
      [:div.application-handling__list-row-hakukohteet-wrapper
       {:class (when direct-form-application? "application-handling__application-hakukohde-cell--form")}]
      (map
        (fn [hakukohde-oid]
          (let [processing-state       (hakukohde-review-state application-hakukohde-reviews hakukohde-oid "processing-state")
                selection-state        (hakukohde-review-state application-hakukohde-reviews hakukohde-oid "selection-state")
                show-state-email-icon? (and
                                         (< 0 (:new-application-modifications application))
                                         (->> application
                                              :application-hakukohde-reviews
                                              (filter #(and (= (:requirement %) "processing-state")
                                                            (= (:state %) "information-request")))
                                              (seq)))
                hakukohde-attachment-states ((keyword hakukohde-oid) attachment-states)]
            [:div.application-handling__list-row-hakukohde
             (when (not direct-form-application?)
               [:span.application-handling__application-hakukohde-cell
                {:class    (when (= selected-hakukohde hakukohde-oid)
                             "application-handling__application-hakukohde-cell--selected")
                 :on-click (fn [] (select-application (:key application)))}
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
                 (review-label-for-hakukohde
                   application-hakukohde-reviews
                   review-states/application-hakukohde-processing-states
                   hakukohde-oid
                   "processing-state")
                 "Käsittelemättä")
               (when show-state-email-icon?
                 [:i.zmdi.zmdi-email.application-handling__list-row-email-icon])]]
             (when (:selection-state review-settings true)
               [:span.application-handling__hakukohde-selection-cell
                [:span.application-handling__hakukohde-selection.application-handling__count-tag
                 [:span.application-handling__state-label
                  {:class (str "application-handling__state-label--" (or selection-state "incomplete"))}]
                 (or
                   (review-label-for-hakukohde
                     application-hakukohde-reviews
                     review-states/application-hakukohde-selection-states
                     hakukohde-oid
                     "selection-state")
                   "Kesken")]])]))
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
  (let [selected-time-column   (subscribe [:state-query [:application :selected-time-column]])
        day-date-time          (clojure.string/split (t/time->str (@selected-time-column application)) #"\s")
        day                    (first day-date-time)
        date-time              (->> day-date-time (rest) (clojure.string/join " "))
        applicant              (str (-> application :person :last-name) ", " (-> application :person :preferred-name))
        review-settings        (subscribe [:state-query [:application :review-settings :config]])
        selected-hakukohde     (subscribe [:state-query [:application :selected-review-hakukohde]])
        attachment-states      (application-attachment-states application)
        form-attachment-states (:form attachment-states)]
    [:div.application-handling__list-row
     {:on-click #(select-application (:key application))
      :class    (clojure.string/join " " [(when selected?
                                            "application-handling__list-row--selected")
                                          (when (= "inactivated" (:state application))
                                            "application-handling__list-row--inactivated")])}
     [:div.application-handling__list-row-person-info
      [:span.application-handling__list-row--application-applicant
       (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
      [:span.application-handling__list-row--application-time
       [:span.application-handling__list-row--time-day day]
       [:span date-time]]
      (when (:attachment-handling @review-settings true)
        [attachment-state-counts form-attachment-states])
      [:span.application-handling__list-row--state]
      (when (:selection-state @review-settings true)
        [:span.application-handling__hakukohde-selection-cell])]
     [applications-hakukohde-rows @review-settings application @selected-hakukohde attachment-states]]))

(defn application-list-contents [applications]
  (let [selected-key (subscribe [:state-query [:application :selected-key]])
        expanded?    (subscribe [:state-query [:application :application-list-expanded?]])]
    (fn [applications]
      (into [:div.application-handling__list
             {:class (str (when (= true @expanded?) "application-handling__list--expanded")
                          (when (> (count applications) 0) " animated fadeIn"))}]
            (for [application applications
                  :let [selected? (= @selected-key (:key application))]]
              (if selected?
                [cljs-util/wrap-scroll-to [application-list-row application selected?]]
                [application-list-row application selected?]))))))

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
    (dispatch [:state-update #(assoc-in % [:application filter-kw] new-filter)])))

(defn hakukohde-state-filter-controls
  [filter-kw title states state-counts-sub]
  (let [filter-sub           (subscribe [:state-query [:application filter-kw]])
        filter-opened        (r/atom false)
        toggle-filter-opened #(swap! filter-opened not)
        get-state-count      (fn [counts state-id] (or (get counts state-id) 0))]
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
                                                                                (map first states)))]))}]
                    [:span "Kaikki"]]]]
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
                         [:span (str review-state-label
                                     (when state-counts-sub
                                       (str " (" (get-state-count @state-counts-sub review-state-id) ")")))]]]))
                   states)))
         (when @filter-opened [:div.application-handling__filter-state-selection-arrow-up])]))))

(defn sortable-column-click [column-id evt]
  (dispatch [:application/update-sort column-id]))

(defn application-list-basic-column-header [column-id heading]
  (let [application-sort (subscribe [:state-query [:application :sort]])]
    (fn [column-id heading]
      [:span.application-handling__basic-list-basic-column-header
       {:on-click (partial sortable-column-click column-id)}
        heading
        (if (= column-id (:column @application-sort))
          (if (= :descending (:order @application-sort))
            [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow]
            [:i.zmdi.zmdi-chevron-up.application-handling__sort-arrow])
          [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow.application-handling__sort-arrow--disabled])])))

(defn created-time-column-header []
  (let [application-sort (subscribe [:state-query [:application :sort]])
        selected-time-column (subscribe [:state-query [:application :selected-time-column]])]
    (fn []
      [:span
       {:class (if (= :created-time @selected-time-column)
                 "application-handling__list-row--created-time"
                 "application-handling__list-row--original-created-time")}
       [:span.application-handling__basic-list-basic-column-header
        [:span.application-handling__created-time-column-header
         {:on-click #(dispatch [:application/toggle-shown-time-column])}
         (if (= :created-time @selected-time-column)
           "Viimeksi muokattu"
           "Hakemus jätetty")]
        "|"
        [:i.zmdi
         {:on-click #(dispatch [:application/update-sort @selected-time-column])
          :class    (if (= @selected-time-column (:column @application-sort))
                      (if (= :descending (:order @application-sort))
                        "zmdi-chevron-down application-handling__sort-arrow"
                        "zmdi-chevron-up application-handling__sort-arrow")
                      "zmdi-chevron-down application-handling__sort-arrow application-handling__sort-arrow--disabled")}]]])))


(defn application-list-loading-indicator []
  (let [fetching (subscribe [:state-query [:application :fetching-applications]])]
    (when @fetching
        [:div.application-handling__list-loading-indicator
         [:i.zmdi.zmdi-spinner]])))

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
  [filters [kw group-label states]]
  [:div.application-handling__filter-group
   {:key (str "application-filter-group-" kw)}
   [:div.application-handling__filter-group-title group-label]
   (into
     [:div.application-handling__filter-group-checkboxes]
     (map
       (fn [[state checkbox-label]]
         (application-filter-checkbox filters checkbox-label kw state))
       states))])

(defn- application-base-education-filters
  [filters]
  (let [checkboxes [[:pohjakoulutus_yo "Suomessa suoritettu ylioppilastutkinto"]
                    [:pohjakoulutus_lk "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"]
                    [:pohjakoulutus_yo_kansainvalinen_suomessa "Suomessa suoritettu kansainvälinen ylioppilastutkinto"]
                    [:pohjakoulutus_yo_ammatillinen "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"]
                    [:pohjakoulutus_am "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"]
                    [:pohjakoulutus_amt "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"]
                    [:pohjakoulutus_kk "Suomessa suoritettu korkeakoulututkinto"]
                    [:pohjakoulutus_yo_ulkomainen "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"]
                    [:pohjakoulutus_kk_ulk "Muualla kuin Suomessa suoritettu korkeakoulututkinto"]
                    [:pohjakoulutus_ulk "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"]
                    [:pohjakoulutus_avoin "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"]
                    [:pohjakoulutus_muu "Muu korkeakoulukelpoisuus"]]
        all-filters-selected? (subscribe [:application/all-pohjakoulutus-filters-selected?])]
    (fn []
      [:div.application-handling__filter-group
       [:label.application-handling__filter-checkbox-label.application-handling__filter-checkbox-label--all
        {:key   (str "application-filter-pohjakoulutus-any")
         :class (when @all-filters-selected? "application-handling__filter-checkbox-label--checked")}
        [:input.application-handling__filter-checkbox
         {:type      "checkbox"
          :checked   @all-filters-selected?
          :on-change #(dispatch [:application/toggle-all-pohjakoulutus-filters @all-filters-selected?])}]
        [:span "Kaikki"]]
       (->> checkboxes
            (map (fn [[id label]] (application-filter-checkbox filters label :base-education id)))
            (doall))])))

(defn- application-filters
  []
  (let [filters                    (subscribe [:state-query [:application :filters]])
        filtered-application-count (subscribe [:application/filtered-applications-count])
        loaded-application-count   (subscribe [:application/loaded-application-count])
        enabled-filter-count       (subscribe [:application/enabled-filter-count])
        review-settings            (subscribe [:state-query [:application :review-settings :config]])
        selected-hakukohde-oid     (subscribe [:state-query [:application :selected-hakukohde]])
        has-base-education-answers (subscribe [:application/applications-have-base-education-answers])
        filters-visible            (r/atom false)
        filters-to-include         #{:language-requirement :degree-requirement :eligibility-state :payment-obligation}]
    (fn []
      [:span.application-handling__filters
       [:a
        {:on-click #(swap! filters-visible not)}
        (str "Rajaa hakemuksia "
             "(" @filtered-application-count "/" @loaded-application-count ")")]
       (when (pos? @enabled-filter-count)
         [:span
          [:span.application-handling__filters-count-separator "|"]
          [:a
           {:on-click #(dispatch [:application/remove-filters])} "Poista rajaimet"]])
       (when @filters-visible
         [:div.application-handling__filters-popup
          {:class (when @has-base-education-answers "application-handling__filters-popup--two-cols")}
          [:div.application-handling__popup-close-button
           {:on-click #(reset! filters-visible false)}
           [:i.zmdi.zmdi-close]]
          [:div.application-handling__popup-column-left
           [:h3.application-handling__filter-group-heading "Yksilöinti"]
           [:div.application-handling__filter-group
            [application-filter-checkbox filters "Yksilöimättömät" :only-identified :unidentified]
            [application-filter-checkbox filters "Yksilöidyt" :only-identified :identified]]
           [:h3.application-handling__filter-group-heading "Käsittelymerkinnät"]
           (when (some? @selected-hakukohde-oid)
             [:div.application-handling__filter-hakukohde-name
              @(subscribe [:application/hakukohde-name @selected-hakukohde-oid])])
           (->> review-states/hakukohde-review-types
                (filter (fn [[kw _ _]]
                          (and
                            (contains? filters-to-include kw)
                            (-> @review-settings (get kw) (false?) (not)))))
                (map (partial review-type-filter filters))
                (doall))]
          (when @has-base-education-answers
            [:div.application-handling__popup-column-right
             [:h3.application-handling__filter-group-heading "Pohjakoulutus"]
             [application-base-education-filters filters]])])])))

(defn application-list [applications]
  (let [fetching        (subscribe [:state-query [:application :fetching-applications]])
        review-settings (subscribe [:state-query [:application :review-settings :config]])]
    [:div
     [:div.application-handling__list-header.application-handling__list-row
      [:span.application-handling__list-row--applicant
       [application-list-basic-column-header
        :applicant-name
        "Hakija"]
       [application-filters]]
      [created-time-column-header]
      (when (:attachment-handling @review-settings true)
        [:span.application-handling__list-row--attachment-state
         [hakukohde-state-filter-controls
          :attachment-state-filter
          "Liitepyynnöt"
          review-states/attachment-hakukohde-review-types-with-no-requirements
          (subscribe [:state-query [:application :attachment-state-counts]])]])
      [:span.application-handling__list-row--state
       [hakukohde-state-filter-controls
        :processing-state-filter
        "Käsittelyvaihe"
        review-states/application-hakukohde-processing-states
        (subscribe [:state-query [:application :review-state-counts]])]]
      (when (:selection-state @review-settings true)
        [:span.application-handling__list-row--selection
         [hakukohde-state-filter-controls
          :selection-state-filter
          "Valinta"
          review-states/application-hakukohde-selection-states]])]
     (when-not @fetching
       [application-list-contents applications])]))

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn review-state-selected-row [on-click label]
  (let [settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])
        enabled?          (and (not @settings-visible?) @can-edit?)]
    [:div.application-handling__review-state-row.application-handling__review-state-selected-row
     {:on-click #(when enabled? (on-click))
      :class    (if enabled?
                  "application-handling__review-state-row--enabled"
                  "application-handling__review-state-row--disabled")}
     [icon-check] label]))

(defn review-state-row [state-name current-review-state [review-state-id review-state-label]]
  (if (= current-review-state review-state-id)
    [review-state-selected-row #() review-state-label]
    [:div.application-handling__review-state-row
     {:on-click #(dispatch [:application/update-review-field state-name review-state-id])}
     review-state-label]))

(defn opened-review-state-list [state-name current-state all-states]
  (mapv (partial review-state-row state-name (or @current-state (ffirst all-states))) all-states))

(defn- toggle-review-list-visibility [list-kwd]
  (dispatch [:application/toggle-review-list-visibility list-kwd]))

(defn- application-deactivate-toggle
  []
  (let [state     (subscribe [:state-query [:application :review :state]])
        can-edit? (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    (fn []
      (let [active? (= "active" @state)]
        [:div.application-handling__review-deactivate-row
         [:span.application-handling__review-deactivate-label (str "Hakemuksen tila")]
         [:div.application-handling__review-deactivate-toggle
          [:div.application-handling__review-deactivate-toggle-slider
           {:class    (cond-> ""
                              active? (str " application-handling__review-deactivate-toggle-slider-right")
                              (not active?) (str " application-handling__review-deactivate-toggle-slider-left")
                              (not @can-edit?) (str " application-handling__review-deactivate-toggle-slider--disabled"))
            :on-click #(when @can-edit?
                         (dispatch [:application/set-application-activeness (not active?)]))}
           [:div.application-handling__review-deactivate-toggle-label-left
            "Aktiivinen"]
           [:div.application-handling__review-deactivate-toggle-divider]
           [:div.application-handling__review-deactivate-toggle-label-right
            "Passiivinen"]]]]))))

(defn- hakukohde-name [hakukohde-oid]
  (if-let [hakukohde-name @(subscribe [:application/hakukohde-name
                                       hakukohde-oid])]
    [:span hakukohde-name]
    [:i.zmdi.zmdi-spinner.spin]))

(defn- opened-review-hakukohde-list-row
  [selected-hakukohde-oid hakukohde-oid]
  (let [selected? (= selected-hakukohde-oid hakukohde-oid)]
    [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde
     {:data-hakukohde-oid hakukohde-oid
      :class              (when selected? "application-handling__review-state-selected-row-hakukohde")
      :on-click           #(dispatch [:application/select-review-hakukohde hakukohde-oid])}
     (when selected? [icon-check])
     [hakukohde-name hakukohde-oid]]))

(defn- selected-review-hakukohde-row
  [selected-hakukohde-oid on-click application-hakukohde-oids]
  (let [application-has-multiple-hakukohde? (< 1 (count application-hakukohde-oids))
        settings-visible?                   (subscribe [:state-query [:application :review-settings :visible?]])]
    [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde.application-handling__review-state-selected-row-hakukohde
     {:on-click (when (and application-has-multiple-hakukohde?
                           (not @settings-visible?))
                  on-click)
      :class (str (when-not application-has-multiple-hakukohde? "application-handling__review-state-row-hakukohde--single-option")
                  (when-not @settings-visible? " application-handling__review-state-row--enabled"))}
     [icon-check]
     [hakukohde-name selected-hakukohde-oid]]))

(defn- application-hakukohde-selection
  []
  (let [selected-hakukohde-oid     (subscribe [:state-query [:application :selected-review-hakukohde]])
        application-hakukohde-oids (subscribe [:state-query [:application :selected-application-and-form :application :hakukohde]])
        list-opened                (subscribe [:application/review-list-visible? :hakukohde])
        select-list-item           (partial toggle-review-list-visibility :hakukohde)]
    (fn []
      (when (not-empty @application-hakukohde-oids)
        [:div.application-handling__review-state-container.application-handling__review-state-container--columnar
         [:div.application-handling__review-header (str "Hakukohteet (" (count @application-hakukohde-oids) ")")]
         (if @list-opened
           [:div.application-handling__review-state-list-opened-anchor
            (into
              [:div.application-handling__review-state-list-opened {:on-click select-list-item}]
              (map #(opened-review-hakukohde-list-row @selected-hakukohde-oid %) @application-hakukohde-oids))]
           (selected-review-hakukohde-row @selected-hakukohde-oid select-list-item @application-hakukohde-oids))]))))

(defn- review-settings-checkbox [setting-kwd]
  (let [checked?  (subscribe [:application/review-state-setting-enabled? setting-kwd])
        disabled? (subscribe [:application/review-state-setting-disabled? setting-kwd])]
    [:input.application-handling__review-state-setting-checkbox
     {:class     (str "application-handling__review-state-setting-checkbox-" (name setting-kwd))
      :type      "checkbox"
      :checked   @checked?
      :disabled  @disabled?
      :on-change #(dispatch [:application/toggle-review-state-setting setting-kwd])}]))

(defn- review-state-comment
  [state-name selected-hakukohde]
  (fn [state-name selected-hakukohde]
    (let [review-note     (subscribe [:state-query [:application :notes selected-hakukohde state-name]])
          review-notes    (subscribe [:state-query [:application :review-notes]])
          previous-note   (->> @review-notes
                               (filter #(and (= (name state-name) (:state-name %))
                                             (= (name selected-hakukohde) (:hakukohde %))))
                               first
                               :notes)
          button-enabled? (and (-> @review-note clojure.string/blank? not)
                               (not= @review-note previous-note))]
      [:div.application-handling__review-state-selected-container
       [:textarea.application-handling__review-note-input.application-handling__eligibility-state-comment
        {:value       @review-note
         :placeholder "Hylkäyksen syy.."
         :on-change   (fn [event]
                        (let [note (.. event -target -value)]
                          (dispatch [:state-update #(assoc-in % [:application :notes selected-hakukohde state-name] note)])))}]
       [:button.application-handling__review-note-submit-button
        {:type     "button"
         :on-click #(dispatch [:application/add-review-note @review-note state-name])
         :disabled (not button-enabled?)
         :class    (if button-enabled?
                     "application-handling__review-note-submit-button--enabled"
                     "application-handling__review-note-submit-button--disabled")}
        "Tallenna"]])))

(defn- application-hakukohde-review-input
  [label kw states]
  (let [current-hakukohde                  (subscribe [:state-query [:application :selected-review-hakukohde]])
        list-opened                        (subscribe [:application/review-list-visible? kw])
        list-click                         (partial toggle-review-list-visibility kw)
        settings-visible?                  (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?                     (subscribe [:application/review-state-setting-enabled? kw])
        eligibility-automatically-checked? (subscribe [:application/eligibility-automatically-checked?])]
    (fn [_ _ _]
      (when (or @settings-visible? @input-visible?)
        (let [review-state-for-current-hakukohde (subscribe [:state-query [:application :review :hakukohde-reviews (keyword @current-hakukohde) kw]])]
          [:div.application-handling__review-state-container
           {:class (str "application-handling__review-state-container-" (name kw))}
           (when @settings-visible?
             [review-settings-checkbox kw])
           [:div.application-handling__review-header
            {:class (str "application-handling__review-header--" (name kw))}
            [:span label]
            (when (and (= :eligibility-state kw)
                       @eligibility-automatically-checked?)
              [:i.zmdi.zmdi-check-circle.zmdi-hc-lg.application-handling__eligibility-automatically-checked
               {:title "Hakukelpoisuus asetettu automaattisesti"}])]
           (if @list-opened
             [:div.application-handling__review-state-list-opened-anchor
              (into [:div.application-handling__review-state-list-opened
                     {:on-click list-click}]
                    (opened-review-state-list kw review-state-for-current-hakukohde states))]
             [:div.application-handling__review-state-selected-container
              [review-state-selected-row
               list-click
               (application-states/get-review-state-label-by-name
                states
                (or @review-state-for-current-hakukohde (ffirst states)))]
              (when (and (= :eligibility-state kw)
                         (= "uneligible" @review-state-for-current-hakukohde))
                [review-state-comment kw (keyword @current-hakukohde)])])])))))

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
    "Vertaile"]])

(defn event-caption [event show-details?]
  (match event
         {:event-type "review-state-change"}
         (let [label (application-states/get-review-state-label-by-name
                       review-states/application-review-states
                       (:new-review-state event))]
           (if (= (:new-review-state event) "information-request")
             [:span.application-handling__event-caption--inner label (virkailija-initials-span event)]
             label))

         {:event-type "updated-by-applicant"}
         (update-event-caption
          [:span
           "Hakijalta "
           (count @(subscribe [:application/changes-made-for-event (:id event)]))
           " muutosta"]
          event
          show-details?)

         {:event-type "updated-by-virkailija"}
         (update-event-caption
          [:span
           (or (virkailija-initials-span event) "Tuntematon")
           " teki "
           (count @(subscribe [:application/changes-made-for-event (:id event)]))
           " muutosta"]
          event
          show-details?)

         {:event-type "received-from-applicant"}
         "Hakemus vastaanotettu"

         {:event-type "received-from-virkailija"}
         [:span.application-handling__event-caption--inner
          (virkailija-initials-span event)
          " syötti hakemuksen"]

         {:event-type "hakukohde-review-state-change"}
         [:span.application-handling__event-caption--inner
          (str (->> review-states/hakukohde-review-types
                    (filter #(= (keyword (:review-key event)) (first %)))
                    (first)
                    (second)) ": "
               (application-states/get-review-state-label-by-name
                 (->> review-states/hakukohde-review-types
                      (map last)
                      (apply concat)
                      (distinct))
                 (:new-review-state event)))
          " "
          (virkailija-initials-span event)]

         {:event-type "eligibility-state-automatically-changed"}
         [:div.application-handling__multi-line-event-caption
          [:span.application-handling__event-caption--inner
           (str "Hakukelpoisuus: "
                (some #(when (= (:new-review-state event) (first %))
                         (second %))
                      review-states/application-hakukohde-eligibility-states))]
          [:span.application-handling__event-caption--inner.application-handling__event-caption--extra-info
           (str "Hakukohteen \""
                @(subscribe [:application/hakukohde-name (:hakukohde event)])
                "\" hakukelpoisuus asetettu automaattisesti")]]

         {:event-type "attachment-review-state-change"}
         [:span.application-handling__event-caption--inner
          (str "Liitepyyntö: "
               (application-states/get-review-state-label-by-name
                 review-states/attachment-hakukohde-review-types
                 (:new-review-state event))
               " ")
          (virkailija-initials-span event)]

         {:event-type "modification-link-sent"}
         "Vahvistussähköposti lähetetty hakijalle"

         {:subject _ :message message}
         [:div.application-handling__multi-line-event-caption
          [:span.application-handling__event-caption--inner "Täydennyspyyntö lähetetty " (virkailija-initials-span event)]
          [:span.application-handling__event-caption--inner.application-handling__event-caption--extra-info (str "\"" message "\"")]]

         :else "Tuntematon"))

(defn event-row
  [_]
  (let [show-details? (r/atom false)]
    (fn [event]
      [:div.application-handling__event-row
       [:span.application-handling__event-timestamp
        (t/time->short-str (or (:time event) (:created-time event)))]
       [:div.application-handling__event-caption-container
        [:div.application-handling__event-caption
         (event-caption event show-details?)]
        (when @show-details?
          [:ul.application-handling__event-row-details
           (for [[key field] @(subscribe [:application/changes-made-for-event (:id event)])]
             [:li
              {:on-click (fn [e]
                           (.stopPropagation e)
                           (dispatch [:application/highlight-field key]))
               :key      (str "event-list-row-for-" (:id event) "-" key)}
              [:a (:label field)]])])]])))

(defn application-review-events []
  [:div.application-handling__event-list
   [:div.application-handling__review-header "Tapahtumat"]
   (doall
    (map-indexed
     (fn [i event]
       ^{:key (str "event-row-for-" i)}
       [event-row event])
     @(subscribe [:application/events-and-information-requests])))])

(defn update-review-field [field convert-fn evt]
  (let [new-value (-> evt .-target .-value)]
    (dispatch [:application/update-review-field field (convert-fn new-value)])))

(defn convert-score [review new-value]
  (let [maybe-number (js/Number new-value)]
    (cond
      (= "" new-value)
      nil

      ;; JS NaN is the only thing not equal with itself
      ;; and this is the way to detect it
      (not= maybe-number maybe-number)
      (:score review)

      :else
      maybe-number)))

(defn- application-review-note-input []
  (let [input-value     (subscribe [:state-query [:application :review-comment]])
        review-notes    (subscribe [:state-query [:application :review-notes]])
        button-enabled? (reaction (and (-> @input-value clojure.string/blank? not)
                                       (every? (comp not :animated?) @review-notes)))]
    (fn []
      [:div.application-handling__review-row.application-handling__review-row--notes-row
       [:input.application-handling__review-note-input
        {:type      "text"
         :value     @input-value
         :on-change (fn [event]
                      (let [review-comment (.. event -target -value)]
                        (dispatch [:application/set-review-comment-value review-comment])))}]
       [:button.application-handling__general-review-note-submit-button
        {:type     "button"
         :class    (if @button-enabled?
                     "application-handling__review-note-submit-button--enabled"
                     "application-handling__review-note-submit-button--disabled")
         :disabled (not @button-enabled?)
         :on-click (fn [_]
                     (dispatch [:application/add-review-note @input-value nil]))}
        "Lisää"]])))

(defn- application-review-note [note-idx]
  (let [note             (subscribe [:state-query [:application :review-notes note-idx]])
        name             (reaction (if (and (:first-name @note) (:last-name @note))
                                     (str (:first-name @note) " " (:last-name @note))
                                     "Virkailija ei tiedossa"))
        created-time     (reaction (when-let [created-time (:created-time @note)]
                                     (temporal/time->short-str created-time)))
        notes            (reaction (:notes @note))
        animated?        (reaction (:animated? @note))
        remove-disabled? (reaction (or (-> @note :state some?)
                                       (-> @note :id not)))
        hakukohde-name   (subscribe [:application/hakukohde-name (:hakukohde @note)])]
    (fn [note-idx]
      [:div.application-handling__review-note
       (when @animated?
         {:class "animated fadeIn"})
       [:span.application-handling__review-note-column
        (when (:hakukohde @note)
          {:data-tooltip-narrow (str "Kelpoisuusmerkinnän selite"
                                     (when (not= "form" (:hakukohde @note))
                                       (str " hakukohteelle " @hakukohde-name)))})
        @notes]
       [:div.application-handling__review-details-column
        [:span @name]
        [:span @created-time]
        [:a.application-handling__review-details-remove-link
         {:href     "#"
          :class    (when @remove-disabled? "application-handling__review-details-remove-link--disabled")
          :on-click (fn [event]
                      (.preventDefault event)
                      (dispatch [:application/remove-review-note note-idx]))}
         [:i.zmdi.zmdi-close]]]])))

(defn application-review-inputs []
  (let [review            (subscribe [:state-query [:application :review]])
        ; React doesn't like null, it leaves the previous value there, hence:
        review-field->str (fn [review field] (if-let [notes (field @review)] notes ""))
        settings-visible? (subscribe [:state-query [:application :review-settings :visible?]])
        input-visible?    (subscribe [:application/review-state-setting-enabled? :score])
        notes-count       (subscribe [:application/review-notes-count])
        can-edit?         (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
    (fn []
      [:div.application-handling__review-inputs
       (when (or @settings-visible? @input-visible?)
         [:div.application-handling__review-row
          (when @settings-visible?
            [review-settings-checkbox :score])
          [:div.application-handling__review-header.application-handling__review-header--points
           "Pisteet"]
          [:input.application-handling__score-input
           {:type       "text"
            :max-length "2"
            :size       "2"
            :value      (review-field->str review :score)
            :disabled   (or @settings-visible? (not @can-edit?))
            :on-change  (when-not @settings-visible?
                          (partial update-review-field :score (partial convert-score @review)))}]])
       [:div.application-handling__review-row--nocolumn
        [:div.application-handling__review-header "Muistiinpanot"]
        [application-review-note-input]
        (->> (range @notes-count)
             (map (fn [idx]
                    ^{:key (str "application-review-note-" idx)}
                    [application-review-note idx])))]])))

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
     "Muokkaa hakemusta"]))

(defn- application-information-request-recipient []
  (let [email (subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading "Vastaanottaja:"]
     [:div @email]]))

(defn- application-information-request-subject []
  (let [subject (subscribe [:state-query [:application :information-request :subject]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading "Aihe:"]
     [:div.application-handling__information-request-text-input-container
      [:input.application-handling__information-request-text-input
       {:value     @subject
        :maxLength 78
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
                                  "Täydennyspyyntöä lähetetään"
                                  "Lähetä täydennyspyyntö"))]
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
     "Lähetä täydennyspyyntö hakijalle"
     (when (nil? @request-state)
       [:i.zmdi.zmdi-close-circle.application-handling__information-request-close-button
        {:on-click #(dispatch [:application/set-information-request-window-visibility false])}])]))

(defn- application-information-request-submitted []
  [:div.application-handling__information-request-row.application-handling__information-request-row--checkmark-container
   [:div.application-handling__information-request-submitted-loader]
   [:div.application-handling__information-request-submitted-checkmark]
   [:span.application-handling__information-request-submitted-text "Täydennyspyyntö lähetetty"]])

(defn- application-information-request-contains-modification-link []
  [:div.application-handling__information-request-row
   [:p.application-handling__information-request-contains-modification-link
    "Muokkauslinkki lähtee viestin mukana automaattisesti"]])

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
          "Lähetä täydennyspyyntö hakijalle"]]))))

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
     [:span "Lähetä vahvistussähköposti hakijalle"]
     [:span.application-handling__resend-modify-application-link-email-text @recipient]]))

(defn- application-resend-modify-link-confirmation []
  (let [state (subscribe [:state-query [:application :modify-application-link :state]])]
    (when @state
      [:div.application-handling__resend-modify-link-confirmation.application-handling__button.animated.fadeIn
       {:class (when (= @state :disappearing) "animated fadeOut")}
       [:div.application-handling__resend-modify-link-confirmation-indicator]
       "Muokkauslinkki lähetetty hakijalle sähköpostilla"])))

(defn- attachment-review-row [review selected-hakukohde lang]
  (let [list-opened (r/atom false)]
    (fn [review selected-hakukohde lang]
      (let [attachment-key (-> review :key keyword)
            selected-state (or (:state review)
                               "not-checked")
            can-edit?      (subscribe [:state-query [:application :selected-application-and-form :application :can-edit?]])]
        [:div.application__attachment-review-row
         [:div.application__attachment-review-row-answer-information
          [:p.application__attachment-review-row-label (some #(-> review :label % not-empty) [lang :fi :sv :en])]
          (for [attachment-file (-> review :values flatten)
                :let [text (str (:filename attachment-file) " (" (util/size-bytes->str (:size attachment-file)) ")")]]
            ^{:key (:key attachment-file)}
            [:div
             (if (= (:virus-scan-status attachment-file) "done")
               [:a {:href (str "/lomake-editori/api/files/content/" (:key attachment-file))}
                text]
               text)])]
         (if @list-opened
           [:div.application-handling__review-state-list-opened
            (doall
              (for [[state label] review-states/attachment-hakukohde-review-types]
                [:div.application-handling__review-state-row.application-handling__review-state-row--small
                 {:class    (when (= state selected-state) "application-handling__review-state-selected-row application-handling__review-state-row--enabled")
                  :on-click (fn []
                              (swap! list-opened not)
                              (dispatch [:application/update-attachment-review attachment-key selected-hakukohde state]))
                  :key      (str attachment-key label)}
                 (when (= state selected-state) (icon-check)) label]))]
           [:div.application-handling__review-state-row.application-handling__review-state-row--small.application-handling__review-state-selected-row
            {:class    (if @can-edit?
                         "application-handling__review-state-row--enabled"
                         "application-handling__review-state-row--disabled")
             :on-click #(when @can-edit? (swap! list-opened not))}
            (icon-check)
            (application-states/get-review-state-label-by-name review-states/attachment-hakukohde-review-types selected-state)])]))))

(defn- attachment-review-area [hakukohde reviews review-positioning lang]
  (fn [hakukohde reviews review-positioning lang]
    [:div.application-handling__attachment-review-container.animated
     {:class (str (when (= :fixed review-positioning)
                    "application-handling__attachment-review-container-floating")
                  (if @(subscribe [:state-query [:application :show-attachment-reviews?]])
                    " fadeInRight"
                    " fadeOutRight"))}
     (when (not-empty reviews)
       [:div
        [:p.application-handling__attachment-review-header
         (str (if (= "form" hakukohde) "Lomakkeen" "Hakukohteen")
              " liitepyynnöt (" (count reviews) ")")]
        (doall
          (for [attachment reviews]
            ^{:key (:id attachment)}
            [attachment-review-row attachment hakukohde lang]))])]))

(defn application-review []
  (let [review-positioning      (subscribe [:state-query [:application :review-positioning]])
        settings-visible        (subscribe [:state-query [:application :review-settings :visible?]])
        show-attachment-review? (r/atom false)]
    (fn []
      (let [selected-review-hakukohde        (subscribe [:state-query [:application :selected-review-hakukohde]])
            attachment-reviews-for-hakukohde (subscribe [:application/get-attachment-reviews-for-selected-hakukohde @selected-review-hakukohde])
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
             [:span.application-handling__review-settings-header-text "Asetukset"]])]
         [:div.application-handling__review
          (when @show-attachment-review?
            [attachment-review-area @selected-review-hakukohde @attachment-reviews-for-hakukohde @review-positioning @lang])
          [:div.application-handling__review-outer-container
           [application-hakukohde-selection]
           (when (not-empty @attachment-reviews-for-hakukohde)
             [:div.application-handling__attachment-review-toggle-container
              {:on-click (fn []
                           (when-not @settings-visible
                             (let [show? (not @show-attachment-review?)]
                               (dispatch [:state-update #(assoc-in % [:application :show-attachment-reviews?] show?)])
                               (if show?
                                 (reset! show-attachment-review? show?)
                                 (js/setTimeout #(reset! show-attachment-review? show?) 500)))))}
              (when @settings-visible
                [review-settings-checkbox :attachment-handling])
              [:span.application-handling__attachment-review-toggle
               (if @show-attachment-review?
                 [:span [:i.zmdi.zmdi-chevron-right] [:i.zmdi.zmdi-chevron-right]]
                 [:span [:i.zmdi.zmdi-chevron-left] [:i.zmdi.zmdi-chevron-left]])]
              " Liitepyynnöt (" (count @attachment-reviews-for-hakukohde) ")"])
           [application-hakukohde-review-inputs review-states/hakukohde-review-types]
           (when @(subscribe [:application/show-info-request-ui?])
             [application-information-request])
           [application-review-inputs]
           [application-modify-link]
           [application-resend-modify-link]
           [application-resend-modify-link-confirmation]
           [application-deactivate-toggle]
           [application-review-events]]]]))))

(defn application-heading [application hakukohteet-by-oid]
  (let [answers            (:answers application)
        pref-name          (-> application :person :preferred-name)
        last-name          (-> application :person :last-name)
        ssn                (-> application :person :ssn)
        birth-date         (-> application :person :birth-date)
        person-oid         (-> application :person :oid)
        yksiloity          (-> application :person :yksiloity)
        email              (get-in answers [:email :value])
        applications-count (:applications-count application)]
    [:div.application__handling-heading
     [:div.application-handling__review-area-main-heading-container
      [:div.application-handling__review-area-main-heading-person-info
       [:div.application-handling__review-area-main-heading-name-row
        (when pref-name
          [:h2.application-handling__review-area-main-heading
           (str last-name ", " pref-name " — " (or ssn birth-date))])
        (when (> applications-count 1)
          [:a.application-handling__review-area-main-heading-applications-link
           {:on-click (fn [_]
                        (dispatch [:application/navigate
                                   (str "/lomake-editori/applications/search"
                                        "?term=" (or ssn email))]))}
           (str applications-count " hakemusta")])]
       (when person-oid
         [:div.application-handling__review-area-main-heading-person-oid-row
          [:div.application-handling__applicant-links
           [:a
            {:href   (str "/henkilo-ui/oppija/"
                          person-oid
                          "?permissionCheckService=ATARU")
             :target "_blank"}
            [:i.zmdi.zmdi-account-circle.application-handling__review-area-main-heading-person-icon]
            [:span.application-handling__review-area-main-heading-person-oid
             (str "Oppija " person-oid)]]
           [:a
            {:href   (str "/suoritusrekisteri/#/opiskelijat?henkilo=" person-oid)
             :target "_blank"}
            [:i.zmdi.zmdi-collection-text.application-handling__review-area-main-heading-person-icon]
            [:span.application-handling__review-area-main-heading-person-oid
             "Henkilön suoritukset"]]]
          (when-not yksiloity
            [:a.individualization
             {:href   (str "/henkilo-ui/oppija/"
                           person-oid
                           "/duplikaatit?permissionCheckService=ATARU")
              :target "_blank"}
             [:i.zmdi.zmdi-account-o]
             [:span "Hakijaa ei ole yksilöity. "
              [:span.important "Tee yksilöinti henkilöpalvelussa."]]])])]
      (when (not (contains? (:answers application) :hakukohteet))
        [:ul.application-handling__hakukohteet-list
         (for [hakukohde-oid (:hakukohde application)]
           ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
           [:li.application-handling__hakukohteet-list-row
            [:div.application-handling__review-area-hakukohde-heading
             [hakukohde-and-tarjoaja-name hakukohde-oid]]])])]]))

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

(defn application-review-area [applications]
  (let [selected-key                  (subscribe [:state-query [:application :selected-key]])
        selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        belongs-to-current-form       (fn [key applications] (first (filter #(= key (:key %)) applications)))
        expanded?                     (subscribe [:state-query [:application :application-list-expanded?]])
        review-positioning            (subscribe [:state-query [:application :review-positioning]])]
    (fn [applications]
      (let [application        (:application @selected-application-and-form)]
        (when (and (belongs-to-current-form @selected-key applications)
                   (not @expanded?))
          [:div.application-handling__detail-container
           [close-application]
           [application-heading application]
           [:div.application-handling__review-area
            [:div.application-handling__application-contents
             [application-contents @selected-application-and-form]]
            [:span#application-handling__review-position-canary]
            (when (= :fixed @review-positioning) [floating-application-review-placeholder])
            [application-review]]])))))

(defn application []
  (let [search-control-all-page (subscribe [:application/search-control-all-page-view?])
        filtered-applications   (subscribe [:application/filtered-applications])]
    [:div
     [:div.application-handling__overview
      [application-search-control]
      (when (not @search-control-all-page)
        [:div.application-handling__bottom-wrapper.select_application_list
         [haku-heading]
         [application-list @filtered-applications]
         [application-list-loading-indicator]])]
     (when (not @search-control-all-page)
       [:div.application-handling__review-area-container
        [application-review-area @filtered-applications]])]))

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
                         "hakija"
                         (str (:first-name @event) " " (:last-name @event)))]
        [:div.application-handling__version-history-header
         [:div.application-handling__version-history-header-text
          "Vertailu muutoksesta " (t/time->short-str (or (:time @event) (:created-time @event)))]
         [:div.application-handling__version-history-header-sub-text
          (str changed-by " muutti " changes-amount " vastausta:")]]))))

(defn- application-version-history-list-value [values]
  [:ol.application-handling__version-history-list-value
   (map-indexed
    (fn [index value]
      ^{:key index}
      [:li value])
    values)])

(defn application-version-history-value [value-or-values]
  (cond
    (every? sequential? value-or-values)
    [:ol.application-handling__version-history-question-group-value
     (map-indexed
      (fn [index values]
        ^{:key index}
        [:li (application-version-history-list-value values)])
      value-or-values)]

    (sequential? value-or-values)
    (application-version-history-list-value value-or-values)

    :else (str value-or-values)))

(defn application-version-history-row [key history-item]
  ^{:key (str "application-change-history-" key)}
  [:div.application-handling__version-history-row
   [:div.application-handling__version-history-row-label
    (:label history-item)]
   [:div.application-handling__version-history-row-value.application-handling__version-history-row-value-old
    [:div.application-handling__version-history-row-value-sign (gstring/unescapeEntities "&minus;")]
    (application-version-history-value (:old history-item))]
   [:div.application-handling__version-history-row-value.application-handling__version-history-row-value-new
    [:div.application-handling__version-history-row-value-sign (gstring/unescapeEntities "&plus;")]
    (application-version-history-value (:new history-item))]])

(defn application-version-changes []
  (let [history-items (subscribe [:application/current-history-items])]
    (when @history-items
      [:div.virkailija-modal__container
       [:div.virkailija-modal__content
        [:a.virkailija-modal__close-link
         {:on-click #(dispatch [:application/close-application-version-history])}
         "Sulje"]
        [application-version-history-header (count @history-items)]
        (for [[key item] @history-items]
          ^{:key (str "application-history-row-for-" key)}
          [application-version-history-row key item])]])))
