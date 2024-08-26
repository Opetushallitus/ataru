(ns ataru.siirtotiedosto.siirtotiedosto-store
  (:require [ataru.db.db :as db]
            [clojure.set :as set]
            [yesql.core :refer [defqueries]]
            [taoensso.timbre :as log]))


(defqueries "sql/siirtotiedosto-queries.sql")

(declare insert-new-siirtotiedosto-operation!<)
(declare upsert-siirtotiedosto-data!)
(declare latest-siirtotiedosto-data)

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn get-latest-successful-data []
  (log/info "Fetching latest siirtotiedosto data")
  (let [db-result (first (exec-db :db latest-siirtotiedosto-data {}))
        processed-result (set/rename-keys db-result {:execution_uuid :execution-uuid
                                                     :window_start   :window-start
                                                     :window_end     :window-end
                                                     :run_start      :run-start
                                                     :run_end        :run-end
                                                     :error_message  :error-message})]
    (log/info "Fetched siirtotiedosto data:" processed-result)
    processed-result))

(defn insert-new-siirtotiedost-operation [data]
  (log/info "Persisting new siirtotiedosto operation" data)
  (let [db-result (first (exec-db :db insert-new-siirtotiedosto-operation!< {:execution_uuid (:execution-uuid data)
                                                                             :window_start   (:window-start data)}))
        processed-result (set/rename-keys db-result {:execution_uuid :execution-uuid
                                                     :window_start   :window-start
                                                     :window_end     :window-end
                                                     :run_start      :run-start
                                                     :run_end        :run-end
                                                     :error_message  :error-message})]
    (log/info "Persisted siirtotiedosto data, result" processed-result)
    processed-result)

  )

(defn update-siirtotiedosto-operation [data]
  (log/info "Persisting siirtotiedosto data" data)
  (exec-db :db upsert-siirtotiedosto-data! (set/rename-keys data {:run-start      :run_start
                                                                  :run-end        :run_end
                                                                  :error-message  :error_message})))