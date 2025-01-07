(ns ataru.hakija.application-tutkinto-handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ataru.tutkinto.tutkinto-util :as tutkinto-util]
            [ataru.hakija.application-handlers :refer [check-schema-interceptor set-empty-value-dispatch]]))

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
        (assoc-in [:application :koski-tutkinnot] (tutkinto-util/sort-koski-tutkinnot tutkinnot-response-body))
        (assoc-in [:oppija-session :tutkinto-fetch-handled] true))))

(reg-event-fx
  :application/fetch-tutkinnot
  [check-schema-interceptor]
  (fn [_ [_ requested-koski-levels]]
    {:http {:method         :get
            :url            (str "/hakemus/api/omat-tutkinnot?tutkinto-levels=" requested-koski-levels)
            :handler        [:application/handle-fetch-tutkinnot]
            :error-handler  [:application/handle-tutkinnot-error]}}))

(reg-event-fx
  :application/add-tutkinto-row
  [check-schema-interceptor]
  (fn add-question-group-row [{db :db} [_ field-descriptor id-field-descriptor tutkinto-id]]
    (let [id           (keyword (:id field-descriptor))
          repeat-count (get-in db [:application :ui id :count] 1)]
      {:db         (assoc-in db [:application :ui id :count] (inc repeat-count))
       :dispatch-n (concat (mapcat (partial set-empty-value-dispatch repeat-count)
                                   (:children field-descriptor))
                           [[:application/set-repeatable-application-field id-field-descriptor (dec repeat-count) nil tutkinto-id]])})))

