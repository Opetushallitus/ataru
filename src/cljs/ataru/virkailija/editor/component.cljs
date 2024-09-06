(ns ataru.virkailija.editor.component
  (:require
   [ataru.application-common.application-field-common :refer [copy-link]]
   [ataru.cljs-util :as util]
   [ataru.component-data.person-info-module :as pm]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
   [cljs.core.match :refer-macros [match]]
   [goog.string :as s]
   [cljs-time.core :as t]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :as r]
   [ataru.component-data.module.module-spec :as module-spec]
   [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
   [ataru.virkailija.editor.components.component-content :as component-content]
   [ataru.virkailija.editor.components.info-addon-component :as info-addon-component]
   [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
   [ataru.virkailija.editor.components.input-field-component :as input-field-component]
   [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]
   [ataru.virkailija.editor.components.repeater-checkbox-component :as repeater-checkbox-component]
   [ataru.virkailija.editor.components.text-component :as text-component]
   [ataru.virkailija.editor.components.text-header-component :as text-header-component]
   [ataru.virkailija.editor.components.validator-checkbox-component :as validator-checkbox-component]
   [clojure.string :as string]
   [ataru.virkailija.editor.components.checkbox-component :as checkbox-component]))

(defn- required-disabled [initial-content]
  (contains? (-> initial-content :validators set) "required-hakija"))

(defn text-field [initial-content followups path]
  [text-component/text-component initial-content followups path
   :header-label @(subscribe [:editor/virkailija-translation :text-field])
   :size-label @(subscribe [:editor/virkailija-translation :text-field-size])])

(defn text-area [initial-content followups path]
  [text-component/text-component initial-content followups path
   :header-label @(subscribe [:editor/virkailija-translation :text-area])
   :size-label @(subscribe [:editor/virkailija-translation :text-area-size])])

(defn component-group [content path children]
  (let [id                (:id content)
        languages         @(subscribe [:editor/languages])
        value             @(subscribe [:editor/get-component-value path])
        group-header-text (case (:fieldClass content)
                            "wrapperElement" @(subscribe [:editor/virkailija-translation :wrapper-element])
                            "questionGroup"  @(subscribe [:editor/virkailija-translation :question-group]))
        header-label-text (case (:fieldClass content)
                            "wrapperElement" @(subscribe [:editor/virkailija-translation :wrapper-header])
                            "questionGroup"  @(subscribe [:editor/virkailija-translation :group-header]))]
    [:div.editor-form__component-wrapper
     {:data-test-id (str "editor-form__" (:fieldClass content) "-component-wrapper")}
     [text-header-component/text-header id group-header-text path (:metadata content)
      :sub-header (:label value)]
     [component-content/component-content
      path ;id
      [:div
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header header-label-text]
        (input-fields-with-lang-component/input-fields-with-lang
         (fn [lang]
           [input-field-component/input-field {:path        path
                                               :lang        lang
                                               :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                             (-> % .-target .-value)
                                                                             path :label lang])}])
         languages
         :header? true)]
       [:div.editor-form__wrapper-element-well
        children]
       [dnd/drag-n-drop-spacer (conj path :children (count children))]
       (when-not @(subscribe [:editor/component-locked? path])
         (case (:fieldClass content)
           "wrapperElement" [toolbar/add-component (conj path :children (count children)) false]
           "questionGroup"  [toolbar/question-group-toolbar path
                             (fn [generate-fn]
                               (dispatch [:generate-component generate-fn (conj path :children (count children))]))]))]]]))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
              "questionGroup" (map #(recursively-get-labels %) (:children component))
              "wrapperElement" (map #(recursively-get-labels %) (:children component))
              :else (or (-> component :label lang)
                        (-> component :label :fi))))]
    (flatten (recursively-get-labels component))))

