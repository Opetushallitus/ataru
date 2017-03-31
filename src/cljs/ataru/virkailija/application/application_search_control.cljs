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
   [:div.application__search-control-tab-selector
    {:on-click #(dispatch [:application/show-complete-haut-list])}
    "Käsitellyt haut"]])

(defn hakukohde-list [hakukohteet]
  [:div (map
         (fn [hakukohde]
           ^{:key (:oid hakukohde)}
           [:div
            [:a
             {:href (str "/lomake-editori/applications/hakukohde/" (:oid hakukohde))}
             (:name hakukohde)
            (str " (" (:application-count hakukohde) ")")
            (str " " (:unprocessed hakukohde) " Käsittelemättä")]])
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
          (when @hakukohteet-opened [hakukohde-list (:hakukohteet haku)])]))))

(defn direct-form-haku [haku]
  [:div.application__search-control-haku
   [:a {:href (str "/lomake-editori/applications/" (:key haku))}
    " "
    (:name haku)
    (str " (" (:application-count haku) ")")
    (str " " (:unprocessed haku) " Käsittelemättä")]])

(defn all-haut-list [haut-subscribe-type]
  (let [haut (subscribe [haut-subscribe-type])]
    [:div
     (map
      (fn [haku] ^{:key (:oid haku)} [tarjonta-haku haku])
      (:tarjonta-haut @haut))
     (map
      (fn [form-haku] ^{:key (:key form-haku)} [direct-form-haku form-haku])
      (:direct-form-haut @haut))]))

(defn incomplete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])]
    (when (= :incomplete @show)
      [all-haut-list :application/incomplete-haut])))

(defn complete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])]
    (when (= :complete @show)
      [all-haut-list :application/complete-haut])))

(defn application-search-control []
  [:div.application-handling__content-wrapper
   [tab-row]
   [incomplete-haut]
   [complete-haut]])
