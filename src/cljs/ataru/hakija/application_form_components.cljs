(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [markdown.core :refer [md->html]]
            [cljs.core.match :refer-macros [match]]
            [ataru.translations.translation-util :refer [get-translations]]
            [ataru.translations.application-view :refer [application-view-translations]]
            [ataru.cljs-util :as cljs-util :refer [console-log]]
            [ataru.application-common.application-field-common
             :refer
             [answer-key
              required-hint
              textual-field-value
              scroll-to-anchor
              is-required-field?]]
            [ataru.hakija.application-validators :as validator]
            [ataru.util :as util]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]
            [clojure.string :as string]
            [ataru.hakija.editing-forbidden-fields :refer [viewing-forbidden-person-info-field-ids editing-forbidden-person-info-field-ids]])
  (:import (goog.html.sanitizer HtmlSanitizer)))

(defonce builder (new HtmlSanitizer.Builder))
(defonce html-sanitizer (.build builder))

(declare render-field)

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- non-blank-val [val default]
  (if-not (clojure.string/blank? val)
    val
    default))

(defn- field-value-valid?
  [field-data value answers-by-key]
  (if (and (not (or
                  (:cannot-view field-data)
                  (:cannot-edit field-data)))
           (not-empty (:validators field-data)))
    (every? true? (map #(validator/validate % value answers-by-key)
                    (:validators field-data)))
    true))

(defn- textual-field-blur [text-field-data answers-by-key evt]
  (let [value  (-> evt .-target .-value)
        blur-rules (:blur-rules text-field-data)]
    (when (and (not-empty blur-rules)
               (field-value-valid? text-field-data value answers-by-key))
      (dispatch [:application/run-rule blur-rules]))))

(defn- textual-field-change [text-field-data answers-by-key evt]
  (let [value  (-> evt .-target .-value)
        valid? (field-value-valid? text-field-data value answers-by-key)]
    (do
      ; dispatch-sync because we really really want the value to change NOW. Is a minor UI speed boost.
      (dispatch-sync [:application/set-application-field (answer-key text-field-data) {:value value :valid valid?}])
      (when-let [rules (not-empty (:rules text-field-data))]
        (dispatch [:application/run-rule rules])))))

(defn- init-dropdown-value
  [dropdown-data lang secret this]
  (let [select (-> (r/dom-node this) (.querySelector "select"))
        value  (or (first
                     (eduction
                       (comp (filter :default-value)
                         (map :value))
                       (:options dropdown-data)))
                 (-> select .-value))
        valid? (field-value-valid? dropdown-data value {})]
    (if-not (some? secret)
      (dispatch [:application/set-application-field (answer-key dropdown-data) {:value value :valid valid?}]))
    (when-let [rules (not-empty (:rules dropdown-data))]
      (dispatch [:application/run-rule rules]))))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(defn- label [field-descriptor]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [field-descriptor]
      (let [label (non-blank-val (get-in field-descriptor [:label @lang])
                                 (get-in field-descriptor [:label @default-lang]))]
        [:label.application__form-field-label
         [:span (str label (required-hint field-descriptor))]
         [scroll-to-anchor field-descriptor]]))))

(defn- show-text-field-error-class?
  [field-descriptor value valid? answers-by-key]
  (and
    (not valid?)
    (is-required-field? field-descriptor)
    (validator/validate "required" value answers-by-key)))

