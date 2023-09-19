(ns ataru.virkailija.editor.components.input-field-component
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reagent.dom :as r-dom]
            [reagent.ratom :refer-macros [reaction]]))

(defn- prevent-default
  [event]
  (.preventDefault event))

(defn input-field [{:keys [class
                           data-test-id
                           dispatch-fn
                           lang
                           path
                           placeholder
                           tag
                           value-fn]
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
                                (let [dom-node (r-dom/dom-node component)]
                                  (.focus dom-node))))
       :reagent-render      (fn [_ _ _ _]
                              [tag
                               {:class        (str "editor-form__text-field " (when-not (empty? class) class))
                                :value        @value
                                :placeholder  placeholder
                                :on-change    dispatch-fn
                                :on-drop      prevent-default
                                :disabled     @component-locked?
                                :data-test-id data-test-id}])})))
