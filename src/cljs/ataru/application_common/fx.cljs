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

(re-frame/reg-fx
  :dispatch-debounced
  (fn [{:keys [id dispatch timeout]}]
    (js/clearTimeout (@debounces id))
    (swap! debounces assoc id (js/setTimeout
                                (fn []
                                  (re-frame/dispatch dispatch)
                                  (swap! debounces dissoc id))
                                timeout))))

(re-frame/reg-fx
  :set-page-title
  (fn [title]
    (aset js/document "title" title)))

(defn- validatep [{:keys [field-descriptor] :as params}]
  (async/merge
   (map (fn [v] (validator/validate (assoc params :validator v)))
        (:validators field-descriptor))))

(defn- all-valid? [valid-ch]
  (async/reduce (fn [[all-valid? all-errors] [valid? errors]]
                  [(and all-valid? valid?) (concat all-errors errors)])
                [true []]
                valid-ch))

(defonce validation-sequences (atom {}))

(defn- next-val [id]
  (get (swap! validation-sequences update id (fnil inc 0)) id))

(defn- current-val [id]
  (get @validation-sequences id))

(re-frame/reg-fx
 :validate
 (fn [{:keys [field-descriptor editing? on-validated] :as params}]
   (let [id (keyword (:id field-descriptor))
         val (next-val id)]
     (if (and editing? (:cannot-edit field-descriptor))
       (on-validated [true []])
       (async/take! (all-valid? (validatep params))
                    (fn [result]
                      (when (= val (current-val id))
                        (on-validated result))))))))

(re-frame/reg-fx
 :validate-every
 (fn [{:keys [values field-descriptor editing? on-validated] :as params}]
   (let [id (keyword (:id field-descriptor))
         val (next-val id)]
     (if (and editing? (:cannot-edit field-descriptor))
       (on-validated [true []])
       (async/take! (all-valid?
                     (async/merge
                      (map (fn [value] (validatep (assoc params :value value)))
                           values)))
                    (fn [result]
                      (when (= val (current-val id))
                        (on-validated result))))))))

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
