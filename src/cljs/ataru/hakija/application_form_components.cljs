(ns ataru.hakija.application-form-components
  (:require [clojure.string :refer [trim]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.ratom :refer-macros [reaction]]
            [markdown.core :refer [md->html]]
            [cljs.core.match :refer-macros [match]]
            [cljs-time.core :as time]
            [ataru.application-common.application-field-common
             :refer
             [answer-key
              required-hint
              scroll-to-anchor
              is-required-field?
              markdown-paragraph]]
            [ataru.hakija.application-hakukohde-component :as hakukohde]
            [ataru.hakija.pohjakoulutusristiriita :as pohjakoulutusristiriita]
            [ataru.util :as util]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [ataru.feature-config :as fc]
            [clojure.string :as string]
            [ataru.hakija.person-info-fields :refer [editing-forbidden-person-info-field-ids]]
            [ataru.translations.translation-util :as tu]))


(defonce autocomplete-off "new-password")

(declare render-field)

(defn- visible? [ui field-descriptor]
  (and (not (get-in @ui [(keyword (:id field-descriptor)) :params :hidden] false))
       (get-in @ui [(keyword (:id field-descriptor)) :visible?] true)
       (or (empty? (:children field-descriptor))
           (some (partial visible? ui) (:children field-descriptor)))))

(defn- text-field-size->class [size]
  (match size
         "S" "application__form-text-input__size-small"
         "M" "application__form-text-input__size-medium"
         "L" "application__form-text-input__size-large"
         :else "application__form-text-input__size-medium"))

(defn- email-verify-field-change [field-descriptor value verify-value]
  (dispatch [:application/set-email-verify-field field-descriptor value verify-value]))

(defn- textual-field-change [field-descriptor evt]
  (let [value (-> evt .-target .-value)]
    (dispatch [:application/set-application-field field-descriptor value])))

(def ->textual-field-change
  (memoize (fn [field-descriptor]
             (fn [evt]
               (let [value (-> evt .-target .-value)]
                 (dispatch [:application/set-application-field
                            field-descriptor
                            value]))))))

(defn textual-field-blur
  [field-descriptor idx value]
  (dispatch [:application/textual-field-blur field-descriptor value idx]))

(defn- multi-value-field-change [field-descriptor data-idx question-group-idx event]
  (let [value (some-> event .-target .-value)]
    (dispatch [:application/set-repeatable-application-field field-descriptor value data-idx question-group-idx])))

(def ->multi-value-field-change
  (memoize (fn [field-descriptor data-idx question-group-idx]
             (fn [evt]
               (let [value (some-> evt .-target .-value)]
                 (dispatch [:application/set-repeatable-application-field
                            field-descriptor
                            value
                            data-idx
                            question-group-idx]))))))

(defn- field-id [field-descriptor]
  (str "field-" (:id field-descriptor)))

(def field-types-supporting-label-for
  "These field types can use the <label for=..> syntax, others will use aria-labelled-by"
  #{"textField" "textArea" "dropdown"})

(defn- id-for-label
  [field-descriptor]
  (when-not (contains? field-types-supporting-label-for (:fieldType field-descriptor))
    (str "application-form-field-label-" (:id field-descriptor))))

(defn- label [field-descriptor]
  (let [languages  (subscribe [:application/default-languages])
        label-meta (if-let [label-id (id-for-label field-descriptor)]
                     {:id label-id}
                     {:for (:id field-descriptor)})]
    (fn [field-descriptor]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)]
        [:label.application__form-field-label
         label-meta
         [:span (str label (required-hint field-descriptor))]
         [scroll-to-anchor field-descriptor]]))))

(defn- show-text-field-error-class?
  [field-descriptor validators-processing value valid?]
  (and
    (false? valid?)
    (or (is-required-field? field-descriptor)
        (-> field-descriptor :params :numeric))
    (if (string? value)
      (not (clojure.string/blank? value))
      (not (empty? value)))
    (not (contains? validators-processing (keyword (:id field-descriptor))))))

(defn info-text [field-descriptor]
  (let [languages              (subscribe [:application/default-languages])
        application-identifier (subscribe [:application/application-identifier])]
    (fn [field-descriptor]
      (when-let [info (util/non-blank-val (-> field-descriptor :params :info-text :label) @languages)]
        [markdown-paragraph info (-> field-descriptor :params :info-text-collapse) @application-identifier]))))

(defn question-hakukohde-names
  ([field-descriptor]
   (question-hakukohde-names field-descriptor :question-for-hakukohde))
  ([field-descriptor translation-key]
   (let [show-hakukohde-list? (r/atom false)]
     (fn [field-descriptor]
       (let [lang                           @(subscribe [:application/form-language])
             selected-hakukohteet-for-field @(subscribe [:application/selected-hakukohteet-for-field field-descriptor])]
         [:div.application__question_hakukohde_names_container
          [:div.application__question_hakukohde_names_belongs-to (str (tu/get-hakija-translation translation-key lang) " ")]
          (when @show-hakukohde-list?
            [:ul.application__question_hakukohde_names
             (for [hakukohde selected-hakukohteet-for-field
                   :let [name          (util/non-blank-val (:name hakukohde) [lang :fi :sv :en])
                         tarjoaja-name (util/non-blank-val (:tarjoaja-name hakukohde) [lang :fi :sv :en])]]
               [:li {:key (str (:id field-descriptor) "-" (:oid hakukohde))}
                name " - " tarjoaja-name])])
          [:a.application__question_hakukohde_names_info
           {:role "button"
            :aria-pressed (str (boolean @show-hakukohde-list?))
            :on-click #(swap! show-hakukohde-list? not)}
           (str (tu/get-hakija-translation
                  (if @show-hakukohde-list? :hide-application-options :show-application-options)
                  lang)
                " (" (count selected-hakukohteet-for-field) ")")]])))))

(defn- belongs-to-hakukohde-or-ryhma? [field]
  (seq (concat (:belongs-to-hakukohteet field)
               (:belongs-to-hakukohderyhma field))))

(defn- validation-error
  [errors]
  (let [languages @(subscribe [:application/default-languages])]
    (when (not-empty errors)
      [:div.application__validation-error-dialog
       (doall
        (map-indexed (fn [idx error]
                       (with-meta (util/non-blank-val error languages)
                         {:key (str "error-" idx)}))
                     errors))])))

(defn info-element [field-descriptor]
  (let [languages              (subscribe [:application/default-languages])
        application-identifier (subscribe [:application/application-identifier])
        header                 (util/non-blank-val (:label field-descriptor) @languages)
        text                   (util/non-blank-val (:text field-descriptor) @languages)]
    [:div.application__form-info-element.application__form-field
     (when (not-empty header)
       [:label.application__form-field-label [:span header]])
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [question-hakukohde-names field-descriptor :info-for-hakukohde])
     [markdown-paragraph text (-> field-descriptor :params :info-text-collapse) @application-identifier]]))

