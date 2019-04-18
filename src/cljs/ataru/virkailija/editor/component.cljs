(ns ataru.virkailija.editor.component
  (:require
   [ataru.application-common.application-field-common :refer [copy-link]]
   [ataru.util :as cutil]
   [ataru.cljs-util :as util :refer [cljs->str str->cljs new-uuid get-virkailija-translation]]
   [ataru.component-data.component :as component]
   [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
   [ataru.virkailija.editor.components.followup-question :refer [followup-question followup-question-overlay]]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
   [ataru.virkailija.temporal :as temporal]
   [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
   [cljs.core.match :refer-macros [match]]
   [goog.dom :as gdom]
   [goog.string :as s]
   [ataru.number :refer [numeric-matcher]]
   [goog.date :as d]
   [cljs-time.core :as t]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]]
   [taoensso.timbre :refer-macros [spy debug]]))

(defn- required-checkbox
  [path initial-content]
  (let [id         (util/new-uuid)
        validators (-> initial-content :validators set)
        disabled?  (or @(subscribe [:editor/form-locked?])
                       (contains? validators "required-hakija"))]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   (contains? validators "required")
                                    :disabled  disabled?
                                    :on-change (fn [event]
                                                 (dispatch [(if (-> event .-target .-checked)
                                                              :editor/add-validator
                                                              :editor/remove-validator) "required" path]))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when disabled? "editor-form__checkbox-label--disabled")}
      (get-virkailija-translation :required)]]))

(defn- repeater-checkbox
  [path initial-content]
  (let [id           (util/new-uuid)
        checked?     (-> initial-content :params :repeatable boolean)
        form-locked? @(subscribe [:editor/form-locked?])]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :disabled  form-locked?
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable]))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when form-locked? "editor-form__checkbox-label--disabled")}
      (get-virkailija-translation :multiple-answers)]]))