(defn- add-link-target-prop
  [text state]
  [(string/replace text #"<a href=([^>]+)>" "<a target=\"_blank\" href=$1>") state])

(defn- markdown-paragraph
  [md-text]
  (let [sanitized-html (as-> md-text v
                            (md->html v :custom-transformers [add-link-target-prop])
                            (.sanitize html-sanitizer v)
                            (.getTypedStringValue v))]
    [:div.application__form-info-text {:dangerouslySetInnerHTML {:__html sanitized-html}}]))

(defn info-text [field-descriptor]
  (let [language (subscribe [:application/form-language])]
    (fn [field-descriptor]
      (when-let [info (@language (some-> field-descriptor :params :info-text :label))]
        [markdown-paragraph info]))))

(defn text-field [field-descriptor & {:keys [div-kwd disabled editing] :or {div-kwd :div.application__form-field disabled false editing false}}]
  (let [id           (keyword (:id field-descriptor))
        answers      (subscribe [:state-query [:application :answers]])
        answer       (subscribe [:state-query [:application :answers id]])
        lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        size-class (text-field-size->class (get-in field-descriptor [:params :size]))]
    (fn [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
      [div-kwd
       [label field-descriptor]
       [:div.application__form-text-input-info-text
        [info-text field-descriptor]]
       (let [cannot-view? (and editing (:cannot-view @answer))]
         [:input.application__form-text-input
          (merge {:id          id
                  :type        "text"
                  :placeholder (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                 (non-blank-val (get input-hint @lang)
                                                (get input-hint @default-lang)))
                  :class       (str size-class (if (show-text-field-error-class? field-descriptor (:value @answer) (:valid @answer) @answers)
                                                 " application__form-field-error"
                                                 " application__form-text-input--normal"))
                  :value       (if cannot-view? "***********" (:value @answer))
                  :on-blur     (partial textual-field-blur field-descriptor @answers)
                  :on-change   (partial textual-field-change field-descriptor @answers)}
                 (when (or disabled cannot-view?) {:disabled true}))])])))

(defn repeatable-text-field [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [id         (keyword (:id field-descriptor))
        values     (subscribe [:state-query [:application :answers id :values]])
        size-class (text-field-size->class (get-in field-descriptor [:params :size]))
        answers-by-key (subscribe [:state-query [:application :answers]])
        lang       (subscribe [:application/form-language])
        on-change  (fn [idx answers-by-key evt]
                     (let [value (some-> evt .-target .-value)
                           valid (field-value-valid? field-descriptor value answers-by-key)]
                       (dispatch [:application/set-repeatable-application-field field-descriptor id idx {:value value :valid valid}])))]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      (into  [div-kwd
              [label field-descriptor]
              [:div.application__form-text-input-info-text
               [info-text field-descriptor]]]
        (cons
         (let [{:keys [value valid]} (first @values)]
            [:div
             [:input.application__form-text-input
              {:type      "text"
               :class     (str size-class (if (show-text-field-error-class? field-descriptor value valid @answers-by-key)
                                            " application__form-field-error"
                                            " application__form-text-input--normal"))
               :value     value
               :on-blur   #(when (empty? (-> % .-target .-value))
                             (dispatch [:application/remove-repeatable-application-field-value id 0]))
               :on-change (partial on-change 0 answers-by-key)}]])
          (map-indexed
           (let [first-is-empty? (empty? (first (map :value @values)))
                 translations    (get-translations (keyword @lang) application-view-translations)]
             (fn [idx {:keys [value last?]}]
               (let [clicky #(dispatch [:application/remove-repeatable-application-field-value id (inc idx)])]
                  [:div.application__form-repeatable-text-wrap
                   [:input.application__form-text-input
                    (merge
                     {:type      "text"
                                        ; prevent adding second answer when first is empty
                      :disabled  (and last? first-is-empty?)
                      :class     (str
                                  size-class " application__form-text-input--normal"
                                  (when-not value " application__form-text-input--disabled"))
                      :value     value
                      :on-blur   #(when (and
                                         (not last?)
                                         (empty? (-> % .-target .-value)))
                                    (clicky))
                      :on-change (partial on-change (inc idx) answers-by-key)}
                      (when last?
                        {:placeholder
                         (:add-more translations)}))]
                   (when value
                     [:a.application__form-repeatable-text--addremove
                      {:on-click clicky}
                      [:i.zmdi.zmdi-close.zmdi-hc-lg]])])))
            (concat (rest @values)
                    [{:value nil :valid true :last? true}])))))))

(defn- text-area-size->class [size]
  (match size
         "S" "application__form-text-area__size-small"
         "M" "application__form-text-area__size-medium"
         "L" "application__form-text-area__size-large"
         :else "application__form-text-area__size-medium"))

(defn text-area [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [application (subscribe [:state-query [:application]])
        answers     (subscribe [:state-query [:application :answers]])]
    (fn [field-descriptor]
      [div-kwd
       [label field-descriptor]
       [:div.application__form-text-area-info-text
        [info-text field-descriptor]]
       [:textarea.application__form-text-input.application__form-text-area
        {:class (text-area-size->class (-> field-descriptor :params :size))
         ; default-value because IE11 will "flicker" on input fields. This has side-effect of NOT showing any
         ; dynamically made changes to the text-field value.
         :default-value (textual-field-value field-descriptor @application)
         :on-change (partial textual-field-change field-descriptor @answers)
         :value (textual-field-value field-descriptor @application)}]])))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])]
    (fn [field-descriptor children]
      (let [label (non-blank-val (get-in field-descriptor [:label @lang])
                                 (get-in field-descriptor [:label @default-lang]))]

        [:div.application__wrapper-element.application__wrapper-element--border
         [:div.application__wrapper-heading
          [:h2 label]
          [scroll-to-anchor field-descriptor]]
         (into [:div.application__wrapper-contents]
           (for [child children]
             [render-field child lang]))]))))

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
        ; flatten fields here because 'rowcontainer' may
        ; have nested fields because
        ; of validation (for example :one-of validator)
        (for [child (util/flatten-form-fields children)]
          [render-field child :div-kwd :div.application__row-field.application__form-field])))