(defn hakukohteet-module [_ path]
  (let [virkailija-lang (subscribe [:editor/virkailija-lang])
        value           (subscribe [:editor/get-component-value path])
        component-locked (subscribe [:editor/component-locked? path])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
        :foldable? false
        :can-cut? true
        :can-copy? false
        :can-remove? false
        :data-test-id "hakukohteet-header"]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
         [:div.editor-form__text-field-checkbox-wrapper
          [:div.editor-form__checkbox-container
           [:input.editor-form__checkbox
            {:id           "hakukohteet-show-hakukohde-in-hakukohde-questions"
             :type         "checkbox"
             :disabled     @component-locked
             :data-test-id "hakukohteet-auto-expand-toggle"
             :checked      @(subscribe [:editor/auto-expand-hakukohteet])
             :on-change    (fn [event]
                             (.preventDefault event)
                             (dispatch [:editor/toggle-auto-expand-hakukohteet]))}]
           [:label.editor-form__checkbox-label
            {:for "hakukohteet-show-hakukohde-in-hakukohde-questions"}
            @(subscribe [:editor/virkailija-translation :auto-expand-hakukohteet])]]
          [:div.editor-form__checkbox-container
           [:input.editor-form__checkbox
            {:id           "hakukohteet-order-by-opetuskieli-in-hakukohde-questions"
             :type         "checkbox"
             :disabled     @component-locked
             :data-test-id "hakukohteet-order-by-opetuskieli-toggle"
             :checked      @(subscribe [:editor/order-hakukohteet-by-opetuskieli])
             :on-change    (fn [event]
                             (.preventDefault event)
                             (dispatch [:editor/toggle-order-hakukohteet-by-opetuskieli]))}]
           [:label.editor-form__checkbox-label
            {:for "hakukohteet-order-by-opetuskieli-in-hakukohde-questions"}
            @(subscribe [:editor/virkailija-translation :order-hakukohteet-by-opetuskieli])]]]
         @(subscribe [:editor/virkailija-translation :hakukohde-info])]]])))

(defn module [content path]
  (let [{:keys [foldable?
                can-cut?
                can-copy?
                can-remove?
                show-child-component-names?
                has-multiple-configurations?]} (-> content :module name module-spec/get-module-spec)
        value             (subscribe [:editor/get-component-value path])
        virkailija-lang   (subscribe [:editor/virkailija-lang])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [content path]
      (let [module-name         (-> content :module keyword)
            data-test-id-prefix (case module-name
                                  :person-info "henkilotietomoduuli"
                                  :arvosanat-peruskoulu "arvosanat-moduuli"
                                  nil)]
        [:div.editor-form__component-wrapper
         [text-header-component/text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
          :foldable? foldable?
          :can-cut? can-cut?
          :can-copy? can-copy?
          :can-remove? can-remove?
          :show-child-component-names? show-child-component-names?
          :data-test-id (some-> data-test-id-prefix (str "-header"))]
         [:div.editor-form__component-content-wrapper
          (when has-multiple-configurations?
            (let [values (set ["onr" "onr-2nd" "muu"])]
              [:div.editor-form__module-fields
               [:select.editor-form__select
                {:on-change    (fn [event]
                                 (let [version    (keyword (-> event .-target .-value))
                                       new-module (pm/person-info-module version)]
                                   (dispatch-sync [:editor/set-component-value
                                                   new-module path])))
                 :disabled     @component-locked?
                 :value        (or (get values (:id content)) "onr")
                 :data-test-id (some-> data-test-id-prefix (str "-select"))}
                (doall (for [opt values]
                         [:option {:value opt
                                   :key   opt} @(subscribe [:editor/virkailija-translation (keyword (str "person-info-module-" opt))])]))]]))
          (when show-child-component-names?
            [:div.editor-form__module-fields
             [:span.editor-form__module-fields-label
              @(subscribe [:editor/virkailija-translation :contains-fields])]
             " "
             [:span
              {:data-test-id (some-> data-test-id-prefix (str "-fields-label"))}
              (string/join ", " (get-leaf-component-labels @value @(subscribe [:editor/virkailija-lang])))]])]]))))

(defn pohjakoulutusristiriita
  [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) (get-in initial-content [:label :fi]) path (:metadata initial-content)]
       [component-content/component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:div.infoelement
            (->> (input-fields-with-lang-component/input-fields-with-lang
                  (fn [lang]
                    [input-field-component/input-field {:path        path
                                                        :lang        lang
                                                        :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                      (-> % .-target .-value)
                                                                                      path :text lang])
                                                        :value-fn    (fn [component] (get-in component [:text lang]))
                                                        :tag         :textarea}])
                  @languages
                  :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help-component/markdown-help)]])))
                 doall)]]]]]])))

(defn adjacent-fieldset [_ path _]
  (let [languages         (subscribe [:editor/languages])
        sub-header        (subscribe [:editor/get-component-value path :label])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       {:data-test-id "editor-form__adjacent-fieldset-component-wrapper"}
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :adjacent-fieldset]) path (:metadata content)
        :sub-header @sub-header]
       [component-content/component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
           (input-fields-with-lang-component/input-fields-with-lang
            (fn [lang]
              [input-field-component/input-field {:path        path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                (-> % .-target .-value)
                                                                                path :label lang])}])
            @languages
            :header? true)]
          [:div.editor-form__checkbox-wrapper
           [repeater-checkbox-component/repeater-checkbox path content]]
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]
         [info-addon-component/info-addon path]
         [:div.editor-form__adjacent-fieldset-container
          children
          (when (and (not @component-locked?)
                     (-> (count children) (< 3)))
            [toolbar/adjacent-fieldset-toolbar
             (concat path [:children])
             (fn [component-fn]
               (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]]]])))

