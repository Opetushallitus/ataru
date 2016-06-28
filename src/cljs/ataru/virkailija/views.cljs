(ns ataru.virkailija.views
    (:require [re-frame.core :as re-frame]
              [reagent.core :as r]
              [ataru.virkailija.views.banner :refer [top-banner]]
              [ataru.virkailija.application.view :refer [application]]
              [ataru.virkailija.dev.lomake :as l]
              [ataru.virkailija.editor.view :refer [editor]]
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
