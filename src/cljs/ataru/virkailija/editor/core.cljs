(ns ataru.virkailija.editor.core
  (:require [ataru.virkailija.dev.lomake :as l]
            [ataru.virkailija.editor.component :as ec]
            [re-frame.core :refer [subscribe dispatch dispatch-sync register-handler register-sub]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [cljs.core.match :refer-macros [match]]
            [cljs-uuid-utils.core :as uuid]
            [taoensso.timbre :refer-macros [spy debug error]]))

(register-sub
  :editor/get-component-value
  (fn [db [_ & path]]
    (reaction (get-in @db
                      (flatten (concat
                                 [:editor :forms (-> @db :editor :selected-form-id) :content]
                                 path))))))

(register-sub
  :editor/languages
  (fn [db]
    (reaction [:fi])))

(defn soresu->reagent [{:keys [children] :as content} path]
  (match [content]
         [{:fieldClass "wrapperElement"
           :children   children}]
         (let [children (for [[index child] (zipmap (range) children)]
                          ^{:key index}
                          [soresu->reagent child (conj path :children index)])]
           [ec/component-group path children])

         [{:fieldClass "formField" :fieldType "textField"}]
         [ec/text-field content path]

         [{:fieldClass "formField" :fieldType "textArea"}]
         [ec/text-area content path]

         :else (do
                 (error content)
                 (throw "error" content))))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (:content @form))]
    (fn []
      [:section.editor-form
       (conj
         (into [:form]
           (for [[index json-blob] (zipmap (range) @content)
                 :when             (not-empty @content)]
             [soresu->reagent json-blob [index]]))
         [ec/add-component (count @content)])])))

