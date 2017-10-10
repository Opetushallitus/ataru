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
            [ataru.translations.application-view :refer [application-view-translations]]
            [ataru.translations.translation-util :refer [get-translations]]
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       textual-field-value
                                                                       scroll-to-anchor
                                                                       question-group-answer?
                                                                       answers->read-only-format
                                                                       value-or-koodi-uri->label
                                                                       group-spacer]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn- multiple-choice-with-koodisto [field-descriptor]
  (and (= (:fieldType field-descriptor) "multipleChoice")
       (contains? field-descriptor :koodisto-source)))

(defn- multi-values->:li
  [field-descriptor lang values]
  (map-indexed (fn [idx value-or-koodi-uri]
                 (let [value (str (if (map? value-or-koodi-uri)
                                    (:value value-or-koodi-uri)
                                    value-or-koodi-uri))]
                   ^{:key (str "value-" idx)}
                   [:li (value-or-koodi-uri->label field-descriptor lang value)]))
               values))

(defn text [field-descriptor application lang question-group-index]
  (let [answer            ((answer-key field-descriptor) (:answers application))
        values (if question-group-index
                 (-> answer :values (nth question-group-index))
                 (:values answer))]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
     (if (:cannot-view answer)
       [:div "***********"]
       [:div.application__readonly-text
        (cond (and (vector? values)
                   (every? vector? values)
                   (< 1 (count values)))
              (into [:ul.application__form-field-list]
                    (map #(multi-values->:li field-descriptor lang %) values))

              (and (vector? values)
                   (some (comp not vector?) values)
                   (< 1 (count values)))
              (into [:ul.application__form-field-list]
                    (multi-values->:li field-descriptor lang values))

              :else
              (textual-field-value field-descriptor application :lang lang :question-group-index question-group-index))])]))

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

(defn question-group [content application lang children]
  (let [ui (subscribe [:state-query [:application :ui]])]
    (fn [content application lang children]
      (let [answers       (-> application
                              :answers
                              (select-keys (map (comp keyword :id) children)))
            groups-amount (->> content :id keyword (get @ui) :count)]
        [:div.application__wrapper-element.application__wrapper-element--border.application__question-group.application__read-only
         [:div.application__wrapper-heading.application__question-group-wrapper-heading]
         (into [:div.application__wrapper-contents.application__question-group-wrapper-contents
                [:p.application__read-only-heading-text (-> content :label lang)]]
               (mapcat
                 (fn [group-index]
                   (conj
                     (mapv
                       (fn [child]
                         ^{:key (str (:id child) "-" group-index)}
                         [field child application lang group-index])
                       children)
                     (when (< group-index (dec groups-amount))
                       [group-spacer group-index])))
                 (range groups-amount)))]))))

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

(defn fieldset [field-descriptor application lang children]
  (let [fieldset-answers (extract-values children (:answers application))]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
     [:table.application__readonly-adjacent
      [:thead
       (into [:tr]
         (for [child children]
           [:th.application__readonly-adjacent--header (str (-> child :label lang)) (required-hint field-descriptor)]))]
      (if (question-group-answer? fieldset-answers)
        (map-indexed (fn [idx fieldset-answers]
                       ^{:key (str (:id field-descriptor) "-" idx)}
                       [fieldset-answer-table fieldset-answers])
                     fieldset-answers)
        [fieldset-answer-table fieldset-answers])]]))

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
         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children]
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

(defn readonly-fields [form application]
  (when form
    (let [lang (or (:selected-language form)                ; languages is set to form in the applicant side
                   (application-language application)       ; language is set to application when in officer side
                   :fi)]
      (into [:div.application__readonly-container]
        (for [content (:content form)
              :when (get-in @(subscribe [:state-query [:application :ui]]) [(keyword (:id content)) :visible?] true)]
          [field content application lang])))))
