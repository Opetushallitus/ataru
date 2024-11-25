(ns ataru.hakija.application-tutkinto-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ataru.hakija.application-handlers :refer [check-schema-interceptor]]))

(reg-event-fx
  :application/handle-tutkinnot-error
  [check-schema-interceptor]
  (fn [{:keys [db]} [_ response]]
    (js/console.error (str "Handle tutkinto fetch error, response " response))
    {:db (assoc-in db [:oppija-session :tutkinto-fetch-handled] true)}))

(reg-event-db
  :application/handle-fetch-tutkinnot
  [check-schema-interceptor]
  (fn [db [_ {tutkinnot-response-body :body}]]
    (-> db
        (assoc-in [:application :tutkinnot] tutkinnot-response-body)
        (assoc-in [:oppija-session :tutkinto-fetch-handled] true))))

(reg-event-fx
  :application/fetch-tutkinnot
  [check-schema-interceptor]
  (fn [_ [_ requested-koski-levels]]
    {:http {:method         :get
            :url            (str "/hakemus/api/omat-tutkinnot?tutkinto-levels=" requested-koski-levels)
            :handler        [:application/handle-fetch-tutkinnot]
            :error-handler  [:application/handle-tutkinnot-error]}}))