(defn email-field [field-descriptor & {:keys [div-kwd disabled editing idx]
                                       :or   {div-kwd  :div.application__form-field
                                              disabled false
                                              editing  false}}]
  (let [id                    (keyword (:id field-descriptor))
        languages             (subscribe [:application/default-languages])
        size                  (get-in field-descriptor [:params :size])
        size-class            (text-field-size->class size)
        validators-processing (subscribe [:state-query [:application :validators-processing]])]
    (fn []
      (let [answer      @(subscribe [:application/answer id idx nil])
            on-change   #(if idx
                           (multi-value-field-change field-descriptor 0 idx %)
                           (textual-field-change field-descriptor %))
            show-error? (show-text-field-error-class? field-descriptor
                          @validators-processing
                          (:value answer)
                          (:valid answer))
            value       (:value answer)
            lang        @(subscribe [:application/form-language])]
        [div-kwd
         [label field-descriptor]
         [:div.application__form-info-element
          [markdown-paragraph (tu/get-hakija-translation :email-info-text lang) false nil]]
         [:input.application__form-text-input
          (merge {:id            id
                  :type          "text"
                  :placeholder   (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                   (util/non-blank-val input-hint @languages))
                  :class         (str size-class
                                      (if show-error?
                                        " application__form-field-error"
                                        " application__form-text-input--normal"))
                  :on-blur       (fn [_] (textual-field-blur field-descriptor idx value))
                  :on-change     on-change
                  :required      (is-required-field? field-descriptor)
                  :aria-invalid  (not (:valid answer))
                  :autoComplete  autocomplete-off
                  :value         value
                  :default-value (if @(subscribe [:application/cannot-view? id])
                                   "***********"
                                   (:value answer))
                  :on-paste (fn [event]
                              (.preventDefault event))}
                 (when (or disabled
                           @(subscribe [:application/cannot-edit? id]))
                   {:disabled true}))]
         [validation-error (:errors answer)]
         (let [id           :verify-email
               verify-label (tu/get-hakija-translation :verify-email lang)]
           [:div
            [:label.application__form-field-label.label.application__form-field-label--verify-email
             {:id  "application-form-field-label-verify-email"
              :for id}
             [:span (str verify-label (required-hint field-descriptor))]]
            [:input.application__form-text-input
             {:id           id
              :type         "text"
              :required     true
              :value        (if @(subscribe [:application/cannot-view? id])
                              "***********"
                              (:verify answer))
              :on-blur      #(email-verify-field-change field-descriptor (:value answer)
                               (clojure.string/trim (or (-> % .-target .-value) "")))
              :on-paste     (fn [event]
                              (.preventDefault event))
              :on-change    #(email-verify-field-change field-descriptor (:value answer) (-> % .-target .-value))
              :class        (str size-class
                                 (if show-error?
                                   " application__form-field-error"
                                   " application__form-text-input--normal"))
              :aria-invalid (not (:valid answer))
              :autoComplete autocomplete-off
              }]])]))))

(defn text-field [field-descriptor & {:keys [div-kwd idx]
                                      :or   {div-kwd :div.application__form-field}}]
  (let [id                     (keyword (:id field-descriptor))
        size                   (get-in field-descriptor [:params :size])
        size-class             (text-field-size->class size)
        languages              (subscribe [:application/default-languages])
        editing?               (subscribe [:application/editing?])
        disabled?              (subscribe [:application/disabled? id])
        cannot-view?           (subscribe [:application/cannot-view? id])
        cannot-edit?           (subscribe [:application/cannot-edit? id])
        local-state            (r/atom {:focused? false :value nil})]
    (fn [field-descriptor & {:keys [div-kwd idx]
                             :or   {div-kwd :div.application__form-field}}]
      (let [languages              @languages
            editing?               @editing?
            disabled?              @disabled?
            {:keys [value
                    valid
                    errors]}       @(subscribe [:application/answer id idx nil])
            cannot-view?           @cannot-view?
            cannot-edit?           @cannot-edit?
            show-error?            @(subscribe [:application/show-validation-error-class? id idx nil])
            on-change              (if idx
                                     (->multi-value-field-change field-descriptor 0 idx)
                                     (->textual-field-change field-descriptor))
            on-blur                (fn [_] (textual-field-blur field-descriptor idx value))]
        [div-kwd
         [label field-descriptor]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [question-hakukohde-names field-descriptor])
         [:div.application__form-text-input-info-text
          [info-text field-descriptor]]
         [:input.application__form-text-input
          (merge {:id           id
                  :type         "text"
                  :placeholder  (when-let [input-hint (-> field-descriptor :params :placeholder)]
                                  (util/non-blank-val input-hint languages))
                  :class        (str size-class
                                     (if show-error?
                                       " application__form-field-error"
                                       " application__form-text-input--normal"))
                  :on-blur      (fn [evt]
                                  (swap! local-state assoc
                                         :focused? false)
                                  (on-blur evt))
                  :on-change    (fn [evt]
                                  (swap! local-state assoc
                                         :focused? true
                                         :value (-> evt .-target .-value))
                                  (on-change evt))
                  :required     (is-required-field? field-descriptor)
                  :aria-invalid (not valid)
                  :autoComplete autocomplete-off
                  :value        (cond cannot-view?
                                      "***********"
                                      (:focused? @local-state)
                                      (:value @local-state)
                                      :else
                                      value)}
                 (when (or disabled? cannot-edit?)
                   {:disabled true}))]
         [validation-error errors]]))))

