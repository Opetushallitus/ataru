(ns ataru.virkailija.application.application-search-control
  (:require [ataru.virkailija.application.application-search-control-handlers]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [clojure.string :as string]
            [ataru.virkailija.application.view.virkailija-application-icons :as icons]))

(defn haku-tab [tab-id selected-tab link-url label-text]
  [:div.application__search-control-tab-selector-wrapper
   [:a.application__search-control-tab-selector-link
    {:href     link-url
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
     (when-not (string/blank? @search-term)
       [:a.application__search-control-clear-search-term
        {:on-click #(dispatch [:application/search-by-term ""])}
        [:i.zmdi.zmdi-close]])]))

(defn search-term-tab [selected-tab link-url label-text title-text]
  (let [tab-selected (or (nil? selected-tab)
                         (= :search-term selected-tab))]
    [:div.application__search-control-tab-selector-wrapper.application__search-control-tab-selector-wrapper--search
     (if tab-selected
       [:div.application__search-control-tab-selector.application__search-control-selected-tab-with-input
        [search-term-field label-text title-text]]
       [:a.application__search-control-tab-selector-link
        {:href     link-url
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
      (str @(subscribe [:editor/virkailija-translation :unprocessed-haut]) (haku-count-str @incomplete-count))]
     [search-term-tab
      @selected-tab
      "/lomake-editori/applications/search"
      @(subscribe [:editor/virkailija-translation :search-by-applicant-info])
      @(subscribe [:editor/virkailija-translation :search-terms-list])]
     [haku-tab
      :complete
      @selected-tab
      "/lomake-editori/applications/complete"
      (str @(subscribe [:editor/virkailija-translation :processed-haut]) (haku-count-str @complete-count))]]))

(defn- hakemus-list-link
  [href title {:keys [haku-application-count application-count unprocessed processed]}
   hakukohde? toisen-asteen-yhteishaku? archived?]
  (let [processing (- application-count unprocessed processed)
        total-apps (or haku-application-count application-count)]
    [:a.application__search-control-haku-link
     {:href href}
     [:span.application__search-control-haku-title
      {:class (when hakukohde? "application__search-control-haku-title--hakukohde")}
      (if title
        [:<>
          (when archived?
            [icons/archived-icon])
        title]
        [:i.zmdi.zmdi-spinner.spin])]
     [:span.application__search-control-haku-hl]
     (when (or toisen-asteen-yhteishaku? haku-application-count)
       [:span.application__search-control-haku-count
        (str total-apps
             " "
             (if (< 1 total-apps)
               @(subscribe [:editor/virkailija-translation :applications])
               @(subscribe [:editor/virkailija-translation :application])))])
     (when (not toisen-asteen-yhteishaku?)
       [:span.application-handling__count-tag.application-handling__count-tag--haku-list
        {:data-tooltip @(subscribe [:editor/virkailija-translation :application-count-unprocessed])}
        [:span.application-handling__state-label.application-handling__state-label--unprocessed]
        unprocessed])
     (when (not toisen-asteen-yhteishaku?)
       [:span.application-handling__count-tag.application-handling__count-tag--haku-list
        {:data-tooltip @(subscribe [:editor/virkailija-translation :application-count-processing])}
        [:span.application-handling__state-label.application-handling__state-label--processing]
        processing])
     (when (not toisen-asteen-yhteishaku?)
       [:span.application-handling__count-tag.application-handling__count-tag--haku-list
        {:data-tooltip @(subscribe [:editor/virkailija-translation :application-count-processed])}
        [:span.application-handling__state-label.application-handling__state-label--processed]
        processed])]))

(defn form-info-link
  [{:keys [key name] :as form}]
  [hakemus-list-link
   (str "/lomake-editori/applications/" key)
   (some #(get name %) [:fi :sv :en])
   form
   false
   false
   false])

(defn haku-info-link
  [{:keys [oid] :as haku} toisen-asteen-yhteishaku?]
  [hakemus-list-link
   @(subscribe [:application/path-to-haku-search oid])
   @(subscribe [:application/haku-name oid])
   haku
   false
   toisen-asteen-yhteishaku?
   false])

(defn hakukohde-info-link
  [{:keys [oid] :as hakukohde} toisen-asteen-yhteishaku?]
  [hakemus-list-link
   @(subscribe [:application/path-to-hakukohde-search oid])
   @(subscribe [:application/hakukohde-and-tarjoaja-name oid])
   hakukohde
   true
   toisen-asteen-yhteishaku?
   @(subscribe [:application/hakukohde-archived? oid])])

(defn hakukohde-list [hakukohteet-opened hakukohteet toisen-asteen-yhteishaku?]
  (let [lang @(subscribe [:editor/virkailija-lang])]
    [:div.application__search-control-hakukohde-container
     (if @hakukohteet-opened
       [:div.application__search-control-hakukohteet
        (when (not-empty hakukohteet)
          [:div.application__search-control-hakukohteet-vline])
        [:div.application__search-control-hakukohde-listing
         (->> hakukohteet
              (sort-by (fn [{:keys [oid]}]
                         @(subscribe [:application/hakukohde-name oid])))
              (map (fn [hakukohde]
                     ^{:key (:oid hakukohde)}
                     [:div.application__search-control-hakukohde
                      [:div.application__search-control-haku-hover-highlight]
                      [hakukohde-info-link hakukohde toisen-asteen-yhteishaku?]])))]]
       [:div.application__search-control-hakukohteet
        [:div.application__search-control-hakukohde-count
         (str (count hakukohteet) " " @(subscribe [:editor/virkailija-translation :application-options lang]))]])]))

(defn tarjonta-haku [haku]
  (let [toisen-asteen-yhteishaku? @(subscribe [:application/toisen-asteen-yhteishaku-oid? (:oid haku)])
        hakukohde-count    (count (:hakukohteet haku))
        hakukohteet-opened (r/atom (= 1 hakukohde-count))]
    (fn [haku]
      [:div.application__search-control-haku
       [:div.application__search-control-tarjonta-haku-info
        [:div.application__search-control-open-hakukohteet-container
         {:on-click #(when (< 1 hakukohde-count)
                       (swap! hakukohteet-opened not))}
         [:i.application__search-control-open-hakukohteet
          {:class (string/join
                    " "
                    [(if @hakukohteet-opened
                       "application__search-control-open-hakukohteet--up"
                       "application__search-control-open-hakukohteet--down")
                     (when (= 1 hakukohde-count) "application__search-control-open-hakukohteet--disabled")])}]]
        [haku-info-link haku toisen-asteen-yhteishaku?]]
       (when (seq (:hakukohteet haku))
         [hakukohde-list hakukohteet-opened (:hakukohteet haku) toisen-asteen-yhteishaku?])])))

(defn direct-form-haku [haku]
  [:div.application__search-control-haku.application__search-control-direct-form-haku
   [form-info-link haku]])

(defn loading-indicator []
  [:div.application__search-control-loading-indicator
   [:i.zmdi.zmdi-spinner]])

(defn- show-hakukierros-paattynyt
  []
  [:div.application__search-control-haku.application__search-control-show-hakukierros-paattynyt
   [:button.application__search-control-show-hakukierros-paattynyt-button
    {:on-click #(dispatch [:application/toggle-show-hakukierros-paattynyt])}
    (if @(subscribe [:application/show-hakukierros-paattynyt?])
      @(subscribe [:editor/virkailija-translation :hide-hakukierros-paattynyt])
      @(subscribe [:editor/virkailija-translation :show-hakukierros-paattynyt]))]])

(defn all-haut-list [haut-subscribe-type]
  (let [haut      @(subscribe [haut-subscribe-type])
        fetching? (and (pos? @(subscribe [:application/fetching-haut]))
                       (not @(subscribe [:application/fetching-haut-and-hakukohteet-errored?])))]
    (if fetching?
      [loading-indicator]
      [:div
       (map
        (fn [haku] ^{:key (:oid haku)} [tarjonta-haku haku])
        (:tarjonta-haut haut))
       (map
        (fn [form-haku] ^{:key (:key form-haku)} [direct-form-haku form-haku])
        (:direct-form-haut haut))
       [show-hakukierros-paattynyt]])))

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
