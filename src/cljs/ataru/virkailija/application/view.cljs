(ns ataru.virkailija.application.view
  (:require
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as string]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.ratom :refer-macros [reaction]]
    [reagent.core :as r]
    [cljs-time.format :as f]
    [taoensso.timbre :refer-macros [spy debug]]
    [ataru.virkailija.application.handlers]
    [ataru.virkailija.application.application-subs]
    [ataru.virkailija.routes :as routes]
    [ataru.virkailija.temporal :as t]
    [ataru.application.review-states :as application-review-states]
    [ataru.virkailija.views.virkailija-readonly :as readonly-contents]
    [ataru.cljs-util :as util]
    [ataru.virkailija.application.application-search-control :refer [application-search-control]]
    [goog.string.format]
    [ataru.application.review-states :as review-states]))

(defn excel-download-link [applications application-filter]
  (let [form-key     (subscribe [:state-query [:application :selected-form-key]])
        hakukohde    (subscribe [:state-query [:application :selected-hakukohde]])
        haku         (subscribe [:state-query [:application :selected-haku]])
        query-string (fn [filters] (str "?state=" (string/join "&state=" (map name filters))))]
    (fn [applications application-filter]
      (when (> (count applications) 0)
        (let [url (cond
                    (some? @form-key)
                    (str "/lomake-editori/api/applications/excel/form/"
                         @form-key
                         (query-string application-filter))

                    (some? @hakukohde)
                    (str "/lomake-editori/api/applications/excel/hakukohde/"
                         (:oid @hakukohde)
                         (query-string application-filter))

                    (some? @haku)
                    (str "/lomake-editori/api/applications/excel/haku/"
                         (:oid @haku)
                         (query-string application-filter)))]
          [:a.application-handling__excel-download-link
           {:href url}
           (str "Lataa hakemukset Excel-muodossa (" (count applications) ")")])))))

(defn haku-header []
  (let [header (subscribe [:application/list-heading])]
    [:div.application-handling__header-haku-name
     @header]))

(defn haku-heading [filtered-applications application-filter]
  (let [belongs-to-haku (subscribe [:application/application-list-belongs-to-haku?])]
    [:div.application-handling__header
     [haku-header]
     (when @belongs-to-haku
       [excel-download-link filtered-applications application-filter])]))

(defn- select-application
  [application-key]
  (util/update-url-with-query-params {:application-key application-key})
  (dispatch [:application/select-application application-key]))

