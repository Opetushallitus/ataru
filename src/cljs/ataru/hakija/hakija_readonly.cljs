; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.hakija.hakija-readonly
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe]]
            [ataru.util :as util]
            [ataru.cljs-util :refer [console-log]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       get-value
                                                                       render-paragraphs
                                                                       replace-with-option-label
                                                                       predefined-value-answer?
                                                                       scroll-to-anchor
                                                                       question-group-answer?
                                                                       answers->read-only-format
                                                                       group-spacer]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- split-if-string [s]
  (if (string? s)
    (clojure.string/split s #"\s*,\s*")
    s))

(defn text [field-descriptor application lang group-idx]
  (let [answer (get-in application [:answers (keyword (:id field-descriptor))])]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
     (if (:cannot-view answer)
       [:div "***********"]
       [:div.application__readonly-text
        (let [values (cond-> (get-value answer group-idx)
                       (contains? field-descriptor :koodisto-source)
                       split-if-string
                       (predefined-value-answer? field-descriptor)
                       (replace-with-option-label (:options field-descriptor) lang))]
          (cond (and (sequential? values) (< 1 (count values)))
                [:ul.application__form-field-list
                 (map-indexed
                  (fn [i value]
                    ^{:key (str (:id field-descriptor) i)}
                    [:li (render-paragraphs value)])
                  values)]
                (sequential? values)
                (render-paragraphs (first values))
                :else
                (render-paragraphs values)))])]))

(defn- attachment-list [attachments]
  [:div
   (map (fn [{:keys [value]}]
          ^{:key (:key value)}
          [:ul.application__form-field-list (str (:filename value) " (" (util/size-bytes->str (:size value)) ")")])
        attachments)])

(defn attachment [field-descriptor application lang question-group-index]
  (let [answer-key (keyword (answer-key field-descriptor))
        values     (if question-group-index
                     (-> application
                         :answers
                         answer-key
                         :values
                         (nth question-group-index))
                     (-> application :answers answer-key :values))]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
     [attachment-list values]]))

(declare field)

(defn child-fields [children application lang ui question-group-id]
  (for [child children
        :when (get-in ui [(keyword (:id child)) :visible?] true)]
    [field child application lang question-group-id]))

(defn wrapper [content application lang children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
      [:div.application__wrapper-element.application__wrapper-element--border
       [:div.application__wrapper-heading
        [:h2 (-> content :label lang)]
        [scroll-to-anchor content]]
       (into [:div.application__wrapper-contents]
         (child-fields children application lang @ui nil))])))

(defn- question-group-row [application lang children idx]
  [:div.application__question-group-row
   [:div.application__question-group-row-content
    (for [child children]
      ^{:key (str (:id child) "-" idx)}
      [field child application lang idx])]])

(defn question-group [content application lang children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
      (let [groups-amount (->> content :id keyword (get @ui) :count)]
        [:div.application__wrapper-element.application__wrapper-element--border.application__question-group.application__read-only
         [:p.application__read-only-heading-text (-> content :label lang)]
         [:div
          (for [idx (range groups-amount)]
            ^{:key (str "question-group-row-" idx)}
            [question-group-row application lang children idx])]]))))

(defn row-container [application lang children question-group-index]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [application lang children question-group-index]
      (into [:div] (child-fields children application lang @ui question-group-index)))))

(defn- extract-values [children answers]
  (let [l?      (fn [x]
                  (or (list? x)
                      (vector? x)))
        answers (->> children
                     (map answer-key)
                     (map (comp (fn [values]
                                  (if (and (l? values)
                                           (every? l? values))
                                    (map (partial map :value) values)
                                    (map :value values)))
                                :values
                                (partial get answers))))]
    (if (question-group-answer? answers)
      (answers->read-only-format answers)
      (apply map vector answers))))

(defn- fieldset-answer-table [answers]
  [:tbody
   (doall
     (for [[idx values] (map vector (range) answers)]
       (into
         [:tr {:key (str idx "-" (apply str values))}]
         (for [value values]
           [:td.application__readonly-adjacent-cell (str value)]))))])

