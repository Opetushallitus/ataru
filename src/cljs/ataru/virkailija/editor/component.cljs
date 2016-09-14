(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.component-data.component :as component]
            [ataru.cljs-util :as util :refer [cljs->str str->cljs new-uuid]]
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

(defn- render-checkbox
  [path initial-content]
  (let [id           (util/new-uuid)
        required?    (true? (some #(= % "required") (:validators initial-content)))]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type "checkbox"
                                    :id id
                                    :checked required?
                                    :on-change (fn [event]
                                                 (let [dispatch-kwd (if (-> event .-target .-checked)
                                                                     :editor/add-validator
                                                                     :editor/remove-validator)]
                                                   (dispatch [dispatch-kwd "required" path])))}]
     [:label.editor-form__checkbox-label {:for id} "Pakollinen tieto"]]))

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
  [label path & {:keys [form-section?]}]
  [:div.editor-form__header-wrapper
   {:draggable true
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

(defn input-field
  ([path lang args]
   (input-field path lang #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang]) args))
  ([path lang dispatch-fn args]
   (let [value (subscribe [:editor/get-component-value path])]
     (input-field
       path
       (:focus? @value)
       (reaction (get-in @value [:label lang]))
       dispatch-fn
       args)))
  ([path focus? value dispatch-fn {:keys [class]}]
   (r/create-class
     {:component-did-mount (fn [component]
                             (when focus?
                               (let [dom-node (r/dom-node component)]
                                 (.focus dom-node))))
      :reagent-render      (fn [_ _ _ _]
                             [:input.editor-form__text-field
                              (cond-> {:value     @value
                                       :on-change dispatch-fn
                                       :on-drop   prevent-default}
                                (not (clojure.string/blank? class))
                                (assoc :class class))])})))

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
       {:class @animation-effect}
       [text-header header-label path]
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header "Kysymys"]
        (doall
          (for [lang @languages]
            ^{:key lang}
            [input-field path lang {}]))]
       [:div.editor-form__size-button-wrapper
        [:header.editor-form__component-item-header size-label]
        [:div.editor-form__size-button-group
         (doall (for [[btn-name btn-id] radio-button-ids]
                  ^{:key (str btn-id "-radio")}
                  [:div
                   [:input.editor-form__size-button.editor-form__size-button
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
                                   "S" "editor-form__size-button--left-edge"
                                   "L" "editor-form__size-button--right-edge"
                                   :else nil)}
                    btn-name]]))]]
       [:div.editor-form__checkbox-wrapper
        (render-checkbox path initial-content)]])))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän koko"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen koko"])

(defn- remove-dropdown-option-button [path option-index]
  [:a {:href "#"
       :on-click (fn [evt]
                   (.preventDefault evt)
                   (dispatch [:editor/remove-dropdown-option path :options option-index]))}
   [:i.zmdi.zmdi-close.zmdi-hc-lg]])

(defn- dropdown-option [option-index option path languages]
  [:div.editor-form__multi-options-wrapper
   {:key (str "options-" option-index)}
   (for [lang languages]
     (let [option-value (:value option)
           option-path [path :options option-index]]
       (when-not (and (clojure.string/blank? option-value)
                      (= option-index 0))
         ^{:key (str "option-" lang "-" option-index)}
         [:div.editor-form__multi-option-wrapper
          [:div.editor-form__text-field-wrapper__option
           [input-field option-path lang #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) option-path :label lang])
            (cond-> {}
              (< 1 (count languages))
              (assoc :class "editor-form__text-field-wrapper__option--with-label"))]
           (when (< 1 (count languages))
             [:div.editor-form__text-field-label (-> lang name clojure.string/upper-case)])
           (remove-dropdown-option-button path option-index)]])))])

(defn dropdown [initial-content path]
  (let [languages (subscribe [:editor/languages])
        value (subscribe [:editor/get-component-value path])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      (let [languages @languages]
        [:div.editor-form__component-wrapper
         {:class @animation-effect}
         (let [header (case (:fieldType @value)
                        "dropdown"       "Pudotusvalikko"
                        "multipleChoice" "Lista, monta valittavissa")]
           [text-header header path])
         [:div.editor-form__multi-question-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header "Kysymys"]
           (doall
             (for [lang languages]
               ^{:key lang}
               [input-field path lang {}]))]
          [:div.editor-form__checkbox-wrapper
           (render-checkbox path initial-content)]]
         [:div.editor-form__multi-options_wrapper
          [:header.editor-form__component-item-header "Vastausvaihtoehdot"]
          (doall
            (let [options (:options @value)
                  option-fields (map-indexed (fn [idx option]
                                               (dropdown-option idx option path languages))
                                             options)]
              (remove nil? option-fields)))]
         [:div.editor-form__add-dropdown-item
          [:a
           {:href "#"
            :on-click (fn [evt]
                        (.preventDefault evt)
                        (dispatch [:editor/add-dropdown-option path]))}
           [:i.zmdi.zmdi-plus-square] " Lisää"]]]))))

(def ^:private toolbar-elements
  {"Lomakeosio"                component/form-section
   "Tekstikenttä"              component/text-field
   "Tekstialue"                component/text-area
   "Pudotusvalikko"            component/dropdown
   "Lista, monta valittavissa" component/multiple-choice})

(defn ^:private component-toolbar [path]
  (fn [path]
    (into [:ul.form__add-component-toolbar--list]
          (for [[component-name generate-fn] toolbar-elements
                :when                        (not (and
                                                    (vector? path)
                                                    (= :children (second path))
                                                    (= "Lomakeosio" component-name)))]
            [:li.form__add-component-toolbar--list-item
             [:a {:href     "#"
                  :on-click (fn [evt]
                              (.preventDefault evt)
                              (dispatch [:generate-component generate-fn path]))}
              component-name]]))))

(defn add-component [path]
  (fn [path]
    [:div.editor-form__add-component-toolbar
     [component-toolbar path]
     [:div.plus-component
      [:span "+"]]]))

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
      [:div.editor-form__section_wrapper
       {:class @animation-effect}
       [:div.editor-form__component-wrapper
        [text-header "Lomakeosio" path :form-section? true]
        [:div.editor-form__text-field-wrapper.editor-form__text-field--section
         [:header.editor-form__component-item-header "Osion nimi"]
         (doall
           (for [lang @languages]
             ^{:key lang}
             [:input.editor-form__text-field
              {:value     (get-in @value [:label lang])
               :on-change #(dispatch-sync [:editor/set-component-value (-> % .-target .-value) path :label lang])
               :on-drop   prevent-default}]))]]
       children
       [drag-n-drop-spacer (conj path :children (count children))]
       [add-component (conj path :children (count children))]])))

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
