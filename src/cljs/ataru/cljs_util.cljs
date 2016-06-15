(ns ataru.cljs-util
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [cljs.core.match :refer-macros [match]]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]))

(defn wrap-debug [f]
  (fn [& args]
    (debug "Wrapped Debug " args)
    (apply f args)))

(defn debounce
  ([f] (debounce f 1000))
  ([f timeout]
   (let [id (atom nil)]
     (fn [& args]
       (if (not (nil? @id))
         (js/clearTimeout @id))
       (reset! id (js/setTimeout
                    (apply partial f args)
                    timeout))))))

(defn debounced-ratom
  ([ratom] (debounced-ratom 1000 ratom))
  ([debounce-ms ratom]
   (let [value (reaction nil)
         on-bounce (debounce #(reset! value %) debounce-ms)
         watch (fn [_ _ old-value new-value]
                 (on-bounce new-value))]
     (do
       (-add-watch ratom :debounce-ratom watch)
       @ratom ; needed to cause initial -add-watch to trigger
       value))))

(defn debounce-subscribe
  ([path] (debounce-subscribe 1000 path))
  ([debounce-ms path]
   {:pre [(vector? path)]}
   (debounced-ratom debounce-ms (subscribe path))))

(defn dispatch-after-state
  [& {:keys [predicate handler]}]
  {:pre [(not (nil? predicate))
         (not (nil? handler))]}
  (let [handler-ref (atom nil)
        sanity-count (atom 0)
        dispatcher (fn [db]
                     (match [(swap! sanity-count inc) (predicate db)]
                            [10 _] (js/clearInterval @handler-ref)
                            [_ (result :guard (comp true? boolean))]
                            (do
                              (js/clearInterval @handler-ref)
                              (handler result))
                            :else nil))]
    (reset!
      handler-ref
      (js/setInterval
        #(dispatch [:state-update dispatcher])
        200))))


(defn set-global-error-handler!
  "Sets the global error handler. Prints stack trace of uncaught
   error"
  [send-to-server-fn]
  (set! (.-onerror js/window)
        (fn [error-msg url line col error-obj]
          (let [user-agent (-> js/window .-navigator .-userAgent)
                stack-string (.-stack error-obj)
                error-details {:error-message error-msg
                               :url url
                               :line line
                               :col col
                               :user-agent user-agent
                               :stack stack-string}]
            (send-to-server-fn error-details)
            (when js/console
              (.log js/console error-msg)
              (.log js/console url)
              (.log js/console (str "line: " line " col: " col))
              (.log js/console (.-stack error-obj)))))))

(defn cljs->str
  [data]
  (->> data clj->js (.stringify js/JSON)))

(defn str->cljs
  [str]
  (->> str (.parse js/JSON) js->clj))

