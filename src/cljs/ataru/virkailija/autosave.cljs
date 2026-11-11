(ns ataru.virkailija.autosave
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.db :refer [app-db]]
            [ataru.cljs-util :refer [debounce]]
            [cljs.core.async :as a :refer [chan <! >! close! timeout sliding-buffer]]
            [taoensso.timbre :as log]))

(defn stop-autosave! [stop-fn]
  (when stop-fn
    (stop-fn)))

(defn interval-loop [{:keys [interval-ms
                             debounce-ms
                             subscribe-path
                             handler
                             ; extra predicate for changing whether a change should be
                             ; propagated to the db
                             changed-predicate
                             last-autosaved-form]
                      :or   {interval-ms       2000
                             debounce-ms       1000
                             changed-predicate not=}}]
  {:pre [(integer? interval-ms)
         (integer? debounce-ms)
         (vector? subscribe-path)]}
  (let [read-value        #(get-in @app-db subscribe-path)
        watch-key         (gensym "autosave")
        previous          (atom (or last-autosaved-form (read-value)))
        ; last-saved seuraa mitä oikeasti viimeksi tallennettiin, jotta silmukasta
        ; poistuttaessa ei lähetetä jo tallennettua arvoa uudelleen
        last-saved        (atom (or last-autosaved-form (read-value)))
        change            (chan (sliding-buffer 1))
        watch             (fn [_ _ old-db new-db]
                            (let [old (get-in old-db subscribe-path)
                                  new (get-in new-db subscribe-path)]
                              (when (not= old new)
                                (reset! previous old)
                                (go (>! change [old new])))))
        stop?             (atom false)
        save!             (fn [current prev]
                            (reset! last-saved current)
                            (handler current prev))
        bounce            (debounce save! debounce-ms)
        ; stop-fn peruu vielä laukeamattoman debouncetun tallennuksen, jotta se ei
        ; laukea myöhemmin ajastimen umpeuduttua ja lähetä samaa arvoa uudelleen sen
        ; jälkeen kun silmukasta on jo poistuttu ja mahdollinen viimeinen tila flushattu
        stop-fn           (fn []
                            (reset! stop? true)
                            (remove-watch app-db watch-key)
                            ((:cancel (meta bounce)))
                            (close! change)
                            true)
        when-changed-save (fn [save-fn current prev]
                            (when (changed-predicate current prev)
                              (save-fn current prev)))]
    (add-watch app-db watch-key watch)

    (go-loop []
      (<! (timeout 500))
      (if (and (read-value)
               (not @stop?))
        (recur)
        (do (remove-watch app-db watch-key)
            (log/info "Stopping autosave at" subscribe-path))))

    (go-loop []
      (when-let [[prev current] (<! change)]
        (when-changed-save bounce current prev)
        (recur))

      (when-let [current (read-value)]
        (when-changed-save save! current @last-saved)))

    (when (some? last-autosaved-form)
      (go (>! change [last-autosaved-form (read-value)])))

    stop-fn))
