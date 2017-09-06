(ns ataru.virkailija.editor.core
  (:require [ataru.feature-config :as fc]
            [ataru.virkailija.dev.lomake :as l]
            [ataru.virkailija.editor.component :as ec]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [ataru.virkailija.editor.components.followup-question :as followup]
            [ataru.util :as util]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [cljs-uuid-utils.core :as uuid]
            [taoensso.timbre :refer-macros [spy debug error]]))

(reg-sub
  :editor/get-component-value
  (fn [db [_ & path]]
    (get-in db
      (flatten
        (concat
          [:editor :forms (-> db :editor :selected-form-key) :content]
          path)))))

(defonce attachments-enabled? (fc/feature-enabled? :attachment))

(defn soresu->reagent [content path]
  (let [render-children (fn [children & [new-path]]
                          (for [[index child] (map vector (range) children)]
                            ^{:key index}
                            [soresu->reagent child (conj (vec path) :children index)]))]
    (fn [content path]
      [:div
       (when-not ((set path) :followups)
         [ec/drag-n-drop-spacer path content])

       (match content
              {:module module}
              [ec/module path]

              {:fieldClass "wrapperElement"
               :fieldType  "adjacentfieldset"
               :children   children}
              [ec/adjacent-fieldset content path (render-children children)]

              {:fieldClass "wrapperElement"
               :children   children}
              [ec/component-group content path (render-children children path)]

              {:fieldClass "questionGroup"
               :fieldType  "fieldset"
               :children   children}
              [ec/component-group content path (render-children children path)]

              {:fieldClass "formField" :fieldType "textField"
               :params     {:adjacent true}}
              [ec/adjacent-text-field content path]

              {:fieldClass "formField" :fieldType "textField"}
              [ec/text-field content path]

              {:fieldClass "formField" :fieldType "textArea"}
              [ec/text-area content path]

              {:fieldClass "formField" :fieldType "dropdown"}
              [ec/dropdown content path]

              {:fieldClass "formField" :fieldType "multipleChoice"}
              [ec/dropdown content path]

              {:fieldClass "infoElement"}
              [ec/info-element content path]

              {:fieldClass "formField"
               :fieldType  "singleChoice"}
              [ec/dropdown content path]

              {:fieldClass "formField"
               :fieldType  "attachment"}
              (if attachments-enabled?
                [ec/attachment content path]
                [:div])

              {:fieldClass "formField"
               :fieldType  "hakukohteet"}
              nil

              :else (do
                      (error content)
                      (throw "error" content)))])))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (:content @form))]
    (fn []
      (-> (into [:section.editor-form]
            (for [[index json-blob] (map vector (range) @content)
                  :when             (not-empty @content)]
              [soresu->reagent json-blob [index]]))
        (conj [ec/drag-n-drop-spacer [(count @content)]])
        (conj [toolbar/add-component (count @content)])))))

