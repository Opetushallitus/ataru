(ns ataru.hakija.try-selection
  (:require [cljs.core.async :as async]
            [ajax.core :refer [PUT]]
            [clojure.string]
            [ataru.cljs-util :as util]
            [ataru.hakija.demo :as demo]))

(defn- try-selection-with-api
  [form-key selection-id selection-group-id question-id answer-id]
  (let [url  (cond-> (str "/hakemus/api/selection-limit?form-key=" form-key)
                     selection-group-id (str "&selection-group-id=" selection-group-id)
                     question-id (str "&question-id=" question-id)
                     (not (clojure.string/blank? answer-id)) (str "&answer-id=" answer-id)
                     selection-id (str "&selection-id=" selection-id))
        c    (async/chan 1)
        send (fn [selection errors]
               (async/put! c [(boolean (:selection-id selection)) errors [selection]] (fn [_] (async/close! c))))]
    (PUT url
      {:handler         #(send % [])
       :error-handler   (fn [response]
                          (let [selection (:response response)]
                            (send selection [:server-error])))
       :headers         {"Caller-Id" (aget js/config "hakija-caller-id")
                         "CSRF"      (util/csrf-token)}
       :format          :json
       :response-format :json
       :keywords?       true})
    c))

(defn- try-selection-demo
  []
  (let [c (async/chan 1)]
    (async/put! c [true []])
    (async/close! c)
    c))

(defn try-selection
  [db & args]
  (if (demo/demo? db)
    (try-selection-demo)
    (apply try-selection-with-api args)))
