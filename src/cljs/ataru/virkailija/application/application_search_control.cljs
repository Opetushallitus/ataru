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

(defn tarjonta-haku [haku]
  [:div
   [:a {:href (str "/lomake-editori/applications/haku/" (:oid haku))}
    (:name haku)
    (str " (" (:application-count haku) ")")
    (str " " (:unprocessed haku) " Käsittelemättä")]])

(defn incomplete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])
        haut (subscribe [:state-query [:application :haut2]])]
    (println "haut value" @haut)
    (when (= :incomplete @show)
      [:div
       (map tarjonta-haku (:tarjonta-haut @haut))])))

(defn application-search-control []
  [:div.application-handling__content-wrapper
   [tab-row]
   [incomplete-haut]])