(defn- belongs-to-hakukohteet-modal
  [path id selected-hakukohteet selected-hakukohderyhmat]
  (let [search-term     (subscribe [:editor/belongs-to-hakukohteet-modal-search-term-value id])
        fetching?       (subscribe [:editor/fetching-haut?])
        used-by-haku?   (subscribe [:editor/used-by-haku?])
        haut            (subscribe [:editor/filtered-haut id])
        hakukohderyhmat (subscribe [:editor/filtered-hakukohderyhmat id])
        on-click        (fn [_] (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        on-change       (fn [e] (dispatch [:editor/on-belongs-to-hakukohteet-modal-search-term-change
                                           id (.-value (.-target e))]))]
    (fn [path id selected-hakukohteet selected-hakukohderyhmat]
      (if @used-by-haku?
        [h-and-h/popup
         [h-and-h/search-input
          {:id                       id
           :haut                     (map second @haut)
           :hakukohderyhmat          @hakukohderyhmat
           :hakukohde-selected?      #(contains? (set selected-hakukohteet) %)
           :hakukohderyhma-selected? #(contains? (set selected-hakukohderyhmat) %)}]
         [h-and-h/visibility-checkbox id path]
         [h-and-h/search-listing
          {:id                         id
           :haut                       (map second @haut)
           :hakukohderyhmat            @hakukohderyhmat
           :hakukohde-selected?        #(contains? (set selected-hakukohteet) %)
           :hakukohderyhma-selected?   #(contains? (set selected-hakukohderyhmat) %)
           :on-hakukohde-select        #(dispatch [:editor/add-to-belongs-to-hakukohteet path %])
           :on-hakukohde-unselect      #(dispatch [:editor/remove-from-belongs-to-hakukohteet path %])
           :on-hakukohderyhma-select   #(dispatch [:editor/add-to-belongs-to-hakukohderyhma path %])
           :on-hakukohderyhma-unselect #(dispatch [:editor/remove-from-belongs-to-hakukohderyhma path %])}]
         #(dispatch [:editor/hide-belongs-to-hakukohteet-modal id])]
        [h-and-h/popup
         [:div.belongs-to-hakukohteet-modal__no-haku-row
          [:p.belongs-to-hakukohteet-modal__no-haku
           (get-virkailija-translation :set-haku-to-form)]]
         nil
         nil
         #(dispatch [:editor/hide-belongs-to-hakukohteet-modal id])]))))

(defn- belongs-to
  [_ _ _ _]
  (let [fetching? (subscribe [:editor/fetching-haut?])]
    (fn [path oid name on-click]
      [:li.belongs-to-hakukohteet__hakukohde-list-item.animated.fadeIn
       [:span.belongs-to-hakukohteet__hakukohde-label
        (if @fetching?
          [:i.zmdi.zmdi-spinner.spin]
          name)]
       [:button.belongs-to-hakukohteet__hakukohde-remove
        {:on-click on-click}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]])))

(defn- belongs-to-hakukohteet
  [path initial-content]
  (let [id            (:id initial-content)
        on-click-show (fn [_]
                        (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide (fn [_]
                        (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?   (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        form-locked?  (subscribe [:editor/form-locked?])]
    (fn [path initial-content]
      (let [visible-hakukohteet     (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohde-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                                                                  path oid]))})
                                      (:belongs-to-hakukohteet initial-content))
            visible-hakukohderyhmat (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohderyhma-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                                                                  path oid]))})
                                      (:belongs-to-hakukohderyhma initial-content))
            hidden?                 (subscribe [:editor/get-component-value path :params :hidden])
            visible                 (sort-by :name (concat visible-hakukohteet
                                                           visible-hakukohderyhmat))]
        [:div.belongs-to-hakukohteet
         [:button.belongs-to-hakukohteet__modal-toggle
          {:disabled @form-locked?
           :class    (when @form-locked? "belongs-to-hakukohteet__modal-toggle--disabled")
           :on-click (when-not @form-locked?
                       (if @show-modal? on-click-hide on-click-show))}
          (str (get-virkailija-translation :visibility-on-form) " ")]
         [:span.belongs-to-hakukohteet__modal-toggle-label
          (cond @hidden?
                (get-virkailija-translation :hidden)

                (and (empty? visible))
                (get-virkailija-translation :visible-to-all)

                :else
                (get-virkailija-translation :visible-to-hakukohteet))]
         (when @show-modal?
           [belongs-to-hakukohteet-modal path
            (:id initial-content)
            (map :oid visible-hakukohteet)
            (map :oid visible-hakukohderyhmat)])
         [:ul.belongs-to-hakukohteet__hakukohde-list
          (for [{:keys [oid name on-click]} visible]
            ^{:key oid}
            [belongs-to path oid name on-click])]]))))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- fade-out-effect
  [path]
  (reaction (case @(subscribe [:state-query [:editor :forms-meta path]])
              :fade-out "fade-out"
              :fade-in "animated fadeInUp"
              nil)))

(defn- cut-component-button [removable? path]
  (case @(subscribe [:editor/component-button-state :cut path])
    :active
    [:button.editor-form__copy-component-button
     {:on-click #(dispatch [:editor/start-component :cut path])}
     (get-virkailija-translation :cut-element)]
    :confirm
    [:div.editor-form__component-button-group
     [:button.editor-form__copy-component-button--pressed.editor-form__copy-component-button
      {:on-click (fn [event]
                   (dispatch [:editor/copy-component path true removable?]))}
      (get-virkailija-translation :confirm-cut)]
     [:button.editor-form__copy-component-button
      {:on-click #(dispatch [:editor/unstart-component :cut path])}
      (get-virkailija-translation :cancel-cut)]]))

(defn- remove-component-button [path]
  (case @(subscribe [:editor/component-button-state :remove path])
    :active
    [:button.editor-form__remove-component-button
     {:on-click #(dispatch [:editor/start-component :remove path])}
     (get-virkailija-translation :remove)]
    :confirm
    [:div.editor-form__component-button-group
     [:button.editor-form__remove-component-button--confirm.editor-form__remove-component-button
      {:on-click (fn [event]
                   (let [target (-> event
                                    .-target
                                    (gdom/getAncestorByClass "editor-form__component-wrapper"))]
                     (set! (.-height (.-style target)) (str (.-offsetHeight target) "px"))
                     (dispatch [:editor/confirm-remove-component path])))}
      (get-virkailija-translation :confirm-delete)]
     [:button.editor-form__remove-component-button
      {:on-click #(dispatch [:editor/unstart-component :remove path])}
      (get-virkailija-translation :cancel-remove)]]
     :disabled
     [:button.editor-form__remove-component-button--disabled.editor-form__remove-component-button
      {:disabled true}
      (get-virkailija-translation :confirm-delete)]))

(defn- header-metadata
  [metadata]
  [:span.editor-form__component-main-header-metadata
   (s/format "%s %s, %s %s %s"
             (get-virkailija-translation :created-by)
             (-> metadata :created-by :name)
             (get-virkailija-translation :last-modified-by)
             (if (= (-> metadata :created-by :oid)
                    (-> metadata :modified-by :oid))
               ""
               (-> metadata :modified-by :name))
             (-> metadata :modified-by :date temporal/str->googdate temporal/time->date))])

(defn- text-header
  [id label path metadata & {:keys [movable?
                                    foldable?
                                    removable?
                                    sub-header]
                             :or   {movable? true
                                    foldable?  true
                                    removable? true}}]
  (let [folded?                 @(subscribe [:editor/folded? id])
        selected-form-key       @(subscribe [:editor/selected-form-key])
        locked?                 @(subscribe [:editor/form-locked?])
        copy-component          @(subscribe [:editor/copy-component])
        copy-component-path     (:copy-component-path copy-component)
        copy-component-cut?     (:copy-component-cut? copy-component)
        copy-component-form-key (:copy-component-form-key copy-component)]
    [:div.editor-form__header-wrapper
     [:header.editor-form__component-header
      (when foldable?
        (if folded?
          [:button.editor-form__component-fold-button
           {:on-click #(dispatch [:editor/unfold id])}
           [:i.zmdi.zmdi-chevron-down]]
          [:button.editor-form__component-fold-button
           {:on-click #(dispatch [:editor/fold id])}
           [:i.zmdi.zmdi-chevron-up]]))
      [:span.editor-form__component-main-header
       label]
      [:span.editor-form__component-sub-header
       {:class (if (and folded? (some? sub-header))
                 "editor-form__component-sub-header-visible"
                 "editor-form__component-sub-header-hidden")}
       (when (some? sub-header)
         (->> [:fi :sv :en]
              (map (partial get sub-header))
              (remove clojure.string/blank?)
              (clojure.string/join " - ")))]]
     (when metadata
       (header-metadata metadata))
     (when (and (not locked?) movable?)
       (cond (and (= copy-component-path path) copy-component-cut? (= selected-form-key copy-component-form-key))
             [:button.editor-form__copy-component-button.editor-form__copy-component-button--pressed
              {:on-click (fn [_] (dispatch [:editor/clear-copy-component]))}
              (get-virkailija-translation :cut-element)]
             (some? copy-component-path)
             [:button.editor-form__copy-component-button.editor-form__copy-component-button--disabled
              {:on-click (fn [_] (dispatch [:editor/clear-copy-component]))}
              (get-virkailija-translation :cut-element)]
             :else
             [cut-component-button removable? path]))
     (when (and (not locked?) removable? movable?)
       (cond (and (= copy-component-path path) (not copy-component-cut?) (= selected-form-key copy-component-form-key))
             [:button.editor-form__copy-component-button.editor-form__copy-component-button--pressed
              {:on-click (fn [_] (dispatch [:editor/clear-copy-component]))}
              (get-virkailija-translation :copy-element)]
             (some? copy-component-path)
             [:button.editor-form__copy-component-button.editor-form__copy-component-button--disabled
              {:on-click (fn [_] (dispatch [:editor/clear-copy-component]))}
              (get-virkailija-translation :copy-element)]
             :else
             [:button.editor-form__copy-component-button
              {:on-click (fn [_] (dispatch [:editor/copy-component path false removable?]))}
              (get-virkailija-translation :copy-element)]))
     (when removable?
       [remove-component-button path])]))

(defn- fold-transition
  [component folded? state height]
  (case [@folded? @state]
    [true :unfolded]
    (do (reset! height (.-scrollHeight (r/dom-node component)))
        (reset! state :set-height))
    [true :set-height]
    (reset! state :folded)
    [false :folded]
    (reset! state :set-height)
    nil))

(defn- component-fold-transition
  [component folded? state height]
  (cond (= [true :unfolded] [@folded? @state])
        ;; folding, calculate and set height
        (do (reset! height (.-scrollHeight (r/dom-node component)))
            (reset! state :set-height))
        (= [true :set-height] [@folded? @state])
        ;; folding, render folded
        (reset! state :folded)
        (= [false :folded] [@folded? @state])
        ;; unfolding, set height
        (reset! state :set-height)))

(defn- unfold-ended-listener
  [folded? state]
  (fn [_]
    (when (= [false :set-height] [@folded? @state])
      ;; unfolding, render unfolded
      (reset! state :unfolded))))

(defn- component-content
  [path _]
  (let [folded?  (subscribe [:editor/path-folded? path])
        state    (r/atom (if @folded?
                           :folded :unfolded))
        height   (r/atom nil)
        listener (unfold-ended-listener folded? state)]
    (r/create-class
     {:component-did-mount
      (fn [component]
        (.addEventListener (r/dom-node component)
                           "transitionend"
                           listener)
        (component-fold-transition component folded? state height))
      :component-will-unmount
      (fn [component]
        (.removeEventListener (r/dom-node component)
                              "transitionend"
                              listener))
      :component-did-update
      (fn [component]
        (component-fold-transition component folded? state height))
      :reagent-render
      (fn [path content-component]
        (let [folded? @folded?]
          (case @state
            :unfolded
            [:div.editor-form__component-content-wrapper
             content-component]
            :set-height
            [:div.editor-form__component-content-wrapper
             {:style {:height @height}}
             content-component]
            :folded
            [:div.editor-form__component-content-wrapper.editor-form__component-content-wrapper--folded])))})))

(defn markdown-help []
  [:div.editor-form__markdown-help
   [:div
    [:div.editor-form__markdown-help-arrow-left]
    [:div.editor-form__markdown-help-content
     [:span (get-virkailija-translation :md-help-title)]
     [:br]
     [:span (get-virkailija-translation :md-help-bold)]
     [:br]
     [:span (get-virkailija-translation :md-help-cursive)]
     [:br]
     [:span (get-virkailija-translation :md-help-link)]
     [:br]
     [:a {:href          "https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet"
          :target        "_blank"
          :on-mouse-down (fn [evt]
                           (let [url (.getAttribute (-> evt .-target) "href")]
                             (.open js/window url "_blank")))}
      (get-virkailija-translation :md-help-more)]]]])

(defn input-field [path lang dispatch-fn {:keys [class value-fn tag]
                                          :or   {tag :input}}]
  (let [component    (subscribe [:editor/get-component-value path])
        focus?       (subscribe [:state-query [:editor :ui (:id @component) :focus?]])
        value        (or
                      (when value-fn
                        (reaction (value-fn @component)))
                      (reaction (get-in @component [:label lang])))
        languages    (subscribe [:editor/languages])
        form-locked? (subscribe [:editor/form-locked?])]
    (r/create-class
     {:component-did-mount (fn [component]
                             (when (cond-> @focus?
                                           (> (count @languages) 1)
                                           (and (= (first @languages) lang)))
                               (let [dom-node (r/dom-node component)]
                                 (.focus dom-node))))
      :reagent-render      (fn [_ _ _ _]
                             [tag
                              {:class     (str "editor-form__text-field " (when-not (empty? class) class))
                               :value     @value
                               :on-change dispatch-fn
                               :on-drop   prevent-default
                               :disabled  @form-locked?}])})))

(defn- add-multi-lang-class [field-spec]
  (let [multi-lang-class "editor-form__text-field-wrapper--with-label"]
    (if (map? (last field-spec))
      (assoc-in field-spec [(dec (count field-spec)) :class] multi-lang-class)
      (conj field-spec {:class multi-lang-class}))))

(defn- input-fields-with-lang [field-fn languages & {:keys [header?] :or {header? false}}]
  (let [multiple-languages? (> (count languages) 1)]
    (map-indexed (fn [idx lang]
                   (let [field-spec (field-fn lang)]
                     ^{:key (str "option-" lang "-" idx)}
                     [:div.editor-form__text-field-container
                      (when-not header?
                        {:class "editor-form__multi-option-wrapper"})
                      (cond-> field-spec
                        multiple-languages? add-multi-lang-class)
                      (when multiple-languages?
                        [:div.editor-form__text-field-label (-> lang name clojure.string/upper-case)])]))
      languages)))

(defn- koodisto-field [component idx lang]
  (let [value (get-in component [:label lang])]
    [:div.editor-form__koodisto-field-container
     {:key (str "option-" lang "-" idx)}
     [:div.editor-form__koodisto-field
      {:on-drop prevent-default}
      value]]))

(defn- koodisto-fields-with-lang [languages option-path]
  (fn [languages option-path]
    (let [multiple-languages? (> (count languages) 1)
          component           @(subscribe [:editor/get-component-value option-path])]
      [:div
       {:title (clojure.string/join ", " (map (fn [lang] (get-in component [:label lang])) languages))}
       (map-indexed (partial koodisto-field component)
                    languages)])))

(defn info-addon
  "Info text which is added to an existing component"
  [path]
  (let [id               (util/new-uuid)
        checked?         (reaction (some? @(subscribe [:editor/get-component-value path :params :info-text :label])))
        collapse-checked (subscribe [:editor/get-component-value path :params :info-text-collapse])
        languages        (subscribe [:editor/languages])
        form-locked?     (subscribe [:editor/form-locked?])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       [:div.editor-form__info-addon-checkbox
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :disabled  @form-locked?
                 :on-change (fn [event]
                              (dispatch [:editor/set-component-value
                                         (if (-> event .-target .-checked) {:fi "" :sv "" :en ""} nil)
                                         path :params :info-text :label]))}]
        [:label
         {:for   id
          :class (when @form-locked? "disabled")}
         (get-virkailija-translation :info-addon)]]
       (when @checked?
         (let [collapsed-id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:type      "checkbox"
                     :id        collapsed-id
                     :checked   (boolean @collapse-checked)
                     :disabled  @form-locked?
                     :on-change (fn [event]
                                  (dispatch [:editor/set-component-value
                                             (-> event .-target .-checked)
                                             path :params :info-text-collapse]))}]
            [:label
             {:for   collapsed-id
              :class (when @form-locked? "editor-form__checkbox-label--disabled")}
             (get-virkailija-translation :collapse-info-text)]]))
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang
                (fn [lang]
                  [input-field
                   (concat path [:params :info-text])
                   lang
                   #(dispatch-sync [:editor/set-component-value
                                    (-> % .-target .-value)
                                    path :params :info-text :label lang])
                   {:tag :textarea}])
                @languages)
               (map (fn [field]
                      (into field [[:div.editor-form__info-addon-markdown-anchor (markdown-help)]])))
               (doall))])])))

(defn- get-val [event]
  (-> event .-target .-value))

(defn- decimal-places-selector [path]
  (let [decimal-places     (subscribe [:editor/get-component-value path :params :decimals])
        form-locked?       (subscribe [:editor/form-locked?])
        min-value          (r/atom @(subscribe [:editor/get-component-value path :params :min-value]))
        max-value          (r/atom @(subscribe [:editor/get-component-value path :params :max-value]))
        [min max]          [{:id     (util/new-uuid)
                             :key    :min-value
                             :value  min-value
                             :valid? (r/atom true)}
                            {:id     (util/new-uuid)
                             :key    :max-value
                             :value  max-value
                             :valid? (r/atom true)}]
        decimals           (subscribe [:editor/get-component-value path :params :decimals])
        number-of-decimals (fn [v]
                             (let [[_ decimals] (clojure.string/split v #",")]
                               (count decimals)))
        format-range       (fn [value]
                             (clojure.string/replace (clojure.string/trim value) "." ","))
        valid?             (fn [value]
                             (let [clean (format-range value)]
                               [(or
                                 (empty? value)
                                 (and (re-matches numeric-matcher clean)
                                      (<= (number-of-decimals clean) @decimals))) value]))
        set-range          (fn [id value]
                             (let [id        id
                                   component (->> [min max]
                                                  (filter #(= (:id %) id))
                                                  (first))
                                   [valid value] (valid? value)]
                               (reset! (:valid? component) valid)
                               (reset! (:value component) value)
                               (when valid
                                 (dispatch [:editor/set-range-value (:key component) (format-range value) path :params]))))
        set-range-event    #(set-range (-> % .-target .-id) (-> % .-target .-value))
        invalidate-values  (fn []
                             (doall
                               (for [component [min max]]
                                 (set-range (:id component) (or @(:value component) "")))))]
    (fn [path]
      [:div
       [:div.editor-form__additional-params-container
        [:header.editor-form__component-item-header (get-virkailija-translation :shape)]
        [:select.editor-form__decimal-places-selector
         {:value     (or @decimal-places "")
          :disabled  @form-locked?
          :on-change (fn [e]
                       (let [new-val (get-val e)
                             value   (when (not-empty new-val)
                                       (js/parseInt new-val))]
                         (dispatch-sync [:editor/set-decimals-value value path :params])
                         (invalidate-values)))}
         [:option {:value "" :key 0} (get-virkailija-translation :integer)]
         (doall
           (for [i (range 1 5)]
             [:option {:value i :key i} (str i " " (get-virkailija-translation :decimals))]))]]
       [:div.editor-form__additional-params-container
        [:label.editor-form__range-label
         {:for   (:id min)
          :class (when @form-locked? "editor-form__checkbox-label--disabled")}
         (get-virkailija-translation :numeric-range)]
        [:input.editor-form__range-input
         {:type      "text"
          :id        (:id min)
          :class     (when-not @(:valid? min) "editor-form__text-field--invalid")
          :disabled  @form-locked?
          :value     @min-value
          :on-blur   #(reset! (:value min) (format-range (-> % .-target .-value)))
          :on-change set-range-event}]
        [:span "—"]
        [:input.editor-form__range-input
         {:type      "text"
          :id        (:id max)
          :class     (when-not @(:valid? max) "editor-form__text-field--invalid")
          :disabled  @form-locked?
          :value     @max-value
          :on-blur   #(reset! (:value max) (format-range (-> % .-target .-value)))
          :on-change set-range-event}]]])))

(defn- text-component-type-selector [path radio-group-id]
  (let [id           (util/new-uuid)
        checked?     (subscribe [:editor/get-component-value path :params :numeric])
        form-locked? (subscribe [:editor/form-locked?])]
    (fn [path radio-group-id]
      [:div
       [:div.editor-form__checkbox-container
        [:input.editor-form__checkbox
         {:type      "checkbox"
          :id        id
          :checked   (or @checked? false)
          :disabled  @form-locked?
          :on-change (fn [event]
                       (let [checked-now? (-> event .-target .-checked)]
                         (dispatch [:editor/set-component-value checked-now? path :params :numeric])
                         (dispatch [(if checked-now?
                                      :editor/add-validator
                                      :editor/remove-validator) "numeric" path])
                         (when-not checked-now?
                           (dispatch [:editor/set-decimals-value nil path :params]))))}]
        [:label.editor-form__checkbox-label
         {:for   id
          :class (when @form-locked? "editor-form__checkbox-label--disabled")}
         (get-virkailija-translation :only-numeric)]]
       (when @checked?
         [decimal-places-selector path])])))

(defn- button-label-class
  [button-name form-locked?]
  (let [button-class (match button-name
                       "S" "editor-form__button--left-edge"
                       "L" "editor-form__button--right-edge"
                       :else nil)]
    (str (when form-locked? "editor-form__button-label--disabled ") button-class)))

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages         (subscribe [:editor/languages])
        sub-header        (subscribe [:editor/get-component-value path :label])
        size              (subscribe [:editor/get-component-value path :params :size])
        max-length        (subscribe [:editor/get-component-value path :params :max-length])
        radio-group-id    (util/new-uuid)
        radio-buttons     ["S" "M" "L"]
        radio-button-ids  (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        max-length-change (fn [new-val]
                            (dispatch-sync [:editor/set-component-value new-val path :params :max-length]))
        size-change       (fn [new-size]
                            (dispatch-sync [:editor/set-component-value new-size path :params :size]))
        text-area?        (= "Tekstialue" header-label)
        numeric?          (subscribe [:editor/get-component-value path :params :numeric])
        animation-effect  (fade-out-effect path)
        form-locked?      (subscribe [:editor/form-locked?])]
    (fn [initial-content path & {:keys [header-label size-label]}]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id initial-content) header-label path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content
        path;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header (get-virkailija-translation :question)
            [copy-link (:id initial-content)]]
           (input-fields-with-lang
            (fn [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (get-val %) path :label lang])])
            @languages
            :header? true)]
          [:div.editor-form__button-wrapper
           [:header.editor-form__component-item-header size-label]
           [:div.editor-form__button-group
            (doall (for [[btn-name btn-id] radio-button-ids]
                     ^{:key (str btn-id "-radio")}
                     [:div
                      [:input.editor-form__button
                       {:type      "radio"
                        :value     btn-name
                        :checked   (or
                                    (= @size btn-name)
                                    (and
                                     (nil? @size)
                                     (= "M" btn-name)))
                        :name      radio-group-id
                        :id        btn-id
                        :disabled  @form-locked?
                        :on-change (fn [] (size-change btn-name))}]
                      [:label.editor-form__button-label
                       {:for   btn-id
                        :class (button-label-class btn-name @form-locked?)}
                       btn-name]]))]
           (when text-area?
             [:div.editor-form__max-length-container
              [:header.editor-form__component-item-header (get-virkailija-translation :max-characters)]
              [:input.editor-form__text-field.editor-form__text-field-auto-width
               {:value     @max-length
                :disabled  @form-locked?
                :on-change #(max-length-change (get-val %))}]])]
          [:div.editor-form__checkbox-wrapper
           [required-checkbox path initial-content]
           (when-not text-area?
             [repeater-checkbox path initial-content])
           (when-not text-area?
             [text-component-type-selector path radio-group-id])]
          [belongs-to-hakukohteet path initial-content]]
         [info-addon path]]]])))

