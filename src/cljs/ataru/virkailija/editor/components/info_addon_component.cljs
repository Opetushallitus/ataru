(ns ataru.virkailija.editor.components.info-addon-component
  (:require
    [ataru.cljs-util :as util]
    [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn- markdown-help []
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

(defn- input-field [path lang dispatch-fn {:keys [class value-fn tag placeholder]
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
          (->> (input-fields-with-lang-component/input-fields-with-lang
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

