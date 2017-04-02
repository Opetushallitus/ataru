(ns ataru.virkailija.application.application-search-control
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [ataru.virkailija.application.application-search-control-handlers]))

(defn tab [tab-id selected-tab click-dispatch-kw label-text]
  [:div.application__search-control-tab-selector-wrapper
   [:div.application__search-control-tab-selector
    {:on-click #(dispatch [click-dispatch-kw])
     :class (when (= tab-id selected-tab) "application__search-control-selected-tab")}
    label-text]
   (when (= tab-id selected-tab)
     [:div.application-handling_search-control-tab-arrow-down])])

(defn tab-row []
  (let [selected-tab (subscribe [:state-query [:application :search-control :show]])]
    [:div.application__search-control-tab-row
     [tab
      :incomplete
      @selected-tab
      :application/show-incomplete-haut-list
      "Käsittelemättä olevat haut"]
     [tab
      :complete
      @selected-tab
      :application/show-complete-haut-list
      "Käsitellyt haut"]]))

(defn haku-info-link [link-href haku-info]
  [:a
   {:href link-href}
   (:name haku-info)
   (str " (" (:application-count haku-info) ")")
   (when (> (:unprocessed haku-info) 0)
     [:span.application__search-control-haku-unprocessed
      (str " " (:unprocessed haku-info) " Käsittelemättä")])])

(defn hakukohde-list [hakukohteet]
  [:div (map
         (fn [hakukohde]
           ^{:key (:oid hakukohde)}
           [:div.application__search-control-hakukohde
            [haku-info-link
             (str "/lomake-editori/applications/hakukohde/" (:oid hakukohde))
             hakukohde]])
         hakukohteet)])

(defn tarjonta-haku [haku]
  (let [hakukohteet-opened (r/atom false)]
    (fn [haku]
      (let [toggle-opened #(reset! hakukohteet-opened (not @hakukohteet-opened))]
        [:div.application__search-control-haku
         [:div.application__search-control-tarjonta-haku-info
          (if @hakukohteet-opened
            [:i.zmdi.zmdi-chevron-up.application__search-control-open-hakukohteet
             {:on-click toggle-opened}]
            [:i.zmdi.zmdi-chevron-down.application__search-control-open-hakukohteet
             {:on-click toggle-opened}])
          [haku-info-link
           (str "/lomake-editori/applications/haku/" (:oid haku))
           haku]]
          (when @hakukohteet-opened [hakukohde-list (:hakukohteet haku)])]))))

(defn direct-form-haku [haku]
  [:div.application__search-control-haku.application__search-control-direct-form-haku
   [haku-info-link
    (str "/lomake-editori/applications/" (:key haku))
    haku]])

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
