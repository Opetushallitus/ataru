(ns ataru.application-common.application-field-common
  (:require [markdown.core :refer [md->html]]
            [markdown.transformers :refer [transformer-vector]]
            [reagent.core :as reagent]
            [reagent.dom :as reagent-dom]
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
                            reagent-dom/dom-node
                            (.getElementsByClassName "application__form-info-text-inner")
                            (aget 0)
                            .-scrollHeight)))

(defn markdown-paragraph-2
  ([md-text]
   (markdown-paragraph-2 md-text false nil))
  ([md-text collapse-enabled? application-identifier]
   (let [collapsed        (reagent/atom true)
         scroll-height    (reagent/atom nil)
         lang             (re-frame/subscribe [:application/form-language])
         sanitized-html (as-> md-text v
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
                {:style                   {:height "auto"}
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
                   [:i.zmdi.zmdi-hc-lg.zmdi-chevron-up]])])])))

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
           (prn "reagender-render")
           (prn md-text)
           (prn sanitized-html)
           [:div.application__form-info-text
            (if (and collapsable? @collapsed)
              [:div.application__form-info-text-inner.application__form-info-text-inner--collapsed
               {:dangerouslySetInnerHTML {:__html sanitized-html}}]
              [:div.application__form-info-text-inner
               {:style                   {:height "auto"}
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

 (defn- urls-to-links
  [content]
  (let [urls (re-seq #"https?://\S+" content)]
    (reduce
     #(string/replace-first %1 %2 (str "<a href=\"" %2 "\" target=_blank rel=\"noopener noreferrer\">" %2 "</a>"))
     content
     urls)))

(defn render-paragraphs [s]
  (->> (clojure.string/split s "\n")
       (map urls-to-links)
       (map-indexed (fn [i p]
                      ^{:key (str "paragraph-" i)}
                      [:div
                       (if (clojure.string/blank? p)
                         [:br]
                         [:p.application__text-field-paragraph
                          {:dangerouslySetInnerHTML {:__html p}}])]))))

(defn is-required-field?
  [field-descriptor]
  (if (contains? field-descriptor :children)
    (some is-required-field? (:children field-descriptor))
    (some contains-required-validators? (:validators field-descriptor))))

(defn required-hint
  [field-descriptor lang]
  (if (is-required-field? field-descriptor)
    (str " " (translations/get-hakija-translation :required lang)) ""))

(defn virkailija-required-hint
  [field-descriptor]
  (if (is-required-field? field-descriptor)
    " *" ""))

(defn get-value [answer group-idx]
  (if (some? group-idx)
    (get-in answer [:value group-idx])
    (:value answer)))

(defn replace-with-option-label
  [values options lang]
  (if (vector? values)
    (mapv #(replace-with-option-label % options lang) values)
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
  [:span.application__scroll-to-anchor {:id (str "scroll-to-" (:id field-descriptor))} ""])

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

(defn copy-link-selectable [id & {:keys [shared-use-warning? answer?]}]
  (let [id (cond-> id
             keyword?
             (name))]
      [:div.editor-form__id-container
       [:a.editor-form__copy-question-id
        {:data-tooltip  (s/format @(re-frame/subscribe [:editor/virkailija-translation (if answer? :copy-answer-id :copy-question-id)])
                                  id)
         :on-mouse-down #(copy id)}
        "id"]
       (when (and (not (false? shared-use-warning?)) (not (valid-uuid? id)))
         [:span.editor-form__id-fixed
          @(re-frame/subscribe [:editor/virkailija-translation :id-in-shared-use])])]))

(defn form-field-id
  [field-descriptor idx]
  (str (when idx (str idx "-")) (:id field-descriptor)))

(defn id-for-label
  [field-descriptor idx]
  (str "application-form-field-label-" (form-field-id field-descriptor idx)))

(defn belongs-to-hakukohde-or-ryhma? [field]
  (seq (concat (:belongs-to-hakukohteet field)
               (:belongs-to-hakukohderyhma field))))

(defn pad [n coll val]
  (vec (take (max (count coll) n) (concat coll (repeat val)))))

(defn sanitize-value [field-descriptor value question-group-highest-dimension]
  (let [keep-allowed-values (fn [allowed-values values]
                              (if (nil? values)
                                values
                                (filterv allowed-values values)))
        keep-allowed-question-group-values (fn [allowed-values values]
                                             (mapv (partial keep-allowed-values allowed-values) values))
        is-options-type-field? (fn [descriptor] (and (not-empty (:options descriptor))
                                                           (#{"dropdown" "multipleChoice" "singleChoice"} (:fieldType descriptor))))
        is-question-group-value? (fn [value] (or (vector? (first value)) (nil? (first value))))]
    ; Fields with allowed options get filtered and nil padded
    (if (is-options-type-field? field-descriptor)
      (let [allowed-values (set (map :value (:options field-descriptor)))]
        (if (vector? value)
          (if (is-question-group-value? value)
            (pad (or question-group-highest-dimension 0)
                 (keep-allowed-question-group-values allowed-values value)
                 nil)
            (keep-allowed-values allowed-values value))
          (allowed-values value)))
      ; Other "freeform" fields just get nil padded when necessary
      (if (and (= "formField" (:fieldClass field-descriptor))
               (vector? value)
               (is-question-group-value? value))
        (pad (or question-group-highest-dimension 0) value nil)
        value))))
