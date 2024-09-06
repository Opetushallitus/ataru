(ns ataru.virkailija.editor.core
  (:require [ataru.virkailija.editor.component :as ec]
            [ataru.virkailija.editor.components.drag-n-drop-spacer :as dnd]
            [ataru.virkailija.editor.components.dropdown-component :as dc]
            [ataru.virkailija.editor.components.modal-info-element :as mie]
            [ataru.virkailija.editor.components.info-component :as ic]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [taoensso.timbre :as log]))

(defn soresu->reagent [_ _ & _]
  (fn [content path & args]
    (let [children  (map-indexed
                     (fn [index child]
                       ^{:key index}
                       [soresu->reagent
                        child
                        (conj (vec path) :children index)
                        :question-group-element? (= (:fieldClass content)
                                                    "questionGroup")])
                     (:children content))
          followups (map-indexed
                     (fn [option-index option]
                       (map-indexed
                        (fn [followup-index followup]
                          ^{:key (str "followup-" option-index "-" followup-index)}
                          [soresu->reagent
                           followup
                           (vec (concat path [:options option-index :followups followup-index]))])
                        (:followups option)))
                     (:options content))]
      (when-let [component
                 (match content
                   {:module _}
                   [ec/module content path]

                   {:fieldClass "wrapperElement"
                    :fieldType  "adjacentfieldset"}
                   [ec/adjacent-fieldset content path children]

                   {:fieldClass "wrapperElement"}
                   [ec/component-group content path children]

                   {:fieldClass "questionGroup"
                    :fieldType  "fieldset"}
                   [ec/component-group content path children]

                   {:fieldClass "formField" :fieldType "textField"
                    :params     {:adjacent true}}
                   [ec/adjacent-text-field content path]

                   {:fieldClass "formField" :fieldType "textField"}
                   [ec/text-field content followups path]

                   {:fieldClass "formField" :fieldType "textArea"}
                   [ec/text-area content followups path]

                   {:fieldClass "formField" :fieldType "dropdown"}
                   [dc/dropdown content followups path args]

                   {:fieldClass "formField" :fieldType "multipleChoice"}
                   [dc/dropdown content followups path args]

                   {:fieldClass "pohjakoulutusristiriita"
                    :fieldType  "pohjakoulutusristiriita"}
                   [ec/pohjakoulutusristiriita content path]

                   {:fieldClass "infoElement"}
                   [ic/info-element content path]

                   {:fieldClass "modalInfoElement"}
                   [mie/modal-info-element content path]

                   {:fieldClass "formField"
                    :fieldType  "singleChoice"}
                   [dc/dropdown content followups path args]

                   {:fieldClass "formField"
                    :fieldType  "attachment"}
                   [ec/attachment content path]

                   {:fieldClass "formField"
                    :fieldType  "hakukohteet"}
                   [ec/hakukohteet-module content path]

                   {:fieldClass "tutkinnot"
                    :fieldType  "tutkinnot"}
                   [ec/tutkinnot content path]

                   :else (do
                           (log/error content)
                           (throw (new js/Error (str "Unknown component type " content)))))]
        [:div
         [dnd/drag-n-drop-spacer path]
         component]))))

(defn editor []
  (let [content (:content @(subscribe [:editor/selected-form]))]
    [:section.editor-form
     (doall
       (map-indexed (fn [index element]
                      ^{:key index}
                      [soresu->reagent element [index]])
                    content))
     [dnd/drag-n-drop-spacer [(count content)]]
     [toolbar/add-component (count content) true]]))

