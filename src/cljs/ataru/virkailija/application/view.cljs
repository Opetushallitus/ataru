(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
            [ataru.cljs-util :refer [wrap-scroll-to]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn applications []
  (let [applications (subscribe [:state-query [:application :applications]])
        selected-key (subscribe [:state-query [:application :selected]])]
    (into [:div.application-list
           [:div.application-list__header.application-list__row
            [:span.application-list__row--applicant "Hakija"]
            [:span.application-list__row--time "Saapunut"]
            [:span.application-list__row--state "Tila"]]]
          (for [application @applications
                :let        [key       (:key application)
                             time      (t/time->str (:modified-time application))
                             applicant (:applicant application)
                             state     (:state application)]]
            [:div.application-list__row
             {:on-click #(dispatch [:application/select-application (:key application)])
              :class    (when (= @selected-key key)
                          "application-list__row--selected")}
             [:span.application-list__row--applicant
              (or applicant [:span.application-list__row--applicant-unknown "Tuntematon"])]
             [:span.application-list__row--time time]
             [:span.application-list__row--state
              (case (:state application)
                "received" "Saapunut"
                "Tuntematon")]]))))

(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div
     [:h1.application__name
      (:name @form)]
     ; Show when we have "henkilötiedot-moduuli", when we can rely on the applicant's name to be there
     ;[applications]
     ]))

(defn form-list-arrow-up [open?-atom]
  [:i.zmdi.zmdi-chevron-up.application-handling__form-list-arrow
   {:on-click #(reset! open?-atom false)}])

(defn form-list-arrow-down [open?-atom]
  [:i.zmdi.zmdi-chevron-down.application-handling__form-list-arrow
   {:on-click #(reset! open?-atom true)}])

(defn form-list-row [form selected? open?-atom]
  [:a.application-handling__form-list-row-link
    {:href  (str "#/applications/" (:id form))}
   (let [row-element [:div.application-handling__form-list-row
                      {:class (if selected? "application-handling__form-list-selected-row" "")
                       :on-click (if (not selected?)
                                   #(do
                                     (reset! open?-atom false)
                                     (dispatch [:editor/select-form (:id form)]))
                                   #(reset! open?-atom false))}
                      (:name form)]]
     (if selected? [wrap-scroll-to row-element] row-element))])

(defn form-list-opened [forms selected-form-id open?-atom]
  [:div.application-handling__form-list-open-wrapper ;; We need this wrapper to anchor up-arrow to be seen at all scroll-levels of the list
   [form-list-arrow-up open?-atom]
   (into [:div.application-handling__form-list-open]
        (for [[id form] forms
              :let [selected? (= id selected-form-id)]]
          ^{:key id}
          [form-list-row form selected? open?-atom]))])

(defn form-list-closed [selected-form open?-atom]
  [:div.application-handling__form-list-closed
   {:on-click #(reset! open?-atom true)}
   [:div.application-handling__form-list-row.application-handling__form-list-selected-row (:name selected-form)]
   [form-list-arrow-down open?-atom]])

(defn form-list []
  (let [forms            (subscribe [:state-query [:editor :forms]])
        selected-form-id (subscribe [:state-query [:editor :selected-form-id]])
        selected-form    (subscribe [:editor/selected-form])
        open?            (r/atom false)]
    (fn []
      [:div.application-handling__form-list-wrapper
       (if @open?
        [form-list-opened @forms @selected-form-id open?]
        [form-list-closed @selected-form open?])])))

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
      [excel-download-link]]]])
