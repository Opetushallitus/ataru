(ns ataru.virkailija.editor.components.dropdown-component
  (:require [ataru.cljs-util :as util]
            [ataru.application-common.application-field-common :refer [copy-link]]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
            [ataru.virkailija.editor.components.input-field-component :as input-field-component]
            [ataru.virkailija.editor.components.validator-checkbox-component :as validator-checkbox-component]
            [ataru.virkailija.editor.components.checkbox-component :as checkbox-component]
            [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
            [ataru.virkailija.editor.components.info-addon-component :as info-addon-component]
            [ataru.virkailija.editor.components.prevent-submission-component :as prevent-submission-component]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [ataru.virkailija.editor.components.followup-question :as followup-question]
            [clojure.string :as string]
            [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]))

(defn- required-disabled [initial-content]
  (contains? (-> initial-content :validators set) "required-hakija"))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- get-val [event]
  (-> event .-target .-value))

(defn- koodisto-field [component idx lang]
  (let [value (get-in component [:label lang])]
    [:div.editor-form__koodisto-field-container
     {:key (str "option-" lang "-" idx)}
     [:div.editor-form__koodisto-field
      {:on-drop      prevent-default
       :data-test-id "editor-form__koodisto-field"}
      value]]))

(defn- koodisto-fields-with-lang [_ _]
  (fn [languages option-path]
    (let [component @(subscribe [:editor/get-component-value option-path])]
      [:div
       {:title (clojure.string/join ", " (map (fn [lang] (get-in component [:label lang])) languages))}
       (map-indexed (partial koodisto-field component)
         languages)])))

(defn- remove-dropdown-option-button [path option-index disabled? parent-key option-value question-group-element?]
  [:div.editor-form__multi-options-remove--cross
   [copy-link (str parent-key "_" (when question-group-element? "groupN_") option-value) :answer? true :shared-use-warning? false]
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (prevent-default evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index])))
     :class    (when disabled? "editor-form__multi-options-remove--cross--disabled")}]])

(defn- dropdown-option
  [option-index _ _ _ path _ show-followups _ _ _ & _]
  (let [component-locked? (subscribe [:editor/component-locked? path])
        on-click          (fn [up? event]
                            (when-not @component-locked?
                              (prevent-default event)
                              (reset! show-followups nil)
                              (dispatch [(if up?
                                           :editor/move-option-up
                                           :editor/move-option-down) path option-index])))
        selection-limit?  (subscribe [:editor/selection-limit? path])
        only-numbers      (fn [value]
                            (let [n (string/replace value #"\D" "")]
                              (if (empty? n)
                                nil
                                (js/parseInt n))))
        toisen-asteen-yhteishaku? (subscribe [:editor/toisen-asteen-yhteishaku?])]
    (fn [option-index option-count option-path followups path languages show-followups parent-key option-value question-group-element? &
         {:keys [editable?]
          :or   {editable? true}}]
      [:div
       [:div.editor-form__multi-options-wrapper-outer
        [:div
         [:div.editor-form__multi-options-arrows-container
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--up
           {:on-click (partial on-click true)
            :class    (when @component-locked? "editor-form__multi-options-arrow--disabled")}]
          [:div.editor-form__multi-options-arrows--stretch]
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--down
           {:on-click (partial on-click false)
            :class    (when @component-locked? "editor-form__multi-options-arrow--disabled")}]]]
        [:div.editor-form__multi-options-wrapper-inner
         {:key (str "options-" option-index)}
         (if editable?
           (input-fields-with-lang-component/input-fields-with-lang
             (fn [lang]
               [input-field-component/input-field {:path        option-path
                                                   :lang        lang
                                                   :dispatch-fn #(dispatch [:editor/set-dropdown-option-value
                                                                            (-> % .-target .-value)
                                                                            option-path :label lang])}])
             languages)
           [koodisto-fields-with-lang languages option-path])]
        (when @selection-limit?
          [:div.editor-form__selection-limit
           [input-field-component/input-field {:path        option-path
                                               :lang        :dont-care
                                               :dispatch-fn #(dispatch [:editor/set-dropdown-option-selection-limit
                                                                        (only-numbers (-> % .-target .-value))
                                                                        option-path :selection-limit])
                                               :placeholder @(subscribe [:editor/virkailija-translation :selection-limit-input])
                                               :class       "editor-form__text-field--selection-limit"
                                               :value-fn    (fn [v] (:selection-limit v))}]])
        [followup-question/followup-question option-index followups show-followups]
        [belongs-to-hakukohteet-component/belongs-to-hakukohteet-option parent-key option-index option-path]
        (when @toisen-asteen-yhteishaku?
          [prevent-submission-component/prevent-submission-option option-path])
        (when editable?
          [remove-dropdown-option-button path option-index (or @component-locked? (< option-count 3)) parent-key option-value question-group-element?])]
       [followup-question/followup-question-overlay option-index followups path show-followups]])))

