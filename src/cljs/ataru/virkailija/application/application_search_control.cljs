(ns ataru.virkailija.application.application-search-control
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [ataru.virkailija.application.application-search-control-handlers]))

(defn tab-row []
  [:div.application__search-control-tab-row
   [:div.application__search-control-tab-selector
    {:on-click #(dispatch [:application/show-incomplete-haut-list])}
    "Käsittelemättä olevat haut"]
   [:div.application__search-control-tab-selector "Käsitellyt haut"]])

(defn hakukohde-list [hakukohteet]
  [:div (map
         (fn [hakukohde]
           ^{:key (:oid hakukohde)} [:div
            (:name hakukohde)
            (str " (" (:application-count hakukohde) ")")
            (str " " (:unprocessed hakukohde) " Käsittelemättä")])
         hakukohteet)])

(defn tarjonta-haku [haku]
  (let [hakukohteet-opened (r/atom false)]
    (fn [haku]
      (let [toggle-opened #(reset! hakukohteet-opened (not @hakukohteet-opened))]
         [:div.application__search-control-haku
         (if @hakukohteet-opened
            [:i.zmdi.zmdi-chevron-up.application__search-control-open-hakukohteet
             {:on-click toggle-opened}]
            [:i.zmdi.zmdi-chevron-down.application__search-control-open-hakukohteet
             {:on-click toggle-opened}])
         [:a {:href (str "/lomake-editori/applications/haku/" (:oid haku))}
          " "
          (:name haku)
          (str " (" (:application-count haku) ")")
          (str " " (:unprocessed haku) " Käsittelemättä")]
          (when @hakukohteet-opened [hakukohde-list (:hakukohteet haku)])
          ]))))

(defn incomplete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])
        haut (subscribe [:state-query [:application :haut2]])]
    (when (= :incomplete @show)
      [:div
       (map (fn [haku] ^{:key (:oid haku)} [tarjonta-haku haku]) (:tarjonta-haut @haut))])))

(defn application-search-control []
  [:div.application-handling__content-wrapper
   [tab-row]
   [incomplete-haut]])
