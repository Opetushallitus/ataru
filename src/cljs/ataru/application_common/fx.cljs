(ns ataru.application-common.fx
  (:require [ajax.core :as ajax]
            [re-frame.core :as re-frame]
            [cljs.core.async :as async]
            [ataru.hakija.has-applied :refer [has-applied]]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]))

(defn http [caller-id
            {:keys [method
                    url
                    post-data
                    headers
                    handler
                    progress-handler
                    error-handler
                    started-handler]}]
  (cond-> ((case method
             :get    ajax/GET
             :post   ajax/POST
             :put    ajax/PUT
             :delete ajax/DELETE)
           url
           (merge {:response-format (ajax/ring-response-format
                                     {:format (ajax/json-response-format
                                               {:keywords? true})})
                   :headers         (merge headers
                                           {"Caller-Id" caller-id}
                                           (when (util/include-csrf-header? method)
                                             {"CSRF" (util/csrf-token)}))
                   :handler         (fn [response]
                                      ;; cljs-ajax calls this handler with nil
                                      ;; if server responded with 204 or 205
                                      (->> (or response {:status 204})
                                           (conj handler)
                                           re-frame/dispatch))
                   :error-handler   (fn [response] (re-frame/dispatch (conj error-handler (:response response))))}
                  (when (some? progress-handler)
                    {:progress-handler (fn [event] (re-frame/dispatch (conj progress-handler event)))})
                  (when (some? post-data)
                    {:format :json
                     :params post-data})))
          (some? started-handler)
          started-handler))

(re-frame/reg-fx :delayed-dispatch
  (fn [{:keys [dispatch-vec timeout]}]
    (js/setTimeout
      (fn []
        (re-frame/dispatch dispatch-vec))
      timeout)))

(defonce live-intervals (atom {}))

(re-frame.core/reg-fx
  :interval
  (fn [{:keys [action id frequency event]}]
    (case action
      :start (if-not ((keyword id) @live-intervals) 
               (do (prn "Starting interval with id " id) (swap! live-intervals assoc id (js/setInterval #(re-frame/dispatch event) frequency)))
               (prn "No need to set interval as it already exists"))
      :stop  (do (js/clearInterval (get @live-intervals id))
                 (swap! live-intervals dissoc id)))))

(defonce debounces (atom {}))

(defn- debounce-dispatch
  [{:keys [id dispatch timeout]}]
  (js/clearTimeout (@debounces id))
  (swap! debounces assoc id (js/setTimeout
                              (fn []
                                (re-frame/dispatch dispatch)
                                (swap! debounces dissoc id))
                              timeout)))

(re-frame/reg-fx
  :dispatch-debounced-n
  (fn [dispatches]
    (doseq [dispatch dispatches]
      (debounce-dispatch dispatch))))

(re-frame/reg-fx
  :dispatch-debounced
  (fn [dispatch]
    (debounce-dispatch dispatch)))

(re-frame/reg-fx
  :set-page-title
  (fn [title]
    (aset js/document "title" title)))

(defn- validatep [{:keys [field-descriptor try-selection] :as params}]
  (async/merge
   (map (fn [v] (validator/validate (assoc params :validator v :has-applied has-applied :try-selection try-selection)))
        (:validators field-descriptor))))

(defn- all-valid? [valid-ch]
  (async/reduce (fn [[all-valid? all-errors all-metadata] [valid? errors metadata]]
                  [(and all-valid? valid?) (concat all-errors errors) (concat all-metadata metadata)])
                [true [] []]
                valid-ch))

(def validation-debounces (atom {}))

(def validation-debounce-ms 500)

(defn- async-validate-value
  [{:keys [field-descriptor editing? on-validated] :as params}]
  (if (and editing? (:cannot-edit field-descriptor))
    (on-validated [true []])
    (async/take! (all-valid? (validatep params))
                 (fn [result]
                   (on-validated result)))))

(re-frame/reg-fx
  :validate-debounced
  (fn [{:keys [field-descriptor field-idx group-idx] :as params}]
    (let [id                           (keyword (:id field-descriptor))
          debounce-id                  (keyword (str (name id) "-" field-idx "-" group-idx))]
      (js/clearTimeout (@validation-debounces debounce-id))
      (swap! validation-debounces assoc debounce-id
        (js/setTimeout
          #(async-validate-value params)
          validation-debounce-ms)))))



(re-frame/reg-fx
  :set-window-close-callback
  (fn []
    (.removeEventListener js/window "beforeunload" util/confirm-window-close!)
    (.addEventListener js/window "beforeunload" util/confirm-window-close!)))

(re-frame/reg-fx
  :update-url-query-params
  util/update-url-with-query-params)

(re-frame/reg-fx
  :scroll-to-application-in-list
  (fn [element-id]
    (when-let [element (.getElementById js/document (str "application-list-row-" element-id))]
      (.scrollIntoView element)
      (.scrollBy js/window 0 -50))))
