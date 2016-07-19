(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
            [ataru.cljs-util :refer [wrap-scroll-to]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn toggle-form-list-open [open]
  (reset! open (not @open))
  nil) ;; Returns nil so that React doesn't whine about event handlers returning false

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])
        selected-key (subscribe [:state-query [:application :selected]])]
    (into [:div.application-handling__list
           [:div.application-handling__list-header.application-handling__list-row
            [:span.application-handling__list-row--applicant "Hakija"]
            [:span.application-handling__list-row--time "Saapunut"]
            [:span.application-handling__list-row--state "Tila"]]]
          (for [application @applications
                :let        [key       (:key application)
                             time      (t/time->str (:modified-time application))
                             applicant (:applicant application)
                             state     (:state application)]]
            [:div.application-handling__list-row
             {:on-click #(dispatch [:application/select-application (:key application)])
              :class    (when (= @selected-key key)
                          "application-handling__list-row--selected")}
             [:span.application-handling__list-row--applicant
              (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
             [:span.application-handling__list-row--time time]
             [:span.application-handling__list-row--state
              (case (:state application)
                "received" "Saapunut"
                "Tuntematon")]]))))

(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div
     [:h1.application__name
      (:name @form)]
     [applications]]))


(defn form-list-arrow-up [open]
  [:i.zmdi.zmdi-chevron-up.application-handling__form-list-arrow
   {:on-click #(toggle-form-list-open open)}])

(defn form-list-row [form selected? open]
  [:a.application-handling__form-list-row-link
    {:href  (str "#/applications/" (:id form))}
   (let [row-element [:div.application-handling__form-list-row
                      {:class (if selected? "application-handling__form-list-selected-row" "")
                       :on-click (if (not selected?)
                                   #(do
                                     (toggle-form-list-open open)
                                     (dispatch [:editor/select-form (:id form)]))
                                   #(toggle-form-list-open open))}
                      (:name form)]]
     (if selected? [wrap-scroll-to row-element] row-element))])

(defn form-list-opened [forms selected-form-id open]
  [:div.application-handling__form-list-open-wrapper ;; We need this wrapper to anchor up-arrow to be seen at all scroll-levels of the list
   [form-list-arrow-up open]
   (into [:div.application-handling__form-list-open]
        (for [[id form] forms
              :let [selected? (= id selected-form-id)]]
          ^{:key id}
          [form-list-row form selected? open]))])

(defn form-list-closed [selected-form open]
  [:div.application-handling__form-list-closed
   {:on-click #(toggle-form-list-open open)}
   [:div.application-handling__form-list-row.application-handling__form-list-selected-row (:name selected-form)]
   [:i.zmdi.zmdi-chevron-down.application-handling__form-list-arrow]])

(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])
        selected-form    (subscribe [:editor/selected-form])
        open             (r/atom false)]
    (fn []
      [:div.application-handling__form-list-wrapper
       (if @open
        [form-list-opened @forms @selected-form-id open]
        [form-list-closed @selected-form open])])))

(defn excel-download-link []
  (let [application-count (subscribe [:state-query [:application :count]])]
    (fn []
      (when (> @application-count 0)
        [:a.application-handling__excel-download-link
         {:href
          (str
            "/lomake-editori/api/applications/"
            @(subscribe [:state-query [:application :form :id]])
            "/excel")}
         (str "Lataa hakemukset Excel-muodossa (" @application-count ")")]))))

(defn application []
  [:div
   [:div.application-handling__container.panel-content
    [:div.application-handling__header
      [form-list]
      [excel-download-link]]
    [application-list]]])
