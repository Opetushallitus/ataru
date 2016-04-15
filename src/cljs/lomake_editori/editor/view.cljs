(ns lomake-editori.editor.view
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [lomake-editori.dev.lomake :as l]))

(defn form-list []
  (let [forms (subscribe [:state-query [:editor :forms]])]
    (fn []
      (into [:div.editor-form__list]
        (mapv (fn [form] [:div.editor-form__row (:name form)]) @forms)))))

(defn editor-panel []
  (fn []
    [component/form-component
     (merge l/controller
            l/translations
            (l/field l/text-field)
            {:lang  :sv
             :value "Valmis arvo"})]))

(defn editor []
    [:div
     [form-list]
     [editor-panel]])
