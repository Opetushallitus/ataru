(ns ataru.hakija.has-applied
  (:require [cljs.core.async :as async]
            [ajax.core :refer [POST]]))

(defn has-applied
  [haku-oid identifier]
  (let [c    (async/chan 1)
        send (fn [has-applied?]
               (async/put! c has-applied?
                 (fn [_] (async/close! c))))
        body {:haku-oid haku-oid
              :ssn (:ssn identifier)
              :email (:email identifier)}]
    (POST "/hakemus/api/has-applied"
         {:handler #(send (:has-applied %))
          :params body
          :error-handler (fn [_] (send false))
          :format :json
          :response-format :json
          :keywords? true
          :headers {"Caller-Id" (aget js/config "hakija-caller-id")}})
    c))
