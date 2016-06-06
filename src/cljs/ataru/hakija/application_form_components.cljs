(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application-field-common :refer [answer-key required-hint textual-field-value]]))
(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- textual-field-change [text-field-data evt]
  (let [value (-> evt .-target .-value)
        valid (if (:required text-field-data) (not (empty? (trim value))) true)]
    (dispatch [:application/set-application-field (answer-key text-field-data) {:value value :valid valid}])))

(defn text-field [field-descriptor]
  (let [application (subscribe [:state-query [:application]])
        label (-> field-descriptor :label :fi)]
    (fn [field-descriptor]
      [:div.application__form-field
       [:label.application_form-field-label label (required-hint field-descriptor)]
       [:input.application__form-text-input
        {:type "text"
         :class (text-field-size->class (-> field-descriptor :params :size))
         :value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)}]])))

(defn- text-area-size->class [size]
  (match size
         "S" "application__form-text-area__size-small"
         "M" "application__form-text-area__size-medium"
         "L" "application__form-text-area__size-large"
         :else "application__form-text-area__size-medium"))

(defn text-area [field-descriptor]
  (let [application (subscribe [:state-query [:application]])
        label (-> field-descriptor :label :fi)]
    (fn [field-descriptor]
      [:div.application__form-field
       [:label.application_form-field-label label (required-hint field-descriptor)]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         :value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)}]])))

(declare render-field)

(defn wrapper-field [content children]
  [:div.application__wrapper-element
   [:h2.application__wrapper-heading (-> content :label :fi)]
   (into [:div.application__wrapper-contents] (mapv render-field children))])

(defn render-field
  [content]
  (match content
         {:fieldClass "wrapperElement"
          :children   children} [wrapper-field content children]
         {:fieldClass "formField" :fieldType "textField"} [text-field content]
         {:fieldClass "formField" :fieldType "textArea"} [text-area content]))

(defn render-editable-fields [form-data]
  (when form-data
    (mapv render-field (:content form-data))))
