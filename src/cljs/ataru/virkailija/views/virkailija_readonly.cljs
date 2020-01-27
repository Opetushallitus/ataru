; Note: the css classes used below have different css implementations
; for virkailija and hakija:
; * virkailija-application.less
; * hakija.less
; This is on purpose, the UI layouts will differ
; in the future and already do to some extent.

(ns ataru.virkailija.views.virkailija-readonly
  (:require [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       get-value
                                                                       render-paragraphs
                                                                       replace-with-option-label
                                                                       predefined-value-answer?
                                                                       scroll-to-anchor
                                                                       copy-link
                                                                       question-group-answer?
                                                                       answers->read-only-format]]
            [ataru.feature-config :as fc]
            [ataru.component-data.component-util :refer [answer-to-always-include?]]
            [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch]]
            [cljs.core.match :refer-macros [match]]
            [clojure.string :refer [trim]]
            [goog.string :as s]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :refer-macros [spy debug]]
            [reagent.core :as r]))

(def exclude-always-included #(not (answer-to-always-include? %)))

(defn- from-multi-lang [text lang]
  (util/non-blank-val text [lang :fi :sv :en]))

(defn- belongs-to-hakukohderyhma? [field application]
  (let [hakukohteet             (-> application :hakukohde set)
        applied-hakukohderyhmat (->> (-> application :tarjonta :hakukohteet)
                                     (filter #(contains? hakukohteet (:oid %)))
                                     (mapcat :hakukohderyhmat)
                                     set)]
    (not-empty (clojure.set/intersection (-> field :belongs-to-hakukohderyhma set)
                                         applied-hakukohderyhmat))))

(defn- belongs-to-hakukohde? [field application]
  (not-empty (clojure.set/intersection (set (:belongs-to-hakukohteet field))
                                       (set (:hakukohde application)))))

(defn- visible? [field-descriptor application]
  (and (not (get-in field-descriptor [:params :hidden] false))
       (not= "infoElement" (:fieldClass field-descriptor))
       (not (:exclude-from-answers field-descriptor))
       (or (and (empty? (:belongs-to-hakukohteet field-descriptor))
                (empty? (:belongs-to-hakukohderyhma field-descriptor)))
           (belongs-to-hakukohde? field-descriptor application)
           (belongs-to-hakukohderyhma? field-descriptor application))
       (or (empty? (:children field-descriptor))
           (some #(visible? % application) (:children field-descriptor)))))

(defn- should-highlight-form-change? [id]
  (let [form-changes @(subscribe [:application/form-changes])
        showing-latest (not (some? @(subscribe [:state-query [:application :latest-form]])))]
    (and showing-latest (contains? form-changes (name id)))))

(defn text [field-descriptor application lang group-idx]
  (let [id               (keyword (:id field-descriptor))
        use-onr-info?    (contains? (:person application) id)
        values           (replace-with-option-label (if use-onr-info?
                                                      (-> application :person id)
                                                      (get-value (-> application :answers id) group-idx))
                                                    (:options field-descriptor)
                                                    lang)
        highlight-field? (subscribe [:application/field-highlighted? id])
        class (clojure.string/join " " [(when @highlight-field? "highlighted") (when (should-highlight-form-change? id) "form-highlighted")])]

    [:div.application__form-field
     {:class class :id id}
     [:label.application__form-field-label
      [:span
      (str (from-multi-lang (:label field-descriptor) lang)
           (required-hint field-descriptor))
        [copy-link id :shared-use-warning? false :include? exclude-always-included]]]
     [:div.application__form-field-value
      (cond (and (sequential? values) (< 1 (count values)))
            [:ul.application__form-field-list
             (map-indexed
              (fn [i value]
                ^{:key (str id i)}
                [:li (render-paragraphs value)])
              values)]
            (sequential? values)
            (render-paragraphs (first values))
            :else
            (render-paragraphs values))]]))

(defn- attachment-item [file-key virus-scan-status virus-status-elem text are-you-sure? removing?]
  [:div.application__virkailija-readonly-attachment-area
   (if (= virus-scan-status "done")
     [:a {:href (str "/lomake-editori/api/files/content/" file-key)}
      text]
     text)
   virus-status-elem])

(defn- attachment-list [attachments]
  [:div.application-handling__nested-container
   (map-indexed (fn attachment->link [idx {file-key :key filename :filename size :size virus-scan-status :virus-scan-status}]
                  (let [text              (str filename " (" (util/size-bytes->str size) ")")
                        component-key     (str "attachment-div-" idx)
                        virus-status-elem (case virus-scan-status
                                            "not_started" [:span.application__virkailija-readonly-attachment-virus-status-not-started
                                                           (s/format "| %s..." @(subscribe [:editor/virkailija-translation :checking]))]
                                            "failed" [:span.application__virkailija-readonly-attachment-virus-status-virus-found
                                                      (s/format "| %s" @(subscribe [:editor/virkailija-translation :virus-found]))]
                                            "done" nil
                                            @(subscribe [:editor/virkailija-translation :error]))]
                    [:div.application__virkailija-readonly-attachment
                     {:key component-key}
                     [attachment-item file-key virus-scan-status virus-status-elem text]]))
                (filter identity attachments))])

(defn attachment [field-descriptor application lang group-idx]
  (let [id         (:id field-descriptor)
        answer-key (keyword (answer-key field-descriptor))
        values     (cond-> (get-in application [:answers answer-key :values])
                           (some? group-idx)
                           (nth group-idx))]
    [:div.application__form-field
     (when (should-highlight-form-change? id) {:class "form-highlighted"})
     [:label.application__form-field-label
      [:span
       (str (from-multi-lang (:label field-descriptor) lang)
            (required-hint field-descriptor))
       [copy-link id :shared-use-warning? false :include? exclude-always-included]]]
     [attachment-list values]]))

(declare field)

(defn wrapper [content application lang children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:div.application__wrapper-heading
    [:h2 (from-multi-lang (:label content) lang)]
    [scroll-to-anchor content]]
   (into [:div.application__wrapper-contents]
         (for [child children]
           [field child application lang]))])

(defn row-container [application lang children group-idx person-info-field?]
  (fn [application lang children]
    (into [:div] (for [child children]
                   [field child application lang group-idx person-info-field?]))))

(defn- extract-values [children answers group-idx]
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
      (if (question-group-answer? concatenated-answers)
        (nth (answers->read-only-format concatenated-answers) group-idx)
        (apply map vector concatenated-answers)))))

(defn- fieldset-answer-table [answers]
  [:tbody
   (doall
     (for [[idx values] (map vector (range) answers)]
       (into
         [:tr {:key (str idx "-" (apply str values))}]
         (for [value values]
           [:td (str value)]))))])


(defn fieldset [field-descriptor application lang children group-idx]
  (let [fieldset-answers (extract-values children (:answers application) group-idx)]
    [:div.application__form-field
     [:label.application__form-field-label
      (str (from-multi-lang (:label field-descriptor) lang)
           (required-hint field-descriptor))]
     [:table.application__readonly-adjacent
      [:thead
       (into [:tr]
         (for [child children]
           [:th.application__readonly-adjacent--header
            (str (from-multi-lang (:label child) lang)
                 (required-hint field-descriptor))]))]
      (if (question-group-answer? fieldset-answers)
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

(defn- selectable [content application lang question-group-idx]
  [:div.application__form-field
   (when (should-highlight-form-change? (:id content)) {:class "form-highlighted"})
   [:div.application__form-field-label--selectable
    [:div.application__form-field-label
     [:span
      (from-multi-lang (:label content) lang)
      [copy-link (:id content) :shared-use-warning? false :include? exclude-always-included]]]
    (let [values           (-> (cond-> (get-in application [:answers (keyword (:id content)) :value])
                                       (some? question-group-idx)
                                       (nth question-group-idx nil))
                               vector
                               flatten
                               set)
          selected-options (filter #(contains? values (:value %))
                                   (:options content))
          values-wo-option (remove (fn [value]
                                     (some #(= value (:value %))
                                           selected-options))
                                   values)]
      [:div.application-handling__nested-container
       (doall
        (for [option selected-options]
          ^{:key (:value option)}
          [:div
           [:p.application__text-field-paragraph
            (from-multi-lang (:label option) lang)]
           (when (some #(visible? % application) (:followups option))
             [:div.application-handling__nested-container
              (for [followup (:followups option)]
                ^{:key (:id followup)}
                [field followup application lang])])]))
       (doall
        (for [value values-wo-option]
          ^{:key (str "unknown-option-" value)}
          [:div
           [:p.application__text-field-paragraph
            (if value
              (str @(subscribe [:editor/virkailija-translation :unknown-option]) " " value)
              (str @(subscribe [:editor/virkailija-translation :empty-option])))]]))])]])

(defn- haku-row [haku-name haku-oid]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper
    [:div.application-handling__review-area-haku-heading
     (str haku-name " ")
     (when haku-oid
       [:a.editor-form__haku-admin-link
        {:href   (str "/tarjonta-app/index.html#/haku/" haku-oid)
         :target "_blank"}
        [:i.zmdi.zmdi-open-in-new]])]]])

(defn- hakukohteet-list-row [hakukohde-oid selected?]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper.application-handling__hakukohde--selectable
    {:class (when selected?
              "application-handling__hakukohde--selected")
     :on-click (fn [_]
                 (dispatch [:application/select-review-hakukohde hakukohde-oid]))}
    (when @(subscribe [:application/prioritize-hakukohteet?])
      [:p.application__hakukohde-priority-number-readonly
       @(subscribe [:application/hakukohde-priority-number hakukohde-oid])])
    [:div
     [:div.application-handling__review-area-hakukohde-heading
      (str @(subscribe [:application/hakukohde-label hakukohde-oid]) " ")
      [:a.editor-form__haku-admin-link
       {:href   (str "/tarjonta-app/index.html#/hakukohde/" hakukohde-oid)
        :target "_blank"}
       [:i.zmdi.zmdi-open-in-new]]]
     [:div.application-handling__review-area-koulutus-heading
      @(subscribe [:application/hakukohde-description hakukohde-oid])]]]])

(defn- hakukohteet [content]
  (let [selected-hakukohde-oids (set @(subscribe [:state-query [:application :selected-review-hakukohde-oids]]))]
    (when-let [hakukohteet (seq @(subscribe [:application/hakutoiveet]))]
      [:div.application__wrapper-element.application__wrapper-element--border
       [:div.application__wrapper-heading
        [:h2 @(subscribe [:application/hakukohteet-header])]
        [scroll-to-anchor content]]
       [:div.application__wrapper-contents
        {:class (clojure.string/join " " [(when @(subscribe [:application/field-highlighted? :hakukohteet]) "highlighted")
                                          (when (should-highlight-form-change? :hakukohteet) "form-highlighted")])
         :id    "hakukohteet"}
        [haku-row @(subscribe [:application/selected-application-haku-name])
         @(subscribe [:state-query [:application :selected-application-and-form :application :haku]])]
        (for [hakukohde-oid hakukohteet]
          ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
          [hakukohteet-list-row hakukohde-oid (contains? selected-hakukohde-oids hakukohde-oid)])]])))

(defn- person-info-module [content application lang]
  [:div.application__person-info-wrapper.application__wrapper-element
   [:div.application__wrapper-element.application__wrapper-element--border
    [:div.application__wrapper-heading
     [:h2 (from-multi-lang (:label content) lang)]
     (when (-> application :person :turvakielto)
       [:p.security-block
        [:i.zmdi.zmdi-account-o]
        "Henkilöllä turvakielto!"])
     [scroll-to-anchor content]]
    (into [:div.application__wrapper-contents]
          (for [child (:children content)
                :when (not (:exclude-from-answers child))]
            [field child application lang nil true]))]])

(defn- repeat-count
  [application question-group-children]
  (util/reduce-form-fields
   (fn [max-count child]
     (max max-count
          (count (get-in application [:answers (keyword (:id child)) :value]))))
   0
   question-group-children))

(defn- question-group [content application lang children]
  [:div.application__question-group
   [:h3.application__question-group-heading
    (from-multi-lang (:label content) lang)]
   (for [idx (range (repeat-count application children))]
     ^{:key (str "question-group-" (:id content) "-" idx)}
     [:div.application__question-group-repeat
      (for [child children]
        ^{:key (str "question-group-" (:id content) "-" idx "-" (:id child))}
        [field child application lang idx])])])

(defn- nationality-field [field-descriptor application lang children]
  (let [field            (first children)
        id               (keyword (:id field-descriptor))
        values           (flatten (replace-with-option-label (-> application :person :nationality)
                                                             (:options field)
                                                             lang))
        highlight-field? (subscribe [:application/field-highlighted? id])]
    [:div.application__form-field
     {:class (clojure.string/join " " [(when @highlight-field? "highlighted")
                                      (when (should-highlight-form-change? id) "form-highlighted")])
      :id    id}
     [:label.application__form-field-label
      (str (from-multi-lang (:label field) lang)
           (required-hint field))]
     [:div.application__form-field-value
      [:p.application__text-field-paragraph
       (clojure.string/join ", " values)]]]))

(defn field
  [content application lang group-idx person-info-field?]
  (when (visible? content application)
    (match content
      {:module "person-info"} [person-info-module content application lang]
      {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application lang children]
      {:fieldClass "questionGroup" :fieldType "fieldset" :children children}
           (if person-info-field?
             (nationality-field content application lang children)
             [question-group content application lang children])
      {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children group-idx person-info-field?]
      {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children group-idx]
      {:fieldClass "formField" :fieldType (:or "dropdown" "multipleChoice" "singleChoice")}
      (if person-info-field?
        (text content application lang group-idx)
        [selectable content application lang group-idx])
      {:fieldClass "formField" :fieldType (:or "textField" "textArea")} (text content application lang group-idx)
      {:fieldClass "formField" :fieldType "attachment"} [attachment content application lang group-idx]
      {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet content]
      {:fieldClass "pohjakoulutusristiriita"} nil)))

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
        (for [content (:content form)]
          [field content application lang nil])))))
