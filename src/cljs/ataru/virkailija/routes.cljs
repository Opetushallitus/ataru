(ns ataru.virkailija.routes
    (:require-macros [secretary.core :refer [defroute]])
    (:import goog.History)
    (:require [secretary.core :as secretary]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [re-frame.core :refer [dispatch]]))

(defonce history (History.))

(defn hook-browser-navigation! []
  (doto history
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

(defn set-history!
  [route]
  (.setToken history route))

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

  (defroute #"/editor/(\d+)" [id]
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms])
    (dispatch [:editor/select-form (js/parseInt id 10)]))

  (defroute "/application" []
    (dispatch [:set-active-panel :application]))

  ;; --------------------
  (hook-browser-navigation!))
