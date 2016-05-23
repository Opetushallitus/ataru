(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe]]))

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    [:div "form contents" (str form)]))

(defn form-view []
  [:div
   [banner]
   [application-contents]])
