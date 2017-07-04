(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.cljs-util :as util]
            [ataru.translations.translation-util :refer [get-translations]]
            [ataru.translations.application-view :as translations]
            [ataru.hakija.application :refer [application-in-complete-state?]]
            [ataru.application-common.koulutus :as koulutus]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [cljs-time.format :refer [unparse formatter]]
            [cljs-time.coerce :refer [from-long]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [reagent.ratom :refer [reaction]]))

(def ^:private language-names
  {:fi "Suomeksi"
   :sv "På svenska"
   :en "In English"})

(def date-format (formatter "d.M.yyyy"))

(defn application-header [form]
  (let [selected-lang    (:selected-language form)
        languages        (filter
                           (partial not= selected-lang)
                           (:languages form))
        submit-status    (subscribe [:state-query [:application :submit-status]])
        application      (subscribe [:state-query [:application]])
        secret           (:modify (util/extract-query-params))

        haku-name        (-> form :tarjonta :haku-name)
        apply-start-date (-> form :tarjonta :hakuaika-dates :start)
        apply-end-date   (-> form :tarjonta :hakuaika-dates :end)
        hakuaika-on      (-> form :tarjonta :hakuaika-dates :on)

        translations     (get-translations
                           (keyword selected-lang)
                           translations/application-view-translations)
        apply-dates      (when haku-name
                           (if (and apply-start-date apply-end-date)
                             (str (:application-period translations)
                                  ": "
                                  (unparse date-format (from-long apply-start-date))
                                  " - "
                                  (unparse date-format (from-long apply-end-date))
                                  (when-not hakuaika-on
                                    (str " (" (:not-within-application-period translations) ")")))
                             (:continuous-period translations)))]
    (fn [form]
      [:div
       [:div.application__header-container
        [:span.application__header (or haku-name (:name form))]
        (when (and (not= :submitted @submit-status)
                   (> (count languages) 0)
                   (nil? secret))
          [:span.application__header-text
           (map-indexed (fn [idx lang]
                          (cond-> [:span {:key (name lang)}
                                   [:a {:href (str "?lang=" (name lang))}
                                    (get language-names lang)]]
                                  (> (dec (count languages)) idx)
                                  (conj [:span.application__header-language-link-separator " | "])))
                        languages)])]
       (when apply-dates
         [:div.application__sub-header-container
          [:span.application__sub-header-dates apply-dates]])
       (when (application-in-complete-state? @application)
         [:div.application__sub-header-container
          [:span.application__sub-header-modifying-prevented
           (:application-processed-cant-modify translations)]])])))