(defn- select-koodisto-dropdown
  [path]
  (let [id                (util/new-uuid)
        selected-koodisto (subscribe [:editor/get-component-value path :koodisto-source])]
    (fn [path]
      [:div.editor-form__koodisto-options
       [:label.editor-form__select-koodisto-dropdown-label
        {:for id}
        @(subscribe [:editor/virkailija-translation :koodisto])]
       [:div.editor-form__select-koodisto-dropdown-wrapper
        [:select.editor-form__select-koodisto-dropdown
         {:id           id
          :class        (if (string/blank? (:uri @selected-koodisto))
                          "editor-form__select-koodisto-dropdown--invalid"
                          "editor-form__select-koodisto-dropdown--regular")
          :value        (:uri @selected-koodisto)
          :on-change    #(dispatch [:editor/select-koodisto-options (.-value (.-target %)) path (:allow-invalid? @selected-koodisto)])
          :data-test-id "editor-form__select-koodisto-dropdown"}
         (when (= (:uri @selected-koodisto) "")
           [:option {:value "" :disabled true} ""])
         (for [{:keys [uri title]} koodisto-whitelist/koodisto-whitelist]
           ^{:key (str "koodisto-" uri)}
           [:option {:value uri} title])]
        [:div.editor-form__select-koodisto-dropdown-arrow
         [:i.zmdi.zmdi-chevron-down]]]])))

(defn- lomakeosion-piilottaminen-arvon-perusteella
  [_path]
  (let [id (util/new-uuid)]
    (fn [path]
      (let [section-visibility-conditions @(subscribe [:editor/get-component-value path :section-visibility-conditions])
            component-locked?             @(subscribe [:editor/component-locked? path])
            has-conditions?               (boolean (seq section-visibility-conditions))
            checked?                      has-conditions?
            disabled?                     (or component-locked?
                                            has-conditions?)]
        [:div.editor-form__text-field-additional-params-container
         [:input.editor-form__text-field-checkbox
          {:id           id
           :type         "checkbox"
           :checked      checked?
           :disabled     disabled?
           :on-change    (fn [evt]
                           (when-not disabled?
                             (prevent-default evt)
                             (when (-> evt .-target .-checked)
                               (dispatch [:editor/lisää-pudotusvalikon-arvon-perusteella-osion-piilottamis-ehto path]))))
           :data-test-id "dropdown-lomakeosion-nayttaminen-arvon-perusteella"}]
         [:label.editor-form__text-field-checkbox-label
          {:for   id
           :class (when disabled? "editor-form__text-field-checkbox-label--disabled")}
          @(subscribe [:editor/virkailija-translation :lomakeosion-piilottaminen-arvon-perusteella])]
         (when checked?
           [:div.editor-form__text-field-checkbox-add-condition
            [:span " | "]
            (let [teksti @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-lisaa-ehto])]
              (if component-locked?
                [:span.editor-form__text-field-checkbox-add-condition-disabled teksti]
                [:a
                 {:on-click (fn [] (dispatch [:editor/lisää-pudotusvalikon-arvon-perusteella-osion-piilottamis-ehto path]))}
                 teksti]))])]))))