(defn- get-review-state-label-by-name
  [states name]
  (->> states (filter #(= (first %) name)) first second))

(defn application-list-row [application selected?]
  (let [time      (t/time->str (:created-time application))
        applicant (str (:preferred-name application) " " (:last-name application))]
    [:div.application-handling__list-row
     {:on-click #(select-application (:key application))
      :class    (when selected?
                  "application-handling__list-row--selected")}
     [:span.application-handling__list-row--applicant
      (or applicant [:span.application-handling__list-row--applicant-unknown "Tuntematon"])]
     [:span.application-handling__list-row--time time]
     [:span.application-handling__list-row--score
      (or (:score application) "")]
     [:span.application-handling__list-row--state
      (or
        (get-review-state-label-by-name application-review-states/application-review-states (:state application))
        "Tuntematon")]]))

(defn application-list-contents [applications]
  (let [selected-key (subscribe [:state-query [:application :selected-key]])
        expanded?    (subscribe [:state-query [:application :application-list-expanded?]])]
    (fn [applications]
      (into [:div.application-handling__list
             {:class (str (when (= true @expanded?) "application-handling__list--expanded")
                          (when (> (count applications) 0) " animated fadeIn"))}]
            (for [application applications
                  :let        [selected? (= @selected-key (:key application))]]
              (if selected?
                [util/wrap-scroll-to [application-list-row application selected?]]
                [application-list-row application selected?]))))))

(defn icon-check []
  [:img.application-handling__review-state-selected-icon
   {:src "/lomake-editori/images/icon_check.png"}])

(defn toggle-filter [application-filters review-state-id selected]
  (let [new-application-filter (if selected
                                 (remove #(= review-state-id %) application-filters)
                                 (conj application-filters review-state-id))
        all-filters-selected?  (= (count (first application-review-states/application-review-states)) (count new-application-filter))]
    (util/update-url-with-query-params
     {:unselected-states (clojure.string/join "," (util/get-unselected-review-states new-application-filter))})
    (dispatch [:state-update #(assoc-in % [:application :filter] new-application-filter)])))


(defn- toggle-all-filters [all-filters-selected?]
  (util/update-url-with-query-params {:unselected-states nil})
  (dispatch [:state-update #(assoc-in % [:application :filter]
                                      (if all-filters-selected?
                                        (map first application-review-states/application-review-states)
                                        []))]))

(defn state-filter-controls []
  (let [application-filters    (subscribe [:state-query [:application :filter]])
        review-state-counts    (subscribe [:state-query [:application :review-state-counts]])
        filter-opened          (r/atom false)
        toggle-filter-opened   (fn [_] (swap! filter-opened not))
        get-review-state-count (fn [counts state-id] (or (get counts state-id) 0))]
    (fn []
      (let [all-filters-selected? (= (count @application-filters)
                                     (count application-review-states/application-review-states))]
        [:span.application-handling__filter-state
         [:a
          {:on-click toggle-filter-opened}
          (str "Tila" (when-not all-filters-selected? " *"))]
         (when @filter-opened
           (into [:div.application-handling__filter-state-selection
                  [:div.application-handling__filter-state-selection-row.application-handling__filter-state-selection-row--all
                   {:class (when all-filters-selected? "application-handling__filter-state-selected-row")}
                   [:label
                    [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                             :type      "checkbox"
                             :checked   all-filters-selected?
                             :on-change #(toggle-all-filters (not all-filters-selected?))}]
                    [:span "Kaikki"]]]]
                 (mapv
                   (fn [review-state]
                     (let [review-state-id (first review-state)
                           filter-selected (some #{review-state-id} @application-filters)]
                       [:div.application-handling__filter-state-selection-row
                        {:class (if filter-selected "application-handling__filter-state-selected-row" "")}
                        [:label
                         [:input {:class     "application-handling__filter-state-selection-row-checkbox"
                                  :type      "checkbox"
                                  :checked   (boolean filter-selected)
                                  :on-change #(toggle-filter @application-filters review-state-id filter-selected)}]
                         [:span (str (second review-state)
                                  " (" (get-review-state-count @review-state-counts review-state-id) ")")]]]))
                   application-review-states/application-review-states)))
         (when @filter-opened [:div.application-handling__filter-state-selection-arrow-up])]))))

(defn sortable-column-click [column-id evt]
  (dispatch [:application/update-sort column-id]))

(defn application-list-basic-column-header [column-id css-class heading]
  (let [application-sort (subscribe [:state-query [:application :sort]])]
    (fn [column-id css-class heading]
      [:span
       {:class    css-class
        :on-click (partial sortable-column-click column-id)}
       [:span.application-handling__basic-list-basic-column-header
        heading]
       (when (= column-id (:column @application-sort))
         (if (= :descending (:order @application-sort))
           [:i.zmdi.zmdi-chevron-down.application-handling__sort-arrow]
           [:i.zmdi.zmdi-chevron-up.application-handling__sort-arrow]))])))

(defn application-list-loading-indicator []
  (let [fetching (subscribe [:state-query [:application :fetching-applications]])]
    (when @fetching
        [:div.application-handling__list-loading-indicator
         [:i.zmdi.zmdi-spinner]])))

(defn application-list [applications]
  [:div
   [:div.application-handling__list-header.application-handling__list-row
    [application-list-basic-column-header
     :applicant-name
     "application-handling__list-row--applicant"
     "Hakija"]
    [application-list-basic-column-header
     :created-time
     "application-handling__list-row--time"
     "Saapunut"]
    [application-list-basic-column-header
     :score
     "application-handling__list-row--score"
     "Pisteet"]
    [:span.application-handling__list-row--state [state-filter-controls]]]
   [application-list-contents applications]])

(defn application-contents [{:keys [form application]}]
  [readonly-contents/readonly-fields form application])

(defn review-state-selected-row [on-click label]
  [:div.application-handling__review-state-row.application-handling__review-state-selected-row
   {:on-click on-click}
   [icon-check] label])

(defn review-state-row [state-name current-review-state review-state]
  (let [[review-state-id review-state-label] review-state]
    (if (= current-review-state review-state-id)
      (review-state-selected-row #() review-state-label)
      [:div.application-handling__review-state-row
       {:on-click #(dispatch [:application/update-review-field state-name review-state-id])}
       review-state-label])))

(defn opened-review-state-list [state-name current-state all-states]
  (mapv (partial review-state-row state-name (or @current-state (ffirst all-states))) all-states))

(defn application-review-state []
  (let [review-state (subscribe [:state-query [:application :review :state]])
        list-opened  (r/atom false)
        list-click   (fn [evt] (swap! list-opened not))]
    (fn []
      [:div.application-handling__review-state-container.application-handling__review-state-container--bottom-border
       [:div.application-handling__review-header "Hakemus"]
       (if @list-opened
         [:div.application-handling__review-state-list-opened-anchor
          (into [:div.application-handling__review-state-list-opened
                 {:on-click list-click}]
                (opened-review-state-list :state review-state application-review-states/application-review-states))]
         (review-state-selected-row
           list-click
           (get-review-state-label-by-name application-review-states/application-review-states @review-state)))])))

(defn- find-hakukohde-by-oid
  [hakukohteet hakukohde-oid]
  (first (filter #(= (:oid %) hakukohde-oid) hakukohteet)))

(defn- hakukohde-label
  [hakukohde]
  (let [name (:name hakukohde)]
    (or (:fi name)
        (:sv name)
        (:en name))))

(defn- opened-hakukohde-list-row
  [selected-hakukohde-oid hakukohteet hakukohde-oid]
  (let [hakukohde (find-hakukohde-by-oid hakukohteet hakukohde-oid)
        selected? (= selected-hakukohde-oid hakukohde-oid)]
    [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde
     {:data-hakukohde-oid hakukohde-oid
      :class              (when selected? "application-handling__review-state-selected-row-hakukohde")
      :on-click           (fn [evt]
                            (dispatch [:application/select-review-hakukohde (aget evt "target" "dataset" "hakukohdeOid")]))}
     (when selected? [icon-check])
     (hakukohde-label hakukohde)]))


(defn- selected-hakukohde-row
  [selected-hakukohde-oid on-click haku-hakukohteet application-hakukohde-oids]
  (let [selected-hakukohde                  (find-hakukohde-by-oid haku-hakukohteet selected-hakukohde-oid)
        application-has-multiple-hakukohde? (< 1 (count application-hakukohde-oids))]
    [:div.application-handling__review-state-row.application-handling__review-state-row-hakukohde.application-handling__review-state-selected-row-hakukohde
     {:on-click (if application-has-multiple-hakukohde? on-click identity)
      :class (if (not application-has-multiple-hakukohde?) "application-handling__review-state-row-hakukohde--single-option")}
     [icon-check]
     (hakukohde-label selected-hakukohde)]))

(defn- application-hakukohde-selection
  []
  (let [selected-hakukohde-oid     (subscribe [:state-query [:application :selected-review-hakukohde]])
        haku-hakukohteet           (subscribe [:state-query [:application :hakukohteet]])
        application-hakukohde-oids (subscribe [:state-query [:application :selected-application-and-form :application :hakukohde]])
        list-opened                (r/atom false)
        select-list-item           #(swap! list-opened not)]
    (fn []
      (when (pos? (count @application-hakukohde-oids))
        [:div.application-handling__review-state-container.application-handling__review-state-container--columnar
         [:div.application-handling__review-header (str "Hakukohteet (" (count @application-hakukohde-oids) ")")]
         (if @list-opened
           [:div.application-handling__review-state-list-opened-anchor
            (into
              [:div.application-handling__review-state-list-opened {:on-click select-list-item}]
              (map #(opened-hakukohde-list-row @selected-hakukohde-oid @haku-hakukohteet %) @application-hakukohde-oids))]
           (selected-hakukohde-row @selected-hakukohde-oid select-list-item @haku-hakukohteet @application-hakukohde-oids))]))))

(defn- application-hakukohde-review-input
  [label kw states]
  (let [current-hakukohde (subscribe [:state-query [:application :selected-review-hakukohde]])
        list-opened       (r/atom false)
        list-click        (fn [_] (swap! list-opened not))]
    (fn []
      (let [review-state-for-current-hakukohde (subscribe [:state-query [:application :review :hakukohde-reviews (keyword @current-hakukohde) kw]])]
        [:div.application-handling__review-state-container
         [:div.application-handling__review-header
          {:class (str "application-handling__review-header--" (name kw))} label]
         (if @list-opened
           [:div.application-handling__review-state-list-opened-anchor
            (into [:div.application-handling__review-state-list-opened
                   {:on-click list-click}]
                  (opened-review-state-list kw review-state-for-current-hakukohde states))]
           (review-state-selected-row
             list-click
             (get-review-state-label-by-name
               states
               (or @review-state-for-current-hakukohde (ffirst states)))))]))))

(defn- application-hakukohde-review-inputs
  [review-types]
  (into [:div]
        (mapv (fn [[kw label states]]
                [application-hakukohde-review-input label kw states])
              review-types)))

(defn- name-and-initials [{:keys [first-name last-name]}]
  (if (and first-name last-name)
    [(str first-name " " last-name)
     (str (subs first-name 0 1)
          (subs last-name 0 1))]
    [nil nil]))

(defn- virkailija-initials-span
  [event]
  (let [[name initials] (name-and-initials event)]
    (when (and name initials)
      [:span.application-handling__review-state-initials {:data-tooltip name} (str " (" initials ")")])))

(defn event-caption [event]
  (case (:event-type event)
    "review-state-change" (get-review-state-label-by-name
                            application-review-states/application-review-states (:new-review-state event))
    "updated-by-applicant" "Hakija muokannut hakemusta"
    "updated-by-virkailija" [:span.application-handling__event-caption
                             "Virkailija "
                             (virkailija-initials-span event)
                             " muokannut hakemusta"]
    "received-from-applicant" "Hakemus vastaanotettu"
    "hakukohde-review-state-change" [:span.application-handling__event-caption
                                     (str
                                       (->> application-review-states/hakukohde-review-types
                                            (filter #(= (keyword (:review-key event)) (first %)))
                                            (first)
                                            (second))
                                       ": "
                                       (get-review-state-label-by-name
                                         (concat application-review-states/application-hakukohde-review-states
                                                 application-review-states/application-hakukohde-eligibility-states
                                                 application-review-states/application-hakukohde-selection-states)
                                         (:new-review-state event)))
                                     (virkailija-initials-span event)]
    "Tuntematon"))

(defn to-event-row
  [time-str caption]
  [:div.application-handling__event-row
   [:span.application-handling__event-timestamp time-str]
   [:span.application-handling__event-caption caption]])

(defn event-row [event]
  (let [time-str      (t/time->short-str (:time event))
        caption       (event-caption event)]
    (to-event-row time-str caption)))

(defn application-review-events []
  (let [events (subscribe [:state-query [:application :events]])]
    (fn []
      (into
        [:div.application-handling__event-list
         [:div.application-handling__review-header "Tapahtumat"]]
        (mapv event-row @events)))))

(defn update-review-field [field convert-fn evt]
  (let [new-value (-> evt .-target .-value)]
    (dispatch [:application/update-review-field field (convert-fn new-value)])))

(defn convert-score [review new-value]
  (let [maybe-number (js/Number new-value)]
    (cond
      (= "" new-value)
      nil

      ;; JS NaN is the only thing not equal with itself
      ;; and this is the way to detect it
      (not= maybe-number maybe-number)
      (:score review)

      :else
      maybe-number)))

(defn application-review-inputs []
  (let [review (subscribe [:state-query [:application :review]])
        ; React doesn't like null, it leaves the previous value there, hence:
        review-field->str (fn [review field] (if-let [notes (field @review)] notes ""))]
    (fn []
      [:div.application-handling__review-inputs
       [:div.application-handling__review-row--nocolumn
        [:div.application-handling__review-header "Muistiinpanot"]
        [:textarea.application-handling__review-notes
         {:value (review-field->str review :notes)
          :on-change (partial update-review-field :notes identity)}]]
       [:div.application-handling__review-row
        [:div.application-handling__review-header.application-handling__review-header--points
         "Pisteet"]
        [:input.application-handling__score-input
         {:type "text"
          :max-length "2"
          :size "2"
          :value (review-field->str review :score)
          :on-change (partial update-review-field :score (partial convert-score @review))}]]])))

(defn- application-modify-link []
  (let [application-key (subscribe [:state-query [:application :selected-key]])]
    [:a.application-handling__edit-link
     {:href   (str "/lomake-editori/api/applications/" @application-key "/modify")
      :target "_blank"}
     "Muokkaa hakemusta"]))

(defn- application-information-request-recipient []
  (let [email (subscribe [:state-query [:application :selected-application-and-form :application :answers :email :value]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading "Vastaanottaja:"]
     [:div @email]]))

(defn- application-information-request-subject []
  (let [subject (subscribe [:state-query [:application :information-request :subject]])]
    [:div.application-handling__information-request-row
     [:div.application-handling__information-request-info-heading "Aihe:"]
     [:div.application-handling__information-request-text-input-container
      [:input.application-handling__information-request-text-input
       {:value     @subject
        :on-change (fn [event]
                     (let [subject (-> event .-target .-value)]
                       (dispatch [:application/set-information-request-subject subject])))}]]]))

(defn- application-information-request-message []
  (let [message (subscribe [:state-query [:application :information-request :message]])]
    [:div.application-handling__information-request-row
     [:textarea.application-handling__information-request-message-area
      {:value     @message
       :on-change (fn [event]
                    (let [message (-> event .-target .-value)]
                      (dispatch [:application/set-information-request-message message])))}]]))

(defn- application-information-request-submit-button []
  (let [enabled?      (subscribe [:application/information-request-submit-enabled?])
        request-state (subscribe [:state-query [:application :information-request :state]])
        button-text   (reaction (if (= @request-state :submitting)
                                  "Täydennyspyyntöä lähetetään"
                                  "Lähetä täydennyspyyntö"))]
    (fn []
      [:div.application-handling__information-request-row
       [:button.application-handling__send-information-request-button
        {:type     "button"
         :disabled (not @enabled?)
         :class    (if @enabled?
                     "application-handling__send-information-request-button--enabled"
                     "application-handling__send-information-request-button--disabled")
         :on-click #(dispatch [:application/submit-information-request])}
        @button-text]])))

(defn- application-information-request []
  (let [request-window-open? (r/atom true)]
    (fn []
      (if @request-window-open?
        [:div.application-handling__information-request-container
         [:div.application-handling__information-request-header
          "Lähetä täydennyspyyntö hakijalle"
          [:i.zmdi.zmdi-close-circle.application-handling__information-request-close-button
           {:on-click #(reset! request-window-open? false)}]]
         [application-information-request-recipient]
         [application-information-request-subject]
         [application-information-request-message]
         [application-information-request-submit-button]]
        [:div.application-handling__information-request-show-container-link
         [:a
          {:on-click #(reset! request-window-open? true)}
          "Lähetä täydennyspyyntö hakijalle"]]))))

(defn application-review []
  (let [review-positioning (subscribe [:state-query [:application :review-positioning]])
        review-state       (subscribe [:state-query [:application :review :state]])]
    [:div.application-handling__review
     {:class (when (= :fixed @review-positioning)
               "application-handling__review-floating")}
     [:div.application-handling__review-inner-container
      [:div.application-handling__review-outer-container
       [application-review-state]
       (when (= @review-state "information-request")
         [application-information-request])
       [application-hakukohde-selection]
       [application-hakukohde-review-inputs review-states/hakukohde-review-types]
       [application-review-inputs]
       [application-modify-link]
       [application-review-events]]]]))

(defn floating-application-review-placeholder
  "Keeps the content of the application in the same place when review-area starts floating (fixed position)"
  []
  [:div.application-handling__floating-application-review-placeholder])

(defn- koulutus->str
  [koulutus]
  (->> [(-> koulutus :koulutuskoodi-name :fi)
        (-> koulutus :tutkintonimike-name :fi)
        (:tarkenne koulutus)]
       (remove #(or (nil? %) (clojure.string/blank? %)))
       (distinct)
       (clojure.string/join ", ")))

(defn- hakukohteet-list-row [hakukohde]
  ^{:key (str "hakukohteet-list-row-" (:oid hakukohde))}
  [:li.application-handling__hakukohteet-list-row
   [:div.application-handling__review-area-hakukohde-heading
    (str (-> hakukohde :name :fi) " - " (-> hakukohde :tarjoaja-name :fi))]
   (doall
    (for [koulutus (:koulutukset hakukohde)]
      ^{:key (str "koulutus-" (:oid koulutus))}
      [:div.application-handling__review-area-koulutus-heading
       (koulutus->str koulutus)]))])

(defn- hakukohteet-list [hakukohteet]
  (into [:ul.application-handling__hakukohteet-list]
        (map hakukohteet-list-row hakukohteet)))

(defn application-heading [application]
  (let [answers            (:answers application)
        pref-name          (-> answers :preferred-name :value)
        last-name          (-> answers :last-name :value)
        ssn                (get-in answers [:ssn :value])
        email              (get-in answers [:email :value])
        birth-date         (get-in answers [:birth-date :value])
        hakukohteet-by-oid (into {} (map (fn [h] [(:oid h) h]) (-> application :tarjonta :hakukohteet)))
        applications-count (:applications-count application)
        person-oid         (:person-oid application)]
    [:div.application__handling-heading
     [:div.application-handling__review-area-main-heading-container
      [:div.application-handling__review-area-main-heading-person-info
       [:div.application-handling__review-area-main-heading-name-row
        [:h2.application-handling__review-area-main-heading
         (str pref-name " " last-name ", " (or ssn birth-date))]
        (when (> applications-count 1)
          [:a.application-handling__review-area-main-heading-applications-link
           {:on-click (fn [_]
                        (dispatch [:application/navigate
                                   (str "/lomake-editori/applications/search"
                                        "?term=" (or ssn email))]))}
           (str applications-count " hakemusta")])]
       (when person-oid
         [:div.application-handling__review-area-main-heading-person-oid-row
          [:a
           {:href (str "/authentication-henkiloui/html/henkilo/"
                       person-oid
                       "?permissionCheckService=ATARU")
            :target "_blank"}
           [:i.zmdi.zmdi-account-circle.application-handling__review-area-main-heading-person-icon]
           [:span.application-handling__review-area-main-heading-person-oid
            (str "Oppija " person-oid)]]])]
      (when (and (not (contains? (:answers application) :hakukohteet))
                 (not-empty hakukohteet-by-oid))
        (hakukohteet-list (map hakukohteet-by-oid (:hakukohde application))))]]))

(defn close-application []
  [:a {:href     "#"
       :on-click (fn [event]
                   (.preventDefault event)
                   (dispatch [:application/close-application]))}
   [:div.close-details-button
    [:i.zmdi.zmdi-close.close-details-button-mark]]])

(defn application-review-area [applications]
  (let [selected-key                  (subscribe [:state-query [:application :selected-key]])
        selected-application-and-form (subscribe [:state-query [:application :selected-application-and-form]])
        review-state                  (subscribe [:state-query [:application :review :state]])
        application-filter            (subscribe [:state-query [:application :filter]])
        belongs-to-current-form       (fn [key applications] (first (filter #(= key (:key %)) applications)))
        included-in-filter            (fn [review-state filter] (some #{review-state} filter))
        expanded?                     (subscribe [:state-query [:application :application-list-expanded?]])
        review-positioning            (subscribe [:state-query [:application :review-positioning]])]
    (fn [applications]
      (let [application        (:application @selected-application-and-form)]
        (when (and (included-in-filter @review-state @application-filter)
                   (belongs-to-current-form @selected-key applications)
                   (not @expanded?))
          [:div.application-handling__detail-container
           [close-application]
           [application-heading application]
           [:div.application-handling__review-area
            [:div.application-handling__application-contents
             [application-contents @selected-application-and-form]]
            [:span#application-handling__review-position-canary]
            (when (= :fixed @review-positioning) [floating-application-review-placeholder])
            [application-review]]])))))

(defn application []
  (let [applications            (subscribe [:state-query [:application :applications]])
        application-filter      (subscribe [:state-query [:application :filter]])
        search-control-all-page (subscribe [:application/search-control-all-page-view?])
        include-filtered        (fn [application-filter applications] (filter #(some #{(:state %)} application-filter) applications))
        filtered-applications   (include-filtered @application-filter @applications)]
    [:div
     [:div.application-handling__overview
      [application-search-control]
      (when (not @search-control-all-page)
        [:div.application-handling__bottom-wrapper.select_application_list
         [haku-heading filtered-applications @application-filter]
         [application-list filtered-applications]
         [application-list-loading-indicator]])]
     (when (not @search-control-all-page)
       [:div
        [application-review-area filtered-applications]])]))

(defn create-review-position-handler []
  (let [review-canary-visible        (atom true)
        positioning-change-threshold 45]
    (fn [_]
      (when-let [canary-element (.getElementById js/document "application-handling__review-position-canary")]
        (if (<= (-> canary-element .getBoundingClientRect .-top) positioning-change-threshold)
          (when @review-canary-visible
            (dispatch [:state-update #(assoc-in % [:application :review-positioning] :fixed)])
            (reset! review-canary-visible false))
          (when-not @review-canary-visible
            (dispatch [:state-update #(assoc-in % [:application :review-positioning] :in-flow)])
            (reset! review-canary-visible true)))))))
