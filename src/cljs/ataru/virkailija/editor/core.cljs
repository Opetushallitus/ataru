(ns ataru.virkailija.editor.core
  (:require [ataru.virkailija.dev.lomake :as l]
            [ataru.virkailija.editor.component :as ec]
            [ataru.virkailija.editor.components.toolbar :as toolbar]
            [ataru.virkailija.editor.components.followup-question :as followup]
            [re-frame.core :refer [subscribe dispatch dispatch-sync reg-sub-raw reg-sub]]
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

(defn soresu->reagent [{:keys [children] :as content} path]
  (fn [{:keys [children] :as content} path]
    [:div
     (when-not ((set path) :followup)
       [ec/drag-n-drop-spacer path content])

     (match content
            {:module module}
            [ec/module path]

            {:fieldClass "wrapperElement"
             :children   children}
            (let [children (for [[index child] (zipmap (range) children)]
                             ^{:key index}
                             [soresu->reagent child (conj path :children index)])]
              [ec/component-group content path children])

            {:fieldClass "formField" :fieldType "textField"}
            [ec/text-field content path]

            {:fieldClass "formField" :fieldType "textArea"}
            [ec/text-area content path]

            {:fieldClass "formField"
             :fieldType "dropdown"
             :options (options :guard followup/followups?)}
            [ec/dropdown content path soresu->reagent]

            {:fieldClass "formField" :fieldType "dropdown"}
            [ec/dropdown content path]

            {:fieldClass "formField" :fieldType "multipleChoice"}
            [ec/dropdown content path]

            :else (do
                    (error content)
                    (throw "error" content)))]))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (:content @form))]
    (fn []
      (-> (into [:section.editor-form]
            (for [[index json-blob] (zipmap (range) @content)
                  :when             (not-empty @content)]
              [soresu->reagent json-blob [index]]))
        (conj [ec/drag-n-drop-spacer [(count @content)]])
        (conj [toolbar/add-component (count @content)])))))

