(ns ataru.virkailija.editor.components.info-component
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [ataru.cljs-util :as util]
            [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.input-field-component :as input-field-component]
            [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]))

(defn info-element
  "Info text which is a standalone component"
  [_ path]
  (let [languages        (subscribe [:editor/languages])
        collapse-checked (subscribe [:editor/get-component-value path :params :info-text-collapse])
        show-identified-checked (subscribe [:editor/get-component-value path :params :show-for-identified])
        sub-header       (subscribe [:editor/get-component-value path :label])
        component-locked?     (subscribe [:editor/component-locked? path])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) @(subscribe [:editor/virkailija-translation :info-element]) path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content/component-content
        path
        [:div
         [:div.editor-form__component-row-wrapper
          [:div.editor-form__text-field-wrapper
           [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :title])]
           (input-fields-with-lang-component/input-fields-with-lang
             (fn [lang]
               [input-field-component/input-field {:path        path
                                                   :lang        lang
                                                   :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                 (-> % .-target .-value)
                                                                                 path :label lang])}])
             @languages
             :header? true)
           [:div.infoelement
            [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :text])]
            (->> (input-fields-with-lang-component/input-fields-with-lang
                   (fn [lang]
                     [input-field-component/input-field {:path        path
                                                         :lang        lang
                                                         :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                                       (-> % .-target .-value)
                                                                                       path :text lang])
                                                         :value-fn    (fn [component] (get-in component [:text lang]))
                                                         :tag         :textarea}])
                   @languages
                   :header? true)
                 (map (fn [field]
                        (into field [[:div.editor-form__markdown-anchor
                                      (markdown-help-component/markdown-help)]])))
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
          [:div.editor-form__checkbox-wrapper
           (let [show-identified-id (util/new-uuid)]
             [:div.editor-form__checkbox-container
              [:input.editor-form__checkbox {:type      "checkbox"
                                             :id        show-identified-id
                                             :checked   (boolean @show-identified-checked)
                                             :disabled  @component-locked?
                                             :on-change (fn [event]
                                                          (dispatch [:editor/set-component-value
                                                                     (-> event .-target .-checked)
                                                                     path :params :show-for-identified]))}]
              [:label.editor-form__checkbox-label
               {:for   show-identified-id
                :class (when @component-locked? "editor-form__checkbox-label--disabled")}
               @(subscribe [:editor/virkailija-translation :show-for-identified-info-text])]])]
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]]])))