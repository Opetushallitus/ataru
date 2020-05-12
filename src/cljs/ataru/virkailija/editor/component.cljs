(ns ataru.virkailija.editor.component
  (:require
   [ataru.application-common.application-field-common :refer [copy-link]]
   [ataru.cljs-util :as util]
   [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
   [ataru.virkailija.editor.components.followup-question :refer [followup-question followup-question-overlay]]
   [ataru.component-data.person-info-module :as pm]
   [ataru.virkailija.editor.components.toolbar :as toolbar]
   [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
   [ataru.virkailija.temporal :as temporal]
   [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
   [cljs.core.match :refer-macros [match]]
   [clojure.string :as string]
   [goog.string :as s]
   [cljs-time.core :as t]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [reagent.core :as r]
   [reagent.ratom :refer-macros [reaction]]
   [ataru.component-data.module.module-spec :as module-spec]))

(defn- required-disabled [initial-content]
  (contains? (-> initial-content :validators set) "required-hakija"))

(defn- validator-checkbox
  [path initial-content key disabled? on-change]
  (let [id         (util/new-uuid)
        disabled?  (or disabled?
                       @(subscribe [:editor/component-locked? path]))
        validators (-> initial-content :validators set)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   (contains? validators (name key))
                                    :disabled  disabled?
                                    :on-change (fn [event]
                                                 (let [checked (boolean (-> event .-target .-checked))]
                                                   (when on-change
                                                     (on-change checked))
                                                   (dispatch [(if checked
                                                                :editor/add-validator
                                                                :editor/remove-validator) (name key) path])))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when disabled? "editor-form__checkbox-label--disabled")}
      @(subscribe [:editor/virkailija-translation key])]]))

(defn- repeater-checkbox
  [path initial-content]
  (let [id           (util/new-uuid)
        checked?     (-> initial-content :params :repeatable boolean)
        component-locked? @(subscribe [:editor/component-locked? path])]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :disabled  component-locked?
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable]))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when component-locked? "editor-form__checkbox-label--disabled")}
      @(subscribe [:editor/virkailija-translation :multiple-answers])]]))

