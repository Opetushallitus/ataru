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

(defn interval-loop [{:keys [interval-ms
                             subscribe-path
                             handler
                             initial changed-predicate]
                      :or   {interval-ms 2000
                             changed-predicate not=}}]
  {:pre [(integer? interval-ms)
         (vector? subscribe-path)]}
  (let [interval-ch        (chan (sliding-buffer 1))
        previous           (atom initial)
        value-to-watch     (subscribe [:state-query subscribe-path])
        stop-fn            (fn [& [force?]]
                             (if force?
                               (go (>! interval-ch true))
                               (save-and-close-autosave! interval-ch))
                             true)
        when-changed-save! (fn [current prev]
                             (let [changed? (changed-predicate current prev)]
                               (when changed?
                                 (do
                                   (reset! previous current)
                                   (handler current prev)))))]
    (do
      (go-loop []
        (match [(alts! [interval-ch (timeout interval-ms)]) @value-to-watch @previous]
               [_ nil _]
               (info "Stopping autosave. Could not find current value for autosave at" subscribe-path)

               ; interval-ch is closed, loop should stop
               [[nil interval-ch] current prev]
               (do
                 (when-changed-save! current prev)
                 (debug "Channel closed, stopping loop" subscribe-path))

               ; matched when interval-ms timeouts or _anything_ is sent to interval-ch
               [_ current prev]
               (do
                 (when-changed-save! current prev)
                 (recur))))
      stop-fn)))
