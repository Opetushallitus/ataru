(ns ataru.application-common.application-readonly
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                           required-hint
                                                           textual-field-value
                                                           wrapper-id]]))

(defn text [application field-descriptor]
  [:div.application__form-field
   [:label.application__form-field-label
    (str (-> field-descriptor :label :fi) (required-hint field-descriptor))]
   [:div (textual-field-value field-descriptor application)]])

(declare field)

(defn wrapper [content application children]
  (let [fieldset? (= "fieldset" (:fieldType content))]
    [:div
     (when fieldset?
       {:class "application__wrapper-element application__wrapper-element--border"})
     [:h2.application__wrapper-heading
      {:id (wrapper-id content)}
      (-> content :label :fi)]
     (into [:div (when fieldset? {:class "application__wrapper-contents"})] (mapv #(field % application) children))]))

(defn field
  [content application]
  (match content
         {:fieldClass "wrapperElement" :children children} [wrapper content application children]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown")} (text application content)))

(defn readonly-fields [form-data application]
  (when form-data
    (into [:div.application__readonly-container] (mapv #(field % application) (:content form-data)))))
