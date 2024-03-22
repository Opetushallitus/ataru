(ns ataru.virkailija.views.hakukohde-and-hakukohderyhma-search
  (:require [ataru.util :as util]
            [ataru.virkailija.application.view.virkailija-application-icons :as icons]
            [ataru.virkailija.editor.editor-selectors :refer [get-virkailija-lang]]
            [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as r]))

(defn- search-input->search-terms
  [search-input]
  (string/split search-input #"\s+"))

(defn- hakukohderyhma->name
  [lang hakukohderyhma]
  (util/non-blank-val (:name hakukohderyhma)
                      [lang :fi :sv :en]))

(defn- hakukohde->hakukohde-name
  [lang hakukohde]
  (let [hakukohde-name (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
        tarjoaja-name  (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]
    (str hakukohde-name " - " tarjoaja-name)))

(defn- hakukohderyhma->hakukohderyhma-hit
  [lang search-terms hakukohderyhma]
  (let [parts (util/match-text (hakukohderyhma->name lang hakukohderyhma)
                               search-terms
                               true)]
    {:id          (:oid hakukohderyhma)
     :label-parts parts
     :match?      (or (not-empty (rest parts))
                      (:hilight (first parts)))}))

(defn- hakukohde->hakukohde-hit
  [lang search-terms hakukohde]
  (let [parts (util/match-text (hakukohde->hakukohde-name lang hakukohde)
                               search-terms
                               true)]
    {:id          (:oid hakukohde)
     :label-parts parts
     :match?      (or (not-empty (rest parts))
                      (:hilight (first parts)))}))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/search-input
  (fn [db [_ id]]
    (get-in db [:hakukohde-and-hakukohderyhma id :search-input] "")))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/hakukohderyhma-hits
  (fn [db [_ id hakukohderyhmat]]
    (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
          search-terms (search-input->search-terms
                        (get-in db [:hakukohde-and-hakukohderyhma id :search-input] ""))]
      (if (some util/should-search? search-terms)
        (get-in db [:hakukohde-and-hakukohderyhma id :hakukohderyhma-hits])
        (map (partial hakukohderyhma->hakukohderyhma-hit
                      lang
                      search-terms)
             hakukohderyhmat)))))

(re-frame/reg-sub
  :hakukohde-and-hakukohderyhma/hakukohde-hits
  (fn [db [_ id haku]]
    (let [lang         @(re-frame/subscribe [:editor/virkailija-lang])
          search-terms (search-input->search-terms
                        (get-in db [:hakukohde-and-hakukohderyhma id :search-input] ""))]
      (if (some util/should-search? search-terms)
        (get-in db [:hakukohde-and-hakukohderyhma id :hakukohde-hits (:oid haku)])
        (map (partial hakukohde->hakukohde-hit
                      lang
                      search-terms)
             (:hakukohteet haku))))))

(re-frame/reg-event-db
  :hakukohde-and-hakukohderyhma/set-search-input
  (fn [db [_ id haut hakukohderyhmat hakukohde-selected? hakukohderyhma-selected? search-input]]
    (let [lang         (get-virkailija-lang db)
          search-terms (search-input->search-terms search-input)
          did-search?  (some util/should-search? search-terms)]
      (-> db
          (assoc-in [:hakukohde-and-hakukohderyhma id :search-input] search-input)
          (assoc-in [:hakukohde-and-hakukohderyhma id :hakukohderyhma-hits]
                    (->> hakukohderyhmat
                         (map (partial hakukohderyhma->hakukohderyhma-hit
                                       lang
                                       search-terms))
                         (filter #(or (hakukohderyhma-selected? (:id %))
                                      (and did-search? (:match? %))))))
          (assoc-in [:hakukohde-and-hakukohderyhma id :hakukohde-hits]
                    (reduce (fn [hits haku]
                              (assoc hits (:oid haku)
                                     (->> (:hakukohteet haku)
                                          (map (partial hakukohde->hakukohde-hit
                                                        lang
                                                        search-terms))
                                          (filter #(or (hakukohde-selected? (:id %))
                                                       (and did-search? (:match? %)))))))
                            {}
                            haut))))))

