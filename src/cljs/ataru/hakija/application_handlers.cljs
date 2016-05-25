(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [lomake-editori.handlers :refer [http post]]
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
  (println "response when not found" form-response)
  (match (:status form-response)
         200 (assoc db :form (:body form-response))
         other-status (assoc db :error-message (str "Hakulomaketta ei voitu hakea, virhekoodi: " other-status))))

(register-handler
  :application/handle-form
  handle-form)
