(ns lomake-editori.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [lomake-editori.handlers]
              [lomake-editori.subs]
              [lomake-editori.routes :as routes]
              [lomake-editori.views :as views]
              [lomake-editori.config :as config]))

(when config/debug?
  (println "dev mode"))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
