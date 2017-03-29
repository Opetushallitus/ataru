(ns ataru.virkailija.application.application-search-control
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [ataru.virkailija.application.application-search-control-handlers]))

(defn tab-row []
  [:div.application__search-control-tab-row
   [:div.application__search-control-tab-selector
    {:on-click #(dispatch [:application/show-incomplete-haut-list])}
    "Käsittelemättä olevat haut"]
   [:div.application__search-control-tab-selector "Käsitellyt haut"]])

(defn incomplete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])]
    (println "show value" @show)
    (when (= :incomplete @show)
      [:div "PLACEHOLDER"])))

(defn application-search-control []
  [:div.application-handling__content-wrapper
   [tab-row]
   [incomplete-haut]])
