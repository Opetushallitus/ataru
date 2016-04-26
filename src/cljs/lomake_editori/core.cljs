(ns lomake-editori.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [lomake-editori.handlers]
              [lomake-editori.subs]
              [lomake-editori.routes :as routes]
              [lomake-editori.views :as views]
              [lomake-editori.config :as config]
              [lomake-editori.editor.handlers]
              [taoensso.timbre :refer-macros [spy info]]))

(when config/debug?
  (info "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch [:fetch-initial-data])
  (mount-root))