(defn- visibility-condition-value-selector
  [{:keys [path]}]
  (let [component-locked? (subscribe [:editor/component-locked? path])
        id                (util/new-uuid)
        dropdown-options  (subscribe [:editor/get-component-value path :options])]
    (fn [{:keys [visibility-condition-index path]}]
      [:div.editor-form__text-field-option-condition
       [:label.editor-form__text-field-option-condition-label
        {:for   id
         :class (when @component-locked? "editor-form__textfield-option-condition--disabled")}
        @(subscribe [:editor/virkailija-translation :lisakysymys-arvon-perusteella-ehto])]
       [:select.editor-form__text-field-option-condition-answer-compared-to
        {:disabled     @component-locked?
         :on-change    (fn [event]
                         (dispatch [:editor/set-visibility-condition-value
                                    path
                                    visibility-condition-index
                                    (get-val event)]))
         :value        (or @(subscribe [:editor/visibility-condition-value path visibility-condition-index]) "")
         :data-test-id "dropdown-lomakeosion-piilottaminen-arvon-perusteella-vertailuarvo"}
        (for [option @dropdown-options
              :let [value (:value option)
                    label (:fi (:label option))]]
          ^{:key (:value option)}
          [:option {:value value} label])]])))

(defn- visibility-condition-section-selector
  [{:keys [path
           visibility-condition-index]}]
  (let [component-locked?      @(subscribe [:editor/component-locked? path])
        section-name           @(subscribe [:editor/visibility-condition-section-name path visibility-condition-index])
        hideable-form-sections @(subscribe [:editor/current-lomakeosiot])
        lang                   @(subscribe [:editor/virkailija-lang])]
    [:div.editor-form__text-field-hideable-section-selector
     [:div.editor-form__text-field-hideable-section-selector--instruction
      @(subscribe [:editor/virkailija-translation :lomakeosion-piilottaminen-arvon-perusteella-valitse-osio])]
     [:select
      {:disabled     component-locked?
       :on-change    (fn [event]
                       (dispatch [:editor/set-visibility-condition-section
                                  path
                                  visibility-condition-index
                                  (get-val event)]))
       :value        (some #(when (= section-name (:id %)) (:id %))
                       hideable-form-sections)
       :data-test-id "dropdown-lomakeosion-piilottaminen-arvon-perusteella-osio"}
      (for [form-section hideable-form-sections]
        ^{:key (:id form-section)}
        [:option {:value (:id form-section)} (-> form-section :label lang)])]]))

(defn- remove-visibility-condition
  [{:keys [disabled?
           path
           visibility-condition-index]}]
  [:div.editor-form__text-field-remove-option
   {:class (when disabled? "editor-form__text-field-remove-option--disabled")}
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (prevent-default evt)
                   (dispatch [:editor/remove-visibility-condition path visibility-condition-index])))}]])

(defn- section-visibility-conditions-row
  [{:keys [visibility-condition-index
           path]}]
  (let [component-locked? @(subscribe [:editor/component-locked? path])]
    [:div.editor-form__text-field-option-followups-header
     [visibility-condition-value-selector
      {:path                       path
       :visibility-condition-index visibility-condition-index}]
     [visibility-condition-section-selector
      {:path                       path
       :visibility-condition-index visibility-condition-index}]
     [remove-visibility-condition
      {:disabled?                  component-locked?
       :path                       path
       :visibility-condition-index visibility-condition-index}]]))

(defn- section-visibility-conditions-component
  [path]
  (let [section-visibility-conditions @(subscribe [:editor/get-component-value path :section-visibility-conditions])]
    [:<>
     (doall
       (map-indexed
         (fn [index visibility-condition]
           ^{:key (str "visibility-conditions-" index)}
           [:div.editor-form__text-field-option-followups-wrapper
            {:data-test-id "pudotusvalikko-lomakeosion-näyttämissääntö"}
            [section-visibility-conditions-row
             {:visibility-condition       visibility-condition
              :visibility-condition-index index
              :path                       path}]])
         section-visibility-conditions))]))

