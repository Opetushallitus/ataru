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
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application children]
      (let [fieldset? (= "fieldset" (:fieldType content))]
        [:div
         (when fieldset?
           {:class "application__wrapper-element application__wrapper-element--border"})
         [:h2.application__wrapper-heading
          {:id (wrapper-id content)}
          (-> content :label :fi)]
         (into [:div (when fieldset? {:class "application__wrapper-contents"})]
               (for [child children
                     :when (get-in @ui [(keyword (:id child)) :visible?] true)]
                 [field child application]))]))))

(defn field [content application]
  (match content
         {:fieldClass "wrapperElement" :children children} [wrapper content application children]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown")} (text application content)))

(defn readonly-fields [form-data application]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [form-data application]
      (when form-data
        (into [:div.application__readonly-container]
              (for [content (:content form-data)
                    :when   (get-in @ui [(keyword (:id content)) :visible?] true)]
                [field content application]))))))
