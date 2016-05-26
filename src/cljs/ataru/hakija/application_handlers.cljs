(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ataru.virkailija.handlers :refer [http post]]
            [cljs.core.match :refer-macros [match]]))

(defn get-form [db [_ form-id]]
  (http
    :get
    (str "/hakemus/api/form/" form-id)
    :application/handle-form)
  db)

(register-handler
  :application/get-form
  get-form)

(defn handle-form [db [_ form-response]]
  (assoc db :form form-response))

(register-handler
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(register-handler
  :application/handle-form
  handle-form)
