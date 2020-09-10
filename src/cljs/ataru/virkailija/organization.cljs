(ns ataru.virkailija.organization
  (:require-macros [ataru.async-macros :as asyncm])
  (:require [cljs.core.async :as async]
            [ajax.core :refer [GET]]))

(defn fetch-hakukohderyhmat
  [c]
  (GET (str "/lomake-editori/api/organization/hakukohderyhmat")
    {:handler #(async/put! c %
                 (fn [_] (async/close! c)))
     :error-handler #(async/put! c (new js/Error %)
                       (fn [_] (async/close! c)))
     :response-format :json
     :keywords? true
     :timeout 15000
     :headers {"Caller-Id" (aget js/config "virkailija-caller-id")}}))
