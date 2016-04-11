(ns lomake-editori.views.clerk-panel
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [re-com.core :as c]
            [lomake-editori.soresu.form      :as f]
            [lomake-editori.soresu.component :as component]
            [lomake-editori.soresu.components :as components]
            [taoensso.timbre :refer-macros [spy]]
            [dev.cljs.lomake :as l]))

(def forms [:div "forms"])

(defn clerk-panel []
  (fn []
    [:div "virkailija"]))