(defn- belongs-to-hakukohteet-modal
  [_ id _ _ _]
  (let [used-by-haku?   (subscribe [:editor/used-by-haku?])
        haut            (subscribe [:editor/filtered-haut id])
        hakukohderyhmat (subscribe [:editor/filtered-hakukohderyhmat id])]
    (fn [path id selected-hakukohteet selected-hakukohderyhmat hidden-disabled?]
      (if @used-by-haku?
        [h-and-h/popup
         [h-and-h/search-input
          {:id                       id
           :haut                     (map second @haut)
           :hakukohderyhmat          @hakukohderyhmat
           :hakukohde-selected?      #(contains? (set selected-hakukohteet) %)
           :hakukohderyhma-selected? #(contains? (set selected-hakukohderyhmat) %)}]
         (when-not hidden-disabled?
           [h-and-h/visibility-checkbox id path])
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
           @(subscribe [:editor/virkailija-translation :set-haku-to-form])]]
         nil
         nil
         #(dispatch [:editor/hide-belongs-to-hakukohteet-modal id])]))))

(defn- belongs-to
  [_ _ _ _]
  (let [fetching? (subscribe [:editor/fetching-haut?])]
    (fn [_ _ name on-click]
      [:li.belongs-to-hakukohteet__hakukohde-list-item.animated.fadeIn
       [:span.belongs-to-hakukohteet__hakukohde-label
        (if @fetching?
          [:i.zmdi.zmdi-spinner.spin]
          name)]
       [:button.belongs-to-hakukohteet__hakukohde-remove
        {:on-click on-click}
        [:i.zmdi.zmdi-close.zmdi-hc-lg]]])))

(defn- belongs-to-hakukohteet-option
  [parent-key index _]
  (let [id            (str parent-key "-" index)
        on-click-show (fn [_]
                        (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide (fn [_]
                        (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?   (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        form-locked?  (subscribe [:editor/form-locked?])]
    (fn [parent-key index path]
      (let [id            (str parent-key "-" index)
            initial-content @(subscribe [:editor/get-component-value path])
            visible-hakukohteet     (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohde-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohteet
                                                                                  path oid]))})
                                      (:belongs-to-hakukohteet initial-content))
            visible-hakukohderyhmat (mapv (fn [oid] {:oid      oid
                                                     :name     @(subscribe [:editor/belongs-to-hakukohderyhma-name oid])
                                                     :on-click (fn [_] (dispatch [:editor/remove-from-belongs-to-hakukohderyhma
                                                                                  path oid]))})
                                      (:belongs-to-hakukohderyhma initial-content))
            visible                 (sort-by :name (concat visible-hakukohteet
                                                           visible-hakukohderyhmat))]
        [:div.belongs-to-hakukohteet
         [:button.belongs-to-hakukohteet__modal-toggle
          {:disabled @form-locked?
           :class    (when @form-locked? "belongs-to-hakukohteet__modal-toggle--disabled")
           :on-click (when-not @form-locked?
                       (if @show-modal? on-click-hide on-click-show))}
          (str @(subscribe [:editor/virkailija-translation :visibility-on-form]) " ")]
         [:span.belongs-to-hakukohteet__modal-toggle-label
          (cond (and (empty? visible))
                @(subscribe [:editor/virkailija-translation :visible-to-all])

                :else
                @(subscribe [:editor/virkailija-translation :visible-to-hakukohteet]))]
         (when @show-modal?
           [belongs-to-hakukohteet-modal path
            id
            (map :oid visible-hakukohteet)
            (map :oid visible-hakukohderyhmat)
            true])
         [:ul.belongs-to-hakukohteet__hakukohde-list
          (for [{:keys [oid name on-click]} visible]
            ^{:key oid}
            [belongs-to path oid name on-click])]]))))

(defn- belongs-to-hakukohteet
  [path initial-content]
  (let [id            (:id initial-content)
        on-click-show (fn [_]
                        (dispatch [:editor/show-belongs-to-hakukohteet-modal id]))
        on-click-hide (fn [_]
                        (dispatch [:editor/hide-belongs-to-hakukohteet-modal id]))
        show-modal?   (subscribe [:editor/show-belongs-to-hakukohteet-modal id])
        component-locked?  (subscribe [:editor/component-locked? path])]
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
          {:disabled @component-locked?
           :class    (when @component-locked? "belongs-to-hakukohteet__modal-toggle--disabled")
           :on-click (when-not @component-locked?
                       (if @show-modal? on-click-hide on-click-show))}
          (str @(subscribe [:editor/virkailija-translation :visibility-on-form]) " ")]
         [:span.belongs-to-hakukohteet__modal-toggle-label
          (cond @hidden?
                @(subscribe [:editor/virkailija-translation :hidden])

                (and (empty? visible))
                @(subscribe [:editor/virkailija-translation :visible-to-all])

                :else
                @(subscribe [:editor/virkailija-translation :visible-to-hakukohteet]))]
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

(defn- cut-component-button [path]
  (case @(subscribe [:editor/component-button-state path :cut])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/copy-component path true])}
     @(subscribe [:editor/virkailija-translation :cut-element])]
    :active
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/cancel-copy-component])}
     @(subscribe [:editor/virkailija-translation :cancel-cut])]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :cut-element])]))

(defn- copy-component-button [path]
  (case @(subscribe [:editor/component-button-state path :copy])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/copy-component path false])}
     @(subscribe [:editor/virkailija-translation :copy-element])]
    :active
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/cancel-copy-component])}
     @(subscribe [:editor/virkailija-translation :cancel-copy])]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :copy-element])]))

