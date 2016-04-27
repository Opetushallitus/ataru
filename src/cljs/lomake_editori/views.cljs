(ns lomake-editori.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [lomake-editori.views.banner :refer [top-banner]]
              [lomake-editori.application.view :refer [application]]
              [lomake-editori.dev.lomake :as l]
              [lomake-editori.editor.view :refer [editor]]
              [lomake-editori.soresu.form      :as f]
              [lomake-editori.soresu.component :as component]
              [lomake-editori.soresu.components :as components]
              [taoensso.timbre :refer-macros [spy]]))

(defmulti panels identity)
(defmethod panels :application [] [application])
(defmethod panels :editor [] [editor])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [:div.main-container
       [top-banner]
        [:div (panels @active-panel)]])))
