(ns ataru.hakija.application-readonly
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-field-common :refer [answer-key required-hint textual-field-value]]))

(defn text [field-descriptor]
  (let [application (subscribe [:state-query [:application]])
        label (-> field-descriptor :label :fi)]
    (fn [field-descriptor]
      [:div.application__form-field
       [:label.application_form-field-label label (required-hint field-descriptor)]
       [:div (textual-field-value field-descriptor @application)]])))

(declare field)

(defn wrapper [content children]
  (into [:div.application__wrapper-element [:h2.application__wrapper-heading (-> content :label :fi)]]
        (mapv field children)))

(defn field
  [content]
  (match content
         {:fieldClass "wrapperElement"
          :children   children} [wrapper content children]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea")} [text content]))

(defn render-readonly-fields [form-data]
  (when form-data
    (mapv field (:content form-data))))
