(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [lomake-editori.handlers]
            [lomake-editori.subs]
            [lomake-editori.editor.handlers]
            [taoensso.timbre :refer-macros [spy info]]))

(enable-console-print!)

(defn mount-root []
  (reagent/render [:div "lomake placeholder"]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (mount-root))