(defn- repeatable-text-field-row
  [field-descriptor question-group-idx repeatable-idx last?]
  (let [id           (keyword (:id field-descriptor))
        size-class   (text-field-size->class (get-in field-descriptor [:params :size]))
        cannot-edit? (subscribe [:application/cannot-edit? id])
        local-state  (r/atom {:focused? false :value nil})]
    (fn [field-descriptor question-group-idx repeatable-idx last?]
      (let [padded?                      (or (zero? repeatable-idx) last?)
            first-is-empty?              (empty? (:value @(subscribe [:application/answer id question-group-idx 0])))
            {:keys [value
                    valid]}              @(subscribe [:application/answer id question-group-idx repeatable-idx])
            show-validation-error-class? @(subscribe [:application/show-validation-error-class? id question-group-idx repeatable-idx])
            cannot-edit?                 @cannot-edit?
            remove-field                 (fn [_]
                                           (swap! local-state assoc
                                                  :focused? false)
                                           (dispatch [:application/remove-repeatable-application-field-value
                                                      field-descriptor
                                                      repeatable-idx
                                                      question-group-idx]))
            on-blur                      (fn [evt]
                                           (let [value (-> evt .-target .-value)]
                                             (swap! local-state assoc
                                                    :focused? false)
                                             (when (and (empty? value) (not last?))
                                               (dispatch [:application/remove-repeatable-application-field-value
                                                          field-descriptor
                                                          repeatable-idx
                                                          question-group-idx]))))
            on-change                    (fn [evt]
                                           (let [value (-> evt .-target .-value)]
                                             (swap! local-state assoc
                                                    :focused? true
                                                    :value value)
                                             (dispatch [:application/set-repeatable-application-field
                                                        field-descriptor
                                                        value
                                                        repeatable-idx
                                                        question-group-idx])))
            lang                         @(subscribe [:application/form-language])]
        [:div.application__form-repeatable-text-wrap
         {:class (when padded? "application__form-repeatable-text-wrap--padded")}
         [:input.application__form-text-input
          {:type         "text"
           :class        (str size-class
                              (if show-validation-error-class?
                                " application__form-field-error"
                                " application__form-text-input--normal")
                              (when last?
                                " application__form-text-input--disabled"))
           :value        (cond last?
                               nil
                               (:focused? @local-state)
                               (:value @local-state)
                               :else
                               value)
           :placeholder  (when last? (tu/get-hakija-translation :add-more lang))
           :disabled     (or cannot-edit? (and last? first-is-empty?))
           :aria-invalid (not valid)
           :autoComplete autocomplete-off
           :on-blur      on-blur
           :on-change    on-change}]
         (when (and (not cannot-edit?) (not last?))
           [:a.application__form-repeatable-text--addremove
            [:i.zmdi.zmdi-close.zmdi-hc-lg
             {:on-click remove-field}]])]))))

(defn repeatable-text-field
  [field-descriptor & {:keys [div-kwd idx]
                       :or   {div-kwd :div.application__form-field}}]
  (let [id           (keyword (:id field-descriptor))
        cannot-edit? @(subscribe [:application/cannot-edit? id])
        answer-count @(subscribe [:application/repeatable-answer-count id idx])]
    [div-kwd
     [label field-descriptor]
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [question-hakukohde-names field-descriptor])
     [:div.application__form-text-input-info-text
      [info-text field-descriptor]]
     (doall
      (map (fn [repeatable-idx]
             ^{:key (str id "-" repeatable-idx)}
             [repeatable-text-field-row
              field-descriptor
              idx
              repeatable-idx
              (= repeatable-idx answer-count)])
           (range (if cannot-edit?
                    answer-count
                    (inc answer-count)))))]))

(defn- text-area-size->class [size]
  (match size
         "S" "application__form-text-area__size-small"
         "M" "application__form-text-area__size-medium"
         "L" "application__form-text-area__size-large"
         :else "application__form-text-area__size-medium"))

(defn- parse-max-length [field]
  (let [max-length (-> field :params :max-length)]
    (when-not (or (empty? max-length) (= "0" max-length))
      max-length)))

