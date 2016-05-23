(ns lomake-editori.editor.core
  (:require [lomake-editori.dev.lomake :as l]
            [lomake-editori.editor.component :as ec]
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
                                 [:editor :forms (-> @db :editor :selected-form :id) :content]
                                 path))))))

(register-handler
  :editor/set-component-value
  (fn [db [_ value & path]]
    (assoc-in db
              (flatten (concat [:editor :forms (-> db :editor :selected-form :id) :content]
                               path))
              value)))

(register-sub
  :editor/languages
  (fn [db]
    (reaction [:fi])))

(defn soresu->reagent [{:keys [children] :as content} path]
  (fn [{:keys [children] :as content} path]
     (match [content]
       [{:fieldClass "wrapperElement"
         :children   children}]
       (let [wrapper-element (->> (for [[index child] (zipmap (range) children)]
                                    [soresu->reagent child (conj path :children index)])
                                  (into [:div.editor-form__section_wrapper (when-let [n (-> content :label)]
                                                                             [:h1 n])]))]
         (conj wrapper-element [ec/add-component (conj path :children (count (:children content)))]))

       [{:fieldClass "formField"}]
       [ec/text-field content path]

       [{:fieldClass "infoElement"
         :fieldType  "link"}]
       [ec/link-info content path]

       [{:fieldClass "infoElement"}]
       [ec/info content path]

       :else (do
               (error content)
               (throw "error" content)))))

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