(defn- toggle-followup-visibility [db followup visible?]
  (let [db (assoc-in db [:application :ui (answer-key followup) :visible?] visible?)]
    (if (= (:fieldType followup) "adjacentfieldset")
      (reduce (fn [db adjacent-fieldset-question]
                (assoc-in db [:application :ui (answer-key adjacent-fieldset-question) :visible?] visible?))
              db
              (:children followup))
      db)))

(defn dropdown-followups [lang value field-descriptor]
  (let [prev (r/atom @value)
        resolve-followups (partial util/resolve-followups (:options field-descriptor))]
    (r/create-class
      {:component-did-update (fn []
                               (let [previous @prev]
                                 (when-not (= previous (reset! prev @value))
                                   (let [previous-followups (resolve-followups previous)
                                         current-followups  (resolve-followups @value)]
                                     (dispatch [:state-update
                                                (fn [db]
                                                  (let [reduced (reduce #(toggle-followup-visibility %1 %2 false) db previous-followups)]
                                                    (reduce #(toggle-followup-visibility %1 %2 true) reduced current-followups)))])))))
       :reagent-render       (fn [lang value field-descriptor]
                               (when-let [followups (resolve-followups @value)]
                                 (into [:div.application__form-dropdown-followups]
                                   (for [followup followups]
                                     [:div.application__form-dropdown-followups.animated.fadeIn
                                      [render-field followup]]))))})))

(defn dropdown
  [field-descriptor & {:keys [div-kwd editing] :or {div-kwd :div.application__form-field editing false}}]
  (let [application  (subscribe [:state-query [:application]])
        answers      (subscribe [:state-query [:application :answers]])
        lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        secret       (subscribe [:state-query [:application :secret]])
        disabled?    (reaction (or
                                 (and @editing
                                      (contains? editing-forbidden-person-info-field-ids (keyword (:id field-descriptor))))
                                 (->
                                   (:answers @application)
                                   (get (answer-key field-descriptor))
                                   :cannot-edit)))
        value        (reaction
                       (or (->
                             (:answers @application)
                             (get (answer-key field-descriptor))
                             :value)
                           ""))]
    (r/create-class
      {:component-did-mount (partial init-dropdown-value field-descriptor @lang @secret)
       :reagent-render      (fn [field-descriptor]
                              (let [lang         @lang
                                    default-lang @default-lang]
                                [:div
                                 [div-kwd
                                  [label field-descriptor]
                                  [:div.application__form-text-input-info-text
                                   [info-text field-descriptor]]
                                  [:div.application__form-select-wrapper
                                   (when (not @disabled?)
                                     [:span.application__form-select-arrow])
                                   [(keyword (str "select.application__form-select" (when (not @disabled?) ".application__form-select--enabled")))
                                    {:value     @value
                                     :on-change (partial textual-field-change field-descriptor @answers)
                                     :disabled  @disabled?}
                                    (concat
                                      (when
                                        (and
                                          (nil? (:koodisto-source field-descriptor))
                                          (not (:no-blank-option field-descriptor))
                                          (not= "" (:value (first (:options field-descriptor)))))
                                        [^{:key (str "blank-" (:id field-descriptor))} [:option {:value ""} ""]])
                                      (map-indexed
                                        (fn [idx option]
                                          (let [label        (non-blank-val (get-in option [:label lang])
                                                                            (get-in option [:label default-lang]))
                                                option-value (:value option)]
                                            ^{:key idx}
                                            [:option {:value option-value} label]))
                                        (:options field-descriptor)))]]]

                                 [dropdown-followups lang value field-descriptor]]))})))

(defn- multi-choice-followups [followups parent-checked?]
  (r/create-class
    {:component-did-mount (fn []
                            (dispatch [:state-update
                                       (fn [db]
                                         (reduce #(toggle-followup-visibility %1 %2 parent-checked?)
                                                 db
                                                 followups))]))
     :reagent-render      (fn [followups parent-checked?]
                            (when (and parent-checked? (not-empty followups))
                              [:div.application__form-multi-choice-followups-outer-container
                               [:div.application__form-multi-choice-followups-indicator]
                               [:div.application__form-multi-choice-followups-container.animated.fadeIn
                                (map (fn [followup]
                                       ^{:key (:id followup)}
                                       [render-field followup])
                                     followups)]]))}))

(defn- multiple-choice-option [option parent-id validators]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        label        (non-blank-val (get-in option [:label @lang])
                                    (get-in option [:label @default-lang]))
        value        (:value option)
        option-id    (util/component-id)
        checked?     (subscribe [:application/multiple-choice-option-checked? parent-id value])]
    [:div {:key option-id}
     [:input.application__form-checkbox
      {:id        option-id
       :type      "checkbox"
       :checked   @checked?
       :value     value
       :on-change (fn [event]
                    (let [value (.. event -target -value)]
                      (dispatch [:application/toggle-multiple-choice-option parent-id value validators])))}]
     [:label
      {:for option-id}
      label]
     [multi-choice-followups (:followups option) @checked?]]))

(defn multiple-choice
  [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [parent-id  (answer-key field-descriptor)
        validators (:validators field-descriptor)]
     [div-kwd
      [label field-descriptor]
      [:div.application__form-text-input-info-text
       [info-text field-descriptor]]
      [:div.application__form-outer-checkbox-container
       (map-indexed (fn [idx option]
                      ^{:key (str "multiple-choice-" (:id field-descriptor) "-" idx)}
                      [multiple-choice-option option parent-id validators])
                    (:options field-descriptor))]]))

(defn- single-choice-option [option parent-id validators]
  (let [lang         (subscribe [:application/form-language])
        default-lang (subscribe [:application/default-language])
        label        (non-blank-val (get-in option [:label @lang])
                                    (get-in option [:label @default-lang]))
        option-value (:value option)
        option-id    (util/component-id)
        checked?     (subscribe [:application/single-choice-option-checked? parent-id option-value])]
    [:div.application__form-single-choice-button-inner-container {:key option-id}
     [:input.application__form-single-choice-button
      {:id        option-id
       :type      "checkbox"
       :checked   @checked?
       :value     option-value
       :on-change (fn [event]
                    (let [value (.. event -target -value)]
                      (dispatch [:application/select-single-choice-button parent-id value validators])))}]
     [:label
      {:for option-id}
      label]
     (when (and @checked? (not-empty (:followups option)))
       [:div.application__form-single-choice-followups-indicator])]))

(defn- hide-followups [db {:keys [followups]}]
  (reduce #(toggle-followup-visibility %1 %2 false)
          db
          followups))

(defn- single-choice-followups [parent-id options]
  (let [single-choice-value (subscribe [:state-query [:application :answers parent-id :value]])
        followups           (reaction (->> options
                                           (filter (comp (partial = @single-choice-value) :value))
                                           (map :followups)
                                           (first)))]
    (r/create-class
      {:reagent-render       (fn [parent-id options]
                               (when (not-empty @followups)
                                 [:div.application__form-multi-choice-followups-container.animated.fadeIn
                                  (map (fn [followup]
                                         ^{:key (str (:id followup))}
                                         [render-field followup])
                                       @followups)]))
       :component-did-update (fn []
                               ; Setting visible? state to true/false determines answer's visibility
                               ; in the "required answers" list on the header, below the submit application
                               ; button
                               (dispatch [:state-update
                                          (fn [db]
                                            (as-> db db'
                                                  (reduce hide-followups
                                                          db'
                                                          (filter (comp (partial not= @single-choice-value) :value) options))
                                                  (reduce #(toggle-followup-visibility %1 %2 true)
                                                          db'
                                                          @followups)))]))})))

(defn single-choice-button [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [button-id  (answer-key field-descriptor)
        validators (:validators field-descriptor)]
    (fn [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
      [div-kwd
       [label field-descriptor]
       [:div.application__form-text-input-info-text
        [info-text field-descriptor]]
       [:div.application__form-single-choice-button-outer-container
        (map-indexed (fn [idx option]
                       ^{:key (str "single-choice-" (:id field-descriptor) "-" idx)}
                       [single-choice-option option button-id validators])
                     (:options field-descriptor))]
       [single-choice-followups button-id (:options field-descriptor)]])))

(defonce max-attachment-size-bytes (* 10 1024 1024))

(defn attachment-upload [field-descriptor component-id attachment-count]
  (let [id       (str component-id "-upload-button")
        language @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment-container
     [:input.application__form-upload-input
      {:id        id
       :type      "file"
       :multiple  "multiple"
       :key       (str "upload-button-" component-id "-" attachment-count)
       :on-change (fn [event]
                    (.preventDefault event)
                    (let [file-list  (or (some-> event .-dataTransfer .-files)
                                         (.. event -target -files))
                          files      (->> (.-length file-list)
                                          (range)
                                          (map #(.item file-list %)))
                          file-sizes (map #(.-size %) files)]
                      (if (some #(> % max-attachment-size-bytes) file-sizes)
                        (dispatch [:application/show-attachment-too-big-error component-id])
                        (dispatch [:application/add-attachments field-descriptor component-id attachment-count files]))))}]
     [:label.application__form-upload-label
      {:for id}
      [:i.zmdi.zmdi-cloud-upload.application__form-upload-icon]
      [:span.application__form-upload-button-add-text (case language
                                                        :fi "Lisää liite..."
                                                        :en "Upload attachment..."
                                                        :sv "Ladda upp bilagan...")]]
     (let [file-size-info-text (case language
                                 :fi "Tiedoston maksimikoko on 10 MB"
                                 :en "Maximum file size is 10 MB"
                                 :sv "Den maximala filstorleken är 10 MB")]
       (if @(subscribe [:state-query [:application :answers (keyword component-id) :too-big]])
         [:span.application__form-upload-button-error.animated.shake file-size-info-text]
         [:span.application__form-upload-button-info file-size-info-text]))]))

(defn- filename->label [{:keys [filename size]}]
  (str filename " (" (util/size-bytes->str size) ")"))

(defn attachment-view-file [field-descriptor component-id attachment-idx]
  [:div.application__form-filename-container
   [:span.application__form-attachment-text
    (filename->label @(subscribe [:state-query [:application :answers (keyword component-id) :values attachment-idx :value]]))
    [:a.application__form-upload-remove-attachment-link
     {:href     "#"
      :on-click (fn remove-attachment [event]
                  (.preventDefault event)
                  (dispatch [:application/remove-attachment field-descriptor component-id attachment-idx]))}
     [:i.zmdi.zmdi-close]]]])

(defn attachment-view-file-error [field-descriptor component-id attachment-idx]
  (let [attachment @(subscribe [:state-query [:application :answers (keyword component-id) :values attachment-idx]])
        lang       @(subscribe [:application/form-language])]
    [:div
     [:div.application__form-filename-container.application__form-file-error.animated.shake
      [:span.application__form-attachment-text
       (-> attachment :value :filename)
       [:a.application__form-upload-remove-attachment-link
        {:href     "#"
         :on-click (fn remove-attachment [event]
                     (.preventDefault event)
                     (dispatch [:application/remove-attachment-error field-descriptor component-id attachment-idx]))}
        [:i.zmdi.zmdi-close.zmdi-hc-inverse]]]]
     [:span.application__form-attachment-error (-> attachment :error lang)]]))

(defn attachment-deleting-file [component-id attachment-idx]
  [:div.application__form-filename-container
   [:span.application__form-attachment-text
    (filename->label @(subscribe [:state-query [:application :answers (keyword component-id) :values attachment-idx :value]]))]])

(defn attachment-uploading-file [component-id attachment-idx]
  [:div.application__form-filename-container
   [:span.application__form-attachment-text
    (filename->label @(subscribe [:state-query [:application :answers (keyword component-id) :values attachment-idx :value]]))]
   [:i.zmdi.zmdi-spinner.application__form-upload-uploading-spinner]])

(defn attachment-row [field-descriptor component-id attachment-idx]
  (let [status @(subscribe [:state-query [:application :answers (keyword component-id) :values attachment-idx :status]])]
    [:li.application__attachment-filename-list-item
     (case status
       :ready [attachment-view-file field-descriptor component-id attachment-idx]
       :error [attachment-view-file-error field-descriptor component-id attachment-idx]
       :uploading [attachment-uploading-file component-id attachment-idx]
       :deleting [attachment-deleting-file component-id attachment-idx])]))

(defn attachment [{:keys [id] :as field-descriptor}]
  (let [language         (subscribe [:application/form-language])
        text             (reaction (get-in field-descriptor [:params :info-text :value @language]))
        attachment-count (reaction (count @(subscribe [:state-query [:application :answers (keyword id) :values]])))]
    (fn [field-descriptor]
      [:div.application__form-field
       [label field-descriptor]
       (when-not (clojure.string/blank? @text)
         [markdown-paragraph @text])
       (when (> @attachment-count 0)
         [:ol.application__attachment-filename-list
          (->> (range @attachment-count)
               (map (fn [attachment-idx]
                      ^{:key (str "attachment-" id "-" attachment-idx)}
                      [attachment-row field-descriptor id attachment-idx])))])
       [attachment-upload field-descriptor id @attachment-count]])))

(defn- selected-hakukohde-row-remove
  [hakukohde]
  [:div.application__hakukohde-row-button-container
   [:a.application__hakukohde-remove-link
    {:on-click #(dispatch [:application/hakukohde-remove-selection hakukohde])}
    "Poista"]])

(defn- selected-hakukohde-row
  [hakukohde]
  ^{:key (str "selected-hakukohde-row-" (:value hakukohde))}
  [:div.application__hakukohde-row
   [:div.application__hakukohde-row-text-container
    [:div.application__hakukohde-selected-row-header
     ; TODO support other languages
     (-> hakukohde :label :fi)]
    [:div.application__hakukohde-selected-row-description
     (-> hakukohde :description :fi)]]
   (when @(subscribe [:application/hakukohteet-editable?])
     (selected-hakukohde-row-remove hakukohde))])

(defn- search-hit-hakukohde-row
  [hakukohde]
  ^{:key (str "found-hakukohde-row-" (:value hakukohde))}
  [:div.application__hakukohde-row
   [:div.application__hakukohde-row-text-container
    [:div.application__hakukohde-selected-row-header
                                        ; TODO support other languages
     (-> hakukohde :label :fi)]
    [:div.application__hakukohde-selected-row-description
     (-> hakukohde :description :fi)]]
   [:div.application__hakukohde-row-button-container
    (if @(subscribe [:application/hakukohde-selected? hakukohde])
      [:i.application__hakukohde-selected-check.zmdi.zmdi-check.zmdi-hc-2x]
      (if @(subscribe [:application/hakukohteet-full?])
        (str "Tässä haussa voit valita "
             @(subscribe [:application/max-hakukohteet])
             " hakukohdetta")
        [:a.application__hakukohde-select-button
         {:on-click #(dispatch [:application/hakukohde-add-selection hakukohde])}
         "Lisää"]))]])

(defn- hakukohde-selection-search
  [field-descriptor selected-hakukohteet]
  (let [hakukohde-query @(subscribe [:application/hakukohde-query])
        search-hit-hakukohteet @(subscribe [:application/hakukohde-hits])]
    [:div
     [:div.application__hakukohde-selection-search-arrow-up]
     [:div.application__hakukohde-selection-search-container
      [:div.application__hakukohde-selection-search-input.application__form-text-input-box
       [:input.application__form-text-input-in-box
        {:on-change   #(dispatch [:application/hakukohde-query-change (aget % "target" "value")])
         :placeholder "Etsi tämän haun koulutuksia"
         :value hakukohde-query}]
       (when (not (empty? hakukohde-query))
         [:div.application__form-clear-text-input-in-box
          [:a
           {:on-click #(dispatch [:application/hakukohde-query-clear])}
           [:i.zmdi.zmdi-close]]])]
      (into
       [:div.application__hakukohde-selection-search-results
        (doall
         (map search-hit-hakukohde-row search-hit-hakukohteet))])]]))

(defn- hakukohde-selection-header
  [lang default-lang field-descriptor selected-hakukohteet]
  (let [label (non-blank-val (get-in field-descriptor [:label lang])
                             (get-in field-descriptor [:label default-lang]))
        max-hakukohteet (get-in field-descriptor [:params :max-hakukohteet])
        counter (if max-hakukohteet
                  (str " (" (count selected-hakukohteet) "/" max-hakukohteet ")")
                  "")]
    [:div.application__wrapper-heading.application__wrapper-heading-block
     [:h2 (str label counter)]
     [scroll-to-anchor field-descriptor]]))

(defn- hakukohde-selection
  [lang
   default-lang
   field-descriptor
   selected-hakukohteet
   show-hakukohde-search?]
  (let [selected-hakukohteet-elements (mapv selected-hakukohde-row
                                            selected-hakukohteet)]
    [:div.application__wrapper-element.application__wrapper-element-border
     (hakukohde-selection-header
      lang
      default-lang
      field-descriptor
      selected-hakukohteet)
     (into
      [:div.application__hakukohde-selected-list]
      (if @(subscribe [:application/hakukohteet-editable?])
        (conj selected-hakukohteet-elements
              [:div.application__hakukohde-row
               [:a.application__hakukohde-selection-open-search
                {:on-click #(dispatch [:application/hakukohde-search-toggle])}
                "Lisää hakukohde"]
               (when show-hakukohde-search?
                 (hakukohde-selection-search
                  field-descriptor
                  selected-hakukohteet))])
        selected-hakukohteet-elements))]))

(defn hakukohteet [_]
  (fn [field-descriptor]
    (let [lang                  (subscribe [:application/form-language])
          default-lang          (subscribe [:application/default-language])
          hakukohteet-by-oid    (into {} (map (juxt :value identity)
                                              (:options field-descriptor)))
          selected-hakukohteet  (subscribe [:state-query [:application :answers :hakukohteet :values]])
          show-hakukohde-search (subscribe [:state-query [:application :show-hakukohde-search]])]
      [hakukohde-selection
       @lang
       @default-lang
       field-descriptor
       (map (comp hakukohteet-by-oid :value) @selected-hakukohteet)
       @show-hakukohde-search])))

(defn info-element [field-descriptor]
  (let [language (subscribe [:application/form-language])
        header   (some-> (get-in field-descriptor [:label @language]))
        text     (some-> (get-in field-descriptor [:text @language]))]
    [:div.application__form-info-element.application__form-field
     (when (not-empty header)
       [:label.application__form-field-label [:span header]])
     [markdown-paragraph text]]))

(defn- adjacent-field-input-change [field-descriptor row-idx answers-by-key event]
  (let [value  (some-> event .-target .-value)
        valid? (field-value-valid? field-descriptor value answers-by-key)
        id     (keyword (:id field-descriptor))]
    (dispatch [:application/set-adjacent-field-answer field-descriptor id row-idx {:value value :valid valid?}])))

(defn- adjacent-field-input [{:keys [id] :as child} row-idx]
  (let [answers-by-key (subscribe [:state-query [:application :answers]])
        on-change (partial adjacent-field-input-change child row-idx)
        value     (subscribe [:state-query [:application :answers (keyword id) :values row-idx :value]])]
    (r/create-class
      {:component-did-mount
       (fn [this]
         (when-not value (partial on-change nil @answers-by-key)))
       :reagent-render
       (fn [{:keys [id]} row-idx]
         [:input.application__form-text-input.application__form-text-input--normal
          {:id        (str id "-" row-idx)
           :type      "text"
           :value     @value
           :on-change (partial on-change @answers-by-key)}])})))

(defn adjacent-text-fields [field-descriptor]
  (let [language   (subscribe [:application/form-language])
        row-amount (subscribe [:application/adjacent-field-row-amount field-descriptor])]
    (fn [field-descriptor]
      (let [row-amount   @row-amount
            child-ids    (map (comp keyword :id) (:children field-descriptor))
            translations (get-translations (keyword @language) application-view-translations)]
        [:div.application__form-field
         [label field-descriptor]
         (when-let [info (@language (some-> field-descriptor :params :info-text :label))]
           [:div.application__form-info-text [markdown-paragraph info]])
         [:div
          (->> (range row-amount)
               (map (fn adjacent-text-fields-row [row-idx]
                      ^{:key (str "adjacent-fields-" row-idx)}
                      [:div.application__form-adjacent-text-fields-wrapper
                       (map-indexed (fn adjacent-text-fields-column [col-idx child]
                                      (let [key (str "adjacent-field-" row-idx "-" col-idx)]
                                        ^{:key key}
                                        [:div.application__form-adjacent-row
                                         [:div (when-not (= row-idx 0)
                                                 {:class "application__form-adjacent-row--mobile-only"})
                                          [label child]]
                                         [adjacent-field-input child row-idx]]))
                                    (:children field-descriptor))
                       (when (pos? row-idx)
                         [:a {:on-click (fn remove-adjacent-text-field [event]
                                          (.preventDefault event)
                                          (dispatch [:application/remove-adjacent-field field-descriptor row-idx]))}
                          [:span.application__form-adjacent-row--mobile-only (:remove-row translations)]
                          [:i.application__form-adjacent-row--desktop-only.i.zmdi.zmdi-close.zmdi-hc-lg]])])))]
         (when (get-in field-descriptor [:params :repeatable])
           [:a.application__form-add-new-row
            {:on-click (fn add-adjacent-text-field [event]
                         (.preventDefault event)
                         (dispatch [:application/add-adjacent-fields field-descriptor]))}
            [:i.zmdi.zmdi-plus-square] (str " " (:add-row translations))])]))))

(defn- feature-enabled? [{:keys [fieldType]}]
  (or (not= fieldType "attachment")
      (fc/feature-enabled? :attachment)))

(defn render-field
  [field-descriptor & args]
  (let [ui       (subscribe [:state-query [:application :ui]])
        editing? (subscribe [:state-query [:application :editing?]])
        visible? (fn [id]
                   (get-in @ui [(keyword id) :visible?] true))]
    (fn [field-descriptor & args]
      (if (feature-enabled? field-descriptor)
        (let [disabled? (get-in @ui [(keyword (:id field-descriptor)) :disabled?] false)]
          (cond-> (match field-descriptor
                         {:fieldClass "wrapperElement"
                          :fieldType  "fieldset"
                          :children   children} [wrapper-field field-descriptor children]
                         {:fieldClass "wrapperElement"
                          :fieldType  "rowcontainer"
                          :children   children} [row-wrapper children]
                         {:fieldClass "formField"
                          :id         (_ :guard (complement visible?))} [:div]
                         {:fieldClass "formField" :fieldType "textField" :params {:repeatable true}} [repeatable-text-field field-descriptor]
                         {:fieldClass "formField" :fieldType "textField"} [text-field field-descriptor :disabled disabled? :editing editing?]
                         {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
                         {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor :editing editing?]
                         {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor]
                         {:fieldClass "formField" :fieldType "singleChoice"} [single-choice-button field-descriptor]
                         {:fieldClass "formField" :fieldType "attachment"} [attachment field-descriptor]
                         {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohteet field-descriptor]
                         {:fieldClass "infoElement"} [info-element field-descriptor]
                         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset"} [adjacent-text-fields field-descriptor])
                  (and (empty? (:children field-descriptor))
                       (visible? (:id field-descriptor))) (into args)))
        [:div]))))

(defn editable-fields [form-data]
  (when form-data
    (into [:div] (for [content (:content form-data)]
                   [render-field content]))))
