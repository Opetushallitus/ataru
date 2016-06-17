(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.soresu.component :as component]
            [ataru.cljs-util :as util :refer [cljs->str str->cljs]]
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

(defn- render-checkbox
  [path initial-content metadata-kwd]
  (let [metadata (get checkbox-metadata metadata-kwd)
        id (str (gensym) (:id-suffix metadata))
        label (:label metadata)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type "checkbox"
                                    :id id
                                    :checked (true? (get initial-content metadata-kwd))
                                    :on-change #(dispatch [:editor/set-component-value (-> % .-target .-checked) path metadata-kwd])}]
     [:label.editor-form__checkbox-label {:for id} label]]))

(defn- text-header
  [label path & {:keys [form-section?]}]
  [:div.editor-form__header-wrapper
   [:header.editor-form__component-header label]
   [:a.editor-form__component-header-link
    {:on-click (fn [event]
                 (dispatch [:remove-component path
                            (if form-section?
                              (-> event .-target .-parentNode .-parentNode .-parentNode)
                              (-> event .-target .-parentNode .-parentNode))]))}
    "Poista"]])

(defn- on-drag-start
  [path]
  (fn [event]
    (-> event .-dataTransfer (.setData "path" (util/cljs->str path)))))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (str "form-size-" (gensym))
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch [:editor/set-component-value new-size path :params :size]))
        animation-effect (reaction (case @(subscribe [:state-query [:editor :forms-meta path]])
                                     :fade-out "animated fadeOutUp"
                                     :fade-in  "animated fadeInUp"
                                     nil))]
    (fn [initial-content path & {:keys [header-label size-label]}]
      [:div.editor-form__component-wrapper
       {:draggable true
        :on-drag-start (on-drag-start path)
        :on-drag-over prevent-default
        :class @animation-effect}
       [text-header header-label path]
       [:div.editor-form__text-field-wrapper
        [:header.editor-form__component-item-header "Kysymys"]
        (doall
          (for [lang @languages]
            ^{:key lang}
            [:input.editor-form__text-field {:value     (get-in @value [:label lang])
                                             :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]
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

(def ^:private toolbar-elements
  {"Lomakeosio"   component/form-section
   "Tekstikenttä" component/text-field
   "Tekstialue"   component/text-area})

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
                   (let [source-path (-> event .-dataTransfer (.getData "path") util/str->cljs)]
                     (dispatch [:editor/move-component source-path path])))
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
        animation-effect (reaction (case @(subscribe [:state-query [:editor :forms-meta path]])
                                     :fade-out "animated fadeOutUp"
                                     :fade-in  "animated fadeInUp"
                                     nil))]
    (fn [content path children]
      [:div.editor-form__section_wrapper
       {:class @animation-effect}
       [:div.editor-form__component-wrapper
        {:draggable true
         :on-drag-start (on-drag-start path)
         :on-drag-over prevent-default}
        [text-header "Lomakeosio" path :form-section? true]
        [:div.editor-form__text-field-wrapper.editor-form__text-field--section
         [:header.editor-form__component-item-header "Osion nimi"]
         (doall
           (for [lang @languages]
             ^{:key lang}
             [:input.editor-form__text-field
              {:value     (get-in @value [:label lang])
               :on-change #(dispatch [:editor/set-component-value (-> % .-target .-value) path :label lang])}]))]]
       children
       [drag-n-drop-spacer (conj path :children (count children))]
       [add-component (conj path :children (count children))]])))
