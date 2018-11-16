(ns ataru.application-common.fx
  (:require [re-frame.core :as re-frame]
            [cljs.core.async :as async]
            [ataru.hakija.has-applied :refer [has-applied]]
            [ataru.hakija.application-validators :as validator]
            [ataru.cljs-util :as util]))

(re-frame/reg-fx :delayed-dispatch
  (fn [{:keys [dispatch-vec timeout]}]
    (js/setTimeout
      (fn []
        (re-frame/dispatch dispatch-vec))
      timeout)))

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

(defn- validatep [{:keys [field-descriptor] :as params}]
  (async/merge
   (map (fn [v] (validator/validate (assoc params :validator v :has-applied has-applied)))
        (:validators field-descriptor))))

(defn- all-valid? [valid-ch]
  (async/reduce (fn [[all-valid? all-errors] [valid? errors]]
                  [(and all-valid? valid?) (concat all-errors errors)])
                [true []]
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

(defn- async-validate-values
  [{:keys [field-descriptor editing? on-validated values] :as params}]
  (if (and editing? (:cannot-edit field-descriptor))
    (on-validated [true []])
    (async/take! (all-valid?
                   (async/merge
                     (map (fn [value] (validatep (merge params {:value value})))
                          values)))
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
  :validate-every-debounced
  (fn [{:keys [field-descriptor field-idx group-idx] :as params}]
    (let [id                           (keyword (:id field-descriptor))
          debounce-id                  (keyword (str (name id) "-" field-idx "-" group-idx))]
      (js/clearTimeout (@validation-debounces debounce-id))
      (swap! validation-debounces assoc debounce-id
        (js/setTimeout
          #(async-validate-values params)
          validation-debounce-ms)))))

(defn- confirm-window-close!
  [event]
  (let [warning-label   (util/get-translation :window-close-warning)
        values-changed? @(re-frame/subscribe [:state-query [:application :values-changed?]])
        submit-status   @(re-frame/subscribe [:state-query [:application :submit-status]])]
    (when (and (some? values-changed?)
               (nil? submit-status))
      (set! (.-returnValue event) warning-label)
      warning-label)))

(re-frame/reg-fx
  :set-window-close-callback
  (fn []
    (.removeEventListener js/window "beforeunload" confirm-window-close!)
    (.addEventListener js/window "beforeunload" confirm-window-close!)))

(re-frame/reg-fx
  :update-url-query-params
  util/update-url-with-query-params)