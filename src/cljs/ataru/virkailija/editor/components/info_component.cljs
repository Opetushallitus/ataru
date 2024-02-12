(ns ataru.virkailija.editor.components.info-component
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [ataru.cljs-util :as util]
            [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.input-field-component :as input-field-component]
            [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]))

(defn- info-checkbox
  [path component-locked? param translation-key]
  (let [checked? (subscribe [:editor/get-component-value path :params param])]
    [:div.editor-form__checkbox-wrapper
     (let [input-id (util/new-uuid)]
       [:div.editor-form__checkbox-container
        [:input.editor-form__checkbox {:type      "checkbox"
                                       :id        input-id
                                       :checked   (boolean @checked?)
                                       :disabled  component-locked?
                                       :on-change (fn [event]
                                                    (dispatch [:editor/set-component-value
                                                               (-> event .-target .-checked)
                                                               path :params param]))}]
        [:label.editor-form__checkbox-label
         {:for   input-id
          :class (when component-locked? "editor-form__checkbox-label--disabled")}
         @(subscribe [:editor/virkailija-translation translation-key])]])])
  )

(defn info-element
  "Info text which is a standalone component"
  [_ path]
  (let [languages        (subscribe [:editor/languages])
        sub-header       (subscribe [:editor/get-component-value path :label])
        component-locked?     (subscribe [:editor/component-locked? path])]
    (fn [initial-content path]
      (let [applying-as-identified-enabled? (subscribe [:editor/allow-hakeminen-tunnistautuneena?])]
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
                                                           :data-test-id "info-input-field"
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
            [info-checkbox path @component-locked? :info-text-collapse :collapse-info-text]
            [info-checkbox
             path
             (or @component-locked? (not @applying-as-identified-enabled?))
             :show-only-for-identified
             :show-for-identified-info-text]
            [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]]]))))