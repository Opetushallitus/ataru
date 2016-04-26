(ns lomake-editori.autosave
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [subscribe]]
            [cljs.core.match :refer-macros [match]]
            [cljs.core.async :as a :refer  [chan <! >! close! alts! timeout sliding-buffer]]
            [taoensso.timbre :refer-macros [spy info debug]]))

(defn stop-autosave! [stop-fn]
  (when stop-fn
    (stop-fn)))

(defn save-and-close-autosave! [interval-ch]
  (go
    (>! interval-ch true)
    (close! interval-ch)))

(defn interval-loop [interval-ms subscribe-path handler & [initial]]
  {:pre [(integer? interval-ms)
         (vector? subscribe-path)]}
  (let [interval-ch    (chan)
        previous       (atom initial)
        value-to-watch (subscribe [:state-query subscribe-path])
        stop-fn        (fn [& [force?]]
                         (if force?
                           (go (>! interval-ch true))
                           (save-and-close-autosave! interval-ch))
                         true)]
    (do
      (go-loop []
        (match [(alts! [interval-ch (timeout interval-ms)]) @value-to-watch @previous]
               ; force handler with true sent to interval-ch
               [[true _] current prev]
               (do
                 (when-not (= current prev)
                   (handler current prev))
                 (recur))

               [_ nil _]
               (info "Stopping autosave. Could not find current value for autosave at" subscribe-path)

               ; interval-ch is closed, loop should stop
               [[nil interval-ch] _ _]
               (debug "Channel closed, stopping loop" subscribe-path)

               [[alt-result _] current prev]
               (do
                 (when-not (= current prev)
                   (do
                     (reset! previous current)
                     (handler current prev)))
                 (recur))))
      stop-fn)))