(defn- remove-component-button [path & {:keys [data-test-id]}]
  (case @(subscribe [:editor/component-button-state path :remove])
    :enabled
    [:button.editor-form__component-button
     {:on-click #(dispatch [:editor/start-remove-component path])
      :data-test-id data-test-id}
     @(subscribe [:editor/virkailija-translation :remove])]
    :confirm
    [:div.editor-form__component-button-group
     [:button.editor-form__component-button.editor-form__component-button--confirm
      {:on-click (fn [_] (dispatch [:editor/confirm-remove-component path]))
       :data-test-id (some-> data-test-id (str "-confirm"))}
      @(subscribe [:editor/virkailija-translation :confirm-delete])]
     [:button.editor-form__component-button
      {:on-click #(dispatch [:editor/cancel-remove-component path])}
      @(subscribe [:editor/virkailija-translation :cancel-remove])]]
    :disabled
    [:button.editor-form__component-button
     {:disabled true}
     @(subscribe [:editor/virkailija-translation :remove])]))

(defn- header-metadata
  [path metadata]
  [:div
   [:span.editor-form__component-main-header-metadata
    (s/format "%s %s, %s %s %s"
              @(subscribe [:editor/virkailija-translation :created-by])
              (-> metadata :created-by :name)
              @(subscribe [:editor/virkailija-translation :last-modified-by])
              (if (= (-> metadata :created-by :oid)
                     (-> metadata :modified-by :oid))
                ""
                (-> metadata :modified-by :name))
              (-> metadata :modified-by :date temporal/str->googdate temporal/time->date))]
   [:button.editor-form__component-lock-button
    {:on-click #(dispatch [:editor/toggle-component-lock path])}
    (if @(subscribe [:editor/component-locked? path])
      [:i.zmdi.zmdi-lock-outline.zmdi-hc-lg.editor-form__component-lock-button--locked]
      [:i.zmdi.zmdi-lock-open.zmdi-hc-lg])]])

(defn- text-header
  [id label path metadata & {:keys [foldable?
                                    can-copy?
                                    can-cut?
                                    can-remove?
                                    sub-header
                                    data-test-id]
                             :or   {foldable?   true
                                    can-copy?   true
                                    can-cut?    true
                                    can-remove? true}}]
  (let [folded? @(subscribe [:editor/folded? id])]
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
       (cond-> {}
               data-test-id
               (assoc :data-test-id (str data-test-id "-label")))
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
       [header-metadata path metadata])
     (when can-cut?
       [cut-component-button path])
     (when can-copy?
       [copy-component-button path])
     (when can-remove?
       [remove-component-button path :data-test-id (some-> data-test-id (str "-remove-component-button"))])]))

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
       (fn [_ content-component]
         (let [_ @folded?]
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
     [:span @(subscribe [:editor/virkailija-translation :md-help-title])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-bold])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-cursive])]
     [:br]
     [:span @(subscribe [:editor/virkailija-translation :md-help-link])]
     [:br]
     [:a {:href          "https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet"
          :target        "_blank"
          :on-mouse-down (fn [evt]
                           (let [url (.getAttribute (-> evt .-target) "href")]
                             (.open js/window url "_blank")))}
      @(subscribe [:editor/virkailija-translation :md-help-more])]]]])

(defn input-field [path lang dispatch-fn {:keys [class value-fn tag placeholder]
                                          :or   {tag :input}}]
  (let [component    (subscribe [:editor/get-component-value path])
        focus?       (subscribe [:state-query [:editor :ui (:id @component) :focus?]])
        value        (or
                      (when value-fn
                        (reaction (value-fn @component)))
                      (reaction (get-in @component [:label lang])))
        languages    (subscribe [:editor/languages])
        component-locked? (subscribe [:editor/component-locked? path])]
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
                               :placeholder placeholder
                               :on-change dispatch-fn
                               :on-drop   prevent-default
                               :disabled  @component-locked?}])})))

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

