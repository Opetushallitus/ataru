(ns ataru.virkailija.editor.components.info-addon-component
  (:require
    [ataru.cljs-util :as util]
    [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
    [ataru.virkailija.editor.components.input-field-component :as input-field-component]
    [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [reagent.ratom :refer-macros [reaction]]))

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
                   [input-field-component/input-field
                    {:path        (concat path [:params :info-text])
                     :lang        lang
                     :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                   (-> % .-target .-value)
                                                   path :params :info-text :label lang])
                     :tag         :textarea}])
                 @languages)
               (map (fn [field]
                      (into field [[:div.editor-form__info-addon-markdown-anchor (markdown-help-component/markdown-help)]])))
               (doall))])])))

