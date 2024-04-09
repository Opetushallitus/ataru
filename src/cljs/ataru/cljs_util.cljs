(ns ataru.cljs-util
  (:require [ataru.translations.translation-util :as tu]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.walk :as walk]
            [cljs.core.match :refer-macros [match]]
            [cljs.reader :as reader]
            [cljs-uuid-utils.core :as uuid]
            [re-frame.core :refer [dispatch] :as re-frame]
            [reagent.dom :as r-dom]
            [cemerick.url :as url]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [goog.string.format])
  (:import [goog.net Cookies]))

(defn confirm-window-close!
  [event]
  (let [lang          @(re-frame/subscribe [:application/form-language])
        warning-label (tu/get-hakija-translation :window-close-warning lang)
        edits?        @(re-frame/subscribe [:application/edits?])
        submit-status @(re-frame/subscribe [:state-query [:application :submit-status]])]
    (when (and edits?
               (nil? submit-status))
      (set! (.-returnValue event) warning-label)
      warning-label)))

(def wrap-scroll-to
  (with-meta identity {:component-did-mount #(let [node (r-dom/dom-node %)]
                                              (if (.-scrollIntoViewIfNeeded node)
                                                (.scrollIntoViewIfNeeded node)
                                                (.scrollIntoView node)))}))


(defn debounce
  ([f] (debounce f 1000))
  ([f timeout]
   (let [id (atom nil)]
     (fn [& args]
       (when (not (nil? @id))
         (js/clearTimeout @id))
       (reset! id (js/setTimeout
                    (apply partial f args)
                    timeout))))))

(defn dispatch-after-state
  [& {:keys [predicate handler]}]
  {:pre [(not (nil? predicate))
         (not (nil? handler))]}
  (let [handler-ref (atom nil)
        sanity-count (atom 0)
        dispatcher (fn [db]
                     (let [pred (predicate db)]
                       (match [(swap! sanity-count inc) pred]
                              [50 _] (js/clearInterval @handler-ref)
                              [_ (result :guard (comp true? boolean))]
                              (do
                                (js/clearInterval @handler-ref)
                                (handler result))
                              :else nil)))]
    (reset!
      handler-ref
      (js/setInterval
        #(dispatch [:state-update dispatcher])
        200))))

(defn set-global-error-handler!
  "Sets the global error handler. Prints stack trace of uncaught
   error"
  [send-to-server-fn description-fn]
  (set! (.-onerror js/window)
        (fn [error-msg url line col error-obj]
          (let [user-agent (-> js/window .-navigator .-userAgent)
                error-details {:error-message error-msg
                               :url url
                               :line line
                               :description (description-fn)
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

(defn valid-uuid? [id]
  (uuid/valid-uuid? id))

(defn get-path []
  (.. js/window -location -pathname))

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
  (into {} (remove #(-> % second nil?)) params))

(defn- update-query-params
  [url params]
  (let [new-params (-> (:query url)
                       (walk/keywordize-keys)
                       (merge params)
                       (remove-empty-query-params))]
    (assoc url :query new-params)))

(defn update-url-with-query-params
  [params]
  (let [url (-> (.. js/window -location -href)
                (url/url)
                (update-query-params params)
                (str))]
    (.replaceState js/history nil nil url)))

(defn set-query-param
  [key value]
  (let [new-url (-> (.. js/window -location -href)
                    (url/url)
                    (assoc-in [:query key] value)
                    str)]
    (.replaceState js/history nil nil new-url)))

(defn unset-query-param
  [key]
  (let [new-url (-> (.. js/window -location -href)
                    (url/url)
                    (update :query dissoc key)
                    str)]
    (.replaceState js/history nil nil new-url)))

(defn get-unselected-review-states
  [unselected-states all-states]
  (set/difference
    (->> all-states
         (map first)
         set)
    (set unselected-states)))

(defn include-csrf-header? [method]
  (contains? #{:patch :post :put :delete} method))

(defn csrf-token []
  (when-let [token (-> js/document
                  Cookies.
                  (.get "CSRF"))]
    (js/decodeURIComponent token)))

(defn flatten-path [db & parts]
  (flatten [:editor :forms (-> db :editor :selected-form-key) :content [parts]]))

(defn- resize-vector [target-length x]
  (let [add-length (- target-length (count x))]
    (cond-> x
      (> add-length 0)
      (into (repeatedly add-length (fn [] nil))))))

(defn vector-of-length [target-length]
  (comp (partial resize-vector target-length)
        (fnil identity [])))

(defn modify-event? [event]
  (some #{(:event-type event)} ["updated-by-applicant" "updated-by-virkailija"]))

(defn to-finnish-number [value]
  (if (js/Number.isNaN value)
    value
    (.toLocaleString (js/Number value) "fi")))

(defn keep-non-empty-changes
  [changes]
  (when (some? changes)
    (let [is-non-empty-value? (fn [value] (and (some? value)
                                               (or (number? value) (seq value))))
          keep-if-non-empty-change (fn [[id change]]
                                     (when (or (is-non-empty-value? (:new change))
                                               (is-non-empty-value? (:old change)))
                                       [id change]))]
      (->> changes
           (map keep-if-non-empty-change)
           (remove nil?)
           (into {})))))

(defn classes [& cs] (string/join " " (vec cs)))