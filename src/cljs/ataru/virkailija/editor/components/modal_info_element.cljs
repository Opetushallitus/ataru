(ns ataru.virkailija.editor.components.modal-info-element
  (:require [ataru.virkailija.editor.components.input-fields-with-lang-component :as input-fields-with-lang-component]
            [ataru.virkailija.editor.components.input-field-component :as input-field-component]
            [ataru.virkailija.editor.components.belongs-to-hakukohteet-component :as belongs-to-hakukohteet-component]
            [ataru.virkailija.editor.components.text-header-component :as text-header-component]
            [ataru.virkailija.editor.components.component-content :as component-content]
            [ataru.virkailija.editor.components.markdown-help-component :as markdown-help-component]
            [re-frame.core :refer [dispatch-sync subscribe]]))

(defn- title-field
  [path languages]
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
     :header? true)])

(defn- text-field
  [path languages]
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
     doall)])

(defn- button-text-field
  [path languages]
  [:div.editor-form__text-field-wrapper
   [:header.editor-form__component-item-header @(subscribe [:editor/virkailija-translation :button-text])]
   (input-fields-with-lang-component/input-fields-with-lang
     (fn [lang]
       [input-field-component/input-field {:path        path
                                           :lang        lang
                                           :dispatch-fn #(dispatch-sync [:editor/set-component-value
                                                                         (-> % .-target .-value)
                                                                         path :button-text lang])
                                           :value-fn    (fn [component] (get-in component [:button-text lang]))}])
     @languages
     :header? true)])

(defn modal-info-element
  [_ path]
  (let [languages         (subscribe [:editor/languages])
        sub-header        (subscribe [:editor/get-component-value path :label])]
    (fn [initial-content path]
      [:div.editor-form__component-wrapper
       [text-header-component/text-header (:id initial-content) @(subscribe [:editor/virkailija-translation :modal-info-element]) path (:metadata initial-content)
        :sub-header @sub-header]
       [component-content/component-content
        path
        [:div
         [:div.editor-form__component-row-wrapper
          [title-field path languages]]
         [:div.editor-form__component-row-wrapper
          [text-field path languages]]
         [:div.editor-form__component-row-wrapper
          [button-text-field path languages]]
         [:div.editor-form__component-row-wrapper
          [belongs-to-hakukohteet-component/belongs-to-hakukohteet path initial-content]]]]])))
