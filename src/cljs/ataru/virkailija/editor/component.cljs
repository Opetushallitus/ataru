(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [ataru.virkailija.editor.components.followup-question :refer [followup-question followup-question-overlay]]
            [ataru.cljs-util :as util :refer [cljs->str str->cljs new-uuid]]
            [ataru.koodisto.koodisto-whitelist :as koodisto-whitelist]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

; IE only allows this data attribute name for drag event dataTransfer
; http://stackoverflow.com/questions/26213011/html5-dragdrop-issue-in-internet-explorer-datatransfer-property-access-not-pos
(def ^:private ie-compatible-drag-data-attribute-name "Text")

(defn- required-checkbox
  [path initial-content]
  (let [id           (util/new-uuid)
        required?    (true? (some? ((set (map keyword (:validators initial-content))) :required)))]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type "checkbox"
                                    :id id
                                    :checked required?
                                    :on-change (fn [event]
                                                 (dispatch [(if (-> event .-target .-checked)
                                                              :editor/add-validator
                                                              :editor/remove-validator) "required" path]))}]
     [:label.editor-form__checkbox-label {:for id} "Pakollinen tieto"]]))

(defn- repeater-checkbox
  [path initial-content]
  (let [id       (util/new-uuid)
        checked? (-> initial-content :params :repeatable)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable]))}]
     [:label.editor-form__checkbox-label {:for id} "Vastaaja voi lisätä useita vastauksia"]]))

(defn- on-drag-start
  [path]
  (fn [event]
    (-> event .-dataTransfer (.setData ie-compatible-drag-data-attribute-name (util/cljs->str path)))))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- fade-out-effect
  [path]
  (reaction (case @(subscribe [:state-query [:editor :forms-meta path]])
              :fade-out "animated fadeOutUp"
              :fade-in  "animated fadeInUp"
              nil)))

(defn- text-header
  [label path & {:keys [form-section? draggable] :or {draggable true}}]
  [:div.editor-form__header-wrapper
   {:draggable (and draggable (nil? ((set path) :followup)))
    :on-drag-start (on-drag-start path)
    :on-drag-over prevent-default}
   [:header.editor-form__component-header label]
   [:a.editor-form__component-header-link
    {:on-click (fn [event]
                 (dispatch [:remove-component path
                            (if form-section?
                              (-> event .-target .-parentNode .-parentNode .-parentNode)
                              (-> event .-target .-parentNode .-parentNode))]))}
    "Poista"]])

(defn markdown-help []
  [:div.application__markdown-help
   [:div
    [:div.application__markdown-help-arrow-left]
    [:div.application__markdown-help-content
     [:span "# otsikko (# h1 – ###### h6)"]
     [:br]
     [:span "**boldattava sisältö**"]
     [:br]
     [:span "*kursivoitava sisältö*"]
     [:br]
     [:span "[linkin teksti](http://linkin osoite)"]
     [:br]
     [:a {:href "https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet" :target "_blank"} "Lisää muotoiluohjeita"]]]])