(defn- koodisto-fields-with-lang [_ _]
  (fn [languages option-path]
    (let [component           @(subscribe [:editor/get-component-value option-path])]
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
        component-locked?     (subscribe [:editor/component-locked? path])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       [:div.editor-form__info-addon-checkbox
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :disabled  @component-locked?
                 :on-change (fn [event]
                              (dispatch [:editor/set-component-value
                                         (if (-> event .-target .-checked) {:fi "" :sv "" :en ""} nil)
                                         path :params :info-text :label]))}]
        [:label
         {:for   id
          :class (when @component-locked? "disabled")}
         @(subscribe [:editor/virkailija-translation :info-addon])]]
       (when @checked?
         (let [collapsed-id (util/new-uuid)]
           [:div.editor-form__info-addon-checkbox
            [:input {:type      "checkbox"
                     :id        collapsed-id
                     :checked   (boolean @collapse-checked)
                     :disabled  @component-locked?
                     :on-change (fn [event]
                                  (dispatch [:editor/set-component-value
                                             (-> event .-target .-checked)
                                             path :params :info-text-collapse]))}]
            [:label
             {:for   collapsed-id
              :class (when @component-locked? "editor-form__checkbox-label--disabled")}
             @(subscribe [:editor/virkailija-translation :collapse-info-text])]]))
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

(defn- decimal-places-selector [component-id path]
  (let [decimal-places (subscribe [:editor/get-component-value path :params :decimals])
        component-locked?   (subscribe [:editor/component-locked? path])
        min-value      (subscribe [:editor/get-range-value component-id :min-value path])
        max-value      (subscribe [:editor/get-range-value component-id :max-value path])
        min-invalid?   (subscribe [:state-query [:editor :ui component-id :min-value :invalid?]])
        max-invalid?   (subscribe [:state-query [:editor :ui component-id :max-value :invalid?]])
        min-id         (util/new-uuid)
        max-id         (util/new-uuid)
        format-range   (fn [value]
                         (clojure.string/replace (clojure.string/trim (or value "")) "." ","))]
    (fn [component-id path]
      [:div
       [:div.editor-form__additional-params-container
        [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :shape])]
        [:select.editor-form__decimal-places-selector
         {:value     (or @decimal-places "")
          :disabled  @component-locked?
          :on-change (fn [e]
                       (let [new-val (get-val e)
                             value   (when (not-empty new-val)
                                       (js/parseInt new-val))]
                         (dispatch [:editor/set-decimals-value component-id value path])))}
         [:option {:value "" :key 0} @(subscribe [:editor/virkailija-translation :integer])]
         (doall
           (for [i (range 1 5)]
             [:option {:value i :key i} (str i " " @(subscribe [:editor/virkailija-translation :decimals]))]))]]
       [:div.editor-form__additional-params-container
        [:label.editor-form__range-label
         {:for   min-id
          :class (when @component-locked? "editor-form__checkbox-label--disabled")}
         @(subscribe [:editor/virkailija-translation :numeric-range])]
        [:input.editor-form__range-input
         {:type      "text"
          :id        min-id
          :class     (when @min-invalid? "editor-form__text-field--invalid")
          :disabled  @component-locked?
          :value     @min-value
          :on-blur   #(dispatch [:editor/set-range-value component-id :min-value (format-range (-> % .-target .-value)) path])
          :on-change #(dispatch [:editor/set-range-value component-id :min-value (-> % .-target .-value) path])}]
        [:span "—"]
        [:input.editor-form__range-input
         {:type      "text"
          :id        max-id
          :class     (when @max-invalid? "editor-form__text-field--invalid")
          :disabled  @component-locked?
          :value     @max-value
          :on-blur   #(dispatch [:editor/set-range-value component-id :max-value (format-range (-> % .-target .-value)) path])
          :on-change #(dispatch [:editor/set-range-value component-id :max-value (-> % .-target .-value) path])}]]])))

(defn- text-component-type-selector [_ path _]
  (let [id           (util/new-uuid)
        checked?     (subscribe [:editor/get-component-value path :params :numeric])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [component-id path _]
      [:div
       [:div.editor-form__checkbox-container
        [:input.editor-form__checkbox
         {:type      "checkbox"
          :id        id
          :checked   (or @checked? false)
          :disabled  @component-locked?
          :on-change (fn [event]
                       (let [checked-now? (-> event .-target .-checked)]
                         (dispatch [:editor/set-component-value checked-now? path :params :numeric])
                         (dispatch [(if checked-now?
                                      :editor/add-validator
                                      :editor/remove-validator) "numeric" path])
                         (when-not checked-now?
                           (dispatch [:editor/set-decimals-value component-id nil path]))))}]
        [:label.editor-form__checkbox-label
         {:for   id
          :class (when @component-locked? "editor-form__checkbox-label--disabled")}
         @(subscribe [:editor/virkailija-translation :only-numeric])]]
       (when @checked?
         [decimal-places-selector component-id path])])))

(defn- button-label-class
  [button-name component-locked?]
  (let [button-class (match button-name
                       "S" "editor-form__button--left-edge"
                       "L" "editor-form__button--right-edge"
                       :else nil)]
    (str (when component-locked? "editor-form__button-label--disabled ") button-class)))

(defn text-component [_ path & {:keys [header-label]}]
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
        component-locked?      (subscribe [:editor/component-locked? path])]
    (fn [initial-content path & {:keys [header-label size-label]}]
      [:div.editor-form__component-wrapper
       [text-header (:id initial-content) header-label path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content
        path;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
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
                        :disabled  @component-locked?
                        :on-change (fn [] (size-change btn-name))}]
                      [:label.editor-form__button-label
                       {:for   btn-id
                        :class (button-label-class btn-name @component-locked?)}
                       btn-name]]))]
           (when text-area?
             [:div.editor-form__max-length-container
              [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :max-characters])]
              [:input.editor-form__text-field.editor-form__text-field-auto-width
               {:value     @max-length
                :disabled  @component-locked?
                :on-change #(max-length-change (get-val %))}]])]
          [:div.editor-form__checkbox-wrapper
           [validator-checkbox path initial-content :required (required-disabled initial-content)]
           (when-not text-area?
             [repeater-checkbox path initial-content])
           (when-not text-area?
             [text-component-type-selector (:id initial-content) path radio-group-id])]
          [belongs-to-hakukohteet path initial-content]]
         [info-addon path]]]])))