(defn adjacent-text-field [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :text-field]) path (:metadata content)
        :foldable? false
        :can-cut? false
        :can-copy? false
        :can-remove? true]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__component-row-wrapper
         [:div.editor-form__text-field-wrapper
          [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
           [copy-link (:id content)]]
          (input-fields-with-lang-component/input-fields-with-lang
           (fn [lang]
             [input-field-component/input-field {:path        path
                                                 :lang        lang
                                                 :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                               (-> % .-target .-value)
                                                                               path :label lang])}])
           @languages
           :header? true)]
         [:div.editor-form__checkbox-wrapper
          [validator-checkbox-component/validator-checkbox path content :required (required-disabled content)]
          [text-component/text-component-type-selector (:id content) path {:adjacent-text-field? true
                                                                           :allow-decimals?      true}]]
         [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]]])))

(defn attachment-textarea [_ path]
  (let [checked?                   (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        mail-attachment?           (subscribe [:editor/get-component-value path :params :mail-attachment?])
        fetch-info-from-kouta?     (subscribe [:editor/get-component-value path :params :fetch-info-from-kouta?])
        selected-attachment-type?  (subscribe [:editor/get-component-value path :params :attachment-type])
        attachment-types-koodisto? (subscribe [:editor/get-attachment-types-koodisto])
        collapse?                  (subscribe [:editor/get-component-value path :params :info-text-collapse])
        languages                  (subscribe [:editor/languages])
        lang                       (subscribe [:editor/virkailija-lang])
        is-per-hakukohde-allowed   (subscribe [:editor/is-per-hakukohde-allowed path])
        component-locked?          (subscribe [:editor/component-locked? path])
        has-parent-per-hakukohde   (subscribe [:editor/has-parent-per-hakukohde path])]
    (fn [initial-content path]
      [:div.editor-form__info-addon-wrapper
       (let [id (util/new-uuid)]
         [:div.editor-form__info-addon-checkbox
          [:input {:id        id
                   :type      "checkbox"
                   :checked   @mail-attachment?
                   :disabled  @component-locked?
                   :on-change (fn toggle-attachment-textarea [event]
                                (.preventDefault event)
                                (let [mail-attachment? (.. event -target -checked)]
                                  (dispatch [:editor/update-mail-attachment mail-attachment? path])))}]
          [:label
           {:for   id
            :class (when @component-locked? "editor-form__checkbox-label--disabled")}
           @(subscribe [:editor/virkailija-translation :mail-attachment-text])]])
       (when-not @mail-attachment?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   @checked?
                     :disabled  @component-locked?
                     :on-change (fn toggle-attachment-textarea [event]
                                  (.preventDefault event)
                                  (let [checked? (.. event -target -checked)]
                                    (dispatch [:editor/set-component-value checked? path :params :info-text :enabled?])))}]
            [:label
             {:for   id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :attachment-info-text])]]))
       (when (and (seq (:belongs-to-hakukohderyhma initial-content))
                  @is-per-hakukohde-allowed
                  @mail-attachment?)
         [checkbox-component/checkbox path initial-content :per-hakukohde
          (fn [] (dispatch [:editor/set-component-value false path :params :fetch-info-from-kouta?]))])
       (when (and @mail-attachment?
                  (or (-> initial-content :per-hakukohde boolean)
                      @has-parent-per-hakukohde))
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   @fetch-info-from-kouta?
                     :disabled  @component-locked?
                     :on-change (fn toggle-attachment-textarea [event]
                                  (.preventDefault event)
                                  (let [checked? (.. event -target -checked)]
                                    (dispatch [:editor/set-component-value checked? path :params :fetch-info-from-kouta?])))}]
            [:label
             {:for  id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :fetch-info-from-kouta])]]))
       (when (and @fetch-info-from-kouta?
                  (seq @attachment-types-koodisto?))
         (let [id (util/new-uuid)]
           [:div.editor-form__koodisto-options
            [:label.editor-form__select-koodisto-dropdown-label
             {:for id}
             @(subscribe [:editor/virkailija-translation :attachment-type])]
            [:div.editor-form__select-koodisto-dropdown-wrapper
             [:select.editor-form__select-koodisto-dropdown
              {:id        id
               :class     (if (string/blank? @selected-attachment-type?)
                            "editor-form__select-koodisto-dropdown--invalid"
                            "editor-form__select-koodisto-dropdown--regular")
               :value     @selected-attachment-type?
               :on-change (fn select-attachment-type [event]
                            (.preventDefault event)
                            (dispatch [:editor/set-component-value (.. event -target -value) path :params :attachment-type]))
               :data-test-id "editor-form__select-koodisto-dropdown"}
              (when (string/blank? @selected-attachment-type?)
                [:option {:value @selected-attachment-type?} ""])
              (doall (for [{:keys [uri label]} @attachment-types-koodisto?]
                       ^{:key (str "attachment-type-" id "-" uri)}
                       [:option {:value uri} (get label @lang)]))]
             [:div.editor-form__select-koodisto-dropdown-arrow
              [:i.zmdi.zmdi-chevron-down]]]]))
       (when @checked?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   (boolean @collapse?)
                     :disabled  @component-locked?
                     :on-change (fn [event]
                                  (dispatch [:editor/set-component-value
                                             (-> event .-target .-checked)
                                             path :params :info-text-collapse]))}]
            [:label
             {:for   id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :collapse-info-text])]]))
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang-component/input-fields-with-lang
                (fn attachment-textarea-input [lang]
                  [input-field-component/input-field {:path        path
                                                      :lang        lang
                                                      :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                    (-> % .-target .-value)
                                                                                    path :params :info-text :value lang])
                                                      :value-fn    #(get-in % [:params :info-text :value lang])
                                                      :tag         :textarea}])
                @languages
                :header? true)
               (map (fn [field]
                      (into field [[:div.editor-form__info-addon-markdown-anchor
                                    (markdown-help-component/markdown-help)]])))
               doall)])])))