(defn text-field [initial-content path]
  [text-component initial-content path
   :header-label (get-virkailija-translation :text-field)
   :size-label (get-virkailija-translation :text-field-size)])

(defn text-area [initial-content path]
  [text-component initial-content path
   :header-label (get-virkailija-translation :text-area)
   :size-label (get-virkailija-translation :text-area-size)])

(defn- remove-dropdown-option-button [path option-index disabled? parent-key option-value question-group-element?]
  [:div.editor-form__multi-options-remove--cross
   [copy-link (str parent-key "_" (when question-group-element? "groupN_") option-value) :answer? true]
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (.preventDefault evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index])))
     :class    (when disabled? "editor-form__multi-options-remove--cross--disabled")}]])

(defn- dropdown-option
  [option-index option-count option-path followups path languages show-followups parent-key option-value question-group-element? &
   {:keys [header? editable?]
    :or   {header? false editable? true}
    :as   opts}]
  (let [multiple-languages? (< 1 (count languages))
        form-locked?        (subscribe [:editor/form-locked?])
        on-click            (fn [up? event]
                              (when-not @form-locked?
                                (.preventDefault event)
                                (reset! show-followups nil)
                                (dispatch [(if up?
                                             :editor/move-option-up
                                             :editor/move-option-down) path option-index])))]
    (fn [option-index option-count option-path followups path languages show-followups parent-key option-value question-group-element? &
         {:keys [header? editable?]
          :or   {header? false editable? true}
          :as   opts}]
      [:div
       [:div.editor-form__multi-options-wrapper-outer
        [:div
         [:div.editor-form__multi-options-arrows-container
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--up
           {:on-click (partial on-click true)
            :class    (when @form-locked? "editor-form__multi-options-arrow--disabled")}]
          [:div.editor-form__multi-options-arrows--stretch]
          [:div.editor-form__multi-options-arrow.editor-form__multi-options-arrow--down
           {:on-click (partial on-click false)
            :class    (when @form-locked? "editor-form__multi-options-arrow--disabled")}]]]
        [:div.editor-form__multi-options-wrapper-inner
         {:key (str "options-" option-index)}
         (if editable?
           (input-fields-with-lang
            (fn [lang]
              [input-field option-path lang #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) option-path :label lang])])
            languages)
           [koodisto-fields-with-lang languages option-path])]
        (when (not question-group-element?)
          [followup-question option-index followups option-path show-followups parent-key option-value question-group-element?])
        (when editable?
          [remove-dropdown-option-button path option-index (or @form-locked? (< option-count 3)) parent-key option-value question-group-element?])]
       (when (not question-group-element?)
         [followup-question-overlay option-index followups option-path show-followups])])))

(defn- dropdown-multi-options [path options-koodisto]
  (let [dropdown-id                (util/new-uuid)
        custom-button-value        (get-virkailija-translation :custom-choice-label)
        custom-button-id           (str dropdown-id "-custom")
        koodisto-button-value      (reaction (str (get-virkailija-translation :koodisto)
                                                  (when-let [koodisto-name (:title @options-koodisto)]
                                                    (str ": " koodisto-name))))
        koodisto-button-id         (str dropdown-id "-koodisto")
        koodisto-popover-expanded? (r/atom false)
        form-locked?               (subscribe [:editor/form-locked?])]
    (fn [path options-koodisto]
      [:div.editor-form__button-group
       [:input.editor-form__button.editor-form__button--large
        {:type      "radio"
         :value     custom-button-value
         :checked   (nil? @options-koodisto)
         :name      dropdown-id
         :id        custom-button-id
         :disabled  @form-locked?
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? false)
                      (dispatch [:editor/select-custom-multi-options path]))}]
       [:label.editor-form__button-label.editor-form__button-label--left-edge
        {:for   custom-button-id
         :class (when @form-locked? "editor-form__button-label--disabled")}
        custom-button-value]
       [:input.editor-form__button.editor-form__button--large
        {:type      "radio"
         :value     @koodisto-button-value
         :checked   (not (nil? @options-koodisto))
         :name      dropdown-id
         :id        koodisto-button-id
         :disabled  @form-locked?
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? true))}]
       [:label.editor-form__button-label.editor-form__button-label--right-edge
        {:for   koodisto-button-id
         :class (when @form-locked? "editor-form__button-label--disabled")}
        @koodisto-button-value]
       (when @koodisto-popover-expanded?
         [:div.editor-form__koodisto-popover
          [:div.editor-form__koodisto-popover-header (get-virkailija-translation :koodisto)
           [:a.editor-form__koodisto-popover-close
            {:on-click (fn [e]
                         (.preventDefault e)
                         (reset! koodisto-popover-expanded? false))}
            [:i.zmdi.zmdi-close.zmdi-hc-lg]]]
          [:ul.editor-form__koodisto-popover-list
           (doall (for [{:keys [uri title version]} koodisto-whitelist/koodisto-whitelist]
                    ^{:key (str "koodisto-" uri)}
                    [:li.editor-form__koodisto-popover-list-item
                     [:a.editor-form__koodisto-popover-link
                      {:on-click (fn [e]
                                   (.preventDefault e)
                                   (reset! koodisto-popover-expanded? false)
                                   (dispatch [:editor/select-koodisto-options uri version title path]))}
                      title]]))]])])))