(defn text-field [initial-content path]
  [text-component initial-content path
   :header-label @(subscribe [:editor/virkailija-translation :text-field])
   :size-label @(subscribe [:editor/virkailija-translation :text-field-size])])

(defn text-area [initial-content path]
  [text-component initial-content path
   :header-label @(subscribe [:editor/virkailija-translation :text-area])
   :size-label @(subscribe [:editor/virkailija-translation :text-area-size])])

(defn- remove-dropdown-option-button [path option-index disabled? parent-key option-value question-group-element?]
  [:div.editor-form__multi-options-remove--cross
   [copy-link (str parent-key "_" (when question-group-element? "groupN_") option-value) :answer? true :shared-use-warning? false]
   [:i.zmdi.zmdi-delete.zmdi-hc-lg
    {:on-click (fn [evt]
                 (when-not disabled?
                   (.preventDefault evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index])))
     :class    (when disabled? "editor-form__multi-options-remove--cross--disabled")}]])

(defn- dropdown-option
  [option-index _ _ _ path _ show-followups _ _ _ & _]
  (let [component-locked?        (subscribe [:editor/component-locked? path])
        on-click            (fn [up? event]
                              (when-not @component-locked?
                                (.preventDefault event)
                                (reset! show-followups nil)
                                (dispatch [(if up?
                                             :editor/move-option-up
                                             :editor/move-option-down) path option-index])))
        selection-limit?    (subscribe [:editor/selection-limit? path])
        only-numbers        (fn [value]
                              (let [n (string/replace value #"\D" "")]
                                (if (empty? n)
                                  nil
                                  (js/parseInt n))))]
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
           (input-fields-with-lang
            (fn [lang]
              [input-field option-path lang #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) option-path :label lang])])
            languages)
           [koodisto-fields-with-lang languages option-path])]
        (when @selection-limit?
          [:div.editor-form__selection-limit
           [input-field option-path :dont-care #(dispatch [:editor/set-dropdown-option-selection-limit
                                                           (only-numbers (-> % .-target .-value)) option-path :selection-limit])
            {:placeholder @(subscribe [:editor/virkailija-translation :selection-limit-input])
             :class       "editor-form__text-field--selection-limit"
             :value-fn    (fn [v] (:selection-limit v))}]])
        (when (not question-group-element?)
          [followup-question option-index followups option-path show-followups parent-key option-value question-group-element?])
        [belongs-to-hakukohteet-option parent-key option-index option-path]
        (when editable?
          [remove-dropdown-option-button path option-index (or @component-locked? (< option-count 3)) parent-key option-value question-group-element?])]
       (when (not question-group-element?)
         [followup-question-overlay option-index followups path show-followups])])))

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
         {:id        id
          :value     (:uri @selected-koodisto)
          :on-change #(dispatch [:editor/select-koodisto-options (.-value (.-target %)) path (:allow-invalid? @selected-koodisto)])}
         (when (= (:uri @selected-koodisto) "")
           [:option {:value "" :disabled true} ""])
         (for [{:keys [uri title]} koodisto-whitelist/koodisto-whitelist]
           ^{:key (str "koodisto-" uri)}
           [:option {:value uri} title])]
        [:div.editor-form__select-koodisto-dropdown-arrow
         [:i.zmdi.zmdi-chevron-down]]]])))

