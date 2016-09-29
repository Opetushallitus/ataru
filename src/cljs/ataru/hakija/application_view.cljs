(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.application-common.application-readonly :as readonly-view]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(def ^:private language-names
  {:fi "Suomeksi"
   :sv "På svenska"
   :en "In English"})

(defn application-header [form]
  (let [selected-lang (:selected-language form)
        languages     (filter
                        (partial not= selected-lang)
                        (:languages form))
        submit-status (subscribe [:state-query [:application :submit-status]])]
    (fn [form]
      (println @submit-status)
      [:div.application__header-container
       [:span.application__header (:name form)]
       (when (and (not= :submitted @submit-status)
                  (> (count languages) 0))
         [:span.application__header-text
          (map-indexed (fn [idx lang]
                         (cond-> [:span {:key (name lang)}
                                  [:a {:href (str "/hakemus/" (:key form) "/" (name lang))}
                                   (get language-names lang)]]
                           (> (dec (count languages)) idx)
                           (conj [:span.application__header-language-link-separator " | "])))
                       languages)])])))

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
  (let [form (subscribe [:state-query [:form]])]
    (fn []
      [:div.application__form-content-area
       ^{:key (:id @form)}
       [application-header @form]
       [render-fields @form]])))

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
