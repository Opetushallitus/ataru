(ns ataru.hakija.application-handlers
  (:require [re-frame.core :refer [register-handler dispatch]]
            [ataru.ajax.http :refer [http post]]
            [cljs.core.match :refer-macros [match]]
            [ataru.hakija.application :refer [create-initial-answers]]))

(defn get-form [db [_ form-id]]
  (http
    :get
    (str "/hakemus/api/form/" form-id)
    :application/handle-form)
  db)

(register-handler
  :application/get-form
  get-form)

(defn handle-form [db [_ form]]
  (-> db
    (assoc :form form)
    (assoc :application {:answers (create-initial-answers form)})))

(register-handler
  :flasher
  (fn [db [_ flash]]
    (assoc db :flasher flash)))

(register-handler
  :application/handle-form
  handle-form)

(defn initialize-db [_ _]
  {:form nil
   :application {:answers {}}})

(register-handler
  :application/initialize-db
  initialize-db)

(defn set-application-field [db [_ key value]]
  (assoc-in db [:application :answers key] value))

(register-handler
  :application/set-application-field
  set-application-field)
