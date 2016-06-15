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

(defn undobox []
  [:div.editor-form__undo-box
   [:p]
   [:p
    [:span.editor-form__undo-box--gray "Sisältö poistettiin."]
    [:a.editor-form__undo-box--blue
     {:on-click #(dispatch [:editor/undo])} "Peruuta poisto?"]]
   [:i.zmdi.zmdi-close.editor-form__undo-box--link
    {:on-click #(dispatch [:editor/clear-undo])}]])

(defn undo [path]
  (let [path-with-last-element-incremented (conj (vec (butlast path))
                                                 (inc (last path)))
        form-meta                          (subscribe [:state-query [:editor :forms-meta path-with-last-element-incremented]])]
    (when (= :removed @form-meta)
      [undobox])))

(defn soresu->reagent [{:keys [children] :as content} path]
  (fn [{:keys [children] :as content} path]
    [:div
     [ec/drag-n-drop-spacer path content]

     (match [content]
            [{:fieldClass "wrapperElement"
              :children   children}]
            (let [children (for [[index child] (zipmap (range) children)]
                             ^{:key index}
                             [soresu->reagent child (conj path :children index)])]
              [ec/component-group content path children])

            [{:fieldClass "formField" :fieldType "textField"}]
            [ec/text-field content path]

            [{:fieldClass "formField" :fieldType "textArea"}]
            [ec/text-area content path]

            [{:fieldClass "formField" :fieldType "dropdown"}]
            [ec/dropdown content path]

            :else (do
                    (error content)
                    (throw "error" content)))

     [undo path]]))

(defn editor []
  (let [form    (subscribe [:editor/selected-form])
        content (reaction (:content @form))]
    (fn []
      [:section.editor-form
       (-> (into [:form]
             (for [[index json-blob] (zipmap (range) @content)
                   :when             (not-empty @content)]
               [soresu->reagent json-blob [index]]))
           (conj [ec/drag-n-drop-spacer [(count @content)]])
           (conj [ec/add-component (count @content)]))])))

