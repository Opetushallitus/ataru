; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.application-common.application-readonly
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       textual-field-value
                                                                       scroll-to-anchor]]))

(defn text [application field-descriptor]
  [:div.application__form-field
   [:label.application__form-field-label
    (str (-> field-descriptor :label :fi) (required-hint field-descriptor))]
   [:div (textual-field-value field-descriptor application)]])

(declare field)

(defn child-fields [children application ui]
  (for [child children
        :when (get-in ui [(keyword (:id child)) :visible?] true)]
    [field child application]))

(defn wrapper [content application children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application children]
        [:div.application__wrapper-element.application__wrapper-element--border
         [:div.application__wrapper-heading
          [:h2 (-> content :label :fi)]
          [scroll-to-anchor content]]
         (into [:div.application__wrapper-contents]
               (child-fields children application @ui))])))

(defn row-container [application children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [application children]
      (into [:div] (child-fields children application @ui)))))

(defn field [content application]
  (match content
         {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application children]
         {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application children]
         {:fieldClass "formField" :exclude-from-answers true} nil
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown" "multipleChoice")} (text application content)))

(defn readonly-fields [form-data application]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [form-data application]
      (when form-data
        (into [:div.application__readonly-container]
          (for [content (:content form-data)
                :when   (get-in @ui [(keyword (:id content)) :visible?] true)]
                [field content application]))))))
