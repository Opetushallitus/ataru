(ns lomake-editori.routes
    (:require-macros [secretary.core :refer [defroute]])
    (:import goog.History)
    (:require [secretary.core :as secretary]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [re-frame.core :refer [dispatch]]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn set-history!
  [route]
  (.setToken (History.) route))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (secretary/dispatch! "/editor")
    (set-history! "/editor"))

  (defroute "/editor" []
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms]))

  (defroute "/application" []
    (dispatch [:set-active-panel :application]))

  ;; --------------------
  (hook-browser-navigation!))