(defn- custom-answer-options [_ _ _ _ _ _ _ _]
  (fn [languages options followups path question-group-element? editable? show-followups parent-key]
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
            {:on-click show-koodisto-options}
            [:i.zmdi.zmdi-chevron-down] (str " " @(subscribe [:editor/virkailija-translation :show-options]))]]
          [:div
           [:div.editor-form__show-koodisto-values
            [:a
             {:on-click hide-koodisto-options}
             [:i.zmdi.zmdi-chevron-up] (str " " @(subscribe [:editor/virkailija-translation :hide-options]))]]
           [custom-answer-options languages (:options value) followups path question-group-element? editable? show-followups parent-key]])))))

(defn dropdown [_ _ path _]
  (let [languages                (subscribe [:editor/languages])
        options-koodisto         (subscribe [:editor/get-component-value path :koodisto-source])
        koodisto-ordered-by-user (subscribe [:editor/get-component-value path :koodisto-ordered-by-user])
        value                    (subscribe [:editor/get-component-value path])
        support-selection-limit? (subscribe [:editor/dropdown-with-selection-limit? path])
        selected-form-key        (subscribe [:editor/selected-form-key])
        koodisto-ordered-id      (util/new-uuid)
        component-locked?        (subscribe [:editor/component-locked? path])
        allow-invalid-koodis-id  (util/new-uuid)]
    (fn [initial-content followups path {:keys [question-group-element?]}]
      (let [languages      @languages
            field-type     (:fieldType @value)
            show-followups (r/atom nil)]
        [:div.editor-form__component-wrapper
         (let [header (case field-type
                        "dropdown"       (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :dropdown-koodisto])
                                           @(subscribe [:editor/virkailija-translation :dropdown]))
                        "singleChoice"   @(subscribe [:editor/virkailija-translation :single-choice-button])
                        "multipleChoice" (if (some? @options-koodisto)
                                           @(subscribe [:editor/virkailija-translation :multiple-choice-koodisto])
                                           @(subscribe [:editor/virkailija-translation :multiple-choice])))]
           [text-header (:id initial-content) header path (:metadata initial-content)
            :sub-header (:label @value)])
         [component-content
          path
          [:div
           [:div.editor-form__component-row-wrapper
            [:div.editor-form__multi-question-wrapper
             [:div.editor-form__text-field-wrapper
              [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
               [copy-link (:id initial-content)]]
              (input-fields-with-lang
               (fn [lang]
                 [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
               languages
               :header? true)]
             [:div.editor-form__checkbox-wrapper
              [validator-checkbox path initial-content :required (required-disabled initial-content)]
              (when @support-selection-limit?
                [validator-checkbox path initial-content :selection-limit nil
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
                  @(subscribe [:editor/virkailija-translation :allow-invalid-koodis])]])]
             [belongs-to-hakukohteet path initial-content]]]
           [info-addon path initial-content]
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
                                (.preventDefault evt)
                                (reset! show-followups nil)
                                (dispatch [:editor/add-dropdown-option path])))
                  :class    (when @component-locked? "editor-form__add-dropdown-item--disabled")}
                 [:i.zmdi.zmdi-plus-square] (str " " @(subscribe [:editor/virkailija-translation :add]))]])]]]]]))))

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
       [:div.editor-form__wrapper-element-well
        children]
       [dnd/drag-n-drop-spacer (conj path :children (count children))]
       (when-not @(subscribe [:editor/component-locked? path])
         (case (:fieldClass content)
           "wrapperElement" [toolbar/add-component (conj path :children (count children))]
           "questionGroup"  [toolbar/question-group-toolbar path
                             (fn [generate-fn]
                               (dispatch [:generate-component generate-fn (conj path :children (count children))]))]))]]]))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
              "questionGroup" (map #(recursively-get-labels %) (:children component))
              "wrapperElement" (map #(recursively-get-labels %) (:children component))
              :else (-> component :label lang)))]
    (flatten (recursively-get-labels component))))

