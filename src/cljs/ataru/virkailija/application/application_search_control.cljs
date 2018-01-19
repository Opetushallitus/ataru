(ns ataru.virkailija.application.application-search-control
  (:require
   [re-frame.core :refer [subscribe dispatch]]
   [reagent.core :as r]
   [ataru.virkailija.application.application-search-control-handlers]))

(defn haku-tab [tab-id selected-tab link-url label-text]
  [:div.application__search-control-tab-selector-wrapper
   [:a {:href link-url
        :on-click (fn [event]
                    (.preventDefault event)
                    (dispatch [:application/navigate link-url]))}
    [:div.application__search-control-tab-selector
     {:class (when (= tab-id selected-tab) "application__search-control-selected-tab")}
     label-text]]
   (when (= tab-id selected-tab)
     [:div.application-handling_search-control-tab-arrow-down])])

(defn haku-count-str [count]
  (if count
    (str " (" count ")")
    ""))

(defn search-term-field [placeholder-text title-text]
  (let [search-term (subscribe [:state-query [:application :search-control :search-term :value]])]
    [:div
     [:input.application__search-control-search-term-input
      {:type        "text"
       :auto-focus  true
       :id          "ssn-search-field"
       :class       (when (true? @(subscribe [:state-query [:application :search-control :search-term :show-error]]))
                      "application__search-control-search-term-input-error animated shake")
       :placeholder placeholder-text
       :title       title-text
       :value       @search-term
       :on-change   (fn [evt] (dispatch [:application/search-by-term (-> evt .-target .-value)]))}]
     (when-not (clojure.string/blank? @search-term)
       [:span.application__search-control-clear-search-term
        {:on-click #(dispatch [:application/clear-applications-haku-and-form-selections])}
        [:i.zmdi.zmdi-close]])]))

(defn search-term-tab [tab-id selected-tab link-url label-text title-text]
  (let [tab-selected (= tab-id selected-tab)]
    [:div.application__search-control-tab-selector-wrapper
     (if tab-selected
       [:div.application__search-control-tab-selector.application__search-control-selected-tab-with-input
        [search-term-field label-text title-text]]
       [:a {:href link-url
            :on-click (fn [event]
                        (.preventDefault event)
                        (dispatch [:application/navigate link-url]))}
        [:div.application__search-control-tab-selector
         [:span.application__search-control-tab-text
          {:title title-text}
          label-text]]])
     (when tab-selected
       [:div.application-handling_search-control-tab-arrow-down])]))

(defn tab-row []
  (let [selected-tab     (subscribe [:state-query [:application :search-control :show]])
        incomplete-count (subscribe [:application/incomplete-haku-count])
        complete-count   (subscribe [:application/complete-haku-count])]
    [:div.application__search-control-tab-row
     [haku-tab
      :incomplete
      @selected-tab
      "/lomake-editori/applications/incomplete"
      (str "Käsittelemättä olevat haut" (haku-count-str @incomplete-count))]
     [search-term-tab
      :search-term
      @selected-tab
      "/lomake-editori/applications/search"
      "Etsi hakijan henkilötiedoilla"
      "Nimi, henkilötunnus, syntymäaika tai sähköpostiosoite"]
     [haku-tab
      :complete
      @selected-tab
      "/lomake-editori/applications/complete"
      (str "Käsitellyt haut" (haku-count-str @complete-count))]]))

(defn haku-info-link [link-href {:keys [name haku-application-count application-count unprocessed processed]}]
  (let [processing (- application-count unprocessed processed)]
    [:a.application__search-control-haku-link
     {:href link-href}
     [:span.application__search-control-haku-title
      (some #(get name %) [:fi :sv :en])]
     [:span.application__search-control-haku-hl]
     (when haku-application-count
       [:span.application__search-control-haku-count (str haku-application-count " hakemus" (when (< 1 haku-application-count) "ta"))])
     [:span.application-handling__count-tag.application-handling__count-tag--haku-list
      [:span.application-handling__state-label.application-handling__state-label--unprocessed]
      unprocessed]
     [:span.application-handling__count-tag.application-handling__count-tag--haku-list
      [:span.application-handling__state-label.application-handling__state-label--processing]
      processing]
     [:span.application-handling__count-tag.application-handling__count-tag--haku-list
      [:span.application-handling__state-label.application-handling__state-label--processed]
      processed]]))

(defn hakukohde-list [hakukohteet-opened hakukohteet]
  [:div.application__search-control-hakukohde-container
   (if @hakukohteet-opened
     [:div.application__search-control-hakukohteet
      (when (not-empty hakukohteet)
        [:div.application__search-control-hakukohteet-vline])
      [:div.application__search-control-hakukohde-listing
       (map
         (fn [hakukohde]
           ^{:key (:oid hakukohde)}
           [:div.application__search-control-hakukohde
            [haku-info-link
             (str "/lomake-editori/applications/hakukohde/" (:oid hakukohde))
             hakukohde]])
         hakukohteet)]]
     [:div.application__search-control-hakukohteet
      [:div.application__search-control-hakukohde-count
       (str (count hakukohteet) " hakukohdetta")]])])

(defn tarjonta-haku [haku]
  (let [hakukohde-count    (count (:hakukohteet haku))
        hakukohteet-opened (r/atom (= 1 hakukohde-count))]
    (fn [haku]
      [:div.application__search-control-haku
       [:div.application__search-control-tarjonta-haku-info
        [:div.application__search-control-open-hakukohteet-container
         {:on-click #(when (< 1 hakukohde-count)
                       (reset! hakukohteet-opened (not @hakukohteet-opened)))}
         [:i.application__search-control-open-hakukohteet
          {:class (clojure.string/join
                    " "
                    [(if @hakukohteet-opened
                       "application__search-control-open-hakukohteet--up"
                       "application__search-control-open-hakukohteet--down")
                     (when (= 1 hakukohde-count) "application__search-control-open-hakukohteet--disabled")])}]]
        [haku-info-link
         (str "/lomake-editori/applications/haku/" (:oid haku))
         haku]]
       (when (seq (:hakukohteet haku))
         [hakukohde-list hakukohteet-opened (:hakukohteet haku)])])))

(defn direct-form-haku [haku]
  [:div.application__search-control-haku.application__search-control-direct-form-haku
   [haku-info-link
    (str "/lomake-editori/applications/" (:key haku))
    haku]])

(defn loading-indicator []
  [:div.application__search-control-loading-indicator
   [:i.zmdi.zmdi-spinner]])

(defn all-haut-list [haut-subscribe-type]
  (let [haut (subscribe [haut-subscribe-type])]
    (if @haut
      [:div
       (map
        (fn [haku] ^{:key (:oid haku)} [tarjonta-haku haku])
        (:tarjonta-haut @haut))
       (map
        (fn [form-haku] ^{:key (:key form-haku)} [direct-form-haku form-haku])
        (:direct-form-haut @haut))]
      [loading-indicator])))

(defn incomplete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])]
    (when (= :incomplete @show)
      [all-haut-list :application/incomplete-haut])))

(defn complete-haut []
  (let [show (subscribe [:state-query [:application :search-control :show]])]
    (when (= :complete @show)
      [all-haut-list :application/complete-haut])))

(defn application-search-control []
  [:div.application-handling__header-wrapper
   [tab-row]
   [incomplete-haut]
   [complete-haut]])