(defn- custom-answer-options [languages
                              options
                              followups
                              path
                              question-group-element?
                              editable?
                              show-followups
                              parent-key]
  (fn [languages options followups path question-group-element? editable?]
    (let [option-count (count options)]
      (when (or (nil? @show-followups)
                (not (= (count @show-followups) option-count)))
        (reset! show-followups (vec (replicate option-count false))))
      [:div.editor-form__multi-options-container
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

(defn koodisto-answer-options [id followups path selected-koodisto question-group-element? parent-key]
  (let [opened? (r/atom false)]
    (fn [id followups path selected-koodisto question-group-element?]
      (let [languages             @(subscribe [:editor/languages])
            value                 @(subscribe [:editor/get-component-value path])
            editable?             false
            hide-koodisto-options (fn [evt]
                                    (reset! opened? false))
            show-koodisto-options (fn [evt]
                                    (dispatch [:editor/fetch-koodisto-for-component-with-id id selected-koodisto])
                                    (reset! opened? true))
            show-followups (r/atom nil)]
        (if (not @opened?)
          [:div.editor-form__show-koodisto-values
           [:a
            {:on-click show-koodisto-options}
            [:i.zmdi.zmdi-chevron-down] (str " " (get-virkailija-translation :show-options))]]
          [:div
           [:div.editor-form__show-koodisto-values
            [:a
             {:on-click hide-koodisto-options}
             [:i.zmdi.zmdi-chevron-up] (str " " (get-virkailija-translation :hide-options))]]
           [custom-answer-options languages (:options value) followups path question-group-element? editable? show-followups parent-key]])))))

(defn dropdown [initial-content followups path]
  (let [languages                (subscribe [:editor/languages])
        options-koodisto         (subscribe [:editor/get-component-value path :koodisto-source])
        koodisto-ordered-by-user (subscribe [:editor/get-component-value path :koodisto-ordered-by-user])
        value                    (subscribe [:editor/get-component-value path])
        animation-effect         (fade-out-effect path)
        koodisto-ordered-id      (util/new-uuid)
        form-locked?             (subscribe [:editor/form-locked?])]
    (fn [initial-content followups path {:keys [question-group-element?]}]
      (let [languages      @languages
            field-type     (:fieldType @value)
            show-followups (r/atom nil)]
        [:div.editor-form__component-wrapper
         {:class @animation-effect}
         (let [header (case field-type
                        "dropdown"       (get-virkailija-translation :dropdown)
                        "singleChoice"   (get-virkailija-translation :single-choice-button)
                        "multipleChoice" (get-virkailija-translation :multiple-choice))]
           [text-header (:id initial-content) header path (:metadata initial-content)
            :sub-header (:label @value)])
         [component-content
          path ; (:id initial-content)
          [:div
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-question-wrapper
             [:div.editor-form__text-field-wrapper
              [:header.editor-form__component-item-header (get-virkailija-translation :question)
               [copy-link (:id initial-content)]]
              (input-fields-with-lang
               (fn [lang]
                 [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
               languages
               :header? true)]
             [:div.editor-form__checkbox-wrapper
              [required-checkbox path initial-content]]
             [belongs-to-hakukohteet path initial-content]]]
           [info-addon path initial-content]
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-options_wrapper
             [:header.editor-form__component-item-header (get-virkailija-translation :options)]
             (when (some? @options-koodisto)
               [:div.editor-form__ordered-by-user-checkbox
                [:input {:id        koodisto-ordered-id
                         :type      "checkbox"
                         :checked   (not @koodisto-ordered-by-user)
                         :disabled  @form-locked?
                         :on-change (fn [event]
                                      (dispatch [:editor/set-ordered-by-user (-> event .-target .-checked) path]))}]
                [:label
                 {:for koodisto-ordered-id}
                 (get-virkailija-translation :alphabetically)]])
             (when-not (= field-type "singleChoice") [dropdown-multi-options path options-koodisto])
             (if (nil? @options-koodisto)
               [custom-answer-options languages (:options @value) followups path question-group-element? true show-followups (:id initial-content)]
               [koodisto-answer-options (:id @value) followups path @options-koodisto question-group-element? (:id initial-content)])
             (when (nil? @options-koodisto)
               [:div.editor-form__add-dropdown-item
                [:a
                 {:on-click (fn [evt]
                              (when-not @form-locked?
                                (.preventDefault evt)
                                (reset! show-followups nil)
                                (dispatch [:editor/add-dropdown-option path])))
                  :class    (when @form-locked? "editor-form__add-dropdown-item--disabled")}
                 [:i.zmdi.zmdi-plus-square] (str " " (get-virkailija-translation :add))]])]]]]]))))

(defn component-group [content path children]
  (let [id                (:id content)
        languages         @(subscribe [:editor/languages])
        value             @(subscribe [:editor/get-component-value path])
        animation-effect  @(fade-out-effect path)
        group-header-text (case (:fieldClass content)
                            "wrapperElement" (get-virkailija-translation :wrapper-element)
                            "questionGroup"  (get-virkailija-translation :question-group))
        header-label-text (case (:fieldClass content)
                            "wrapperElement" (get-virkailija-translation :wrapper-header)
                            "questionGroup"  (get-virkailija-translation :group-header))]
    [:div.editor-form__component-wrapper
     {:class animation-effect}
     [text-header id group-header-text path (:metadata content)
      :sub-header (:label value)]
     [component-content
      path ;id
      [:div
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header header-label-text]
        (input-fields-with-lang
         (fn [lang]
           [input-field path lang #(dispatch-sync [:editor/set-component-value
                                                   (-> % .-target .-value)
                                                   path
                                                   :label lang])])
         languages
         :header? true)]
       children
       [dnd/drag-n-drop-spacer (conj path :children (count children))]
       (case (:fieldClass content)
         "wrapperElement" [toolbar/add-component (conj path :children (count children))]
         "questionGroup"  [toolbar/question-group-toolbar path
                           (fn [generate-fn]
                             (dispatch [:generate-component generate-fn (conj path :children (count children))]))])]]]))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
              "questionGroup" (map #(recursively-get-labels %) (:children component))
              "wrapperElement" (map #(recursively-get-labels %) (:children component))
              :else (-> component :label lang)))]
    (flatten (recursively-get-labels component))))

