(ns ataru.application-common.application-field-common
  (:require [markdown.core :refer [md->html]]
            [markdown.transformers :refer [transformer-vector]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clojure.string :as string]
            [goog.string :as s]
            [ataru.util :as util]
            [ataru.cljs-util :refer [valid-uuid?]]
            [ataru.translations.translation-util :as translations])
  (:import (goog.html.sanitizer HtmlSanitizer)))

(defn answer-key [field-data]
  (keyword (:id field-data)))

(def required-validators
  #{"required"
    "required-hakija"
    "postal-code"
    "postal-office"
    "home-town"
    "city"
    "hakukohteet"
    "birthplace"})

(def contains-required-validators? (partial contains? required-validators))

(defonce builder (new HtmlSanitizer.Builder))
(defonce html-sanitizer (.build builder))

(defonce application-identifier-block-start-tag "BEGIN_APPLICATION_IDENTIFIER")
(defonce application-identifier-block-end-tag "END_APPLICATION_IDENTIFIER")

(defn- handle-application-identifier-block-text
  [application-identifier text]
  (if (and application-identifier text)
    (string/replace text "$APPLICATION_IDENTIFIER" application-identifier)
    ""))

(defn- split-first [s tag]
  (clojure.string/split s (re-pattern tag) 2))

(defn- application-identifier-block
  ([application-identifier text-before-block text-in-block {:keys [eof] :as state}]
   (let [[in-block outside-block] (split-first text-in-block application-identifier-block-end-tag)]
     [(str text-before-block
           (handle-application-identifier-block-text application-identifier in-block)
           outside-block)
      (if (or outside-block eof)
        (dissoc state :in-application-identifier-block)
        state)]))
  ([application-identifier text {:keys [in-application-identifier-block] :as state}]
   (if in-application-identifier-block
     (application-identifier-block application-identifier nil text state)
     (let [[before after] (split-first text application-identifier-block-start-tag)]
       (if after
         (application-identifier-block application-identifier before after
           (assoc state :in-application-identifier-block true))
         [text state])))))

