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


(defn form-list-arrow [open]
  [:i.zmdi.application-handling__form-list-arrow
   {:class (if @open "zmdi-chevron-up" "zmdi-chevron-down")}])

(defn form-list-header []
  (let [selected-hakukohde (subscribe [:state-query [:editor :selected-hakukohde]])
        selected-form      (subscribe [:editor/selected-form])]
    (fn []
      [:div.application-handling__form-list-header
       (or (:name @selected-form)
           (:hakukohde-name @selected-hakukohde))])))

(defn index-of [s val from-index]
  (clojure.string/index-of (clojure.string/lower-case s)
                           (clojure.string/lower-case val)
                           from-index))

(defn match-text [text search-term]
  (if (clojure.string/blank? search-term)
    [{:text text :hilight false}]
    (loop [res           []
           current-index 0]
      (let [match-index (index-of text search-term current-index)]
        (cond
          (nil? match-index)
          (conj res {:text    (subs text current-index)
                     :hilight false})

          (< current-index match-index)
          (recur (conj res
                       {:text    (subs text current-index match-index)
                        :hilight false}
                       {:text    (subs text match-index (+ (count search-term) match-index))
                        :hilight true})
                 (+ match-index (count search-term)))

          :else
          (recur (conj res {:text    (subs text current-index (+ (count search-term) current-index))
                            :hilight true})
                 (+ current-index (count search-term))))))))

(defn hilighted-text->span [idx {:keys [text hilight]}]
  (let [key (str "hilight-" idx)]
    [:span
     (cond-> {:key key}
       (true? hilight)
       (assoc :class "application-handling__form-list-link--hilight"))
     text]))

(def text-with-hilighted-parts (comp (partial some :hilight) :text))

(defn form-list-column [forms url-fn open]
  [:div.application-handling__form-list-column
   (->> forms
        (map-indexed (fn [idx {:keys [deleted text] :as form}]
                       (let [key  (str "form-list-item-" idx)
                             text (map-indexed hilighted-text->span text)
                             href (url-fn form)]
                         [:div.application-handling__form-list-link-container
                          {:key key}
                          [:a (cond-> {:href     href
                                       :on-click #(toggle-form-list-open! open)}
                                (true? deleted)
                                (assoc :class "application-handling__form-list-link--deleted"))
                           text]])))
        (doall))])

(defn hakukohde->form-list-item [{:keys [hakukohde-name] :as hakukohde}]
  (assoc hakukohde :name hakukohde-name))

(defn hakukohde-url [{:keys [hakukohde]}]
  (str "/lomake-editori/applications/hakukohde/" hakukohde))

(defn form-url [{:keys [key]}]
  (str "/lomake-editori/applications/" key))

(defn form-column-header [header-text forms]
  (let [search-term (subscribe [:state-query [:application :search-term]])]
    (fn [header-text forms]
      [:span.application-handling__form-list-column-header
       (when (and (not (clojure.string/blank? @search-term))
                  (empty? forms))
         {:class "application-handling__form-list-column-header--no-results"})
       header-text])))

(defn hilighted-text [forms mutate search-term]
  (let [forms (mutate forms)]
    (cond->> (map (fn [{:keys [name application-count] :as form}]
                    (let [text (conj (match-text name search-term)
                                     {:text (str " (" (or application-count 0) ")") :hilight false})]
                      (assoc form :text text)))
                  forms)
      (not (clojure.string/blank? search-term))
      (filter text-with-hilighted-parts))))

(defn form-columns [open]
  (let [search-term    (subscribe [:state-query [:application :search-term]])
        hakukohde-list (reaction (hilighted-text @(subscribe [:state-query [:editor :hakukohteet]])
                                                 (partial map hakukohde->form-list-item)
                                                 @search-term))
        form-list      (reaction (hilighted-text @(subscribe [:state-query [:editor :forms]])
                                                 (partial reduce-kv (fn [forms _ form] (conj forms form)) [])
                                                 @search-term))]
    (fn [open]
      [:div.application-handling__form-list-column-wrapper-outer
       [:div.application-handling__form-list-header-row
        [form-column-header "Hakukohde" @hakukohde-list]
        [form-column-header "Lomake" @form-list]]
       [:div.application-handling__form-list-column-wrapper-inner
        [form-list-column @hakukohde-list hakukohde-url open] ;[hakukohde-column open]
        [form-list-column @form-list form-url open] ;[forms-column open]
        ]])))

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

(defn form-list-search [open]
  [:div.application-handling__form-list-search-row
   [:div.application-handling__form-list-column
    [:input.application-handling__form-list-search-row-item.application-handling__form-list-search-input
     {:type      "text"
      :on-change (fn [event]
                   (let [search-term (.. event -target -value)]
                     (dispatch [:application/search-form-list search-term])))}]]
   [:div.application-handling__form-list-close-container
    [:i.application-handling__form-list-search-row-item.zmdi.zmdi-close.application-handling__form-list-close-button
     {:on-click #(toggle-form-list-open! open)}]]])

(defn form-list [filtered-applications application-filter]
  (let [open (r/atom false)]
    (fn [filtered-applications application-filter]
      [:div.application-handling__form-list-wrapper-outer
       [:div.application-handling__header
        {:on-click #(toggle-form-list-open! open)}
        [:div
         [form-list-arrow open]
         [form-list-header]]
        [excel-download-link filtered-applications application-filter]]
       [:div.application-handling__form-list-indicator
        (when-not @open {:style {:display "none"}})]
       [:div.application-handling__form-list-wrapper-inner
        (when-not @open {:style {:display "none"}})
        [form-list-search open]
        [form-columns open]]])))

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
       [:span.application-handling__basic-list-basic-column-header
        heading]
       (when (= column-id (:column @application-sort))
         (if (= :descending (:order @application-sort))
           [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow]
           [:i.zmdi.zmdi-chevron-up.application-handling__sort-arrow]))])))

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
           [form-list filtered-applications @application-filter]
           [application-list filtered-applications]]]
         [application-review-area filtered-applications]]))))
