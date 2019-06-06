(ns ataru.application-common.application-field-common
  (:require [markdown.core :refer [md->html]]
            [markdown.transformers :refer [transformer-vector]]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [goog.string :as s]
            [ataru.util :as util]
            [ataru.cljs-util :refer [get-translation get-virkailija-translation]])
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

(defonce personblock-start-tag "BEGIN_PERSON")
(defonce personblock-end-tag "END_PERSON")

(defn- handle-person-block-text
  [person-oid text]
  (if (and person-oid text)
    (string/replace text "$PERSON_OID" person-oid)
    ""))

(defn- split-first [s tag]
  (clojure.string/split s (re-pattern tag) 2))

(defn- person-block
  ([person-oid text-before-block text-in-block {:keys [eof] :as state}]
   (let [[in-block outside-block] (split-first text-in-block personblock-end-tag)]
     [(str text-before-block
           (handle-person-block-text person-oid in-block)
           outside-block)
      (if (or outside-block eof)
        (dissoc state :personblock)
        state)]))
  ([person-oid text {:keys [personblock] :as state}]
   (if personblock
     (person-block person-oid nil text state)
     (let [[before after] (split-first text personblock-start-tag)]
       (if after
         (person-block person-oid before after
           (assoc state :personblock true))
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
  ([md-text collapse-enabled? person-oid]
   (let [collapsed        (reagent/atom true)
         scroll-height    (reagent/atom nil)
         listener         (reagent/atom nil)
         timeout          (atom nil)
         debounced-resize (fn [component]
                            (js/clearTimeout @timeout)
                            (reset!
                             timeout
                             (js/setTimeout #(set-markdown-height component scroll-height) 200)))]
     (reagent/create-class
      {:component-did-mount
       (fn [component]
         (set-markdown-height component scroll-height)
         (reset! listener #(debounced-resize component))
         (.addEventListener js/window "resize" @listener))

       :component-will-unmount
       (fn [component]
         (.removeEventListener js/window "resize" @listener))

       :component-did-update
       (fn [component]
         (set-markdown-height component scroll-height))

       :reagent-render
       (fn []
         (let [sanitized-html (as-> md-text v
                                (md->html v
                                  :replacement-transformers (cons (partial person-block person-oid)
                                                                  transformer-vector)
                                  :custom-transformers [add-link-target-prop])
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
                 [:span (str (get-translation :read-more) " ") [:i.zmdi.zmdi-hc-lg.zmdi-chevron-down]]
                 [:span (str (get-translation :read-less) " ") [:i.zmdi.zmdi-hc-lg.zmdi-chevron-up]])])]))}))))

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

(defn- get-koodi [field-descriptor koodi-uri]
  (->> (:options field-descriptor)
       (filter (fn [koodi]
                 (= (:value koodi) koodi-uri)))
       first))

(defn- value-or-koodi-uri->label [field-descriptor lang value-or-koodi-uri]
  (if-let [koodi (get-koodi field-descriptor value-or-koodi-uri)]
    (get-in koodi [:label lang])
    value-or-koodi-uri))

(defn- wrap-value [value]
  ^{:key value}
  [:li value])

(defn get-value [answer group-idx]
  (if-let [value (:value answer)]
    (cond-> value
      (some? group-idx)
      (nth group-idx))
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

(defn copy-link [id & {:keys [answer? include?]}]
  (let [id (cond-> id
                   keyword?
                   (name))]
    (when-not (and include?
                   (not (include? id)))
      [:a.editor-form__copy-question-id
       {:data-tooltip  (s/format (get-virkailija-translation (if answer? :copy-answer-id :copy-question-id))
                         id)
        :on-mouse-down #(copy id)}
       "id"])))
