(ns ataru.virkailija.application.view
  (:require
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as string]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.ratom :refer-macros [reaction]]
    [reagent.core :as r]
    [cljs-time.format :as f]
    [ataru.virkailija.application.handlers]
    [ataru.virkailija.routes :as routes]
    [ataru.virkailija.temporal :as t]
    [ataru.application.review-states :refer [application-review-states]]
    [ataru.virkailija.views.virkailija-readonly :as readonly-contents]
    [ataru.cljs-util :refer [wrap-scroll-to]]
    [taoensso.timbre :refer-macros [spy debug]]
    [ataru.application-common.koulutus :as koulutus]))

(defn index-of [s val from-index]
  (clojure.string/index-of (clojure.string/lower-case s)
                           (clojure.string/lower-case val)
                           from-index))

(defn- should-search? [search-term]
  (> (count search-term) 1))

(defn match-text [text search-term]
  (if-not (should-search? search-term)
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

(defn toggle-form-list-open! [open]
  (swap! open not)
  (dispatch [:application/clear-search-term]))

(defn form-list-arrow [open]
  [:i.zmdi.application-handling__form-list-arrow
   {:class (if @open "zmdi-chevron-up" "zmdi-chevron-down")}])

(defn form-list-header []
  (let [selected-hakukohde (subscribe [:state-query [:application :selected-hakukohde]])
        selected-form-key  (subscribe [:state-query [:application :selected-form-key]])
        forms              (subscribe [:state-query [:application :forms]])
        selected-haku      (subscribe [:state-query [:application :selected-haku]])]
    (fn []
      [:div.application-handling__form-list-header
       (or (:name (get @forms @selected-form-key))
           (:hakukohde-name @selected-hakukohde)
           (:haku-name @selected-haku)
           "Valitse haku/hakukohde")])))

(defn form-list-column [forms header-text url-fn open]
  (let [search-term (subscribe [:state-query [:application :search-term]])]
    (fn [forms header-text url-fn open]
      (let [forms (cond->> (map (fn [{:keys [name application-count] :as form}]
                                  (let [text (conj (match-text name @search-term)
                                                   {:text (str " (" (or application-count 0) ")") :hilight false})]
                                    (assoc form :text text)))
                                forms)
                    (should-search? @search-term)
                    (filter text-with-hilighted-parts))]
        [:div.application-handling__form-list-column-and-header-container
         [:span.application-handling__form-list-column-header
          (when (and (should-search? @search-term)
                     (empty? forms))
            {:class "application-handling__form-list-column-header--no-results"})
          header-text]
         [:div.application-handling__form-list-column-links-container
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
               (doall))]]))))

(defn hakukohde->form-list-item [{:keys [hakukohde-name] :as hakukohde}]
  (assoc hakukohde :name hakukohde-name))

(defn haku->form-list-item [{:keys [haku-name] :as haku}]
  (assoc haku :name haku-name))

(defn hakukohde-url [{:keys [hakukohde]}]
  (str "/lomake-editori/applications/hakukohde/" hakukohde))

(defn form-url [{:keys [key]}]
  (str "/lomake-editori/applications/" key))

(defn haku-url [{:keys [haku]}]
  (str "/lomake-editori/applications/haku/" haku))

(defn haku-column [open]
  (let [haut (reaction (->> @(subscribe [:state-query [:application :haut]])
                            (map haku->form-list-item)))]
    (fn [open]
      [form-list-column @haut "Haku" haku-url open])))

(defn hakukohde-column [open]
  (let [hakukohteet (reaction (->> @(subscribe [:state-query [:application :hakukohteet]])
                                   (map hakukohde->form-list-item)))]
    (fn [open]
      [form-list-column @hakukohteet "Hakukohde" hakukohde-url open])))

(defn forms-column [open]
  (let [forms (reaction (->> @(subscribe [:state-query [:application :forms]])
                             (reduce-kv (fn [forms _ form]
                                          (conj forms form))
                                        [])))]
    (fn [open]
      [form-list-column @forms "Lomake (ilman hakukohdetta)" form-url open])))

(defn excel-download-link [applications application-filter]
  (let [form-key     (subscribe [:state-query [:application :selected-form-key]])
        hakukohde    (subscribe [:state-query [:application :selected-hakukohde]])
        haku         (subscribe [:state-query [:application :selected-haku]])
        query-string (fn [filters] (str "?state=" (string/join "&state=" (map name filters))))]
    (fn [applications application-filter]
      (when (> (count applications) 0)
        (let [url (cond
                    (some? @form-key)
                    (str "/lomake-editori/api/applications/excel/form/"
                         @form-key
                         (query-string application-filter))

                    (some? @hakukohde)
                    (str "/lomake-editori/api/applications/excel/hakukohde/"
                         (:hakukohde @hakukohde)
                         (query-string application-filter))

                    (some? @haku)
                    (str "/lomake-editori/api/applications/excel/haku/"
                         (:haku @haku)
                         (query-string application-filter)))]
          [:a.application-handling__excel-download-link
           {:href url}
           (str "Lataa hakemukset Excel-muodossa (" (count applications) ")")])))))

(defn form-list-search [open]
  (let [search-term (subscribe [:state-query [:application :search-term]])]
    (fn [open]
      [:div.application-handling__form-list-search-row
       [:input.application-handling__form-list-search-input
        {:type      "text"
         :value     @search-term
         :on-change (fn [event]
                      (let [search-term (.. event -target -value)]
                        (dispatch [:application/search-form-list search-term])))}]
       [:i.application-handling__input-field-clear-button.zmdi.zmdi-close
        (cond-> {:on-click (fn [_]
                             (dispatch [:application/clear-search-term]))}
          (clojure.string/blank? @search-term)
          (assoc :class "application-handling__input-field-clear-button--disabled"))]])))

(defn form-list [filtered-applications application-filter]
  (let [open (r/atom false)]
    (fn [filtered-applications application-filter]
      [:div.application-handling__form-list-wrapper-outer
       [:div.application-handling__header
        [:div.application-handling__header-text-container
         {:on-click #(toggle-form-list-open! open)}
         [form-list-arrow open]
         [form-list-header]]
        [excel-download-link filtered-applications application-filter]]
       [:div.application-handling__form-list-wrapper-inner
        (when-not @open {:style {:display "none"}})
        [form-list-search open]
        [:div.application-handling__form-list-column-wrapper
         [haku-column open]
         [hakukohde-column open]
         [forms-column open]]
        [:i.zmdi.zmdi-close.application-handling__form-list-close-button
         {:on-click #(toggle-form-list-open! open)}]]])))

(defn application-list-row [application selected?]
  (let [time      (t/time->str (:created-time application))
        applicant (:applicant-name application)]
    [:div.application-handling__list-row
     {:on-click #(dispatch [:application/select-application (:key application)])
      :class    (when selected?
                  "application-handling__list-row--selected")}
     [:span.application-handling__list-row--applicant
      (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
     [:span.application-handling__list-row--time time]
     [:span.application-handling__list-row--score
      (or (:score application) "")]
     [:span.application-handling__list-row--state
      (or
       (get application-review-states (:state application))
       "Tuntematon")]]))

(defn application-list-contents [applications]
  (let [selected-key (subscribe [:state-query [:application :selected-key]])
        expanded?    (subscribe [:state-query [:application :form-list-expanded?]])]
    (fn [applications]
      (into [:div.application-handling__list
             {:class (when (= true @expanded?)
                       "application-handling__list--expanded")}]
            (for [application applications
                  :let        [selected? (= @selected-key (:key application))]]
              (if selected?
                [wrap-scroll-to [application-list-row application selected?]]
                [application-list-row application selected?]))))))

(defn icon-check []
  [:img.application-handling__review-state-selected-icon
   {:src "/lomake-editori/images/icon_check.png"}])

(defn toggle-filter [application-filters review-state-id selected all-filters-selected]
  (let [new-application-filter (if selected
                                 (remove #(= review-state-id %) application-filters)
                                 (conj application-filters review-state-id))
        all-filters-selected?  (= (count (keys application-review-states)) (count new-application-filter))]
    (reset! all-filters-selected all-filters-selected?)
    (dispatch [:state-update #(assoc-in % [:application :filter] new-application-filter)])))

(defn- toggle-all-filters [all-filters-selected?]
  (dispatch [:state-update #(assoc-in % [:application :filter]
                                      (if all-filters-selected?
                                        (keys application-review-states)
                                        []))]))

(defn state-filter-controls []
  (let [application-filters    (subscribe [:state-query [:application :filter]])
        review-state-counts    (subscribe [:state-query [:application :review-state-counts]])
        filter-opened          (r/atom false)
        all-filters-selected   (r/atom true)
        toggle-filter-opened   (fn [_] (swap! filter-opened not))
        get-review-state-count (fn [counts state-id] (or (get counts state-id) 0))]
    (fn []
      [:span.application-handling__filter-state
       [:a
        {:on-click toggle-filter-opened}
        (str "Tila"
             (when-not (= (count @application-filters)
                          (count (keys application-review-states)))
               " *"))]
       (when @filter-opened
         (into [:div.application-handling__filter-state-selection
                [:div.application-handling__filter-state-selection-row.application-handling__filter-state-selection-row--all
                 {:class (when @all-filters-selected "application-handling__filter-state-selected-row")}
                 [:label
                  [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                           :type      "checkbox"
                           :checked   @all-filters-selected
                           :on-change #(toggle-all-filters (swap! all-filters-selected not))}]
                  [:span "Kaikki"]]]]
               (mapv
                 (fn [review-state]
                   (let [review-state-id (first review-state)
                         filter-selected (some #{review-state-id} @application-filters)]
                     [:div.application-handling__filter-state-selection-row
                      {:class (if filter-selected "application-handling__filter-state-selected-row" "")}
                      [:label
                       [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                :type      "checkbox"
                                :checked   (boolean filter-selected)
                                :on-change #(toggle-filter @application-filters review-state-id filter-selected all-filters-selected)}]
                       [:span (str (second review-state)
                                   " (" (get-review-state-count @review-state-counts review-state-id) ")")]]]))
                 application-review-states)))
       (when @filter-opened [:div.application-handling__filter-state-selection-arrow-up])])))

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
  (let [answers        (:answers application)
        pref-name      (-> answers :preferred-name :value)
        last-name      (-> answers :last-name :value)
        ssn            (or (-> answers :ssn :value) (-> answers :birth-date :value))
        hakukohde-name (-> application :tarjonta :hakukohde-name)
        koulutus-info  (koulutus/koulutukset->str (-> application :tarjonta :koulutukset))]
    [:div.application__handling-heading
     [:h2.application-handling__review-area-main-heading (str pref-name " " last-name ", " ssn)]
     (when-not (string/blank? hakukohde-name)
       [:div.application-handling__review-area-hakukohde-heading hakukohde-name])
     (when-not (or
                 (= hakukohde-name koulutus-info)
                 (string/blank? koulutus-info))
       [:div.application-handling__review-area-koulutus-heading koulutus-info])]))

(defn close-application []
  [:a {:href     "#"
       :on-click (fn [event] (dispatch [:application/close-application]))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn application-review-area [applications]
  (let [selected-key                  (subscribe [:state-query [:application :selected-key]])
        selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        review-state                  (subscribe [:state-query [:application :review :state]])
        application-filter            (subscribe [:state-query [:application :filter]])
        belongs-to-current-form       (fn [key applications] (first (filter #(= key (:key %)) applications)))
        included-in-filter            (fn [review-state filter] (some #{review-state} filter))
        expanded?                     (subscribe [:state-query [:application :form-list-expanded?]])]
    (fn [applications]
      (when (and (included-in-filter @review-state @application-filter)
                 (belongs-to-current-form @selected-key applications)
                 (not @expanded?))
        [:div.panel-content.application-handling__detail-container
         [close-application]
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
          [:div.panel-content.select_application_list
           [form-list filtered-applications @application-filter]
           [application-list filtered-applications]]]
         [application-review-area filtered-applications]]))))
