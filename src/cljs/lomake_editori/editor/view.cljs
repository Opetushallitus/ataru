(ns lomake-editori.editor.view
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [lomake-editori.soresu.component :as component]
            [re-com.core :as re-com]
            [lomake-editori.dev.lomake :as l]))

(defn editor []
  (fn []
    [component/form-component
     (merge l/controller
            l/translations
            (l/field l/text-field)
            {:lang  :sv
             :value "Valmis arvo"})]))