(defn- list-item
  [selected? on-select on-unselect {:keys [id label-parts]}]
  ^{:key (str "list-item-" id)}
  [:li.hakukohde-and-hakukohderyhma-category-list-item
   {:class    (when (selected? id)
                "hakukohde-and-hakukohderyhma-category-list-item--selected")
    :on-click (if (selected? id) #(on-unselect id) #(on-select id))}
   (map-indexed (fn [i {:keys [text hilight]}]
                  ^{:key (str i)}
                  [:span.hakukohde-and-hakukohderyhma-list-item-label
                   {:class
                    (str (when (selected? id)
                           "hakukohde-and-hakukohderyhma-list-item-label--selected")
                         (when hilight
                           " hakukohde-and-hakukohderyhma-list-item-label--highlighted"))}
                   (when @(re-frame/subscribe [:application/hakukohde-archived? id])
                     [icons/archived-icon])
                   text])
                label-parts)])

(defn- category-listing
  [_category-name _items _selected? _on-select _on-unselect]
  (let [show-n (r/atom 10)]
    (fn [category-name items selected? on-select on-unselect]
      [:li.hakukohde-and-hakukohderyhma-category-listing
       [:span.hakukohde-and-hakukohderyhma-category-name
        category-name]
       [:ul.hakukohde-and-hakukohderyhma-category-list
        (->> items
             (take @show-n)
             (map (partial list-item selected? on-select on-unselect))
             doall)
        (when (< @show-n (count items))
          [:li.hakukohde-and-hakukohderyhma-category-list-item.hakukohde-and-hakukohderyhma-category-list-item--show-more
           {:on-click #(swap! show-n + 10)}
           [:span.hakukohde-and-hakukohderyhma-show-more
            @(re-frame/subscribe [:editor/virkailija-translation :show-more])]])]])))

(defn search-input
  [{:keys [id
           haut
           hakukohderyhmat
           hakukohde-selected?
           hakukohderyhma-selected?
           only-hakukohteet?]}]
  (let [placeholder-translation-key (if only-hakukohteet?
                                      :search-hakukohde-placeholder
                                      :search-hakukohde-and-hakukohderyhma-placeholder)]
    [:input.hakukohde-and-hakukohderyhma-search-input
     {:value       @(re-frame/subscribe
                      [:hakukohde-and-hakukohderyhma/search-input id])
      :on-change   #(re-frame/dispatch
                      [:hakukohde-and-hakukohderyhma/set-search-input
                       id
                       haut
                       hakukohderyhmat
                       hakukohde-selected?
                       hakukohderyhma-selected?
                       (.-value (.-target %))])
      :placeholder @(re-frame/subscribe [:editor/virkailija-translation placeholder-translation-key])}]))

(defn visibility-checkbox
  [id path is-option?]
  [:div.hakukohde-and-hakukohderyhma-visibility-checkbox
   [:input
    {:id        id
     :type      "checkbox"
     :checked   (boolean
                  (if is-option?
                    @(re-frame/subscribe [:editor/get-component-value path :hidden])
                    @(re-frame/subscribe [:editor/get-component-value path :params :hidden])))
     :disabled  @(re-frame/subscribe [:editor/component-locked? path])
     :on-change #(if is-option?
                   (re-frame/dispatch [:editor/toggle-option-visibility-on-form path])
                   (re-frame/dispatch [:editor/toggle-element-visibility-on-form path]))}]
   [:label
    {:for id}
    @(re-frame/subscribe [:editor/virkailija-translation :is-hidden?])]])

(defn search-listing
  [{:keys [id
           haut
           hakukohderyhmat
           hakukohde-selected?
           hakukohderyhma-selected?
           on-hakukohde-select
           on-hakukohde-unselect
           on-hakukohderyhma-select
           on-hakukohderyhma-unselect
           only-hakukohteet?]}]
  (let [lang                      @(re-frame/subscribe [:editor/virkailija-lang])]
    [:ul.hakukohde-and-hakukohderyhma-search-listing
     (when-let [hits (seq @(re-frame/subscribe
                            [:hakukohde-and-hakukohderyhma/hakukohderyhma-hits
                             id
                             hakukohderyhmat]))]
       (when (not only-hakukohteet?)
         [category-listing
          @(re-frame/subscribe [:editor/virkailija-translation :hakukohderyhmat])
          hits
          hakukohderyhma-selected?
          on-hakukohderyhma-select
          on-hakukohderyhma-unselect]))
     (doall
      (for [haku  haut
            :let  [hits @(re-frame/subscribe
                          [:hakukohde-and-hakukohderyhma/hakukohde-hits
                           id
                           haku])]
            :when (seq hits)]
        ^{:key (str "category-" (:oid haku))}
        [category-listing
         (util/non-blank-val (:name haku) [lang :fi :sv :en])
         hits
         hakukohde-selected?
         on-hakukohde-select
         on-hakukohde-unselect]))]))

(defn popup
  [header-component visibility-component content-component on-close]
  [:div.hakukohde-and-hakukohderyhma-search-popup
   [:div.hakukohde-and-hakukohderyhma-search-popup-header
    header-component
    [:button.virkailija-close-button
     {:on-click on-close}
     [:i.zmdi.zmdi-close]]]
   visibility-component
   [:div.hakukohde-and-hakukohderyhma-search-popup-content
    content-component]])
