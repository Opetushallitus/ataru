(ns ataru.hakija.application-view
  (:require [clojure.string :refer [trim]]
            [ataru.hakija.banner :refer [banner]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]))

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- answer-key [field-data]
  (keyword (:id field-data)))

(defn- required-hint [field-descriptor] (if (-> field-descriptor :required) " *" ""))

(defn- textual-field-value [field-descriptor application]
  (:value ((answer-key field-descriptor) (:answers application))))

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
  (into [:div.application__wrapper-element [:h2.application__wrapper-heading (-> content :label :fi)]]
        (mapv render-field children)))

(defn render-field
  [content]
  (match content
         {:fieldClass "wrapperElement"
          :children   children} [wrapper-field content children]
         {:fieldClass "formField" :fieldType "textField"} [text-field content]
         {:fieldClass "formField" :fieldType "textArea"} [text-area content]))

(defn render-fields [form-data]
  (when form-data
    (mapv render-field (:content form-data))))

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
