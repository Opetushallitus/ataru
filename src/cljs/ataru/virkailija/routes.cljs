(ns ataru.virkailija.routes
    (:require-macros [secretary.core :refer [defroute]])
    (:import goog.History)
    (:require [ataru.cljs-util :refer [dispatch-after-state]]
              [secretary.core :as secretary]
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
    (secretary/dispatch! "/editor"))

  (defroute "/editor" []
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/select-form nil])
    (dispatch [:editor/refresh-forms]))

  (defroute #"/editor/([a-f0-9-]{36})" [key]
    (dispatch [:set-active-panel :editor])
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
      :predicate
      (fn [db]
        (not-empty (get-in db [:editor :forms key])))
      :handler
      (fn [form]
        (dispatch [:editor/select-form (:key form)]))))

  (defroute #"/applications/" []
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
      :predicate
      (fn [db] (not-empty (get-in db [:editor :forms])))
      :handler
      (fn [forms]
        (let [form (-> forms first second)]
          (.replaceState js/history nil nil (str "#/applications/" (:key form)))
          (dispatch [:editor/select-form (:key form)])
          (dispatch [:application/fetch-applications (:id form)])
          (dispatch [:set-active-panel :application])))))

  (defroute #"/applications/:key" [key]
    (dispatch [:editor/refresh-forms])
    (dispatch-after-state
      :predicate
      (fn [db] (not-empty (get-in db [:editor :forms key])))
      :handler
      (fn [form]
        (dispatch [:editor/select-form (:key form)])
        (dispatch [:application/fetch-applications (:id form)])))
    (dispatch [:set-active-panel :application]))

  ;; --------------------
  (hook-browser-navigation!))