(defn hakukohteet-module [content path]
  (let [virkailija-lang (subscribe [:editor/virkailija-lang])
        value           (subscribe [:editor/get-component-value path])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
        :foldable? false
        :removable? false]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
         (get-virkailija-translation :hakukohde-info)]]])))

(defn module [content path]
  (let [languages       (subscribe [:editor/languages])
        value           (subscribe [:editor/get-component-value path])
        virkailija-lang (subscribe [:editor/virkailija-lang])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
        :foldable? false
        :removable? false]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
         [:span.editor-form__module-fields-label
          (get-virkailija-translation :contains-fields)]
         " "
         (clojure.string/join ", " (get-leaf-component-labels @value :fi))]]])))

(defn info-element
  "Info text which is a standalone component"
  [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)
        collapse-checked (subscribe [:editor/get-component-value path :params :info-text-collapse])
        sub-header       (subscribe [:editor/get-component-value path :label])
        form-locked?     (subscribe [:editor/form-locked?])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id initial-content) (get-virkailija-translation :info-element) path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header (get-virkailija-translation :title)]
           (input-fields-with-lang
            (fn [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
            @languages
            :header? true)
           [:div.infoelement
            [:header.editor-form__component-item-header (get-virkailija-translation :text)]
            (->> (input-fields-with-lang
                  (fn [lang]
                    [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :text lang])
                     {:value-fn (fn [component] (get-in component [:text lang]))
                      :tag      :textarea}])
                  @languages
                  :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help)]])))
                 doall)]]
          [:div.editor-form__checkbox-wrapper
           (let [collapsed-id (util/new-uuid)]
             [:div.editor-form__checkbox-container
              [:input.editor-form__checkbox {:type      "checkbox"
                                             :id        collapsed-id
                                             :checked   (boolean @collapse-checked)
                                             :disabled  @form-locked?
                                             :on-change (fn [event]
                                                          (dispatch [:editor/set-component-value
                                                                     (-> event .-target .-checked)
                                                                     path :params :info-text-collapse]))}]
              [:label.editor-form__checkbox-label
               {:for   collapsed-id
                :class (when @form-locked? "editor-form__checkbox-label--disabled")}
               (get-virkailija-translation :collapse-info-text)]])]
          [belongs-to-hakukohteet path initial-content]]]]])))

