(ns ataru.virkailija.autosave
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :refer [subscribe]]
            [ataru.cljs-util :refer [debounce]]
            [cljs.core.async :as a :refer [chan <! >! close! timeout sliding-buffer]]
            [taoensso.timbre :as log]))

(defn stop-autosave! [stop-fn]
  (when stop-fn
    (stop-fn)))

(defn interval-loop [{:keys [interval-ms
                             subscribe-path
                             handler
                             ; extra predicate for changing whether a change should be
                             ; propagated to the db
                             changed-predicate
                             last-autosaved-form]
                      :or   {interval-ms       2000
                             changed-predicate not=}}]
  {:pre [(integer? interval-ms)
         (vector? subscribe-path)]}
  (let [value-to-watch    (subscribe [:state-query subscribe-path])
        previous          (atom (or last-autosaved-form @value-to-watch))
        change            (chan (sliding-buffer 1))
        watch             (fn [_ _ old new]
                            (reset! previous old)
                            (go (>! change [old new])))
        stop?             (atom false)
        stop-fn           (fn []
                            (reset! stop? true)
                            (close! change)
                            true)
        bounce            (debounce handler)
        when-changed-save (fn [save-fn current prev]
                            (when (changed-predicate current prev)
                              (save-fn current prev)))]
    (-add-watch value-to-watch :autosave watch)

    (go-loop []
      (<! (timeout 500))
      (if (and @value-to-watch
               (not @stop?))
        (recur)
        (log/info "Stopping autosave at" subscribe-path)))

    (go-loop []
      (when-let [[prev current] (<! change)]
        (when-changed-save bounce current prev)
        (recur))

      ; save right before exiting loop
      (when-let [current @value-to-watch]
        (when-changed-save handler current @previous)))

    (when (some? last-autosaved-form)
      (go (>! change [last-autosaved-form @value-to-watch])))

    stop-fn))