(defn- custom-answer-options [_ _ _ _ _ _ show-followups parent-key]
  (fn [languages options followups path question-group-element? editable? _ _]
    (let [option-count (count options)]
      (when (or (nil? @show-followups)
              (not (= (count @show-followups) option-count)))
        (reset! show-followups (vec (replicate option-count false))))
      [:div.editor-form__multi-options-container
       {:data-test-id "editor-form__multi-options-container"}
       (doall (map-indexed (fn [idx option]
                             ^{:key (str "options-" idx)}
                             [dropdown-option
                              idx
                              option-count
                              (conj path :options idx)
                              (nth followups idx)
                              path
                              languages
                              show-followups
                              parent-key
                              (:value option)
                              question-group-element?
                              :editable? editable?])
                options))])))

(defn koodisto-answer-options [_ _ _ _ parent-key]
  (let [opened? (r/atom false)]
    (fn [_ followups path question-group-element?]
      (let [languages             @(subscribe [:editor/languages])
            value                 @(subscribe [:editor/get-component-value path])
            editable?             false
            hide-koodisto-options (fn [_]
                                    (reset! opened? false))
            show-koodisto-options (fn [_]
                                    (reset! opened? true))
            show-followups        (r/atom nil)]
        (if (not @opened?)
          [:div.editor-form__show-koodisto-values
           [:a
            {:on-click     show-koodisto-options
             :data-test-id "editor-form__show_koodisto-values__link"}
            [:i.zmdi.zmdi-chevron-down] (str " " @(subscribe [:editor/virkailija-translation :show-options]))]]
          [:div
           [:div.editor-form__show-koodisto-values
            [:a
             {:on-click hide-koodisto-options}
             [:i.zmdi.zmdi-chevron-up] (str " " @(subscribe [:editor/virkailija-translation :hide-options]))]]
           [custom-answer-options languages (:options value) followups path question-group-element? editable? show-followups parent-key]])))))