(defn pohjakoulutusristiriita
  [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id initial-content) (get-in initial-content [:label :fi]) path (:metadata initial-content)]
       [component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:div.infoelement
            (->> (input-fields-with-lang
                  (fn [lang]
                    [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :text lang])
                     {:value-fn (fn [component] (get-in component [:text lang]))
                      :tag      :textarea}])
                  @languages
                  :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help)]])))
                 doall)]]]]]])))

(defn adjacent-fieldset [content path children]
  (let [languages        (subscribe [:editor/languages])
        sub-header       (subscribe [:editor/get-component-value path :label])
        animation-effect (fade-out-effect path)]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id content) (get-virkailija-translation :adjacent-fieldset) path (:metadata content)
        :sub-header @sub-header]
       [component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header (get-virkailija-translation :title)]
           (input-fields-with-lang
            (fn [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
            @languages
            :header? true)]
          [:div.editor-form__checkbox-wrapper
           [repeater-checkbox path content]]
          [belongs-to-hakukohteet path content]]
         [info-addon path]
         [:div.editor-form__adjacent-fieldset-container
          children
          (when (-> (count children) (< 3))
            [toolbar/adjacent-fieldset-toolbar
             (concat path [:children])
             (fn [component-fn]
               (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]]]])))

(defn adjacent-text-field [content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id content) (get-virkailija-translation :text-field) path (:metadata content)
        :foldable? false
        :movable? false]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__component-row-wrapper
         [:div.editor-form__text-field-wrapper
          [:header.editor-form__component-item-header (get-virkailija-translation :question)
           [copy-link (:id content)]]
          (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
         [:div.editor-form__checkbox-wrapper
          [required-checkbox path content]]
         [belongs-to-hakukohteet path content]]]])))

