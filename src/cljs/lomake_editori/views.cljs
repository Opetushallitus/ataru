(ns lomake-editori.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [re-com.core :as re-com]
              [lomake-editori.views.banner :refer [top-banner]]
              [lomake-editori.applications.view :refer [application]]
              [lomake-editori.editor.view :refer [editor]]
              [lomake-editori.soresu.form      :as f]
              [lomake-editori.soresu.component :as component]
              [lomake-editori.soresu.components :as components]
              [taoensso.timbre :refer-macros [spy]]
              [dev.cljs.lomake :as l]))

(defn home-title []
  (fn []
    [:div
     [re-com/title
      :label "Opintopolku.fi"
      :level :level1]

     [:div "KATO TÄTÄ"]

     [component/form-component
      (merge l/controller
             l/translations
             (l/field l/text-field)
             {:lang  :sv
              :value "foo"})]]))

(defmulti panels identity)
(defmethod panels :application [] [application])
(defmethod panels :editor [] [editor])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [re-com/v-box
       :height "100%"
       :children [top-banner
                  [re-com/box :child (panels @active-panel)]]])))
