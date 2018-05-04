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
            [cljs.core.match :refer-macros [match]]
            [ataru.application-common.application-field-common :refer [answer-key
                                                                       required-hint
                                                                       get-value
                                                                       render-paragraphs
                                                                       replace-with-option-label
                                                                       predefined-value-answer?
                                                                       scroll-to-anchor
                                                                       question-group-answer?
                                                                       answers->read-only-format]]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]))

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
  (and (not= "infoElement" (:fieldClass field-descriptor))
       (not (:exclude-from-answers field-descriptor))
       (or (empty? (:belongs-to-hakukohteet field-descriptor))
           (belongs-to-hakukohde? field-descriptor application))
       (or (empty? (:belongs-to-hakukohderyhma field-descriptor))
           (belongs-to-hakukohderyhma? field-descriptor application))
       (or (empty? (:children field-descriptor))
           (some #(visible? % application) (:children field-descriptor)))))

(defn text [field-descriptor application lang group-idx]
  (let [id               (keyword (:id field-descriptor))
        use-onr-info?    (contains? (:person application) id)
        values           (replace-with-option-label (if use-onr-info?
                                                      (-> application :person id)
                                                      (get-value (-> application :answers id) group-idx))
                                                    (:options field-descriptor)
                                                    lang)
        highlight-field? (subscribe [:application/field-highlighted? id])]
    [:div.application__form-field
     {:class (when @highlight-field? "highlighted")
      :id    id}
     [:label.application__form-field-label
      (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
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

(defn- attachment-list [attachments]
  [:div.application-handling__nested-container
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
                attachments)])

(defn attachment [field-descriptor application lang group-idx]
  (when (fc/feature-enabled? :attachment)
    (let [answer-key (keyword (answer-key field-descriptor))
          values     (cond-> (get-in application [:answers answer-key :values])
                       (some? group-idx)
                       (nth group-idx))]
      [:div.application__form-field
       [:label.application__form-field-label
        (str (-> field-descriptor :label lang) (required-hint field-descriptor))]
       [attachment-list values]])))

(declare field)

(defn wrapper [content application lang children]
  [:div.application__wrapper-element.application__wrapper-element--border
   [:div.application__wrapper-heading
    [:h2 (-> content :label lang)]
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
  (if (not-empty (:children followup))
    (some #(followup-has-answer? % application) (:children followup))
    (when-let [answer-value (:value ((answer-key followup) (:answers application)))]
      (and
        (boolean answer-value)
        (if (sequential? answer-value)
          (< 0 (count answer-value))
          true)))))

(defn- selectable [content application lang question-group-idx]
  [:div
   [:div.application__form-field-label (some (:label content) [lang :fi :sv :en])]
   (let [values           (-> (cond-> (get-in application [:answers (keyword (:id content)) :value])
                                      (some? question-group-idx)
                                      (nth question-group-idx))
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
           (some (:label option) [lang :fi :sv :en])]
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
           (str "Tuntematon vastausvaihtoehto " value)]]))])])

(defn- haku-row [haku-name]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper
    [:div.application-handling__review-area-haku-heading
     haku-name]]])

(defn- hakukohteet-list-row [hakukohde-oid]
  [:div.application__form-field
   [:div.application-handling__hakukohde-wrapper
    (when @(subscribe [:application/prioritize-hakukohteet?])
      [:p.application__hakukohde-priority-number-readonly
       @(subscribe [:application/hakukohde-priority-number hakukohde-oid])])
    [:div
     [:div.application-handling__review-area-hakukohde-heading
      @(subscribe [:application/hakukohde-label hakukohde-oid])]
     [:div.application-handling__review-area-koulutus-heading
      @(subscribe [:application/hakukohde-description hakukohde-oid])]]]])

(defn- hakukohteet [content]
  (when-let [hakukohteet (seq @(subscribe [:application/hakukohteet]))]
    [:div.application__wrapper-element.application__wrapper-element--border
     [:div.application__wrapper-heading
      [:h2 @(subscribe [:application/hakukohteet-header])]
      [scroll-to-anchor content]]
     [:div.application__wrapper-contents
      {:class (when @(subscribe [:application/field-highlighted? :hakukohteet])
                "highlighted")
       :id    "hakukohteet"}
      [haku-row @(subscribe [:application/selected-application-haku-name])]
      (for [hakukohde-oid hakukohteet]
        ^{:key (str "hakukohteet-list-row-" hakukohde-oid)}
        [hakukohteet-list-row hakukohde-oid])]]))

(defn- person-info-module [content application lang]
  [:div.application__person-info-wrapper.application__wrapper-element
   [:div.application__wrapper-element.application__wrapper-element--border
    [:div.application__wrapper-heading
     [:h2 (-> content :label lang)]
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
    (-> content :label lang)]
   (for [idx (range (repeat-count application children))]
     ^{:key (str "question-group-" (:id content) "-" idx)}
     [:div.application__question-group-repeat
      (for [child children]
        ^{:key (str "question-group-" (:id content) "-" idx "-" (:id child))}
        [field child application lang idx])])])

(defn field
  [content application lang group-idx person-info-field?]
  (when (visible? content application)
    (match content
      {:module "person-info"} [person-info-module content application lang]
      {:fieldClass "wrapperElement" :fieldType "fieldset" :children children} [wrapper content application lang children]
      {:fieldClass "questionGroup" :fieldType "fieldset" :children children} [question-group content application lang children]
      {:fieldClass "wrapperElement" :fieldType "rowcontainer" :children children} [row-container application lang children group-idx person-info-field?]
      {:fieldClass "wrapperElement" :fieldType "adjacentfieldset" :children children} [fieldset content application lang children group-idx]
      {:fieldClass "formField" :fieldType (:or "dropdown" "multipleChoice" "singleChoice")}
      (if person-info-field?
        (text content application lang group-idx)
        [selectable content application lang group-idx])
      {:fieldClass "formField" :fieldType (:or "textField" "textArea")} (text content application lang group-idx)
      {:fieldClass "formField" :fieldType "attachment"} [attachment content application lang group-idx]
      {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet content])))

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