(defn attachment-textarea [path]
  (let [checked?         (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        mail-attachment? (subscribe [:editor/get-component-value path :params :mail-attachment?])
        collapse?        (subscribe [:editor/get-component-value path :params :info-text-collapse])
        languages        (subscribe [:editor/languages])
        form-locked?      (subscribe [:editor/form-locked?])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       (let [id (util/new-uuid)]
         [:div.editor-form__info-addon-checkbox
          [:input {:id        id
                   :type      "checkbox"
                   :checked   @mail-attachment?
                   :disabled  @form-locked?
                   :on-change (fn toggle-attachment-textarea [event]
                                (.preventDefault event)
                                (let [mail-attachment? (.. event -target -checked)]
                                  (dispatch [:editor/update-mail-attachment mail-attachment? path])))}]
          [:label
           {:for   id
            :class (when @form-locked? "editor-form__checkbox-label--disabled")}
           (get-virkailija-translation :mail-attachment-text)]])
       (when-not @mail-attachment?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   @checked?
                     :disabled  @form-locked?
                     :on-change (fn toggle-attachment-textarea [event]
                                  (.preventDefault event)
                                  (let [checked? (.. event -target -checked)]
                                    (dispatch [:editor/set-component-value checked? path :params :info-text :enabled?])))}]
            [:label
             {:for   id
              :class (when @form-locked? "editor-form__checkbox-label--disabled")}
             (get-virkailija-translation :attachment-info-text)]]))
       (when @checked?
         (let [id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:id        id
                     :type      "checkbox"
                     :checked   (boolean @collapse?)
                     :disabled  @form-locked?
                     :on-change (fn [event]
                                  (dispatch [:editor/set-component-value
                                             (-> event .-target .-checked)
                                             path :params :info-text-collapse]))}]
            [:label
             {:for   id
              :class (when @form-locked? "editor-form__checkbox-label--disabled")}
             (get-virkailija-translation :collapse-info-text)]]))
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang
                (fn attachment-textarea-input [lang]
                  [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :params :info-text :value lang])
                   {:value-fn #(get-in % [:params :info-text :value lang])
                    :tag      :textarea}])
                @languages
                :header? true)
               (map (fn [field]
                      (into field [[:div.editor-form__info-addon-markdown-anchor
                                    (markdown-help)]])))
               doall)])])))


