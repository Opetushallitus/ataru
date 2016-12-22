(ns ataru.virkailija.application.view
  (:require
   [cljs.core.match :refer-macros [match]]
   [clojure.string :as string]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.ratom :refer-macros [reaction]]
   [reagent.core :as r]
   [cljs-time.format :as f]
   [ataru.virkailija.temporal :as t]
   [ataru.virkailija.application.handlers]
   [ataru.application.review-states :refer [application-review-states]]
   [ataru.application-common.application-readonly :as readonly-contents]
   [ataru.cljs-util :refer [wrap-scroll-to classnames]]
   [taoensso.timbre :refer-macros [spy debug]]))

(defn toggle-form-list-open! [open]
  (swap! open not)
  nil) ;; Returns nil so that React doesn't whine about event handlers returning false


(defn form-list-arrow-up [open]
  [:i.zmdi.zmdi-chevron-up.application-handling__form-list-arrow
   {:on-click #(toggle-form-list-open! open)}])

(defn form-list-row [name href selected? deleted? open]
  [:a.application-handling__form-list-row-link
   {:href href}
   (let [row-element [:div.application-handling__form-list-row
                      {:class (classnames {:application-handling__form-list-selected-row selected?
                                           :application-handling__form-list-deleted-row deleted?})
                              :on-click #(toggle-form-list-open! open)}
                      (str name (if deleted? " (poistettu) ") "")]]
     (if selected? [wrap-scroll-to row-element] row-element))])

(defn form-list-opened [forms hakukohteet selected-form-key selected-hakukohde open]
  (let [form-rows      (for [[id form] forms
                             :let [selected? (= id selected-form-key)
                                   deleted? (:deleted form)]]
                         ^{:key id}
                         [form-list-row (str "Lomake – " (:name form)) (str "/lomake-editori/applications/" (:key form)) selected? deleted? open])
        hakukohde-rows (for [{:keys [hakukohde hakukohde-name]} hakukohteet
                             :let [selected? (= hakukohde (:hakukohde selected-hakukohde))]]
                         ^{:key hakukohde}
                         [form-list-row (str "Hakukohde – " hakukohde-name) (str "/lomake-editori/applications/hakukohde/" hakukohde) selected? false open])]
    [:div.application-handling__form-list-open-wrapper ;; We need this wrapper to anchor up-arrow to be seen at all scroll-levels of the list
     [form-list-arrow-up open]
     (into [:div.application-handling__form-list-open] (into form-rows hakukohde-rows))]))

(defn form-list-closed [selected-form selected-hakukohde open]
  [:div.application-handling__form-list-closed
   {:on-click #(toggle-form-list-open! open)}
   [:div.application-handling__form-list-row.application-handling__form-list-selected-row (or
                                                                                            (:name selected-form)
                                                                                            (:hakukohde-name selected-hakukohde))]
   [:i.zmdi.zmdi-chevron-down.application-handling__form-list-arrow]])

(defn form-list []
  (let [forms                  (subscribe [:state-query [:editor :forms]])
        hakukohteet            (subscribe [:state-query [:editor :hakukohteet]])
        selected-form-key      (subscribe [:state-query [:editor :selected-form-key]])
        selected-hakukohde     (subscribe [:state-query [:editor :selected-hakukohde]])
        selected-form          (subscribe [:editor/selected-form])
        open                   (r/atom false)]
    (fn []
      [:div.application-handling__form-list-wrapper
       (if @open
        [form-list-opened @forms @hakukohteet @selected-form-key @selected-hakukohde open]
        [form-list-closed @selected-form @selected-hakukohde open])])))

(defn excel-download-link [applications application-filter]
  (let [form-key     (reaction (:key @(subscribe [:editor/selected-form])))
        hakukohde    (reaction @(subscribe [:state-query [:editor :selected-hakukohde]]))
        query-string (fn [filters] (str "?state=" (string/join "&state=" (map name filters))))]
    (fn [applications application-filter]
      (when (> (count applications) 0)
        (let [url (if @form-key
                    (str "/lomake-editori/api/applications/excel/"
                         @form-key
                         (query-string application-filter))
                    (str "/lomake-editori/api/applications/excel/"
                         (:form-key @hakukohde)
                         "/"
                         (:hakukohde @hakukohde)
                         (query-string application-filter)))]
          [:a.application-handling__excel-download-link
           {:href url}
           (str "Lataa hakemukset Excel-muodossa (" (count applications) ")")])))))

(defn application-list-contents [applications]
  (let [selected-key       (subscribe [:state-query [:application :selected-key]])]
    (fn [applications]
      (into [:div.application-handling__list]
            (for [application applications
                  :let        [key       (:key application)
                               time      (t/time->str (:created-time application))
                               applicant (:applicant-name application)]]
              [:div.application-handling__list-row
               {:on-click #(dispatch [:application/select-application (:key application)])
                :class    (when (= @selected-key key)
                            "application-handling__list-row--selected")}
               [:span.application-handling__list-row--applicant
                (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
               [:span.application-handling__list-row--time time]
               [:span.application-handling__list-row--score
                (or (:score application) "")]
               [:span.application-handling__list-row--state
                (or
                 (get application-review-states (:state application))
                 "Tuntematon")]])))))

(defn icon-check []
  [:img.application-handling__review-state-selected-icon
   {:src "/lomake-editori/images/icon_check.png"}])

(defn toggle-filter [application-filter review-state-id selected]
  (let [new-application-filter (if selected
                                 (remove #(= review-state-id %) application-filter)
                                 (conj application-filter review-state-id))]
    (dispatch [:state-update (fn [db _] (assoc-in db [:application :filter] new-application-filter))])))

(defn state-filter-controls []
  (let [application-filter     (subscribe [:state-query [:application :filter]])
        review-state-counts    (subscribe [:state-query [:application :review-state-counts]])
        filter-opened          (r/atom false)
        toggle-filter-opened   (fn [_] (reset! filter-opened (not @filter-opened)))
        get-review-state-count (fn [counts state-id] (or (get counts state-id) 0))]
    (fn []
      [:span.application-handling__filter-state
       [:a
        {:on-click toggle-filter-opened}
        "Tila"]
       (when @filter-opened
         (into [:div.application-handling__filter-state-selection]
               (mapv
                (fn [review-state]
                  (let [review-state-id (first review-state)
                        filter-selected (some #{review-state-id} @application-filter)]
                    [:div.application-handling__filter-state-selection-row
                     {:class    (if filter-selected "application-handling__filter-state-selected-row" "")
                      :on-click #(toggle-filter @application-filter review-state-id filter-selected)}
                     (if filter-selected [icon-check] nil)
                     (str (second review-state)
                          " ("
                          (get-review-state-count @review-state-counts review-state-id)
                          ")")]))
                application-review-states)))
       (when @filter-opened [:div.application-handling__filter-state-selection-arrow-down])])))

(defn sortable-column-click [column-id evt]
  (dispatch [:application/update-sort column-id]))

(defn application-list-basic-column-header [column-id css-class heading]
  (let [application-sort (subscribe [:state-query [:application :sort]])]
    (fn [column-id css-class heading]
      [:span
       {:class    css-class
        :on-click (partial sortable-column-click column-id)}
       heading
       (when (= column-id (:column @application-sort))
         (if (= :ascending (:order @application-sort))
           [:i.zmdi.zmdi-chevron-down]
           [:i.zmdi.zmdi-chevron-up]))])))

(defn application-list [applications]
  [:div
   [:div.application-handling__list-header.application-handling__list-row
    [application-list-basic-column-header
     :applicant-name
     "application-handling__list-row--applicant"
     "Hakija"]
    [application-list-basic-column-header
     :created-time
     "application-handling__list-row--time"
     "Saapunut"]
    [application-list-basic-column-header
     :score
     "application-handling__list-row--score"
     "Pisteet"]
    [:span.application-handling__list-row--state [state-filter-controls]]]
   [application-list-contents applications]])

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn review-state-selected-row [label]
  [:div.application-handling__review-state-row.application-handling__review-state-selected-row
   [icon-check] label])

(defn review-state-row [current-review-state review-state]
  (let [[review-state-id review-state-label] review-state]
    (if (= current-review-state review-state-id)
      (review-state-selected-row review-state-label)
      [:div.application-handling__review-state-row
       {:on-click #(dispatch [:application/update-review-field :state review-state-id])}
       review-state-label])))

(defn opened-review-state-list [review-state]
  (mapv (partial review-state-row @review-state) application-review-states))

(defn application-review-state []
  (let [review-state (subscribe [:state-query [:application :review :state]])
        list-opened  (r/atom false)
        list-click   (fn [evt] (swap! list-opened not))]
    (fn []
      [:div.application-handling__review-state-container
       [:div.application-handling__review-header "Tila"]
       (if @list-opened
         [:div.application-handling__review-state-list-opened-anchor
          (into [:div.application-handling__review-state-list-opened
                 {:on-click list-click}]
                (opened-review-state-list review-state))]
         [:div
          {:on-click list-click}
          (review-state-selected-row (get application-review-states @review-state))])])))

(defn event-caption [event]
  (case (:event-type event)
    "review-state-change"     (get application-review-states (:new-review-state event))
    "updated-by-applicant"    "Hakija muokannut hakemusta"
    "received-from-applicant" "Hakemus vastaanotettu"
    "Tuntematon"))

(defn event-row [event]
  (let [time-str     (t/time->short-str (:time event))
        to-event-row (fn [caption] [:div
                                    [:span.application-handling__event-timestamp time-str]
                                    [:span.application-handling__event-caption caption]])
        event-type   (:event-type event)
        event-caption (event-caption event)]
    (to-event-row event-caption)))

(defn application-review-events []
  (let [events (subscribe [:state-query [:application :events]])]
    (fn []
      (into
        [:div.application-handling__event-list
         [:div.application-handling__review-header "Tapahtumat"]]
        (mapv event-row @events)))))

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

(defn application-review-inputs []
  (let [review (subscribe [:state-query [:application :review]])
        ; React doesn't like null, it leaves the previous value there, hence:
        review-field->str (fn [review field] (if-let [notes (field @review)] notes ""))]
    (fn []
      [:div.application-handling__review-inputs
       [:div.application-handling__review-header "Hakijan arviointi"]
       [:div.application-handling__review-row
        [:div.application-handling__review-sub-header "Muistiinpanot"]
        [:textarea.application-handling__review-notes
         {:value (review-field->str review :notes)
          :on-change (partial update-review-field :notes identity)}]]
       [:div.application-handling__review-row
        [:div.application-handling__review-sub-header "Pisteet"]
        [:input.application-handling__score-input
         {:type "text"
          :max-length "2"
          :size "2"
          :value (review-field->str review :score)
          :on-change (partial update-review-field :score (partial convert-score @review))}]]])))

(defn application-review []
  [:div.application-handling__review
   [application-review-state]
   [application-review-inputs]
   [application-review-events]])

(defn application-heading [application]
  (let [answers (:answers application)
        pref-name (-> answers :preferred-name :value)
        last-name (-> answers :last-name :value)
        ssn       (or (-> answers :ssn :value) (-> answers :birth-date :value))]
    [:h2.application-handling__review-area-main-heading (str pref-name " " last-name ", " ssn)]))

(defn application-review-area [applications]
  (let [selected-key                  (subscribe [:state-query [:application :selected-key]])
        selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        review-state                  (subscribe [:state-query [:application :review :state]])
        application-filter            (subscribe [:state-query [:application :filter]])
        belongs-to-current-form       (fn [key applications] (first (filter #(= key (:key %)) applications)))
        included-in-filter            (fn [review-state filter] (some #{review-state} filter))]
    (fn [applications]
      (when (and (included-in-filter @review-state @application-filter)
                 (belongs-to-current-form @selected-key applications))
        [:div.panel-content
         [application-heading (:application @selected-application-and-form)]
         [:div.application-handling__review-area
          [application-contents @selected-application-and-form]
          [application-review]]]))))

(defn application []
  (let [applications       (subscribe [:state-query [:application :applications]])
        application-filter (subscribe [:state-query [:application :filter]])
        include-filtered   (fn [application-filter applications] (filter #(some #{(:state %)} application-filter) applications))]
    (fn []
      (let [filtered-applications (include-filtered @application-filter @applications)]
        [:div
         [:div.application-handling__overview
          [:div.panel-content
           [:div.application-handling__header
            [form-list]
            [excel-download-link filtered-applications @application-filter]]
           [application-list filtered-applications]]]
         [application-review-area filtered-applications]]))))
