(ns lomake-editori.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [re-com.core :as re-com]
              [lomake-editori.soresu.form      :as f]
              [lomake-editori.soresu.component :as component]
              [lomake-editori.soresu.components :as components]
              [taoensso.timbre :refer-macros [spy]]
              [dev.cljs.lomake :as l]))

;; home

(defn adapter [props children this]
  (.dir js/console component/form-component)
  [component/form-component props])

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

(defn link-to-about-page []
  [re-com/hyperlink-href
   :label "go to About Page"
   :href "#/about"])

(defn home-panel []
  [re-com/v-box
   :gap "1em"
   :children [[home-title]]])


;; about

(defn about-title []
  [re-com/title
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink-href
   :label "go to Home Page"
   :href "#/"])

(defn about-panel []
  [re-com/v-box
   :gap "1em"
   :children [[about-title] [link-to-home-page]]])


;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [re-com/v-box
       :height "100%"
       :children [(panels @active-panel)]])))
