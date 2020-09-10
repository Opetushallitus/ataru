(ns ataru.hakija.has-applied
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]))

(defn has-applied
  [haku-oid identifier]
  (let [url  (str "/hakemus/api/has-applied"
               "?hakuOid=" haku-oid
               (if (contains? identifier :ssn)
                 (str "&ssn=" (:ssn identifier))
                 (str "&email=" (:email identifier))))
        c    (async/chan 1)
        send (fn [has-applied?]
               (async/put! c has-applied?
                 (fn [_] (async/close! c))))]
    (GET url
         {:handler #(send (:has-applied %))
          :error-handler (fn [response]
                           (send false))
          :format :json
          :response-format :json
          :keywords? true
          :headers {"Caller-Id" (aget js/config "hakija-caller-id")}})
    c))
