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
    (into [:div.editor-form__list]
          (for [application @applications
                :let        [key (:key application)
                             time (t/time->str (:modified-time application))]]
            [:div.editor-form__row
             {:class    (when (= @selected-key key)
                          "editor-form__selected-row")
              :on-click #(dispatch [:application/select-application (:key application)])}
             "Hakemus jätetty " time]))))

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