(defn text-area [field-descriptor & {:keys [div-kwd idx]
                                     :or   {div-kwd :div.application__form-field}}]
  (let [id           (keyword (:id field-descriptor))
        size         (-> field-descriptor :params :size)
        size-class   (text-area-size->class size)
        max-length   (parse-max-length field-descriptor)
        cannot-edit? (subscribe [:application/cannot-edit? id])
        local-state  (r/atom {:focused? false :value nil})]
    (fn [field-descriptor & {:keys [div-kwd idx]
                             :or   {div-kwd :div.application__form-field}}]
      (let [{:keys [value
                    valid]} @(subscribe [:application/answer id idx nil])
            cannot-edit?    @cannot-edit?
            on-change       (if idx
                              (->multi-value-field-change field-descriptor 0 idx)
                              (->textual-field-change field-descriptor))]
        [div-kwd
         [label field-descriptor]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [question-hakukohde-names field-descriptor])
         [:div.application__form-text-area-info-text
          [info-text field-descriptor]]
         [:textarea.application__form-text-input.application__form-text-area
          (merge {:id           id
                  :class        size-class
                  :maxLength    max-length
                  :value        (if (:focused? @local-state)
                                  (:value @local-state)
                                  value)
                  :on-blur      (fn [_]
                                  (swap! local-state assoc
                                         :focused? false))
                  :on-change    (fn [evt]
                                  (swap! local-state assoc
                                         :focused? true
                                         :value (-> evt .-target .-value))
                                  (on-change evt))
                  :required     (is-required-field? field-descriptor)
                  :aria-invalid (not valid)}
                 (when cannot-edit?
                   {:disabled true}))]
         (when max-length
           [:span.application__form-textarea-max-length (str (count value) " / " max-length)])]))))

(declare render-field)

(defn wrapper-field [field-descriptor children]
  (let [languages           (subscribe [:application/default-languages])]
    (fn [field-descriptor children]
      (let [label (util/non-blank-val (:label field-descriptor) @languages)]
        [:div.application__wrapper-element
         [:div.application__wrapper-heading
          [:h2 label]
          [scroll-to-anchor field-descriptor]]
         (into [:div.application__wrapper-contents]
           (for [child children]
             [render-field child]))]))))

(defn- remove-question-group-button [field-descriptor idx]
  (let [mouse-over? (subscribe [:application/mouse-over-remove-question-group-button
                                field-descriptor
                                idx])
        on-mouse-over (fn [_]
                        (dispatch [:application/remove-question-group-mouse-over
                                   field-descriptor
                                   idx]))
        on-mouse-out (fn [_]
                       (dispatch [:application/remove-question-group-mouse-out
                                  field-descriptor
                                  idx]))
        on-click (fn [_]
                   (dispatch [:application/remove-question-group-row
                              field-descriptor
                              idx]))]
    (fn [_ _]
      [(if @mouse-over?
         :i.zmdi.zmdi-close.application__remove-question-group-row.application__remove-question-group-row-mouse-over
         :i.zmdi.zmdi-close.application__remove-question-group-row)
       {:on-mouse-over on-mouse-over
        :on-mouse-out on-mouse-out
        :on-click on-click}])))

(defn- question-group-row [field-descriptor children idx can-remove?]
  (let [mouse-over? (subscribe [:application/mouse-over-remove-question-group-button
                                field-descriptor
                                idx])]
    [(if @mouse-over?
       :div.application__question-group-row.application__question-group-row-mouse-over
       :div.application__question-group-row)
     [:div.application__question-group-row-content
      (for [child children]
        ^{:key (str (:id child) "-" idx)}
        [render-field child :idx idx])]
     (when can-remove?
       [remove-question-group-button field-descriptor idx])]))

(defn question-group [field-descriptor children]
  (let [languages     (subscribe [:application/default-languages])
        label         (util/non-blank-val (:label field-descriptor) @languages)
        row-count     (subscribe [:state-query [:application :ui (-> field-descriptor :id keyword) :count]])
        cannot-edits? (map #(subscribe [:application/cannot-edit? (keyword (:id %))])
                        (util/flatten-form-fields children))
        lang          @(subscribe [:application/form-language])]
    [:div.application__question-group
     (when-not (clojure.string/blank? label)
       [:h3.application__question-group-heading label])
     [scroll-to-anchor field-descriptor]
     [:div
      (doall
       (for [idx (range (or @row-count 1))]
         ^{:key (str "question-group-row-" idx)}
         [question-group-row
          field-descriptor
          children
          idx
          (and (< 1 @row-count) (not (some deref cannot-edits?)))]))]
     (when (not (some deref cannot-edits?))
       [:div.application__add-question-group-row
        [:a {:href     "#"
             :on-click (fn add-question-group-row [event]
                         (.preventDefault event)
                         (dispatch [:application/add-question-group-row field-descriptor]))}
         [:span.zmdi.zmdi-plus-circle.application__add-question-group-plus-sign]
         (tu/get-hakija-translation :add lang)]])]))

(defn row-wrapper [children]
  (into [:div.application__row-field-wrapper]
        ; flatten fields here because 'rowcontainer' may
        ; have nested fields because
        ; of validation (for example :one-of validator)
        (for [child (util/flatten-form-fields children)]
          [render-field child :div-kwd :div.application__row-field.application__form-field])))

(defn- dropdown-followups [field-descriptor value]
  (when-let [followups (seq (util/resolve-followups
                             (:options field-descriptor)
                             value))]
    [:div.application__form-dropdown-followups.animated.fadeIn
     (for [followup followups]
       ^{:key (:id followup)}
       [render-field followup])]))

(defn- non-blank-option-label [option langs]
  (util/non-blank-val (:label option) langs))