(defn dropdown [_ _ path _]
  (let [languages                 (subscribe [:editor/languages])
        options-koodisto          (subscribe [:editor/get-component-value path :koodisto-source])
        koodisto-ordered-by-user  (subscribe [:editor/get-component-value path :koodisto-ordered-by-user])
        value                     (subscribe [:editor/get-component-value path])
        support-selection-limit?  (subscribe [:editor/dropdown-with-selection-limit? path])
        selected-form-key         (subscribe [:editor/selected-form-key])
        koodisto-ordered-id       (util/new-uuid)
        component-locked?         (subscribe [:editor/component-locked? path])
        is-per-hakukohde-allowed  (subscribe [:editor/is-per-hakukohde-allowed path])
        allow-invalid-koodis-id   (util/new-uuid)
        toisen-asteen-yhteishaku? (subscribe [:editor/toisen-asteen-yhteishaku?])
        admin?                    (subscribe [:editor/superuser?])]
    (fn [initial-content followups path {:keys [question-group-element?]}]
      (let [languages      @languages
            field-type     (:fieldType @value)
            show-followups (r/atom nil)]
        [:div.editor-form__component-wrapper
         {:data-test-id (str "editor-form__" field-type "-component-wrapper")}
         (let [header       (case field-type
                              "dropdown" (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :dropdown-koodisto])
                                           @(subscribe [:editor/virkailija-translation :dropdown]))
                              "singleChoice" (if (some? @options-koodisto)
                                               @(subscribe [:editor/virkailija-translation :single-choice-button-koodisto])
                                               @(subscribe [:editor/virkailija-translation :single-choice-button]))
                              "multipleChoice" (if (some? @options-koodisto)
                                                 @(subscribe [:editor/virkailija-translation :multiple-choice-koodisto])
                                                 @(subscribe [:editor/virkailija-translation :multiple-choice])))
               data-test-id (str "editor-form__" field-type "-component-main")]
           [text-header-component/text-header (:id initial-content) header path (:metadata initial-content)
            :sub-header (:label @value) :data-test-id data-test-id])
         [component-content/component-content
          path
          [:div
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-question-wrapper
             [:div.editor-form__text-field-wrapper
              {:data-test-id (str "editor-form__" field-type "-component-question-wrapper")}
              [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
               [copy-link (:id initial-content)]]
              (input-fields-with-lang-component/input-fields-with-lang
                (fn [lang]
                  [input-field-component/input-field {:path        path
                                                      :lang        lang
                                                      :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                    (-> % .-target .-value)
                                                                                    path :label lang])
                                                      :data-test-id (str "editor-form__" field-type "-label")}])
                languages
                :header? true)]
             [:div.editor-form__checkbox-wrapper
              [validator-checkbox-component/validator-checkbox path initial-content :required (required-disabled initial-content)]
              (when (or @toisen-asteen-yhteishaku? (and @admin? (-> initial-content :sensitive-answer boolean)))
                [checkbox-component/checkbox path initial-content :sensitive-answer])
              (when (and (seq (:belongs-to-hakukohderyhma initial-content))
                      @is-per-hakukohde-allowed
                      (nil? @options-koodisto))
                [checkbox-component/checkbox path initial-content :per-hakukohde
                 #(dispatch [:editor/clean-per-hakukohde-followups path])])
              (when @support-selection-limit?
                [validator-checkbox-component/validator-checkbox path initial-content :selection-limit nil
                 #(dispatch [:editor/set-selection-group-id (when % @selected-form-key) path])])
              (when (some? @options-koodisto)
                [:div.editor-form__checkbox-container
                 [:input.editor-form__checkbox
                  {:id        koodisto-ordered-id
                   :type      "checkbox"
                   :checked   (not @koodisto-ordered-by-user)
                   :disabled  @component-locked?
                   :on-change (fn [event]
                                (dispatch [:editor/set-ordered-by-user (-> event .-target .-checked) path]))}]
                 [:label.editor-form__checkbox-label
                  {:for koodisto-ordered-id}
                  @(subscribe [:editor/virkailija-translation :alphabetically])]])
              (when (some? @options-koodisto)
                [:div.editor-form__checkbox-container
                 [:input.editor-form__checkbox
                  {:id        allow-invalid-koodis-id
                   :type      "checkbox"
                   :checked   (boolean (:allow-invalid? @options-koodisto))
                   :disabled  @component-locked?
                   :on-change #(dispatch [:editor/select-koodisto-options
                                          (:uri @options-koodisto)
                                          path
                                          (not (:allow-invalid? @options-koodisto))])}]
                 [:label.editor-form__checkbox-label
                  {:for allow-invalid-koodis-id}
                  @(subscribe [:editor/virkailija-translation :allow-invalid-koodis])]])
              [lomakeosion-piilottaminen-arvon-perusteella path]]
             [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]
           [info-addon-component/info-addon path initial-content]
           [:div.editor-form__component-row-wrapper
            [section-visibility-conditions-component path]]
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-options_wrapper
             (if (some? @options-koodisto)
               [select-koodisto-dropdown path]
               [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :options])])
             (if (nil? @options-koodisto)
               [custom-answer-options languages (:options @value) followups path question-group-element? true show-followups (:id initial-content)]
               [koodisto-answer-options (:id @value) followups path question-group-element? (:id initial-content)])
             (when (nil? @options-koodisto)
               [:div.editor-form__add-dropdown-item
                [:a
                 {:on-click (fn [evt]
                              (when-not @component-locked?
                                (prevent-default evt)
                                (reset! show-followups nil)
                                (dispatch [:editor/add-dropdown-option path])))
                  :class    (when @component-locked? "editor-form__add-dropdown-item--disabled")}
                 [:i.zmdi.zmdi-plus-square] (str " " @(subscribe [:editor/virkailija-translation :add]))]])]]]]]))))
