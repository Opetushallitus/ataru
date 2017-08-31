(ns ataru.application-common.fx
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-fx :delayed-dispatch
  (fn [{:keys [dispatch-vec timeout]}]
    (js/setTimeout
      (fn []
        (re-frame/dispatch dispatch-vec))
      timeout)))

(defonce debounces (atom {}))

(re-frame/reg-fx
  :dispatch-debounced
  (fn [{:keys [id dispatch timeout]}]
    (js/clearTimeout (@debounces id))
    (swap! debounces assoc id (js/setTimeout
                                (fn []
                                  (re-frame/dispatch dispatch)
                                  (swap! debounces dissoc id))
                                timeout))))

(re-frame/reg-fx
  :set-page-title
  (fn [title]
    (aset js/document "title" title)))
