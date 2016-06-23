(ns ataru.virkailija.application.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [ataru.virkailija.temporal :as t]
            [ataru.virkailija.application.handlers]
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
             [:span.application-list__row--state "Saapunut"]]))))

(defn selected-application []
  [:div.panel-content
   [:h2 "Valitse hakemus listalta"]
   [:p [:a {:href "#"
            :on-click #()}
        "Lataa Excel -tiedostona"]]])

(defn application-list []
  (let [form (subscribe [:state-query [:application :form]])]
    [:div
     [:h1.application__name
      (:name @form)]
     [applications]]))

(defn application []
  [:div
   [:div.editor-form__container.panel-content
    [application-list]
    [:p [:a {:href     "#"
             :on-click #()}
         "Lataa kaikki hakemukset Excel -tiedostona"]]]
   [selected-application]])
