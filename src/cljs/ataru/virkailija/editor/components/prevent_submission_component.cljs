(ns ataru.virkailija.editor.components.prevent-submission-component
  (:require [re-frame.core :refer [subscribe dispatch]]
            [ataru.cljs-util :as util]))

(def prevent-submission-key :prevent-submission)

(defn prevent-submission-option
  [path]
  (let [id      (util/new-uuid)
        checked (subscribe [:editor/get-prevent-submission path])]
    [:div.editor-form__checkbox-container
     [:input.editor-form__checkbox {:type      "checkbox"
                                    :id        id
                                    :checked   @checked
                                    :on-change (fn [event]
                                                 (dispatch [:editor/set-prevent-submission path (-> event .-target .-checked)]))}]
     [:label.editor-form__checkbox-label
      {:for id}
      @(subscribe [:editor/virkailija-translation prevent-submission-key])]]))
