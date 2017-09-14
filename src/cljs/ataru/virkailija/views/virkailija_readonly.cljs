; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.virkailija.views.virkailija-readonly
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
                                                                       multi-value-answers?]]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]))

(defn text [field-descriptor application lang]
  [:div.application__form-field
   [:label.application__form-field-label
    (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
   [:div
    (let [answer       ((answer-key field-descriptor) (:answers application))
          values       (:value answer)
          multi-value? (or (seq? values) (vector? values))]
      (if multi-value?
        (into [:ul.application__form-field-list] (for [value values] [:li value]))
        (textual-field-value field-descriptor application :lang lang)))]])

(defn attachment [field-descriptor application lang]
  (when (fc/feature-enabled? :attachment)
    (let [answer-key (keyword (answer-key field-descriptor))
          values     (get-in application [:answers answer-key :value])]
      [:div.application__form-field
       [:label.application__form-field-label
        (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
       [:div
        (map-indexed (fn attachment->link [idx {file-key :key filename :filename size :size virus-scan-status :virus-scan-status}]
                       (let [text              (str filename " (" (util/size-bytes->str size) ")")
                             component-key     (str "attachment-div-" idx)
                             virus-status-elem (case virus-scan-status
                                                 "not_started" [:span.application__virkailija-readonly-attachment-virus-status-not-started
                                                                " | Tarkastetaan..."]
                                                 "failed" [:span.application__virkailija-readonly-attachment-virus-status-virus-found
                                                           " | Virus löytyi"]
                                                 "done" nil
                                                 "Virhe")]
                         [:div.application__virkailija-readonly-attachment-text
                          {:key component-key}
                          (if (= virus-scan-status "done")
                            [:a {:href (str "/lomake-editori/api/files/content/" file-key)}
                             text]
                            text)
                          virus-status-elem]))
                     values)]])))

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

(defn- extract-values [children answers]
  (let [child-answers  (->> (map answer-key children)
                            (select-keys answers))
        ; applicant side stores values as hashmaps
        applicant-side (map (comp
                              (fn [values]
                                (map :value values))
                              :values
                              second))
        ; editor side loads values as vectors of strings
        editor-side    (map (comp :value second))]
    (when-let [concatenated-answers (->>
                                      (concat
                                        (eduction applicant-side child-answers)
                                        (eduction editor-side child-answers))
                                      (filter not-empty)
                                      not-empty)]
      (if (multi-value-answers? concatenated-answers)
        ;; Let adjacent fieldset with repeatable answers in a question group:
        ;; Group 1:
        ;; a1 - b1 - c1
        ;; a2 - b2 - c2
        ;; Group 2:
        ;; d1 - e1 - f1
        ;; This reduce converts
        ;; ([["a1" "a2"] ["d1"]] [["b1" "b2"] ["e1"]] [["c1" "c2"] ["f1"]])
        ;; to
        ;; [[["a1" "b1" "c1"] ["a2" "b2" "c2"]] [["d1" "e1" "f1"]]]
        (let [val-or-empty-vec (fnil identity [])]
          (reduce (fn [acc [col-idx answers]]
                    (reduce (fn [acc [question-group-idx answers]]
                              (reduce (fn [acc [row-idx answer]]
                                        (-> acc
                                            (update-in [question-group-idx row-idx] val-or-empty-vec)
                                            (assoc-in [question-group-idx row-idx col-idx] answer)))
                                      (update acc question-group-idx val-or-empty-vec)
                                      (map vector (range) answers)))
                            acc
                            (map vector (range) answers)))
                  []
                  (map vector (range) concatenated-answers)))
        (apply map vector concatenated-answers)))))

(defn- fieldset-answer-table [answers]
  [:tbody
   (doall
     (for [[idx values] (map vector (range) answers)]
       (into
         [:tr {:key (str idx "-" (apply str values))}]
         (for [value values]
           [:td (str value)]))))])


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
      (if (multi-value-answers? fieldset-answers)
        (map-indexed (fn [idx fieldset-answers]
                       ^{:key (str (:id field-descriptor) "-" idx)}
                       [fieldset-answer-table fieldset-answers])
                     fieldset-answers)
        [fieldset-answer-table fieldset-answers])]]))

(defn- followup-has-answer?
  [followup application]
  (if (not-empty (:children followup))
    (some #(followup-has-answer? % application) (:children followup))
    (when-let [answer-value (:value ((answer-key followup) (:answers application)))]
      (and
        (boolean answer-value)
        (if (sequential? answer-value)
          (< 0 (count answer-value))
          true)))))

(defn- followups [followups content application lang]
  [:div
   (text content application lang)
   (into [:div]
     (for [followup followups
           :let [followup-is-visible? (get-in @(subscribe [:state-query [:application :ui]]) [(keyword (:id followup)) :visible?])]
           :when (if (boolean? followup-is-visible?)
                   followup-is-visible?
                   (followup-has-answer? followup application))]
       [:div
        [field followup application lang]]))])

(defn- hakukohteet-list-row [hakukohde-oid]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper
    [:div.application-handling__review-area-hakukohde-heading
     @(subscribe [:application/hakukohde-label hakukohde-oid])]
    [:div.application-handling__review-area-koulutus-heading
     @(subscribe [:application/hakukohde-description hakukohde-oid])]]])

(defn- hakukohteet [content application]
  (let [hakukohteet-by-oid (into {} (map (juxt :value identity) (:options content)))
        hakukohteet (map hakukohteet-by-oid
                         (get-in application [:answers :hakukohteet :value] []))]
    [:div.application__wrapper-element.application__wrapper-element--border
     [:div.application__wrapper-heading
      [:h2 @(subscribe [:application/hakukohteet-header])]
      [scroll-to-anchor content]]
     [:div.application__wrapper-contents
      (for [hakukohde-oid @(subscribe [:application/hakukohteet])]
        ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
        [hakukohteet-list-row hakukohde-oid])]]))

(defn field [{field-hakukohteet :belongs-to-hakukohteet :as content}
             {application-hakukohteet :hakukohde :as application}
             lang]
  ;; render the field if either
  ;; 1) the field isn't a hakukohde specific question
  ;; 2) the field is a hakukohde specific question and the user has applied to one of
  ;;    those hakukohteet to whom the field belongs to
  (when (or (empty? field-hakukohteet)
            (not-empty (clojure.set/intersection (set field-hakukohteet)
                                                 (set application-hakukohteet))))
    (match content
           {:fieldClass (:or "wrapperElement" "questionGroup") :fieldType "fieldset" :children children} [wrapper content application lang children]
           {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children]
           {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children]
           {:fieldClass "formField" :exclude-from-answers true} nil
           {:fieldClass "infoElement"} nil
           {:fieldClass "formField" :fieldType (:or "dropdown" "multipleChoice" "singleChoice") :options (options :guard util/followups?)}
           [followups (mapcat :followups options) content application lang]
           {:fieldClass "formField" :fieldType (:or "textField" "textArea" "dropdown" "multipleChoice" "singleChoice")} (text content application lang)
           {:fieldClass "formField" :fieldType "attachment"} [attachment content application lang]
           {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet content application])))

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