(def ^:private deadline-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4}) (\d{1,2}):(\d{1,2})$")
(def ^:private deadline-format "%d.%d.%d %02d:%02d")

(defn deadline-date [deadline]
  (when-let [[_ day month year hours minutes] (map js/parseInt (re-matches deadline-pattern deadline))]
    (let [dt (t/date-time year month day hours minutes)]
      (when (= [year month day hours minutes]
               [(.getYear dt) (inc (.getMonth dt)) (.getDate dt)
                (.getHours dt)
                (.getMinutes dt)])
        (s/format deadline-format day month year hours minutes)))))

(defn attachment [_ path]
  (let [component        (subscribe [:editor/get-component-value path])
        languages        (subscribe [:editor/languages])
        deadline-value   (r/atom (get-in @component [:params :deadline]))
        valid            (r/atom true)
        mail-attachment? (subscribe [:editor/get-component-value path :params :mail-attachment?])
        format-deadline  (fn [event]
                           (some->> (deadline-date (-> event .-target .-value))
                                    (reset! deadline-value)))
        update-value     (fn [unformatted-value value valid?]
                           (reset! deadline-value unformatted-value)
                           (reset! valid valid?)
                           (dispatch-sync [:editor/set-component-value value path :params :deadline]))
        update-deadline  (fn [event]
                           (let [value    (-> event .-target .-value)
                                 deadline (deadline-date value)]
                             (cond
                               (string/blank? value) (update-value value nil true)
                               (and value deadline) (update-value value deadline true)
                               :else (update-value value nil false))))]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id content) @(subscribe [:editor/virkailija-translation :attachment]) path (:metadata content)
        :sub-header (:label @component)]
       [component-content/component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :attachment-name])
            [copy-link (:id content)]]
           (input-fields-with-lang-component/input-fields-with-lang
            (fn attachment-file-name-input [lang]
              [input-field-component/input-field {:path        path
                                                  :lang        lang
                                                  :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                (-> % .-target .-value)
                                                                                path :label lang])}])
            @languages
            :header? true)]
          [:div.editor-form__text-field-wrapper
           [:label.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :attachment-deadline])]
           [:input.editor-form__attachment-deadline-field
            {:type        "text"
             :class       (when-not @valid "editor-form__text-field--invalid")
             :value       @deadline-value
             :on-blur     format-deadline
             :placeholder "pp.kk.vvvv hh:mm"
             :on-change   update-deadline}]]
          (when-not @mail-attachment?
            [:div.editor-form__checkbox-wrapper
             [validator-checkbox-component/validator-checkbox path content :required (required-disabled content)]])
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path content]]
         [attachment-textarea content path]]]])))

;;TODO Muokkaa UI -speksin mukaiseksi
(defn tutkinnot
  [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) (get-in initial-content [:label :fi]) path (:metadata initial-content)]
       [component-content/component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:div.infoelement
            (->> (input-fields-with-lang-component/input-fields-with-lang
                   (fn [lang]
                     [input-field-component/input-field {:path        path
                                                         :lang        lang
                                                         :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                       (-> % .-target .-value)
                                                                                       path :text lang])
                                                         :value-fn    (fn [component] (get-in component [:text lang]))
                                                         :tag         :textarea}])
                   @languages
                   :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help-component/markdown-help)]])))
                 doall)]]]]]])))