(defn input-field [path lang dispatch-fn {:keys [class value-fn tag]
                                          :or   {tag :input}}]
  (let [component (subscribe [:editor/get-component-value path])
        focus?    (subscribe [:state-query [:editor :ui (:id @component) :focus?]])
        value     (or
                    (when value-fn
                      (reaction (value-fn @component)))
                    (reaction (get-in @component [:label lang])))
        languages (subscribe [:editor/languages])]
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
                                :on-drop   prevent-default}])})))

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
  (let [id        (util/new-uuid)
        checked?  (reaction (some? @(subscribe [:editor/get-component-value path :params :info-text :label])))
        languages (subscribe [:editor/languages])]
    (fn [path]
      [:div.editor-form__info-addon-wrapper
       [:div.editor-form__info-addon-checkbox
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :on-change (fn [event]
                              (dispatch [:editor/set-component-value
                                         (if (-> event .-target .-checked) {:fi "" :sv "" :en ""} nil)
                                         path :params :info-text :label]))}]
        [:label {:for id} "Kysymys sisältää ohjetekstin"]]
       (when @checked?
         [:div.editor-form__info-addon-inputs
          (->> (input-fields-with-lang
                 (fn [lang]
                   [input-field (concat path [:params :info-text]) lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :params :info-text :label lang])])
                 @languages)
               (map (fn [field]
                      (into field [(markdown-help)]))))])])))

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages        (subscribe [:editor/languages])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (util/new-uuid)
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch-sync [:editor/set-component-value new-size path :params :size]))
        animation-effect (fade-out-effect path)]
    (fn [initial-content path & {:keys [header-label size-label]}]
      [:div.editor-form__component-wrapper
       [:div.editor-form__component-row-wrapper
        {:class @animation-effect}
        [text-header header-label path]
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Kysymys"]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
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
                      :on-change (fn [] (size-change btn-name))}]
                    [:label
                     {:for   btn-id
                      :class (match btn-name
                               "S" "editor-form__button--left-edge"
                               "L" "editor-form__button--right-edge"
                               :else nil)}
                     btn-name]]))]]
        [:div.editor-form__checkbox-wrapper
         [required-checkbox path initial-content]
         (when-not (= "Tekstialue" header-label)
           [repeater-checkbox path initial-content])]]

       [info-addon path]])))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän koko"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen koko"])

(defn- remove-dropdown-option-button [path option-index]
  [:a.editor-form__multi-options-remove--cross {:on-click (fn [evt]
                   (.preventDefault evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index]))}
   [:i.zmdi.zmdi-close.zmdi-hc-lg]])

