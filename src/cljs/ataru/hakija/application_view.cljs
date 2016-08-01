(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [editable-fields]]
            [ataru.application-common.application-readonly :as readonly-view]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(defn application-header [form-name]
  [:h1.application__header form-name])

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
          (dispatch [:application/run-rules])
          [editable-fields form])))))

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    (fn []
      [:div.application__form-content-area
       [application-header (:name @form)]
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
