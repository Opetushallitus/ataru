(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.soresu.component :as component]
            [ataru.cljs-util :as util :refer [cljs->str str->cljs new-uuid]]
            [reagent.core :as r]
            [reagent.ratom :refer-macros [reaction]]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch]]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn language [lang]
  (fn [lang]
    [:div.language
     [:div (clojure.string/upper-case (name lang))]]))

(def ^:private checkbox-metadata
  {:required {:id-suffix "_required"
              :label "Pakollinen tieto"}})

; IE only allows this data attribute name for drag event dataTransfer
; http://stackoverflow.com/questions/26213011/html5-dragdrop-issue-in-internet-explorer-datatransfer-property-access-not-pos
(def ^:private ie-compatible-drag-data-attibute-name "Text")

(defn- render-checkbox
  [path initial-content metadata-kwd]
  (let [metadata (get checkbox-metadata metadata-kwd)
        id (util/new-uuid)
        label (:label metadata)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type "checkbox"
                                    :id id
                                    :checked (true? (get initial-content metadata-kwd))
                                    :on-change #(dispatch [:editor/set-component-value (-> % .-target .-checked) path metadata-kwd])}]
     [:label.editor-form__checkbox-label {:for id} label]]))

(defn- on-drag-start
  [path]
  (fn [event]
    (-> event .-dataTransfer (.setData ie-compatible-drag-data-attibute-name (util/cljs->str path)))))

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
   [:i.zmdi.zmdi-menu.editor-form__draggable-indicator]
   [:a.editor-form__component-header-link
    {:on-click (fn [event]
                 (dispatch [:remove-component path
                            (if form-section?
                              (-> event .-target .-parentNode .-parentNode .-parentNode)
                              (-> event .-target .-parentNode .-parentNode))]))}
    "Poista"]])

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (util/new-uuid)
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch [:editor/set-component-value new-size path :params :size]))
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
            [:input.editor-form__text-field {:value     (get-in @value [:label lang])
                                             :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])
                                             :on-drop prevent-default}]))]
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
        (render-checkbox path initial-content :required)]])))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän koko"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen koko"])

(defn dropdown [initial-content path]
  (let [languages (subscribe [:editor/languages])
        value (subscribe [:editor/get-component-value path])
        animation-effect (fade-out-effect path)]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       {:class @animation-effect}
       [text-header "Pudotusvalikko" path]
       [:div.editor-form__multi-question-wrapper
        [:div.editor-form__text-field-wrapper
         [:header.editor-form__component-item-header "Kysymys"]
         (doall
           (for [lang @languages]
             ^{:key lang}
             [:input.editor-form__text-field {:value     (get-in @value [:label lang])
                                              :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])
                                              :on-drop prevent-default}]))]
        [:div.editor-form__checkbox-wrapper
         (render-checkbox path initial-content :required)]]
       [:div.editor-form__multi-options_wrapper
        [:header.editor-form__component-item-header "Vastausvaihtoehdot"]
        (doall
          (let [options-raw (:options @value)
                options (if (clojure.string/blank? (:value (last options-raw)))
                          options-raw
                          (into options-raw [(component/dropdown-option)]))
                options-count (count options)]
            (for [lang @languages
                  option-with-index (map vector (range options-count) options)]
              (let [[option-index option] option-with-index
                    option-label (get-in option [:label lang])]
                (if (and (clojure.string/blank? option-label) (= option-index 0) (not= options-count 1))
                  nil
                  ^{:key (str "option-" lang "-" option-index)}
                  [:div.editor-form__multi-option-wrapper
                   [:div.editor-form__text-field-wrapper__option
                    [:input.editor-form__text-field
                     {:value       option-label
                      :placeholder "Lisää..."
                      :on-change   #(dispatch [:editor/set-dropdown-option-value (-> % .-target .-value) path :options option-index :label lang])
                      :on-drop prevent-default}]]])))))]])))

(def ^:private toolbar-elements
  {"Lomakeosio"     component/form-section
   "Tekstikenttä"   component/text-field
   "Tekstialue"     component/text-area
   "Pudotusvalikko" component/dropdown})

(defn ^:private component-toolbar [path]
  (fn [path]
    (into [:ul.form__add-component-toolbar--list]
          (for [[component-name generate-fn] toolbar-elements
                :when                        (not (and
                                                    (vector? path)
                                                    (= :children (second path))
                                                    (= "Lomakeosio" component-name)))]
            [:li.form__add-component-toolbar--list-item {:on-click #(dispatch [:generate-component generate-fn path])}
             component-name]))))

(defn add-component [path]
  (let [show-bar? (r/atom nil)
        show-bar #(reset! show-bar? true)
        hide-bar #(reset! show-bar? false)]
    (fn [path]
      (if @show-bar?
        [:div.editor-form__add-component-toolbar
         {:on-mouse-leave hide-bar
          :on-mouse-enter show-bar}
         [component-toolbar path]]
        [:div.editor-form__add-component-toolbar
         {:on-mouse-enter show-bar
          :on-mouse-leave hide-bar}
         [:div.plus-component
          [:span "+"]]]))))

(defn drag-n-drop-spacer [path content]
  (let [expanded? (r/atom false)]
    (fn [path content]
      [:div
       {:on-drop (fn [event]
                   (.preventDefault event)
                   (reset! expanded? false)
                   (let [source-path (-> event .-dataTransfer (.getData ie-compatible-drag-data-attibute-name) util/str->cljs)]
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
               :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])
               :on-drop prevent-default}]))]]
       children
       [drag-n-drop-spacer (conj path :children (count children))]
       [add-component (conj path :children (count children))]])))
