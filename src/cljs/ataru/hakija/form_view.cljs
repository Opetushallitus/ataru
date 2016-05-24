(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]))

(defn text-field [content]
  [:div.application__form-field [:label (-> content :label :fi)]])

(declare render-field)

(defn wrapper-field [children]
  (into [:div.application__wrapper-element] (mapv render-field children)))

(defn render-field
  [content]
   (match [content]
          [{:fieldClass "wrapperElement"
            :children   children}] [wrapper-field children]
          [{:fieldClass "formField" :fieldType "textField"}] [text-field content]))

(defn render-fields [form-data]
  (if form-data
    (mapv render-field (:content form-data))
    nil))

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    (fn [] (into [:div] (render-fields @form)))))

(defn form-view []
  [:div
   [banner]
   [application-contents]])
