(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [ataru.hakija.application-form-components :refer [render-fields]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(defn application-header [form-name]
  [:h1.application__header form-name])

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    (fn []
      (into [:div.application__form-content-area [application-header (:name @form)]] (render-fields @form)))))

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