(defn hakukohteet-module [_ path]
  (let [virkailija-lang (subscribe [:editor/virkailija-lang])
        value           (subscribe [:editor/get-component-value path])]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
        :foldable? false
        :can-cut? true
        :can-copy? false
        :can-remove? false
        :data-test-id "hakukohteet-header"]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__module-fields
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
         [text-header (:id content) (get-in @value [:label @virkailija-lang]) path nil
          :foldable? foldable?
          :can-cut? can-cut?
          :can-copy? can-copy?
          :can-remove? can-remove?
          :show-child-component-names? show-child-component-names?
          :data-test-id (some-> data-test-id-prefix (str "-header"))]
         [:div.editor-form__component-content-wrapper
          (when has-multiple-configurations?
            (let [values (set ["onr" "muu"])]
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
              (clojure.string/join ", " (get-leaf-component-labels @value :fi))]])]]))))

(defn info-element
  "Info text which is a standalone component"
  [_ path]
  (let [languages        (subscribe [:editor/languages])
        collapse-checked (subscribe [:editor/get-component-value path :params :info-text-collapse])
        sub-header       (subscribe [:editor/get-component-value path :label])
        component-locked?     (subscribe [:editor/component-locked? path])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header (:id initial-content) @(subscribe [:editor/virkailija-translation :info-element]) path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content
        path ;(:id initial-content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
           (input-fields-with-lang
            (fn [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
            @languages
            :header? true)
           [:div.infoelement
            [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :text])]
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
                                             :disabled  @component-locked?
                                             :on-change (fn [event]
                                                          (dispatch [:editor/set-component-value
                                                                     (-> event .-target .-checked)
                                                                     path :params :info-text-collapse]))}]
              [:label.editor-form__checkbox-label
               {:for   collapsed-id
                :class (when @component-locked? "editor-form__checkbox-label--disabled")}
               @(subscribe [:editor/virkailija-translation :collapse-info-text])]])]
          [belongs-to-hakukohteet path initial-content]]]]])))

(defn pohjakoulutusristiriita
  [_ _]
  (let [languages (subscribe [:editor/languages])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
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

(defn adjacent-fieldset [_ path _]
  (let [languages         (subscribe [:editor/languages])
        sub-header        (subscribe [:editor/get-component-value path :label])
        component-locked? (subscribe [:editor/component-locked? path])]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       [text-header (:id content) @(subscribe [:editor/virkailija-translation :adjacent-fieldset]) path (:metadata content)
        :sub-header @sub-header]
       [component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
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
          (when (and (not @component-locked?)
                     (-> (count children) (< 3)))
            [toolbar/adjacent-fieldset-toolbar
             (concat path [:children])
             (fn [component-fn]
               (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]]]])))

(defn adjacent-text-field [_ _]
  (let [languages (subscribe [:editor/languages])
        radio-group-id    (util/new-uuid)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header (:id content) @(subscribe [:editor/virkailija-translation :text-field]) path (:metadata content)
        :foldable? false
        :can-cut? false
        :can-copy? false
        :can-remove? true]
       [:div.editor-form__component-content-wrapper
        [:div.editor-form__component-row-wrapper
         [:div.editor-form__text-field-wrapper
          [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :question])
           [copy-link (:id content)]]
          (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
         [:div.editor-form__checkbox-wrapper
          [validator-checkbox path content :required (required-disabled content)]
          [text-component-type-selector (:id content) path radio-group-id]]
        [belongs-to-hakukohteet path content]]]])))

(defn attachment-textarea [path]
  (let [checked?         (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        mail-attachment? (subscribe [:editor/get-component-value path :params :mail-attachment?])
        collapse?        (subscribe [:editor/get-component-value path :params :info-text-collapse])
        languages        (subscribe [:editor/languages])
        component-locked?      (subscribe [:editor/component-locked? path])]
    (fn [path]
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
                              (clojure.string/blank? value) (update-value value nil true)
                              (and value deadline) (update-value value deadline true)
                              :else (update-value value nil false))))]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [text-header (:id content) @(subscribe [:editor/virkailija-translation :attachment]) path (:metadata content)
        :sub-header (:label @component)]
       [component-content
        path ;(:id content)
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :attachment-name])
            [copy-link (:id content)]]
           (input-fields-with-lang
            (fn attachment-file-name-input [lang]
              [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
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
             [validator-checkbox path content :required (required-disabled content)]])
          [belongs-to-hakukohteet path content]]
         [attachment-textarea path]]]])))
