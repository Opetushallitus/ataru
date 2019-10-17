(ns ataru.hakija.hakija-ajax
  (:require [ajax.core :as ajax]
            [re-frame.core :as re-frame]
            [ataru.application-common.fx :as fx]))

(defn http [args]
  (fx/http (aget js/config "hakija-caller-id")
           (cond-> args
                   (nil? (:error-handler args))
                   (assoc :error-handler [:application/default-handle-error]))))

(re-frame/reg-fx
  :http http)

(re-frame/reg-fx :http-abort ajax/abort)
