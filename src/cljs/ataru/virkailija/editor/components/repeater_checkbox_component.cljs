(ns ataru.virkailija.editor.components.repeater-checkbox-component
  (:require [ataru.cljs-util :as util]
            [re-frame.core :refer [subscribe dispatch]]))

(defn repeater-checkbox
  [path initial-content]
  (let [id                (util/new-uuid)
        checked?          (-> initial-content :params :repeatable boolean)
        has-options?      (not (empty? (:options initial-content)))
        component-locked? @(subscribe [:editor/component-locked? path])
        disabled?         (or has-options? component-locked?)]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   checked?
                                    :disabled  disabled?
                                    :data-test-id "tekstikenttä-valinta-voi-lisätä-useita"
                                    :on-change (fn [event]
                                                 (when (not disabled?)
                                                   (dispatch [:editor/set-component-value (-> event .-target .-checked) path :params :repeatable])))}]
     [:label.editor-form__checkbox-label
      {:for   id
       :class (when disabled? "editor-form__checkbox-label--disabled")}
      @(subscribe [:editor/virkailija-translation :multiple-answers])]]))
