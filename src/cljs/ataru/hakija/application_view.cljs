(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.hakija.hakija-readonly :as readonly-view]
            [ataru.cljs-util :as util]
            [ataru.translations.translation-util :refer [get-translations]]
            [ataru.translations.application-view :as translations]
            [ataru.application-common.koulutus :as koulutus]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [cljs-time.format :refer [unparse formatter]]
            [cljs-time.coerce :refer [from-long]]
            [clojure.string :as string]))

(def ^:private language-names
  {:fi "Suomeksi"
   :sv "På svenska"
   :en "In English"})

(def date-format (formatter "d.M.yyyy"))

(defn application-header [form]
  (let [selected-lang     (:selected-language form)
        languages         (filter
                            (partial not= selected-lang)
                            (:languages form))
        submit-status     (subscribe [:state-query [:application :submit-status]])
        secret            (:modify (util/extract-query-params))
        hakukohde-name    (-> form :tarjonta :hakukohde-name)
        haku-tarjoja-name (-> form :tarjonta :haku-tarjoaja-name)
        koulutukset-str   (koulutus/koulutukset->str (-> form :tarjonta :koulutukset))
        apply-start-date  (-> form :tarjonta :hakuaika-dates :start)
        apply-end-date    (-> form :tarjonta :hakuaika-dates :end)
        hakuaika-on       (-> form :tarjonta :hakuaika-dates :on)
        translations      (get-translations
                           (keyword selected-lang)
                           translations/application-view-translations)
        apply-dates       (when hakukohde-name
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
        [:span.application__header (or hakukohde-name (:name form))]
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
       (when (and haku-tarjoja-name apply-dates)
         [:div.application__sub-header-container
          (when-not (string/blank? koulutukset-str) [:div.application__sub-header-koulutus koulutukset-str])
          [:span.application__sub-header-organization haku-tarjoja-name]
          [:span.application__sub-header-dates apply-dates]])])))

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
          (dispatch [:application/run-rule])
          [editable-fields form])))))

(defn application-contents []
  (let [form       (subscribe [:state-query [:form]])
        can-apply? (subscribe [:application/can-apply?])]
    (fn []
      [:div.application__form-content-area
       ^{:key (:id @form)}
       [application-header @form]
       (when @can-apply?
         [render-fields @form])])))

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
   [application-contents]])