(defn- dropdown-option [option-index path languages & {:keys [header? include-followup?] :or {header? false include-followup? true} :as opts}]
  (let [multiple-languages? (< 1 (count languages))
        option-path         [path :options option-index]]
    ^{:key (str "options-" option-index)}
    [:div
     [:div.editor-form__multi-options-wrapper-outer
      [:div
       (cond-> {:key (str "options-" option-index)}
         multiple-languages?
         (assoc :class "editor-form__multi-options-wrapper-inner"))
       (input-fields-with-lang
         (fn [lang]
           [input-field option-path lang #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) option-path :label lang])])
         languages)]
      [remove-dropdown-option-button path option-index]
      (when include-followup?
        [followup-question option-path])]
     (when include-followup?
       [followup-question-overlay option-path])]))

(defn- dropdown-multi-options [path options-koodisto]
  (let [dropdown-id                (util/new-uuid)
        custom-button-value        "Omat vastausvaihtoehdot"
        custom-button-id           (str dropdown-id "-custom")
        koodisto-button-value      (reaction (str "Koodisto" (if-let [koodisto-name (:title @options-koodisto)] (str ": " koodisto-name) "")))
        koodisto-button-id         (str dropdown-id "-koodisto")
        koodisto-popover-expanded? (r/atom false)]
    (fn [path options-koodisto]
      [:div.editor-form__button-group
       [:input
        {:type      "radio"
         :class     "editor-form__button editor-form__button--large"
         :value     custom-button-value
         :checked   (nil? @options-koodisto)
         :name      dropdown-id
         :id        custom-button-id
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? false)
                      (dispatch [:editor/select-custom-multi-options path]))}]
       [:label
        {:for   custom-button-id
         :class "editor-form__button--left-edge"}
        custom-button-value]
       [:input
        {:type      "radio"
         :class     "editor-form__button editor-form__button--large"
         :value     @koodisto-button-value
         :checked   (not (nil? @options-koodisto))
         :name      dropdown-id
         :id        koodisto-button-id
         :on-change (fn [evt]
                      (.preventDefault evt)
                      (reset! koodisto-popover-expanded? true))}]
       [:label
        {:for   koodisto-button-id
         :class "editor-form__button--right-edge"}
        @koodisto-button-value]
       (when @koodisto-popover-expanded?
         [:div.editor-form__koodisto-popover
          [:div.editor-form__koodisto-popover-header "Koodisto"
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

(defn dropdown [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        options-koodisto (subscribe [:editor/get-component-value path :koodisto-source])
        value            (subscribe [:editor/get-component-value path]) 
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      (let [languages  @languages
            field-type (:fieldType @value)]
        [:div.editor-form__component-wrapper
         {:class @animation-effect}
         (let [header (case field-type
                        "dropdown"       "Pudotusvalikko"
                        "singleChoice"   "Painikkeet, yksi valittavissa"
                        "multipleChoice" "Lista, monta valittavissa")]
           [text-header header path])
         [:div.editor-form__multi-question-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header "Kysymys"]
           (input-fields-with-lang
             (fn [lang]
               [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
             languages
             :header? true)]
          [:div.editor-form__checkbox-wrapper
           [required-checkbox path initial-content]]]

         [info-addon path initial-content]

         [:div.editor-form__multi-options_wrapper
          [:div.editor-form--padded
           [:header.editor-form__component-item-header "Vastausvaihtoehdot"]
           (when-not (= field-type "singleChoice") [dropdown-multi-options path options-koodisto])]

          (when (nil? @options-koodisto)
            (seq [
                  ^{:key "options-input"}
                  [:div.editor-form__multi-options-container
                   (map-indexed (fn [idx _]
                                  (dropdown-option idx path languages :include-followup? (= field-type "dropdown")))
                     (:options @value))]
                  ^{:key "options-input-add"}
                  [:div.editor-form__add-dropdown-item
                   [:a
                    {:on-click (fn [evt]
                                 (.preventDefault evt)
                                 (dispatch [:editor/add-dropdown-option path]))}
                    [:i.zmdi.zmdi-plus-square] " Lisää"]]]))]]))))

(defn drag-n-drop-spacer [path content]
  (let [expanded? (r/atom false)]
    (fn [path content]
      [:div
       {:on-drop (fn [event]
                   (.preventDefault event)
                   (reset! expanded? false)
                   (let [source-path (-> event .-dataTransfer (.getData ie-compatible-drag-data-attribute-name) util/str->cljs)]
                     (dispatch [:editor/move-component source-path path])))
        :on-drag-enter (fn [event] (.preventDefault event)) ;; IE needs this, otherwise on-drag-over doesn't occur
        :on-drag-over (fn [event]
                        (.preventDefault event)
                        (reset! expanded? true)
                        nil)
        :on-drag-leave (fn [event]
                         (.preventDefault event)
                         (reset! expanded? false)
                         nil)
        :class (if (and
                     (= 1 (count path))
                     (contains? content :children))
                   "editor-form__drag_n_drop_spacer_container_for_component_group"
                   "editor-form__drag_n_drop_spacer_container_for_component")}
       [:div
        {:class (if @expanded?
                  "editor-form__drag_n_drop_spacer--dashbox-visible"
                  "editor-form__drag_n_drop_spacer--dashbox-hidden")}]])))

(defn component-group [content path children]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        animation-effect (fade-out-effect path)]
    (fn [content path children]
      (let [languages @languages
            value     @value]
        [:div.editor-form__section_wrapper
         {:class @animation-effect}
         [:div.editor-form__component-wrapper
          [text-header "Lomakeosio" path :form-section? true]
          [:div.editor-form__text-field-wrapper.editor-form__text-field--section
           [:header.editor-form__component-item-header "Osion nimi"]
           (input-fields-with-lang
             (fn [lang]
               [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
             languages
             :header? true)]]
         children
         [drag-n-drop-spacer (conj path :children (count children))]
         [toolbar/add-component (conj path :children (count children))]]))))

(defn get-leaf-component-labels [component lang]
  (letfn [(recursively-get-labels [component]
            (match (:fieldClass component)
                   "wrapperElement" (map #(recursively-get-labels %) (:children component))
                   :else (-> component :label lang)))]
    (flatten (recursively-get-labels component))))

(defn module [path]
  (let [languages (subscribe [:editor/languages])
        value     (subscribe [:editor/get-component-value path])]
    (fn [path]
      [:div.editor-form__module-wrapper
       [:header.editor-form__module-header
        [:span.editor-form__module-header-label (get-in @value [:label :fi])]
        " "
        [:span (get-in @value [:label-amendment :fi])]]
       [:div.editor-form__module-fields
        [:span.editor-form__module-fields-label "Sisältää kentät:"]
        " "
        (clojure.string/join ", " (get-leaf-component-labels @value :fi))]])))

(defn info-element
  "Info text which is a standalone component"
  [initial-content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Infoteksti" path]
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header "Otsikko"]
        (input-fields-with-lang
          (fn [lang]
            [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
          @languages
          :header? true)]
       [:div.editor-form__text-field-wrapper.infoelement
        [:header.editor-form__component-item-header "Teksti"]
        (input-fields-with-lang
          (fn [lang]
            [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :text lang])
             {:value-fn (fn [component] (get-in component [:text lang]))
              :tag :textarea}])
          @languages
          :header? true)]])))

(defn adjacent-fieldset [content path children]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path children]
      [:div.editor-form__component-wrapper
       [:div.editor-form__component-row-wrapper
        {:class @animation-effect}
        [text-header "Vierekkäiset tekstikentät" path]
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Otsikko"]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__checkbox-wrapper
         [repeater-checkbox path content]]]

       [info-addon path]

       [:div.editor-form__adjacent-fieldset-container
        children

        (when (-> (count children) (< 3))
          [toolbar/adjacent-fieldset-toolbar
           (concat path [:children])
           (fn [component-fn]
             (dispatch [:generate-component component-fn (concat path [:children (count children)])]))])]])))

