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

(defn text [field-descriptor application lang]
  [:div.application__form-field
   [:label.application__form-field-label
    (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
   [:div
    (or
      (let [values (:value ((answer-key field-descriptor) (:answers application)))]
        (when (or (seq? values) (vector? values))
          (into [:ul.application__form-field-list] (for [value values] [:li value]))))
      (textual-field-value field-descriptor application :lang lang))]])

(declare field)

(defn child-fields [children application lang ui]
  (for [child children
        :when (get-in ui [(keyword (:id child)) :visible?] true)]
    [field child application lang]))

(defn wrapper [content application lang children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
        [:div.application__wrapper-element.application__wrapper-element--border
         [:div.application__wrapper-heading
          [:h2 (-> content :label lang)]
          [scroll-to-anchor content]]
         (into [:div.application__wrapper-contents]
               (child-fields children application lang @ui))])))

(defn row-container [application lang children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [application lang children]
      (into [:div] (child-fields children application lang @ui)))))

(defn field [content application lang]
  (match content
         {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application lang children]
         {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children]
         {:fieldClass "formField" :exclude-from-answers true} nil
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown" "multipleChoice")} (text content application lang)))

(defn- application-language [{:keys [lang]}]
  (when (some? lang)
    (-> lang
        clojure.string/lower-case
        keyword)))

(defn readonly-fields [form application]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [form application]
      (when form
        (let [lang (or (:selected-language form)          ; languages is set to form in the applicant side
                       (application-language application) ; language is set to application when in officer side
                       :fi)]
          (into [:div.application__readonly-container]
            (for [content (:content form)
                  :when (get-in @ui [(keyword (:id content)) :visible?] true)]
              [field content application lang])))))))