(defn fieldset [field-descriptor application lang children question-group-idx]
  (let [fieldset-answers (cond-> (extract-values children (:answers application))
                           question-group-idx
                           (nth question-group-idx))]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
     [:table.application__readonly-adjacent
      [:thead
       (into [:tr]
         (for [child children]
           [:th.application__readonly-adjacent--header (str (-> child :label lang)) (required-hint field-descriptor)]))]
      [fieldset-answer-table fieldset-answers]]]))

(defn- followup-has-answer?
  [followup application]
  (when-let [answer-value (:value ((answer-key followup) (:answers application)))]
    (and
      (boolean answer-value)
      (if (sequential? answer-value)
        (< 0 (count answer-value))
        true))))

(defn- followups [followups content application lang question-group-index]
  [:div
   (text content application lang question-group-index)
   (into [:div]
     (for [followup followups
           :let [followup-is-visible? (get-in @(subscribe [:state-query [:application :ui]]) [(keyword (:id followup)) :visible?])]
           :when (if (boolean? followup-is-visible?)
                   followup-is-visible?
                   (followup-has-answer? followup application))]
       [:div
        [field followup application lang question-group-index]]))])

(defn- selected-hakukohde-row
  [hakukohde-oid]
  [:div.application__hakukohde-row
   [:div.application__hakukohde-row-text-container
    [:div.application__hakukohde-selected-row-header
     @(subscribe [:application/hakukohde-label hakukohde-oid])]
    [:div.application__hakukohde-selected-row-description
     @(subscribe [:application/hakukohde-description hakukohde-oid])]]])

(defn- hakukohde-selection-header
  [content]
  [:div.application__wrapper-heading.application__wrapper-heading-block
   [:h2 @(subscribe [:application/hakukohteet-header])]
   [scroll-to-anchor content]])

(defn- hakukohteet
  [content]
  [:div.application__wrapper-element.application__wrapper-element-border
   [hakukohde-selection-header content]
   [:div.application__hakukohde-selected-list
    (for [hakukohde-oid @(subscribe [:application/selected-hakukohteet])]
      ^{:key (str "selected-hakukohde-row-" hakukohde-oid)}
      [selected-hakukohde-row hakukohde-oid])]])

(defn field
  [content application lang question-group-index]
  (match content
         {:fieldClass "wrapperElement" :module "person-info" :children children} [wrapper content application lang children]
         {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application lang children]
         {:fieldClass "questionGroup" :fieldType "fieldset" :children children} [question-group content application lang children]
         {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children question-group-index]
         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children question-group-index]
         {:fieldClass "formField" :exclude-from-answers true} nil
         {:fieldClass "infoElement"} nil
         {:fieldClass "formField" :fieldType (:or "dropdown" "multipleChoice" "singleChoice") :options (options :guard util/followups?)}
         [followups (mapcat :followups options) content application lang question-group-index]
         {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown" "multipleChoice" "singleChoice")} (text content application lang question-group-index)
         {:fieldClass "formField" :fieldType "attachment"} [attachment content application lang question-group-index]
         {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet content]))

(defn- application-language [{:keys [lang]}]
  (when (some? lang)
    (-> lang
        clojure.string/lower-case
        keyword)))

(defn- visible? [ui field-descriptor]
  (and (get-in @ui [(keyword (:id field-descriptor)) :visible?] true)
       (or (empty? (:children field-descriptor))
           (some (partial visible? ui) (:children field-descriptor)))))

(defn readonly-fields [form application]
  (when form
    (let [lang (or (:selected-language form)                ; languages is set to form in the applicant side
                   (application-language application)       ; language is set to application when in officer side
                   :fi)
          ui   (subscribe [:state-query [:application :ui]])]
      (into [:div.application__readonly-container]
        (for [content (:content form)
              :when (visible? ui content)]
          [field content application lang])))))
