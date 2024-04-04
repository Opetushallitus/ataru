(ns dfreeman.re-frame
  (:require [dfreeman.re-frame.db-arg-in-reg-event-fx :as db-arg-in-reg-event-fx]
            [dfreeman.re-frame.sub-in-event-handler :as sub-in-event-handler]))

(defn reg-event-db
  [{:keys [node] :as s}]
  (sub-in-event-handler/hook s)
  node)

(defn reg-event-*
  [{:keys [node] :as s}]
  (db-arg-in-reg-event-fx/hook s)
  (sub-in-event-handler/hook s)
  node)