(def ^:private deadline-pattern #"^(\d{1,2})\.(\d{1,2})\.(\d{4}) (\d{1,2}):(\d{1,2})$")
(def ^:private deadline-format "%d.%d.%d %02d:%02d")

(defn deadline-date [deadline]
  (when-let [[_ day month year hours minutes] (map js/parseInt (re-matches deadline-pattern deadline))]
    (if-let [dt (t/date-time year month day hours minutes)]
      (if (= [year month day hours minutes]
             [(.getYear dt) (inc (.getMonth dt)) (.getDate dt)
              (.getHours dt)
              (.getMinutes dt)])
        (s/format deadline-format day month year hours minutes)))))

(defn attachment [content path]
  (let [component        (subscribe [:editor/get-component-value path])
        languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)
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
                              (clojure.string/blank? value) (update-value value nil true)
                              (and value deadline) (update-value value deadline true)
                              :else (update-value value nil false))))]
    (fn [content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header (:id content) (get-virkailija-translation :attachment) path (:metadata content)
        :sub-header (:label @component)]
       [component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header (get-virkailija-translation :attachment-name)
            [copy-link (:id content)]]
           (input-fields-with-lang
            (fn attachment-file-name-input [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
            @languages
            :header? true)]
          [:div.editor-form__text-field-wrapper
           [:label.editor-form__component-item-header (get-virkailija-translation :attachment-deadline)]
           [:input.editor-form__attachment-deadline-field
            {:type        "text"
             :class       (when-not @valid "editor-form__text-field--invalid")
             :value       @deadline-value
             :on-blur     format-deadline
             :placeholder "pp.kk.vvvv hh:mm"
             :on-change   update-deadline}]]
          (when-not @mail-attachment? [:div.editor-form__checkbox-wrapper
           [required-checkbox path content]])
          [belongs-to-hakukohteet path content]]
         [attachment-textarea path]]]])))