(defn adjacent-text-field [content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [:div.editor-form__component-row-wrapper
        {:class @animation-effect}
        [text-header "Tekstikenttä" path :draggable false]
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Kysymys"]
         (input-fields-with-lang
           (fn [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__checkbox-wrapper
         [required-checkbox path content]]]])))

(defn attachment-textarea [path]
  (let [id        (util/new-uuid)
        checked?  (subscribe [:editor/get-component-value path :params :info-text :enabled?])
        languages (subscribe [:editor/languages])]
    (fn [path]
      [:div.editor-form__text-field-wrapper.infoelement
       [:div.editor-form__attachment-info-checkbox-wrapper
        [:input {:id        id
                 :type      "checkbox"
                 :checked   @checked?
                 :on-change (fn toggle-attachment-textarea [event]
                              (.preventDefault event)
                              (let [checked? (.. event -target -checked)]
                                (dispatch [:editor/set-component-value checked? path :params :info-text :enabled?])))}]
        [:label
         {:for id}
         "Liitepyyntö sisältää ohjetekstin"]]
       (when @checked?
         (input-fields-with-lang
           (fn attachment-textarea-input [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :params :info-text :value lang])
              {:value-fn #(get-in % [:params :info-text :value lang])
               :tag      :textarea}])
           @languages
           :header? true))])))

(defn attachment [content path]
  (let [languages        (subscribe [:editor/languages])
        animation-effect (fade-out-effect path)]
    (fn [content path]
      [:div.editor-form__component-wrapper
       [:div.editor-form__component-row-wrapper
        {:class @animation-effect}
        [text-header "Liitepyyntö" path]
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Liitteen nimi"]
         (input-fields-with-lang
           (fn attachment-file-name-input [lang]
             [input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])])
           @languages
           :header? true)]
        [:div.editor-form__single-checkbox-wrapper
         [required-checkbox path content]]]
       [attachment-textarea path]])))
