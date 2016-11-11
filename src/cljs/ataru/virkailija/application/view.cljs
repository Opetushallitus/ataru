(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs-time.format :as f]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
            [ataru.virkailija.application.review-states :refer [application-review-states]]
            [ataru.application-common.application-readonly :as readonly-contents]
            [ataru.cljs-util :refer [wrap-scroll-to]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn toggle-form-list-open! [open]
  (swap! open not)
  nil) ;; Returns nil so that React doesn't whine about event handlers returning false


(defn form-list-arrow-up [open]
  [:i.zmdi.zmdi-chevron-up.application-handling__form-list-arrow
   {:on-click #(toggle-form-list-open! open)}])

(defn form-list-row [name href selected? open]
  [:a.application-handling__form-list-row-link
   {:href href}
   (let [row-element [:div.application-handling__form-list-row
                      {:class    (if selected? "application-handling__form-list-selected-row" "")
                       :on-click #(toggle-form-list-open! open)}
                      name]]
     (if selected? [wrap-scroll-to row-element] row-element))])

(defn form-list-opened [forms hakukohteet selected-form-key selected-hakukohde open]
  (let [form-rows      (for [[id form] forms
                             :let [selected? (= id selected-form-key)]]
                         ^{:key id}
                         [form-list-row (str "Lomake: " (:name form)) (str "/lomake-editori/applications/" (:key form)) selected? open])
        hakukohde-rows (for [{:keys [hakukohde hakukohde-name]} hakukohteet
                             :let [selected? (= hakukohde (:hakukohde selected-hakukohde))]]
                         ^{:key hakukohde}
                         [form-list-row (str "Hakukohde: " hakukohde-name) (str "/lomake-editori/applications/hakukohde/" hakukohde) selected? open])]
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

(defn excel-download-link [applications]
  (let [form-key (reaction (:key @(subscribe [:editor/selected-form])))]
    (fn [applications]
      (when (> (count applications) 0)
        [:a.application-handling__excel-download-link
         {:href (str "/lomake-editori/api/applications/excel/" @form-key)}
         (str "Lataa hakemukset Excel-muodossa (" (count applications) ")")]))))

(defn application-list-contents [applications]
  (let [selected-key (subscribe [:state-query [:application :selected-key]])]
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
           [:span.application-handling__list-row--state
            (or
             (get application-review-states (:state application))
             "Tuntematon")]])))))

(defn icon-check []
  [:img.application-handling__review-state-selected-icon
   {:src "/lomake-editori/images/icon_check.png"}])

(defn state-filter-controls []
  (let [application-filter   (subscribe [:state-query [:application :filter]])
        filter-opened        (r/atom false)
        toggle-filter-opened (fn [evt] (reset! filter-opened (not @filter-opened)) nil)
        toggle-filter        (fn [review-state-id selected]
                               (let [new-application-filter (if selected
                                                              (remove #(= review-state-id %) @application-filter)
                                                              (conj @application-filter review-state-id))]
                                 (dispatch [:state-update (fn [db _] (assoc-in db [:application :filter] new-application-filter))])))]
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
                        filter-selected (some #{review-state-id} @application-filter) ]
                    [:div.application-handling__filter-state-selection-row
                     {:class    (if filter-selected "application-handling__filter-state-selected-row" "")
                      :on-click #(toggle-filter review-state-id filter-selected)}
                     (if filter-selected [icon-check] nil)
                     (second review-state)]))
                application-review-states)))
       [:div.application-handling__filter-state-selection-arrow-down]])))

(defn application-list [applications]
  [:div
   [:div.application-handling__list-header.application-handling__list-row
    [:span.application-handling__list-row--applicant "Hakija"]
    [:span.application-handling__list-row--time "Saapunut"]
    [:span.application-handling__list-row--state [state-filter-controls]]]
   [application-list-contents applications]])

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn review-state-row [current-review-state review-state]
  (let [review-state-id (first review-state)
        review-state-label (second review-state)]
    (if (= current-review-state review-state-id)
      [:div.application-handling__review-state-row.application-handling__review-state-selected-row
       [icon-check]
       review-state-label]
      [:div.application-handling__review-state-row
       {:on-click #(dispatch [:application/update-review-state review-state-id])}
       review-state-label])))

(defn application-review-state []
  (let [review-state (subscribe [:state-query [:application :review :state]])]
    (fn []
      (into
       [:div.application-handling__review-state-container
        [:div.application-handling__review-header "Tila"]]
       (mapv (partial review-state-row @review-state) application-review-states)))))

(defn event-row [event]
  (let [time-str     (t/time->short-str (:time event))
        to-event-row (fn [caption] [:div
                                    [:span.application-handling__event-timestamp time-str]
                                    [:span.application-handling__event-caption caption]])
        event-type   (:event-type event)
        event-caption (if (= "review-state-change" event-type)
                        (get application-review-states (:new-review-state event))
                        "Tuntematon")]
    (to-event-row event-caption)))

(defn application-review-events []
  (let [events (subscribe [:state-query [:application :events]])]
    (fn []
      (into
        [:div.application-handling__event-list
         [:div.application-handling__review-header "Tapahtumat"]]
        (mapv event-row @events)))))

(defn application-review-notes []
  (let [review (subscribe [:state-query [:application :review]])
        ; React doesn't like null, it leaves the previous value there, hence:
        review->notes-str (fn [review] (if-let [notes (:notes @review)] notes ""))]
    (fn []
      [:div
       [:div.application-handling__review-header "Muistiinpanot"]
       [:textarea.application-handling__review-notes
        {:value (review->notes-str review)
         :on-change (fn [evt]
                      (let [new-value (-> evt .-target .-value)]
                        (dispatch [:state-update (fn [db _] (update-in db [:application :review] assoc :notes new-value))])))}]])))

(defn application-review []
  [:div.application-handling__review
   [application-review-state]
   [application-review-notes]
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
        belongs-to-current-form       (fn [key applications] (first (filter #(= key (:key %)) applications)))]
    (fn [applications]
      (when (belongs-to-current-form @selected-key applications)
        [:div.panel-content
         [application-heading (:application @selected-application-and-form)]
         [:div.application-handling__review-area
          [application-contents @selected-application-and-form]
          [application-review]]]))))

(defn application []
  (let [applications (subscribe [:state-query [:application :applications]])]
    (fn []
      [:div
       [:div.application-handling__overview
        [:div.panel-content
          [:div.application-handling__header
            [form-list]
            [excel-download-link @applications]]
          [application-list @applications]]]
       [application-review-area @applications]])))