(defn dropdown [field-descriptor & {:keys [div-kwd editing idx] :or {div-kwd :div.application__form-field editing false}}]
  (let [application (subscribe [:state-query [:application]])
        languages   (subscribe [:application/default-languages])
        id          (answer-key field-descriptor)
        disabled?   @(subscribe [:application/cannot-edit? id])
        answer      @(subscribe [:application/answer id idx nil])
        on-change   (fn [e]
                      (dispatch [:application/dropdown-change
                                 field-descriptor
                                 (.-value (.-target e))
                                 idx]))
        options     @(subscribe [:application/visible-options field-descriptor])]
    [div-kwd
     [label field-descriptor]
     (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
       [question-hakukohde-names field-descriptor])
     [:div.application__form-text-input-info-text
      [info-text field-descriptor]]
     [:div.application__form-select-wrapper
      (if disabled?
        [:span.application__form-select-arrow.application__form-select-arrow__disabled
         [:i.zmdi.zmdi-chevron-down]]
        [:span.application__form-select-arrow
         [:i.zmdi.zmdi-chevron-down]])
      [(keyword (str "select.application__form-select" (when (not disabled?) ".application__form-select--enabled")))
       {:id           (:id field-descriptor)
        :value        (or (:value answer) "")
        :on-change    on-change
        :disabled     disabled?
        :required     (is-required-field? field-descriptor)
        :aria-invalid (not (:valid answer))}
       (doall
         (concat
           (when
             (and
               (nil? (:koodisto-source field-descriptor))
               (not (:no-blank-option field-descriptor))
               (not= "" (:value (first options))))
             [^{:key (str "blank-" (:id field-descriptor))} [:option {:value ""} ""]])
           (map
             (fn [option]
               [:option {:value (:value option)
                         :key   (:value option)}
                (non-blank-option-label option @languages)])
             (cond->> options
                      (and (some? (:koodisto-source field-descriptor))
                           (not (:koodisto-ordered-by-user field-descriptor)))
                      (sort-by #(non-blank-option-label % @languages))))))]]
     (when-not idx
       (dropdown-followups field-descriptor (:value answer)))]))

(defn- multi-choice-followups [followups]
  [:div.application__form-multi-choice-followups-outer-container
   [:div.application__form-multi-choice-followups-indicator]
   [:div.application__form-multi-choice-followups-container.animated.fadeIn
    (map (fn [followup]
           ^{:key (:id followup)}
           [render-field followup])
         followups)]])

(defn- multiple-choice-option [field-descriptor option parent-id question-group-idx]
  (let [languages    (subscribe [:application/default-languages])
        label        (non-blank-option-label option @languages)
        value        (:value option)
        option-id    (util/component-id)
        cannot-edit? (subscribe [:application/cannot-edit? (keyword (:id field-descriptor))])]
    (fn [field-descriptor option parent-id question-group-idx]
      (let [on-change (fn [_]
                        (dispatch [:application/toggle-multiple-choice-option field-descriptor option question-group-idx]))
            checked?  (subscribe [:application/multiple-choice-option-checked? parent-id value question-group-idx])
            ui        (subscribe [:state-query [:application :ui]])]
        [:div {:key option-id}
         [:input.application__form-checkbox
          (merge {:id        option-id
                  :type      "checkbox"
                  :checked   @checked?
                  :value     value
                  :on-change on-change
                  :role "option"}
                 (when @cannot-edit? {:disabled true}))]
         [:label
          (merge {:for option-id}
                 (when @cannot-edit? {:class "disabled"}))
          label]
         (when (and @checked?
                    (not-empty (:followups option))
                    (some (partial visible? ui) (:followups option))
                    (not question-group-idx))
           [multi-choice-followups (:followups option)])]))))

(defn multiple-choice
  [field-descriptor & {:keys [div-kwd disabled] :or {div-kwd :div.application__form-field disabled false}}]
  (let [id           (answer-key field-descriptor)
        languages    (subscribe [:application/default-languages])]
    (fn [field-descriptor & {:keys [div-kwd disabled idx] :or {div-kwd :div.application__form-field disabled false}}]
      [div-kwd
       [label field-descriptor]
       (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
         [question-hakukohde-names field-descriptor])
       [:div.application__form-text-input-info-text
        [info-text field-descriptor]]
       [:div.application__form-outer-checkbox-container
        {:aria-labelledby (id-for-label field-descriptor)
         :aria-invalid    (not (:valid @(subscribe [:application/answer id idx nil])))
         :role       "listbox"}
        (doall
          (map-indexed (fn [option-idx option]
                         ^{:key (str "multiple-choice-" (:id field-descriptor) "-" option-idx (when idx (str "-" idx)))}
                         [multiple-choice-option field-descriptor option id idx])
                       (cond->> (:options field-descriptor)
                         (and (some? (:koodisto-source field-descriptor))
                              (not (:koodisto-ordered-by-user field-descriptor)))
                         (sort-by #(non-blank-option-label % @languages)))))]])))

(defn- single-choice-option [option parent-id field-descriptor question-group-idx languages use-multi-choice-style? verifying?]
  (let [cannot-edit?   (subscribe [:application/cannot-edit? (keyword (:id field-descriptor))])
        label          (util/non-blank-val (:label option) @languages)
        uncertain?     (subscribe [:state-query [:application :selection-over-network-uncertain?]])
        option-value   (:value option)
        option-id      (util/component-id)
        limit-reached? (subscribe [:application/limit-reached? parent-id option-value])
        checked?       (subscribe [:application/single-choice-option-checked? parent-id option-value question-group-idx])
        valid?         (subscribe [:application/single-choice-option-valid? parent-id question-group-idx])
        on-change      (fn [event]
                         (let [value (.. event -target -value)]
                           (dispatch [:application/select-single-choice-button value field-descriptor question-group-idx])))
        lang           (subscribe [:application/form-language])]
    (fn [option parent-id field-descriptor question-group-idx languages use-multi-choice-style? verifying?]
      (let [unselectable?        (and (or (not @checked?)
                                          (not @valid?))
                                      @limit-reached?)
            disabled?            (or @verifying? @cannot-edit? unselectable?)
            selection-uncertain? @uncertain?
            has-selection-limit? (:selection-limit option)
            sure-if-selected?  (or (not has-selection-limit?) (and (-> field-descriptor :params :selection-group-id) (not selection-uncertain?)))]
        [:div.application__form-single-choice-button-inner-container {:key option-id}
         [:input
          (merge {:id        option-id
                  :type      "checkbox"
                  :checked   (and (not @verifying?) (not unselectable?) sure-if-selected? @checked?)
                  :value     option-value
                  :on-change on-change
                  :role      "radio"
                  :class     (if use-multi-choice-style?
                               "application__form-checkbox"
                               "application__form-single-choice-button")}
            (when disabled? {:disabled true}))]
         [:label
          (merge {:for option-id}
            (when disabled? {:class "disabled"}))
          (when (and @verifying? @checked?)
            [:span.application__form-single-choice-button--verifying
             [:i.zmdi.zmdi-spinner.spin]])
          label
          (when (and (not @verifying?) unselectable?)
            (str " (" (tu/get-hakija-translation :limit-reached @lang) ")"))]
         (when (and (and @checked? @valid?)
                    (not-empty (:followups option))
                    (some (partial visible? (subscribe [:state-query [:application :ui]])) (:followups option)))
           (if use-multi-choice-style?
             (multi-choice-followups (:followups option))
             [:div.application__form-single-choice-followups-indicator]))]))))

(defn- use-multi-choice-style? [single-choice-field langs]
  (or (< 3 (count (:options single-choice-field)))
      (some (fn [option]
              (let [label (util/non-blank-val (:label option) langs)]
                (< 50 (count label))))
        (:options single-choice-field))))

(defn single-choice-button [field-descriptor & {:keys [div-kwd] :or {div-kwd :div.application__form-field}}]
  (let [button-id               (answer-key field-descriptor)
        validators              (:validators field-descriptor)
        languages               (subscribe [:application/default-languages])
        verifying?              (subscribe [:application/fetching-selection-limits? button-id])
        use-multi-choice-style? (use-multi-choice-style? field-descriptor @languages)]
    (fn [field-descriptor & {:keys [div-kwd idx] :or {div-kwd :div.application__form-field}}]
      (let [answer    @(subscribe [:application/answer button-id idx nil])
            options   @(subscribe [:application/visible-options field-descriptor])
            followups (->> options
                           (filter (comp (partial = (:value answer)) :value))
                           (map :followups)
                           (first))]
        [div-kwd
         {:class "application__form-single-choice-button-container"}
         [label field-descriptor]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [question-hakukohde-names field-descriptor])
         [:div.application__form-text-input-info-text
          [info-text field-descriptor]]
         [:div.application__form-single-choice-button-outer-container
          {:aria-labelledby (id-for-label field-descriptor)
           :aria-invalid    (not (:valid answer))
           :role            "radiogroup"
           :class           (when use-multi-choice-style? "application__form-single-choice-button-container--column")}
          (doall
            (map (fn [option]
                   ^{:key (str "single-choice-" (when idx (str idx "-")) (:id field-descriptor) "-" (:value option))}
                   [single-choice-option option button-id field-descriptor idx languages use-multi-choice-style? verifying?])
                 options))]
         (when (and (not idx)
                    (not use-multi-choice-style?)
                    (seq followups)
                    (some (partial visible? (subscribe [:state-query [:application :ui]])) followups))
           [:div.application__form-multi-choice-followups-container.animated.fadeIn
            (for [followup followups]
              ^{:key (:id followup)}
              [render-field followup])])]))))

(defonce max-attachment-size-bytes
  (get (js->clj js/config) "attachment-file-max-size-bytes" (* 10 1024 1024)))

(defn- upload-attachment [field-descriptor question-group-idx event]
  (.preventDefault event)
  (let [file-list (or (some-> event .-dataTransfer .-files)
                      (.. event -target -files))
        files     (->> (.-length file-list)
                       (range)
                       (map #(.item file-list %)))]
    (dispatch [:application/add-attachments field-descriptor question-group-idx files])))

(defn deadline-info [deadline]
  (let [lang @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment--deadline
     (str (tu/get-hakija-translation :deadline-in lang) " " deadline)]))

(defn attachment-upload [field-descriptor component-id attachment-count question-group-idx]
  (let [id       (str component-id (when question-group-idx "-" question-group-idx) "-upload-button")
        lang     @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment-container
     [:input.application__form-upload-input
      {:id           id
       :type         "file"
       :multiple     "multiple"
       :key          (str "upload-button-" component-id "-" attachment-count)
       :on-change    (partial upload-attachment field-descriptor question-group-idx)
       :required     (is-required-field? field-descriptor)
       :aria-invalid (not (:valid @(subscribe [:application/answer id question-group-idx nil])))
       :autoComplete autocomplete-off}]
     [:label.application__form-upload-label
      {:for id}
      [:i.zmdi.zmdi-cloud-upload.application__form-upload-icon]
      [:span.application__form-upload-button-add-text (tu/get-hakija-translation :add-attachment lang)]]
     [:span.application__form-upload-button-info
      [:div
       (tu/get-hakija-translation :file-size-info lang (util/size-bytes->str max-attachment-size-bytes))]
      (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
               (deadline-info deadline))]]))

(defn- attachment-filename
  [id question-group-idx attachment-idx show-size?]
  (let [file (:value @(subscribe [:application/answer
                                  id
                                  question-group-idx
                                  attachment-idx]))
        link @(subscribe [:application/attachment-download-link (:key file)])]
    [:div
     (if (:final file)
       [:a.application__form-attachment-filename
        {:href link}
        (:filename file)]
       [:span.application__form-attachment-filename
        (:filename file)])
     (when (and (some? (:size file)) show-size?)
       [:span (str " (" (util/size-bytes->str (:size file)) ")")])]))

(defn- attachment-remove-button
  [field-descriptor question-group-idx attachment-idx]
  (let [id       (keyword (:id field-descriptor))
        confirm? (r/atom false)]
    (fn [field-descriptor question-group-idx attachment-idx]
      (let [lang         @(subscribe [:application/form-language])
            cannot-edit? @(subscribe [:application/cannot-edit? id])]
        [:div.application__form-attachment-remove-button-container
         (when-not cannot-edit?
           [:button.application__form-attachment-remove-button
            {:on-click #(swap! confirm? not)}
            (if @confirm?
              (tu/get-hakija-translation :cancel-remove lang)
              (tu/get-hakija-translation :remove lang))])
         (when @confirm?
           [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
            {:on-click (fn [event]
                         (reset! confirm? false)
                         (dispatch [:application/remove-attachment
                                    field-descriptor
                                    question-group-idx
                                    attachment-idx]))}
            (tu/get-hakija-translation :confirm-remove lang)])]))))

(defn- cancel-attachment-upload-button
  [field-descriptor question-group-idx attachment-idx]
  (let [confirm? (r/atom false)
        lang     (subscribe [:application/form-language])]
    (fn [field-descriptor question-group-idx attachment-idx]
      [:div.application__form-attachment-remove-button-container
       [:button.application__form-attachment-remove-button
        {:on-click #(swap! confirm? not)}
        (if @confirm?
          (tu/get-hakija-translation :cancel-cancel-upload @lang)
          (tu/get-hakija-translation :cancel-upload @lang))]
       (when @confirm?
         [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
          {:on-click (fn [event]
                       (reset! confirm? false)
                       (dispatch [:application/cancel-attachment-upload
                                  field-descriptor
                                  question-group-idx
                                  attachment-idx]))}
          (tu/get-hakija-translation :confirm-cancel-upload @lang)])])))

(defn attachment-view-file [field-descriptor component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__success
    [attachment-filename component-id question-group-idx attachment-idx true]]
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-check-mark-container
    [:i.zmdi.zmdi-check.application__form-attachment-check-mark]]
   [:div.application__form-attachment-list-item-sub-container
    [attachment-remove-button field-descriptor question-group-idx attachment-idx]]])

(defn attachment-view-file-error [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment @(subscribe [:application/answer
                                component-id
                                question-group-idx
                                attachment-idx])
        lang       @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__error
      [attachment-filename component-id question-group-idx attachment-idx true]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-error-container
      (doall
       (map-indexed (fn [i [error]]
                      ^{:key (str "attachment-error-" i)}
                      [:span.application__form-attachment-error
                       (tu/get-hakija-translation error lang)])
                    (:errors attachment)))]
     [:div.application__form-attachment-list-item-sub-container
      [attachment-remove-button field-descriptor question-group-idx attachment-idx]]]))

(defn attachment-deleting-file [_ component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
    [attachment-filename component-id question-group-idx attachment-idx true]]])

(defn attachment-uploading-file
  [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment       @(subscribe [:application/answer component-id question-group-idx attachment-idx])
        size             (:size (:value attachment))
        uploaded-size    (:uploaded-size attachment)
        upload-complete? (<= size uploaded-size)
        percent          (int (* 100 (/ uploaded-size size)))
        lang             @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
      [attachment-filename component-id question-group-idx attachment-idx false]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-uploading-container
      [:i.zmdi.zmdi-spinner.application__form-upload-uploading-spinner]
      [:span (str (tu/get-hakija-translation
                    (if upload-complete? :processing-file :uploading)
                    lang)
                  "... ")]
      [:span (str percent " % "
                  "(" (util/size-bytes->str uploaded-size false)
                  "/"
                  (util/size-bytes->str size) ")")]]
     [:div.application__form-attachment-list-item-sub-container
      [cancel-attachment-upload-button field-descriptor question-group-idx attachment-idx]]]))

(defn attachment-row [field-descriptor component-id attachment-idx question-group-idx]
  (let [{:keys [status]} @(subscribe [:application/answer
                                      component-id
                                      question-group-idx
                                      attachment-idx])]
    [:li.application__attachment-filename-list-item
     [(case status
        :ready     attachment-view-file
        :error     attachment-view-file-error
        :uploading attachment-uploading-file
        :deleting  attachment-deleting-file)
      field-descriptor component-id question-group-idx attachment-idx]]))

(defn attachment [{:keys [id] :as field-descriptor} & {question-group-idx :idx}]
  (let [languages              (subscribe [:application/default-languages])
        application-identifier (subscribe [:application/application-identifier])
        text                   (reaction (util/non-blank-val (get-in field-descriptor [:params :info-text :value]) @languages))]
    (fn [{:keys [id] :as field-descriptor} & {question-group-idx :idx}]
      (let [attachment-count (reaction (count @(subscribe [:state-query [:application :answers (keyword id) :values question-group-idx]])))]
        [:div.application__form-field
         [label field-descriptor]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [question-hakukohde-names field-descriptor :liitepyynto-for-hakukohde])
         (when-not (clojure.string/blank? @text)
           [markdown-paragraph @text (-> field-descriptor :params :info-text-collapse) @application-identifier])
         (when (> @attachment-count 0)
           [:ol.application__attachment-filename-list
            (->> (range @attachment-count)
                 (map (fn [attachment-idx]
                        ^{:key (str "attachment-" (when question-group-idx (str question-group-idx "-")) id "-" attachment-idx)}
                        [attachment-row field-descriptor id attachment-idx question-group-idx])))])
         (if (get-in field-descriptor [:params :mail-attachment?])
           (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
             [:div.application__mail-attachment--deadline
              [deadline-info deadline]])
           (when-not @(subscribe [:application/cannot-edit? (keyword id)])
             [attachment-upload field-descriptor id @attachment-count question-group-idx]))]))))

(defn- adjacent-field-input [field-descriptor row-idx question-group-idx]
  (let [id          (keyword (:id field-descriptor))
        local-state (r/atom {:focused? false :value nil})]
    (fn [field-descriptor row-idx question-group-idx]
      (let [{:keys [value
                    valid]}       @(subscribe [:application/answer id question-group-idx row-idx])
            cannot-edit? @(subscribe [:application/cannot-edit? id])
            show-error?            @(subscribe [:application/show-validation-error-class? id question-group-idx row-idx nil])
            on-blur      (fn [_]
                           (swap! local-state assoc
                                  :focused? false))
            on-change    (fn [evt]
                           (let [value (-> evt .-target .-value)]
                             (swap! local-state assoc
                                    :focused? true
                                    :value value)
                             (dispatch [:application/set-adjacent-field-answer
                                        field-descriptor
                                        row-idx
                                        value
                                        question-group-idx])))]
        [:input.application__form-text-input
         {:class    (if show-error?
                               " application__form-field-error"
                               " application__form-text-input--normal")
          :id           (str id "-" row-idx)
          :type         "text"
          :value        (if (:focused? @local-state)
                          (:value @local-state)
                          value)
          :on-blur      on-blur
          :on-change    on-change
          :disabled     cannot-edit?
          :aria-invalid (not valid)
          :autoComplete autocomplete-off}]))))

(defn adjacent-text-fields [field-descriptor]
  (let [cannot-edits? (map #(subscribe [:application/cannot-edit? (keyword (:id %))])
                           (util/flatten-form-fields (:children field-descriptor)))]
    (fn [field-descriptor & {question-group-idx :idx}]
      (let [row-amount      (subscribe [:application/adjacent-field-row-amount field-descriptor question-group-idx])
            remove-on-click (fn remove-adjacent-text-field [event]
                              (let [row-idx (int (.getAttribute (.-currentTarget event) "data-row-idx"))]
                                (.preventDefault event)
                                (dispatch [:application/remove-adjacent-field
                                           field-descriptor
                                           row-idx
                                           question-group-idx])))
            add-on-click    (fn add-adjacent-text-field [event]
                              (.preventDefault event)
                              (dispatch [:application/add-adjacent-fields
                                         field-descriptor
                                         question-group-idx]))
            lang            @(subscribe [:application/form-language])]
        [:div.application__form-field
         [label field-descriptor]
         (when (belongs-to-hakukohde-or-ryhma? field-descriptor)
           [question-hakukohde-names field-descriptor])
         [info-text field-descriptor]
         [:div
          (->> (range @row-amount)
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
                                         [adjacent-field-input child row-idx question-group-idx]]))
                                    (:children field-descriptor))
                       (when (and (pos? row-idx) (not (some deref cannot-edits?)))
                         [:a {:data-row-idx row-idx
                              :on-click     remove-on-click}
                          [:span.application__form-adjacent-row--mobile-only (tu/get-hakija-translation :remove-row lang)]
                          [:i.application__form-adjacent-row--desktop-only.i.zmdi.zmdi-close.zmdi-hc-lg]])]))
               doall)]
         (when (and (get-in field-descriptor [:params :repeatable])
                    (not (some deref cannot-edits?)))
           [:a.application__form-add-new-row
            {:on-click add-on-click}
            [:i.zmdi.zmdi-plus-square] (str " " (tu/get-hakija-translation :add-row lang))])]))))

(defn render-field
  [field-descriptor & args]
  (let [ui       (subscribe [:state-query [:application :ui]])
        editing? (subscribe [:state-query [:application :editing?]])]
    (fn [field-descriptor & {:keys [idx] :as args}]
      (if (visible? ui field-descriptor)
        (let [disabled? (get-in @ui [(keyword (:id field-descriptor)) :disabled?] false)]
          (cond-> (match field-descriptor
                         {:id "email"
                          :fieldClass "formField"
                          :fieldType "textField"} [email-field field-descriptor :disabled disabled? :editing editing?]
                         {:fieldClass "wrapperElement"
                          :fieldType  "fieldset"
                          :children   children} [wrapper-field field-descriptor children]
                         {:fieldClass "questionGroup"
                          :fieldType  "fieldset"
                          :children   children} [question-group field-descriptor children]
                         {:fieldClass "wrapperElement"
                          :fieldType  "rowcontainer"
                          :children   children} [row-wrapper children]
                         {:fieldClass "formField" :fieldType "textField" :params {:repeatable true}} [repeatable-text-field field-descriptor]
                         {:fieldClass "formField" :fieldType "textField" :id id} [text-field field-descriptor]
                         {:fieldClass "formField" :fieldType "textArea"} [text-area field-descriptor]
                         {:fieldClass "formField" :fieldType "dropdown"} [dropdown field-descriptor :editing editing?]
                         {:fieldClass "formField" :fieldType "multipleChoice"} [multiple-choice field-descriptor]
                         {:fieldClass "formField" :fieldType "singleChoice"} [single-choice-button field-descriptor]
                         {:fieldClass "formField" :fieldType "attachment"} [attachment field-descriptor]
                         {:fieldClass "formField" :fieldType "hakukohteet"} [hakukohde/hakukohteet field-descriptor]
                         {:fieldClass "pohjakoulutusristiriita" :fieldType "pohjakoulutusristiriita"} [pohjakoulutusristiriita/pohjakoulutusristiriita field-descriptor]
                         {:fieldClass "infoElement"} [info-element field-descriptor]
                         {:fieldClass "wrapperElement" :fieldType "adjacentfieldset"} [adjacent-text-fields field-descriptor])
            (or (:idx args)
                (empty? (:children field-descriptor)))
            (into (flatten (seq args)))))
        [:div]))))

(defn editable-fields [form-data]
  (r/create-class
    {:component-did-mount #(dispatch [:application/setup-window-unload])
     :reagent-render      (fn [form-data]
                            (into
                              [:div.application__editable-content.animated.fadeIn]
                              (for [content (:content form-data)]
                                [render-field content])))}))
