(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]))

(defn render-field
  ([content] (render-field content [:div]))
  ([content result]
   (match [content]
          [{:fieldClass "wrapperElement"
            :children   children}] (into result (mapv #(render-field % [:div]) children))
          [{:fieldClass "formField" :fieldType "textField"}] [:label (-> content :label :fi)])))

(defn render-fields [form-data]
  (if form-data
    (let [rendered (mapv render-field (:content form-data))]
      (into [:div] rendered))
    nil))

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    [:div "form contents" (render-fields @form)]))

(defn form-view []
  [:div
   [banner]
   [application-contents]])
