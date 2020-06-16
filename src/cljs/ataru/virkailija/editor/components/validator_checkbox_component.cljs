(ns ataru.virkailija.editor.components.validator-checkbox-component
  (:require [ataru.cljs-util :as util]
            [re-frame.core :refer [subscribe dispatch]]))

(defn validator-checkbox
  [path initial-content key disabled? on-change]
  (let [id         (util/new-uuid)
        disabled?  (or disabled?
                       @(subscribe [:editor/component-locked? path]))
        validators (-> initial-content :validators set)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   (contains? validators (name key))
                                    :disabled  disabled?
                                    :on-change (fn [event]
                                                 (let [checked (boolean (-> event .-target .-checked))]
                                                   (when on-change
                                                     (on-change checked))
                                                   (dispatch [(if checked
                                                                :editor/add-validator
                                                                :editor/remove-validator) (name key) path])))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when disabled? "editor-form__checkbox-label--disabled")}
      @(subscribe [:editor/virkailija-translation key])]]))
