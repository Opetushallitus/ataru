(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                           required-hint
                                                           textual-field-value
                                                           scroll-to-anchor]]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- field-value-valid?
  [field-data value]
  (if (not-empty (:validators field-data))
    (every? true? (map #(validator/validate % value)
                       (:validators field-data)))
    true))

(defn- textual-field-change [text-field-data evt]
  (let [value  (-> evt .-target .-value)
        valid? (field-value-valid? text-field-data value)]
    (do
      ; dispatch-sync because we really really want the value to change NOW. Is a minor UI speed boost.
      (dispatch-sync [:application/set-application-field (answer-key text-field-data) {:value value :valid valid?}])
      (when-let [rules (not-empty (:rules text-field-data))]
        (dispatch [:application/run-rule rules])))))

(defn- init-dropdown-value
  [dropdown-data this]
  (let [select (-> (r/dom-node this) (.querySelector "select"))
        value  (or (first
                     (eduction
                       (comp (filter :default-value)
                             (map (comp :fi :label)))
                       (:options dropdown-data)))
                   (-> select .-value))
        valid  (field-value-valid? dropdown-data value)]
    (dispatch [:application/set-application-field (answer-key dropdown-data) {:value value :valid valid}])
    (when-let [rules (not-empty (:rules dropdown-data))]
      (dispatch [:application/run-rule rules]))))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(defn- label [field-descriptor & [size-class]]
  (let [id     (keyword (:id field-descriptor))
        valid? (subscribe [:state-query [:application :answers id :valid]])
        value  (subscribe [:state-query [:application :answers id :value]])]
    (fn [field-descriptor & [size-class]]
      [:label.application__form-field-label {:class size-class}
       [:span (str (get-in field-descriptor [:label :fi]) (required-hint field-descriptor))]
       [scroll-to-anchor field-descriptor]])))

(defn- show-text-field-error-class?
  [field-descriptor value valid?]
  (and
    (not valid?)
    (some #(= % "required") (:validators field-descriptor))
    (validator/validate "required" value)))

(defn text-field [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [id (keyword (:id field-descriptor))
        value (subscribe [:state-query [:application :answers id :value]])
        valid? (subscribe [:state-query [:application :answers id :valid]])]
    (fn [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
      (let [size-class (text-field-size->class (get-in field-descriptor [:params :size]))]
        [div-kwd
         [label field-descriptor size-class]
         [:input.application__form-text-input
          (merge {:type        "text"
                  :placeholder (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                 (:fi input-hint))
                  :class       (str size-class (if (show-text-field-error-class? field-descriptor @value @valid?)
                                                 " application__form-field-error"
                                                 " application__form-text-input--normal"))
                  :value       @value
                  :on-change   (partial textual-field-change field-descriptor)}
                 (when disabled {:disabled true}))]]))))

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
       [label field-descriptor "application__form-text-area"]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         ; default-value because IE11 will "flicker" on input fields. This has side-effect of NOT showing any
         ; dynamically made changes to the text-field value.
         :default-value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor)}]])))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:div.application__wrapper-heading
    [:h2 (-> field-descriptor :label :fi)]
    [scroll-to-anchor field-descriptor]]
   (into [:div.application__wrapper-contents]
         (for [child children]
           [render-field child]))])

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
        ; flatten fields here because 'rowcontainer' may
        ; have nested fields because
        ; of validation (for example :one-of validator)
        (for [child (util/flatten-form-fields children)]
          [render-field child :div-kwd :div.application__row-field.application__form-field])))

(defn dropdown
  [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])]
    (r/create-class
      {:component-did-mount (partial init-dropdown-value field-descriptor)
       :reagent-render      (fn [field-descriptor]
                              [div-kwd
                               {:on-change (partial textual-field-change field-descriptor)}
                               [label field-descriptor "application__form-select-label"]
                               [:div.application__form-select-wrapper
                                [:span.application__form-select-arrow]
                                [:select.application__form-select
                                 {:value (textual-field-value field-descriptor @application)}
                                 (for [option (:options field-descriptor)]
                                   (let [value (get-in option [:label :fi])]
                                     ^{:key value}
                                     [:option {:value value} value]))]]])})))

(defn multiple-choice
  [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [multiple-choice-id (answer-key field-descriptor)
        answers            (subscribe [:state-query [:application :answers multiple-choice-id :value]])]
    (fn [field-descriptor]
      (let [answers @answers]
        [div-kwd
         [:div.application__form-outer-checkbox-container
          [:div ; This is the inner container, acts as the growing component for outer container
           (map (fn [option]
                  (let [label     (get-in option [:label :fi])
                        option-id (util/component-id)
                        value     (:value option)]
                    [:div {:key value}
                     [:input.application__form-checkbox
                      {:id        option-id
                       :type      "checkbox"
                       :checked   (and (not (nil? answers))
                                       (clojure.string/includes? answers value))
                       :value     value
                       :on-change (fn [event]
                                    (let [value (.. event -target -value)]
                                      (dispatch [:application/toggle-multiple-choice-option multiple-choice-id value])))}]
                     [:label
                      {:for option-id}
                      label]]))
                (:options field-descriptor))]]]))))

(defn render-field
  [field-descriptor & args]
  (let [ui (subscribe [:state-query [:application :ui]])
        visible? (fn [id]
                   (get-in @ui [(keyword id) :visible?] true))]
    (fn [field-descriptor & args]
      (let [disabled? (get-in @ui [(keyword (:id field-descriptor)) :disabled?] false)]
        (cond-> (match field-descriptor
                       {:fieldClass "wrapperElement"
                        :fieldType  "fieldset"
                        :children   children} [wrapper-field field-descriptor children]
                       {:fieldClass "wrapperElement"
                        :fieldType  "rowcontainer"
                        :children   children} [row-wrapper children]
                       {:fieldClass "formField"
                        :id         (_ :guard (complement visible?))} [:div]

                       {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor :disabled disabled?]
                       {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
                       {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor]
                       {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor])
                (and (empty? (:children field-descriptor))
                     (visible? (:id field-descriptor))) (into args))))))

(defn editable-fields [form-data]
  (when form-data
    (into [:div] (for [content (:content form-data)]
                   [render-field content]))))
