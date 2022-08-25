(ns ataru.application-common.accessibility-util)

(defn is-enter-or-space?
  [event]
  (or (= 13 (.-keyCode event)) (= 32 (.-keyCode event))))