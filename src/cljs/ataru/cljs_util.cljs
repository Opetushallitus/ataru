(ns ataru.cljs-util
  (:require [clojure.string :refer [join]]
            [cljs.core.match :refer-macros [match]]
            [cljs.reader :as reader :refer [read-string]]
            [cljs-uuid-utils.core :as uuid]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [taoensso.timbre :refer-macros [spy debug]]
            [cemerick.url :as url]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [goog.string.format]
            [ataru.application.review-states :refer [application-review-states]])
  (:import [goog.net Cookies]))

(defn console-log [& args]
  "With pretty cljs-devtools formatting"
  (.apply (.-log js/console) js/console (clj->js args))
  args)

(defn wrap-debug [f]
  (fn [& args]
    (debug "Wrapped Debug " args)
    (apply f args)))

(def wrap-scroll-to
  (with-meta identity {:component-did-mount #(let [node (r/dom-node %)]
                                              (if (.-scrollIntoViewIfNeeded node)
                                                (.scrollIntoViewIfNeeded node)
                                                (.scrollIntoView node)))}))


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
   (let [value (r/atom nil)
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
                            [50 _] (js/clearInterval @handler-ref)
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

(defn map-vals
  [m f]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(defn set-global-error-handler!
  "Sets the global error handler. Prints stack trace of uncaught
   error"
  [send-to-server-fn]
  (set! (.-onerror js/window)
        (fn [error-msg url line col error-obj]
          (let [user-agent (-> js/window .-navigator .-userAgent)
                error-details {:error-message error-msg
                               :url url
                               :line line
                               :col col
                               :user-agent user-agent}]
            (-> ((.-fromError js/StackTrace) error-obj)
                (.then (fn [frames]
                         (->> (for [frame frames]
                                (.toString frame))
                              (interpose "\n")
                              (apply str)
                              (assoc error-details :stack)
                              (send-to-server-fn)))))))))

(defn cljs->str
  [data]
  (str data))

(defn str->cljs
  [str]
  (reader/read-string str))

(defn new-uuid []
  (uuid/uuid-string (uuid/make-random-uuid)))

(defn get-path []
  (.. js/window -location -pathname))

(defn- classnames-map [m]
  (reduce-kv (fn [acc k v]
               (if v
                 (conj acc (name k))
                 acc))
             [] m))

(defn classnames [& args]
  "Works more or less like https://github.com/JedWatson/classnames
  (classnames :a :b \"c\" {:d (true? true) :e false} :f) => \"a b c d f\""
  (join " " (reduce (fn [acc e]
                      (if (map? e)
                        (into acc (classnames-map e))
                        (conj acc (name e))))
                    [] args)))

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn extract-query-params
  "Returns query params as map with keywordized keys
   ?param=foo&biz=42 -> {:param \"foo\" :biz \"42\"}"
  []
  (-> (.. js/window -location -href)
      (url/url)
      (:query)
      (->kebab-case-kw)))

(defn remove-empty-query-params
  [params]
  (into {} (remove #(-> % second empty?)) params))

(defn- update-query-params
  [url params]
  (let [new-params (-> (:query url)
                       (clojure.walk/keywordize-keys)
                       (merge params)
                       (remove-empty-query-params))]
    (assoc url :query new-params)))

(defn update-url-with-query-params
  [params]
  (let [url (-> (.. js/window -location -href)
                (url/url)
                (update-query-params params)
                (str)
                (clojure.string/split #"/")
                (last))]
    (.replaceState js/history nil nil url)))

(defn get-unselected-review-states
  [selected-states]
  (clojure.set/difference (-> application-review-states keys set) (set selected-states)))

(defn include-csrf-header? [method]
  (contains? #{:post :put :delete} method))

(defn csrf-token []
  (when-let [token (-> js/document
                  Cookies.
                  (.get "CSRF"))]
    (js/decodeURIComponent token)))

(defn flatten-path [db & parts]
  (flatten [:editor :forms (-> db :editor :selected-form-key) :content [parts]]))

(defn- text-area-size->max-length [text-area-size]
  (condp = text-area-size
    "S" "500"
    "L" "1500"
    "1000"))
