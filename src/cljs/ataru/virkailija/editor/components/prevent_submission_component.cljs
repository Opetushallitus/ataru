(ns ataru.virkailija.editor.components.prevent-submission-component
  (:require [re-frame.core :refer [subscribe dispatch]]
            [ataru.cljs-util :as util]))

(def prevent-submission-key :prevent-submission)

(defn- parent-field-path
  [option-path]
  (pop (pop option-path)))

(defn- on-change-handler
  [path]
  (let [parent-path (parent-field-path path)]
    (fn [event]
      (let [checked (boolean (-> event .-target .-checked))
            event (if checked
                    :editor/add-invalid-value-validator
                    :editor/remove-invalid-value-validator)
            option-value @(subscribe [:editor/get-component-value path :value])]
        (dispatch [event option-value parent-path])))))

(defn prevent-submission-option
  [path]
  (let [id      (util/new-uuid)
        checked (subscribe [:editor/invalid-option-validator-present? path])]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   @checked
                                    :on-change (on-change-handler path)}]
     [:label.editor-form__checkbox-label
      {:for id}
      @(subscribe [:editor/virkailija-translation prevent-submission-key])]]))
