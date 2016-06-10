(ns ataru.virkailija.editor.component
  (:require [ataru.virkailija.editor.component-macros :refer-macros [animation-did-end-handler
                                                                     component-with-fade-effects]]
            [ataru.virkailija.soresu.component :as component]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [subscribe dispatch]]))

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

(def ^:private events
  ["webkitAnimationEnd" "mozAnimationEnd" "MSAnimationEnd" "oanimationend" "animationend"])

(defn- text-header
  [label path & {:keys [form-section?]}]
  [:div.editor-form__header-wrapper
   [:header.editor-form__component-header label]
   [:a.editor-form__component-header-link
    {:on-click (fn [event]
                 (let [target     (if
                                    form-section?
                                    (-> event .-target .-parentNode .-parentNode .-parentNode)
                                    (-> event .-target .-parentNode .-parentNode))
                       handler-fn (animation-did-end-handler
                                    (dispatch [:remove-component path]))]
                   (doseq [event events]
                     (.addEventListener target event handler-fn)))
                 (dispatch [:hide-component path]))}
    "Poista"]])

(defn text-component [initial-content path & {:keys [header-label size-label]}]
  (let [languages        (subscribe [:editor/languages])
        value            (subscribe [:editor/get-component-value path])
        size             (subscribe [:editor/get-component-value path :params :size])
        radio-group-id   (str "form-size-" (gensym))
        radio-buttons    ["S" "M" "L"]
        radio-button-ids (reduce (fn [acc btn] (assoc acc btn (str radio-group-id "-" btn))) {} radio-buttons)
        size-change      (fn [new-size] (dispatch [:editor/set-component-value new-size path :params :size]))]
    (component-with-fade-effects [path]
      (fn [initial-content path & {:keys [header-label size-label]}]
        [:div.editor-form__component-wrapper
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
                      {:for btn-id
                       :class     (match btn-name
                                    "S" "editor-form__size-button--left-edge"
                                    "L" "editor-form__size-button--right-edge"
                                    :else nil)}
                      btn-name]]))]]
         [:div.editor-form__checkbox-wrapper
          (render-checkbox path initial-content :required)]]))))

(defn text-field [initial-content path]
  [text-component initial-content path :header-label "Tekstikenttä" :size-label "Tekstikentän koko"])

(defn text-area [initial-content path]
  [text-component initial-content path :header-label "Tekstialue" :size-label "Tekstialueen koko"])

(def ^:private toolbar-elements
  {"Lomakeosio"                component/form-section
   "Tekstikenttä"              component/text-field
   "Tekstialue"                component/text-area})

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

(defn component-group [content path children]
  (let [languages  (subscribe [:editor/languages])
        value      (subscribe [:editor/get-component-value path])]
    (component-with-fade-effects [path]
      (fn [content path children]
        [:div.editor-form__section_wrapper
         [:div.editor-form__component-wrapper
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
         [add-component (conj path :children (count children))]]))))
