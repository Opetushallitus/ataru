(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs-time.format :as f]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
            [ataru.application-common.application-readonly :as readonly-contents]
            [ataru.cljs-util :refer [wrap-scroll-to]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn toggle-form-list-open! [open]
  (swap! open not)
  nil) ;; Returns nil so that React doesn't whine about event handlers returning false


(defn form-list-arrow-up [open]
  [:i.zmdi.zmdi-chevron-up.application-handling__form-list-arrow
   {:on-click #(toggle-form-list-open! open)}])

(defn form-list-row [form selected? open]
  [:a.application-handling__form-list-row-link
    {:href  (str "#/applications/" (:id form))}
   (let [row-element [:div.application-handling__form-list-row
                      {:class (if selected? "application-handling__form-list-selected-row" "")
                       :on-click (if (not selected?)
                                   #(toggle-form-list-open! open)
                                   #(toggle-form-list-open! open))}
                      (:name form)]]
     (if selected? [wrap-scroll-to row-element] row-element))])

(defn form-list-opened [forms selected-form-key open]
  [:div.application-handling__form-list-open-wrapper ;; We need this wrapper to anchor up-arrow to be seen at all scroll-levels of the list
   [form-list-arrow-up open]
   (into [:div.application-handling__form-list-open]
        (for [[id form] forms
              :let [selected? (= id selected-form-key)]]
          ^{:key id}
          [form-list-row form selected? open]))])

(defn form-list-closed [selected-form open]
  [:div.application-handling__form-list-closed
   {:on-click #(toggle-form-list-open! open)}
   [:div.application-handling__form-list-row.application-handling__form-list-selected-row (:name selected-form)]
   [:i.zmdi.zmdi-chevron-down.application-handling__form-list-arrow]])

(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-key (subscribe [:state-query [:editor :selected-form-key]])
        selected-form    (subscribe [:editor/selected-form])
        open             (r/atom false)]
    (fn []
      [:div.application-handling__form-list-wrapper
       (if @open
        [form-list-opened @forms @selected-form-key open]
        [form-list-closed @selected-form open])])))

(defn excel-download-link [applications]
  (let [form-key (reaction (:key @(subscribe [:editor/selected-form])))]
    (fn [applications]
      (when (> (count applications) 0)
        [:a.application-handling__excel-download-link
         {:href (str "/lomake-editori/api/applications/excel/" @form-key)}
         (str "Lataa hakemukset Excel-muodossa (" (count applications) ")")]))))

(defn application-list-contents [applications]
  (let [selected-id (subscribe [:state-query [:application :selected-id]])]
    (fn [applications]
      (into [:div.application-handling__list]
        (for [application applications
              :let        [id       (:id application)
                           time      (t/time->str (:modified-time application))
                           applicant (:applicant-name application)]]
          [:div.application-handling__list-row
           {:on-click #(dispatch [:application/select-application (:id application)])
            :class    (when (= @selected-id id)
                        "application-handling__list-row--selected")}
           [:span.application-handling__list-row--applicant
            (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
           [:span.application-handling__list-row--time time]
           [:span.application-handling__list-row--state
            (case (:state application)
              "received" "Saapunut"
              "Tuntematon")]])))))

(defn application-list [applications]
  [:div
   [:div.application-handling__list-header.application-handling__list-row
    [:span.application-handling__list-row--applicant "Hakija"]
    [:span.application-handling__list-row--time "Saapunut"]
    [:span.application-handling__list-row--state "Tila"]]
   [application-list-contents applications]])

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn event-row [event]
  (let [time-str     (t/time->short-str (:time event))
        to-event-row (fn [caption] [:div [:span.application-handling__event-timestamp time-str] caption])]
    (case (:event-type event)
      "received" (to-event-row "Hakemus saapunut")
      "Tuntematon")))

(defn application-review-events []
  (let [events (subscribe [:state-query [:application :events]])]
    (fn []
      (into
        [:div.application-handling__event-list
         [:div.application-handling__review-header "Tapahtumat"]]
        (mapv #(event-row %) @events)))))

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
   [application-review-notes]
   [application-review-events]])

(defn application-heading [application]
  (let [answers (:answers application)
        pref-name (-> answers :preferred-name :value)
        last-name (-> answers :last-name :value)
        ssn       (or (-> answers :ssn :value) (-> answers :birth-date :value))]
    [:h2.application-handling__review-area-main-heading (str pref-name " " last-name ", " ssn)]))

(defn application-review-area [applications]
  (let [selected-id                   (subscribe [:state-query [:application :selected-id]])
        selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        belongs-to-current-form       (fn [id applications] (first (filter #(= id (:id %)) applications)))]
    (fn [applications]
      (when (belongs-to-current-form @selected-id applications)
        [:div.application-handling__container.panel-content
         [application-heading (:application @selected-application-and-form)]
         [:div.application-handling__review-area
          [application-contents @selected-application-and-form]
          [application-review]]]))))

(defn application []
  (let [applications (subscribe [:state-query [:application :applications]])]
    (fn []
      [:div
       [:div.application-handling__overview
        [:div.application-handling__container.panel-content
          [:div.application-handling__header
            [form-list]
            [excel-download-link @applications]]
          [application-list @applications]]]
       [application-review-area @applications]])))
