(ns ataru.hakija.core
  (:require [reagent.core :as reagent]
            [lomake-editori.handlers]
            [lomake-editori.subs]
            [lomake-editori.editor.handlers]
            [taoensso.timbre :refer-macros [spy info]]
            [ataru.hakija.form-view :refer [form-view]]))

(enable-console-print!)

(defn mount-root []
  (reagent/render [form-view]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (mount-root))
