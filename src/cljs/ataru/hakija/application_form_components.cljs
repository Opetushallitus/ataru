(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application-field-common :refer [answer-key
                                                           required-hint
                                                           textual-field-value
                                                           wrapper-id]]
            [reagent.core :as r]))
(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- field-value-valid?
  [field-data value]
  (if (:required field-data) (not (clojure.string/blank? value)) true))

(defn- textual-field-change [text-field-data evt]
  (let [value (-> evt .-target .-value)
        valid (field-value-valid? text-field-data value)]
    (dispatch [:application/set-application-field (answer-key text-field-data) {:value value :valid valid}])))

(defn- init-dropdown-value
  [dropdown-data this]
  (let [select (-> (r/dom-node this) (.querySelector "select"))
        value (-> select .-value)
        valid (field-value-valid? dropdown-data value)]
    (dispatch [:application/set-application-field (answer-key dropdown-data) {:value value :valid valid}])))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(defn- label [field-descriptor]
  [:label.application_form-field-label
   {:id (field-id field-descriptor)}
   [:span (get-in field-descriptor [:label :fi]) (required-hint field-descriptor)]])

(defn text-field [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      [div-kwd
       [label field-descriptor]
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

(defn text-area [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])]
    (fn [field-descriptor]
      [div-kwd
       [label field-descriptor]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         :value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)}]])))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:h2.application__wrapper-heading
    {:id (wrapper-id field-descriptor)}
    (-> field-descriptor :label :fi)]
   (into [:div.application__wrapper-contents] (mapv render-field children))])

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
    (mapv #(render-field % :div-kwd :div.application__row-field.application__form-field) children)))

(defn dropdown
  [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (r/create-class
    {:component-did-mount (partial init-dropdown-value field-descriptor)
     :reagent-render      (fn [field-descriptor]
                            [div-kwd
                             {:on-change (partial textual-field-change field-descriptor)}
                             [label field-descriptor]
                             [:div.application__form-select-wrapper
                              [:span.application__form-select-arrow]
                              [:select.application__form-select
                               (for [option (:options field-descriptor)]
                                 ^{:key (:value option)}
                                 [:option {:value (:value option)} (-> option :label :fi)])]]])}))

(defn render-field
  [field-descriptor & args]
  (cond-> (match field-descriptor
            {:fieldClass "wrapperElement"
             :fieldType  "fieldset"
             :children   children} [wrapper-field field-descriptor children]
            {:fieldClass "wrapperElement"
             :fieldType  "rowcontainer"
             :children   children} [row-wrapper children]
            {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor]
            {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
            {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor])
    (not (contains? field-descriptor :children)) (into args)))

(defn render-editable-fields [form-data]
  (when form-data
    (mapv render-field (:content form-data))))