(defn- selected-hakukohde-row
  [hakukohde]
  ^{:key (str "selected-hakukohde-row-" (:oid hakukohde))}
  [:div.application__hakukohde-row
   [:div.application__hakukohde-selected-row-header
    ; TODO support other languages
    (-> hakukohde :name :fi)]
   [:div.application__hakukohde-selected-row-description
    (koulutus/koulutukset->str (:koulutukset hakukohde))]
   [:div.application__hakukohde-row-button-container
    [:a.application__hakukohde-remove-link
     {:on-click #(dispatch [:application/hakukohde-remove-selection hakukohde])}
     "Poista"]]])

(defn- search-hit-hakukohde-row
  [hakukohde]
  ^{:key (str "found-hakukohde-row-" (:oid hakukohde))}
  [:div.application__hakukohde-row
   [:div.application__hakukohde-row-text-container
    [:div.application__hakukohde-selected-row-header
     ; TODO support other languages
     (-> hakukohde :name :fi)]
    [:div.application__hakukohde-selected-row-description
     (koulutus/koulutukset->str (:koulutukset hakukohde))]]
   [:div.application__hakukohde-row-button-container
    [:a.application__hakukohde-select-button
     {:on-click #(dispatch [:application/hakukohde-add-selection hakukohde])}
     "Lisää"]]])

(defn- hakukohde-selection
  [form selected-hakukohteet hakukohde-query]

  (let [tarjonta                (:tarjonta form)
        hakukohteet             (:hakukohteet tarjonta)
        query-pattern           (re-pattern (str "(?i)" hakukohde-query))
        search-hit-hakukohteet  (if (< 1 (count hakukohde-query))
                                  ; TODO support other languages
                                  (filter #(re-find query-pattern (get-in % [:name :fi] "")) hakukohteet)
                                  [])
        multiple-hakukohde?     (< 1 (count (:hakukohteet tarjonta)))]
    [:div.application__hakukohde-selection
     [:h3.application__hakukohde-selection-header
      (if multiple-hakukohde?
        "Hakemasi koulutukset"
        "Hakemasi koulutus")]
     (into
       [:div.application__hakukohde-selected-list]
       (map selected-hakukohde-row selected-hakukohteet))
     [:div.application__hakukohde-selection-search-container
      [:input.application__hakukohde-selection-search-input.application__form-text-input
       {:on-change   #(dispatch [:application/hakukohde-query-change (aget % "target" "value")])
        :placeholder "Etsi tämän haun koulutuksia"}]
      (into
        [:div.application__hakukohde-selection-search-results
         (map search-hit-hakukohde-row search-hit-hakukohteet)])]]))

(defn readonly-fields [form]
  (let [application (subscribe [:state-query [:application]])]
    (fn [form]
      [readonly-view/readonly-fields form @application])))

(defn render-fields [form]
  (let [submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn [form]
      (if (= :submitted @submit-status)
        [readonly-fields form]
        (do
          (dispatch [:application/run-rule])                ; wtf
          [editable-fields form])))))

(defn application-contents []
  (let [form                 (subscribe [:state-query [:form]])
        can-apply?           (subscribe [:application/can-apply?])
        selected-hakukohteet (subscribe [:state-query [:application :selected-hakukohteet]])
        hakukohde-query      (subscribe [:state-query [:application :hakukohde-query]])]
    (fn []
      [:div.application__form-content-area
       ^{:key (:id @form)}
       [application-header @form]

       (when (pos? (count (-> @form :tarjonta :hakukohteet)))
         ^{:key "application-hakukohde-selection"}
         [hakukohde-selection @form @selected-hakukohteet @hakukohde-query])

       (when @can-apply?
         ^{:key "form-fields"}
         [render-fields @form])])))

(defn- star-number-from-event
  [event]
  (-> event
      (aget "target" "dataset" "starN")
      (js/parseInt 10)))

(defn feedback-form
  []
  (let [form           (subscribe [:state-query [:form]])
        submit-status  (subscribe [:state-query [:application :submit-status]])
        star-hovered   (subscribe [:state-query [:application :feedback :star-hovered]])
        stars          (subscribe [:state-query [:application :feedback :stars]])
        hidden?        (subscribe [:state-query [:application :feedback :hidden?]])
        rating-status  (subscribe [:state-query [:application :feedback :status]])
        show-feedback? (reaction (and (= :submitted @submit-status)
                                      (not @hidden?)))]
    (fn []
      (let [translations (get-translations
                           (keyword (:selected-language @form))
                           translations/application-view-translations)
            rated?       (= :rating-given @rating-status)
            submitted?   (= :feedback-submitted @rating-status)]
        (when @show-feedback?
          [:div.application-feedback-form
           [:a.application-feedback-form__close-button
            {:on-click #(dispatch [:application/rating-form-toggle])}
            [:i.zmdi.zmdi-close.close-details-button-mark]]
           [:div.application-feedback-form-container
            (when (not submitted?)
              [:h2.application-feedback-form__header (:feedback-header translations)])
            (when (not submitted?)
              [:div.application-feedback-form__rating-container.animated.zoomIn
               {:on-click      #(dispatch [:application/rating-submit (star-number-from-event %)])
                :on-mouse-out  #(dispatch [:application/rating-hover 0])
                :on-mouse-over #(dispatch [:application/rating-hover (star-number-from-event %)])}
               (let [stars-active (or @stars @star-hovered 0)]
                 (map (fn [n]
                        (let [star-classes (if (< n stars-active)
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--active.zmdi.zmdi-star
                                             :i.application-feedback-form__rating-star.application-feedback-form__rating-star--inactive.zmdi.zmdi-star-outline)]
                          [star-classes
                           {:key         (str "rating-star-" n)
                            :data-star-n (inc n)}])) (range 5)))])
            (when (not submitted?)
              [:div.application-feedback-form__rating-text
               (let [stars-selected (or @stars @star-hovered)]
                 (if (and (int? stars-selected)
                          (< 0 stars-selected 6))
                   (get (:feedback-ratings translations) stars-selected)
                   (gstring/unescapeEntities "&nbsp;")))])
            (when (not submitted?)
              [:div.application-feedback-form__text-feedback-container
               [:textarea.application__form-text-input.application__form-text-area.application__form-text-area__size-medium
                {:on-change   #(dispatch [:application/rating-update-feedback (.-value (.-target %))])
                 :placeholder (:feedback-text-placeholder translations)
                 :max-length  2000}]])
            (when (and (not submitted?)
                     rated?)
              [:a.application__send-feedback-button.application__send-feedback-button--enabled
               {:on-click (fn [evt]
                            (.preventDefault evt)
                            (dispatch [:application/rating-feedback-submit]))}
               (:feedback-send translations)])
            (when (and (not submitted?)
                       (not rated?))
              [:a.application__send-feedback-button.application__send-feedback-button--disabled
               (:feedback-send translations)])
            (when (not submitted?)
              [:div.application-feedback-form__disclaimer (:feedback-disclaimer translations)])
            (when submitted?
              [:div.application__thanks [:i.zmdi.zmdi-thumb-up.application__thanks-icon] [:span.application__thanks-text (:feedback-thanks translations)]])]])))))

(defn error-display []
  (let [error-message (subscribe [:state-query [:error :message]])
        detail (subscribe [:state-query [:error :detail]])]
    (fn [] (if @error-message
             [:div.application__error-display @error-message (str @detail)]
             nil))))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]
   [feedback-form]])
