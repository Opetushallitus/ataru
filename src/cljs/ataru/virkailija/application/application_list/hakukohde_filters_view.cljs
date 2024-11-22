(ns ataru.virkailija.application.application-list.hakukohde-filters-view
  (:require [ataru.cljs-util :as cljs-util]
            [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]))

(defn- toggle-state-filter!
  [hakukohde-filters states filter-kw filter-id selected?]
  (let [new-filter (if selected?
                     (remove #(= filter-id %) hakukohde-filters)
                     (conj hakukohde-filters filter-id))]
    (cljs-util/update-url-with-query-params
      {filter-kw (string/join ","
                              (cljs-util/get-unselected-review-states
                                new-filter
                                states))})
    (dispatch [:state-update #(assoc-in % [:application filter-kw] new-filter)])
    (dispatch [:application/reload-applications])))

(defn- hakukohde-state-filter-controls-title
  [{:keys [title on-click all-filters-selected?]}]
  [:a.application-handling__basic-list-basic-column-header
   {:on-click on-click}
   title
   [:i.zmdi.zmdi-assignment-check.application-handling__filter-state-link-icon
    {:class (when-not all-filters-selected? "application-handling__filter-state-link-icon--enabled")}]])

(defn hakukohde-state-filter-controls
  []
  (let [filter-opened        (r/atom false)
        toggle-filter-opened #(swap! filter-opened not)
        get-state-count      (fn [counts state-id] (or (get counts state-id) 0))]
    (fn [{:keys [title
                 states
                 state-counts-subs
                 filter-titles]}]
      (let [lang                  @(subscribe [:editor/virkailija-lang])
            has-more?             @(subscribe [:application/has-more-applications?])
            kk? @(subscribe [:virkailija-kevyt-valinta-filter/korkeakouluhaku?])
            all-filters-selected? (->> (keys states)
                                       (map (fn [filter-kw]
                                              [filter-kw @(subscribe [:state-query [:application filter-kw]])]))
                                       (every? (fn [[filter-kw filter-sub]]
                                                 (= (if (and (not kk?)
                                                             (= :kevyt-valinta-vastaanotto-state-filter filter-kw)) ;one less vastaanotto state for non-kk
                                                      (count (filter #(not= "EHDOLLISESTI_VASTAANOTTANUT" %) filter-sub))
                                                      (count filter-sub))
                                                    (-> states filter-kw count)))))
            all-counts-zero?      (->> (keys states)
                                       (every? (fn [filter-kw]
                                                 (let [state-counts-sub (some-> state-counts-subs filter-kw)]
                                                   (or (not state-counts-sub)
                                                       (every? (fn [[review-state-id]]
                                                                 (= 0 (get-state-count state-counts-sub review-state-id)))
                                                               (filter-kw states)))))))]
        [:div.application-handling__filter-state.application-handling__filter-state--application-state
         [hakukohde-state-filter-controls-title
          {:title                 title
           :on-click              toggle-filter-opened
           :all-filters-selected? all-filters-selected?}]
         (when @filter-opened
           [:div.application-handling__filter-state-selection
            (->> (keys states)
                 (filter (fn [filter-kw]
                           (let [state-counts-sub (some-> state-counts-subs filter-kw)]
                             (or (not state-counts-sub)
                                 (some (fn [[review-state-id]]
                                         (or all-counts-zero?
                                             (< 0 (get-state-count state-counts-sub review-state-id))))
                                       (filter-kw states))))))
                 (map (fn [filter-kw]
                        (let [filter-sub                     @(subscribe [:state-query [:application filter-kw]])
                              all-filters-of-state-selected? (= (if (and (not kk?)
                                                                         (= :kevyt-valinta-vastaanotto-state-filter filter-kw)) ;one less vastaanotto state for non-kk
                                                                  (count (filter #(not= "EHDOLLISESTI_VASTAANOTTANUT" %) filter-sub))
                                                                  (count filter-sub))
                                                                (-> states filter-kw count))
                              state-counts-sub               (some-> state-counts-subs filter-kw)]
                          (into ^{:key (str "filter-state-column-" filter-kw)}
                                [:div.application-handling__filter-state-selection-column
                                 (when-let [translation-key (filter-kw filter-titles)]
                                   [:div.application-handling__filter-state-selection-row-header
                                    @(subscribe [:editor/virkailija-translation translation-key])])
                                 [:div.application-handling__filter-state-selection-row.application-handling__filter-state-selection-row--all
                                  {:class (when all-filters-of-state-selected? "application-handling__filter-state-selected-row")}
                                  [:label
                                   [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                            :type      "checkbox"
                                            :checked   all-filters-of-state-selected?
                                            :on-change (fn [_]
                                                         (cljs-util/update-url-with-query-params
                                                           {filter-kw (if all-filters-of-state-selected?
                                                                        (string/join "," (->> states filter-kw (map first)))
                                                                        nil)})
                                                         (dispatch [:state-update #(assoc-in % [:application filter-kw]
                                                                                             (if all-filters-of-state-selected?
                                                                                               []
                                                                                               (->> states filter-kw (map first))))])
                                                         (dispatch [:application/reload-applications]))}]
                                   [:span @(subscribe [:editor/virkailija-translation :all])]]]]
                                (mapv
                                  (fn [[review-state-id review-state-label]]
                                    (let [filter-selected? (contains? (set filter-sub) review-state-id)]
                                      [:div.application-handling__filter-state-selection-row
                                       {:class (if filter-selected? "application-handling__filter-state-selected-row" "")}
                                       [:label
                                        [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                                 :type      "checkbox"
                                                 :checked   filter-selected?
                                                 :on-change #(toggle-state-filter! filter-sub (filter-kw states) filter-kw review-state-id filter-selected?)}]
                                        [:span (str (get review-state-label lang)
                                                    (when state-counts-sub
                                                      (str " ("
                                                           (get-state-count state-counts-sub review-state-id)
                                                           (when has-more? "+")
                                                           ")")))]]]))
                                  (filter-kw states))))))
                 doall)
            [:div.application-handling__filter-state-selection-close-button-container
             [:button.virkailija-close-button.application-handling__filter-state-selection-close-button
              {:on-click #(reset! filter-opened false)}
              [:i.zmdi.zmdi-close]]]])]))))
