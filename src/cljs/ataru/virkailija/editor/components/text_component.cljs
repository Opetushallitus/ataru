(ns ataru.virkailija.editor.components.text-component
  (:require
    [ataru.application-common.application-field-common :refer [copy-link]]
    [ataru.cljs-util :as util]
    [ataru.virkailija.editor.components.followup-question :refer [followup-question-overlay]]
    [ataru.virkailija.temporal :as temporal]
    [ataru.virkailija.views.hakukohde-and-hakukohderyhma-search :as h-and-h]
    [cljs.core.match :refer-macros [match]]
    [clojure.string :as string]
    [goog.string :as s]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

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
  (let [id                (util/new-uuid)
        checked?          (-> initial-content :params :repeatable boolean)
        has-options?      (not (empty? (:options initial-content)))
        component-locked? @(subscribe [:editor/component-locked? path])
        disabled?         (or has-options? component-locked?)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :disabled  disabled?
                                    :on-change (fn [event]
                                                 (when (not disabled?)
                                                   (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable])))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when disabled? "editor-form__checkbox-label--disabled")}
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
              (remove string/blank?)
              (string/join " - ")))]]
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
                               {:class        (str "editor-form__text-field " (when-not (empty? class) class))
                                :value        @value
                                :placeholder  placeholder
                                :on-change    dispatch-fn
                                :on-drop      prevent-default
                                :disabled     @component-locked?
                                :data-test-id "tekstikenttä-kysymys"}])})))

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

(defn text-component-type-selector [_ path _]
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

(declare custom-answer-options)

(defn text-field-option-followups-wrapper
  [options followups path show-followups]
  (let [option-count (count options)]
    (when (or (nil? @show-followups)
              (not (= (count @show-followups) option-count)))
      (reset! show-followups (vec (replicate option-count true))))
    (when (< 0 option-count)
      (let [option-index 0
            followups    (nth followups option-index)]
        [:div.editor-form__text-field-option-followups-wrapper
         [followup-question-overlay option-index followups path show-followups]]))))

(defn- text-field-has-an-option [_ _ _ _]
  (let [id (util/new-uuid)]
    (fn [value followups path component-locked?]
      (let [option-index 0
            has-options? (not (empty? (:options value)))
            repeatable?  (-> value :params :repeatable boolean)
            disabled?    (or component-locked?
                             (not (empty? (first followups)))
                             repeatable?)]
        [:div.editor-form__text-field-checkbox-container
         [:input.editor-form__text-field-checkbox
          {:id        id
           :type      "checkbox"
           :checked   has-options?
           :disabled  disabled?
           :on-change (fn [evt]
                        (when-not disabled?
                          (.preventDefault evt)
                          (if (-> evt .-target .-checked)
                            (dispatch [:editor/add-text-field-option path])
                            (dispatch [:editor/remove-text-field-option path :options option-index]))))}]
         [:label.editor-form__text-field-checkbox-label
          {:for   id
           :class (when disabled? "editor-form__text-field-checkbox-label--disabled")}
          @(subscribe [:editor/virkailija-translation :lisakysymys])]]))))

(defn- text-field-option-followups
  [value followups path show-followups]
  [:div.editor-form__component-row-wrapper
   [text-field-option-followups-wrapper (:options value) followups path show-followups]])

(defn text-component [_ _ path & {:keys [header-label]}]
  (let [languages         (subscribe [:editor/languages])
        value             (subscribe [:editor/get-component-value path])
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
        component-locked? (subscribe [:editor/component-locked? path])
        show-followups    (r/atom nil)]                     ; TODO: pitäisikö olla kuten dropdown???
    (fn [initial-content followups path & {:keys [header-label _ size-label]}]
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
         [:div.editor-form__text-field-checkbox-wrapper
          [info-addon path]
          (when-not text-area?
            [text-field-has-an-option @value followups path @component-locked?])]
         (when-not text-area?
           [text-field-option-followups @value followups path show-followups])]]])))
