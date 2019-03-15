(ns ataru.hakija.try-selection
  (:require [cljs.core.async :as async]
            [ajax.core :refer [PUT]]))

(defn try-selection
  [form-key selection-id selection-group-id question-id answer-id]
  ;(prn "TRY")
  (let [url  (cond-> (str "/hakemus/api/selection-limit?form-key=" form-key)
                     selection-group-id (str "&selection-group-id=" selection-group-id)
                     question-id (str "&question-id=" question-id)
                     answer-id (str "&answer-id=" answer-id)
                     selection-id (str "&selection-id=" selection-id))
        c    (async/chan 1)
        send (fn [selection]
               (async/put! c [(boolean (:selection-id selection)) [] [selection]] (fn [_] (async/close! c))))]
    (PUT url
      {:handler         #(send %)
       :error-handler   (fn [response]
                          (let [selection (:response response)]
                            (send selection)))
       :format          :json
       :response-format :json
       :keywords?       true})
    c))

(defn remove-selection
  [form-key selection-id selection-group-id question-id]
  (let [url  (cond-> (str "/hakemus/api/selection-limit?form-key=" form-key)
                     selection-group-id (str "&selection-group-id=" selection-group-id)
                     question-id (str "&question-id=" question-id)
                     selection-id (str "&selection-id=" selection-id))
        c    (async/chan 1)
        send (fn [selection]
               (async/put! c [(boolean (:selection-id selection)) [] [selection]] (fn [_] (async/close! c))))]
    (PUT url
      {:handler         #(send %)
       :error-handler   (fn [response]
                          (let [selection (:response response)]
                            (send selection)))
       :format          :json
       :response-format :json
       :keywords?       true})
    c))