(defn- add-link-target-prop
  [text state]
  [(string/replace text #"<a href=([^>]+)>" "<a target=\"_blank\" href=$1>") state])

(defn- set-markdown-height
  [component scroll-height]
  (reset! scroll-height (-> component
                            reagent/dom-node
                            (.getElementsByClassName "application__form-info-text-inner")
                            (aget 0)
                            .-scrollHeight)))

(defn markdown-paragraph
  ([md-text]
   (markdown-paragraph md-text false nil))
  ([md-text collapse-enabled? application-identifier]
   (let [collapsed        (reagent/atom true)
         scroll-height    (reagent/atom nil)
         listener         (reagent/atom nil)
         timeout          (atom nil)
         debounced-resize (fn [component]
                            (js/clearTimeout @timeout)
                            (reset!
                             timeout
                             (js/setTimeout #(set-markdown-height component scroll-height) 200)))
         lang             (re-frame/subscribe [:application/form-language])]
     (reagent/create-class
      {:component-did-mount
       (fn [component]
         (set-markdown-height component scroll-height)
         (reset! listener #(debounced-resize component))
         (.addEventListener js/window "resize" @listener))

       :component-will-unmount
       (fn [_]
         (.removeEventListener js/window "resize" @listener))

       :component-did-update
       (fn [component]
         (set-markdown-height component scroll-height))

       :reagent-render
       (fn []
         (let [sanitized-html (as-> md-text v
                                (md->html v
                                          :replacement-transformers (concat [(partial application-identifier-block application-identifier)]
                                                                            transformer-vector
                                                                            [add-link-target-prop]))
                                (.sanitize html-sanitizer v)
                                (.getTypedStringValue v))
               collapsable?   (and collapse-enabled? (< 140 (or @scroll-height 0)))]
           [:div.application__form-info-text
            (if (and collapsable? @collapsed)
              [:div.application__form-info-text-inner.application__form-info-text-inner--collapsed
               {:dangerouslySetInnerHTML {:__html sanitized-html}}]
              [:div.application__form-info-text-inner
               {:style                   {:height @scroll-height}
                :dangerouslySetInnerHTML {:__html sanitized-html}}])
            (when collapsable?
              [:button.application__form-info-text-collapse-button
               {:on-click (fn [] (swap! collapsed not))}
               (if @collapsed
                 [:span (str (translations/get-hakija-translation
                               :read-more
                               @lang)
                             " ")
                  [:i.zmdi.zmdi-hc-lg.zmdi-chevron-down]]
                 [:span (str (translations/get-hakija-translation
                               :read-less
                               @lang)
                             " ")
                  [:i.zmdi.zmdi-hc-lg.zmdi-chevron-up]])])]))}))))

(defn render-paragraphs [s]
  (->> (clojure.string/split s "\n")
       (map-indexed (fn [i p]
                      ^{:key (str "paragraph-" i)}
                      [:div
                       (if (clojure.string/blank? p)
                         [:br]
                         [:p.application__text-field-paragraph p])]))))

(defn is-required-field?
  [field-descriptor]
  (if (contains? field-descriptor :children)
    (some is-required-field? (:children field-descriptor))
    (some contains-required-validators? (:validators field-descriptor))))

(defn required-hint
  [field-descriptor]
  (if (is-required-field? field-descriptor)
    " *"
    ""))

(defn get-value [answer group-idx]
  (if-let [value (:value answer)]
    (cond-> value
      (some? group-idx)
      (nth group-idx nil))
    (map :value (cond-> (:values answer)
                  (some? group-idx)
                  (nth group-idx)))))

(defn replace-with-option-label
  [values options lang]
  (if (sequential? values)
    (map #(replace-with-option-label % options lang) values)
    (let [option (some #(when (= values (:value %)) %) options)]
      (or (util/non-blank-val (:label option) [lang :fi :sv :en])
          values))))

(defn predefined-value-answer?
  "Does the answer have predefined values? Form elements like dropdowns
   and single and multi-choice buttons have fixed, predefined values, as
   opposed to a text input field where an user can provide anything as
   the answer."
  [{:keys [fieldClass fieldType options]}]
  (and (= fieldClass "formField")
       (some #{fieldType} ["dropdown" "singleChoice" "multipleChoice"])
       (not-empty options)))

(defn scroll-to-anchor
  [field-descriptor]
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} "."])

(defn question-group-answer? [answers]
  (letfn [(l? [x]
            (or (list? x)
                (vector? x)
                (seq? x)))]
    (and (every? l? answers)
         (every? (partial every? l?) answers))))

(defn answers->read-only-format
  "Converts format of repeatable answers in a question group from the one
   stored by a form component into a format required by read-only views.

   Let adjacent fieldset with repeatable answers in a question group:

   Group 1:
   a1 - b1 - c1
   a2 - b2 - c2

   Group 2:
   d1 - e1 - f1

   This reduce converts:
   ([[\"a1\" \"a2\"] [\"d1\"]] [[\"b1\" \"b2\"] [\"e1\"]] [[\"c1\" \"c2\"] [\"f1\"]])

   to:
   [[[\"a1\" \"b1\" \"c1\"] [\"a2\" \"b2\" \"c2\"]] [[\"d1\" \"e1\" \"f1\"]]]"
  [answers]
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
            (map vector (range) answers))))

(defn copy [id]
  (let [copy-container (.getElementById js/document "editor-form__copy-question-id-container")]
    (set! (.-value copy-container) id)
    (.select copy-container)
    (.execCommand js/document "copy")))

(defn copy-link [id & {:keys [shared-use-warning? answer? include?]}]
  (let [id (cond-> id
                   keyword?
                   (name))]
    (when-not (and include?
                   (not (include? id)))
      [:div.editor-form__id-container
       [:a.editor-form__copy-question-id
        {:data-tooltip  (s/format @(re-frame/subscribe [:editor/virkailija-translation (if answer? :copy-answer-id :copy-question-id)])
                          id)
         :on-mouse-down #(copy id)}
        "id"]
       (when (and (not (false? shared-use-warning?)) (not (valid-uuid? id)))
         [:span.editor-form__id-fixed
          @(re-frame/subscribe [:editor/virkailija-translation :id-in-shared-use])])])))

(def ^:private field-types-supporting-label-for
  "These field types can use the <label for=..> syntax, others will use aria-labelled-by"
  #{"textField" "textArea" "dropdown"})

(defn id-for-label
  [field-descriptor]
  (when-not (contains? field-types-supporting-label-for (:fieldType field-descriptor))
    (str "application-form-field-label-" (:id field-descriptor))))
