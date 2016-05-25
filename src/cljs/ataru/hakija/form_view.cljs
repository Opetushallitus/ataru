(ns ataru.hakija.form-view
  (:require [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]))

(defn text-field [content]
  [:div.application__form-field
   [:label.application_form-field-label (-> content :label :fi)]
   [:input.application__form-text-input {:type "text"}]])

(declare render-field)

(defn wrapper-field [content children]
  (into [:div.application__wrapper-element [:h2.application__wrapper-heading (-> content :label :fi)]]
        (mapv render-field children)))

(defn render-field
  [content]
   (match [content]
          [{:fieldClass "wrapperElement"
            :children   children}] [wrapper-field content children]
          [{:fieldClass "formField" :fieldType "textField"}] [text-field content]))

(defn render-fields [form-data]
  (if form-data
    (mapv render-field (:content form-data))
    nil))

(defn application-header [form-name]
  [:h1.application__header form-name])

(defn application-contents []
  (let [form (subscribe [:state-query [:form]])]
    (fn [] (into [:div.application__form-content-area [application-header (:name @form)]] (render-fields @form)))))

(defn error-display []
  (let [error-message (subscribe [:state-query [:error-message]])]
    (fn [] [:div.application__error-display @error-message])))

(defn form-view []
  [:div
   [banner]
   [error-display]
   [application-contents]])
