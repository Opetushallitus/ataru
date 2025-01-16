(ns ataru.information-request.information-request-reminder-job
  (:require [ataru.db.db :as db]
            [ataru.information-request.information-request-store :as ir-store]
            [ataru.information-request.information-request-service :as ir-service]
            [ataru.applications.application-store :as application-store]
            [ataru.config.core :refer [config]]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(declare connection)

(defn- handle-reminder [information-request job-runner]
  (log/info "Handling reminder for information request" (:id information-request))
  (if (.isBefore (:created-time information-request) (:application-updated-time information-request))
    (do
      (log/info "Application has been modified, reminder" (:id information-request) "not sent")
      (ir-store/set-information-request-reminder-processed-time-by-id! (:id information-request)))
    (jdbc/with-db-transaction
      [connection {:datasource (db/get-datasource :db)}]
      (ir-service/start-email-job
        job-runner
        connection
        (assoc information-request :reminder? true))
      (application-store/add-application-event-in-tx
        connection
        {:application-key (:application-key information-request)
         :event-type "information-request-reminder-sent"}
        nil)
      (ir-store/set-information-request-reminder-processed-time-by-id-in-tx! connection (:id information-request)))))

(defn handler [_ job-runner]
  (let [information-requests (ir-store/get-information-requests-to-remind)]
    (doseq [information-request information-requests]
      (handle-reminder information-request job-runner))))

(def job-definition {:handler handler
                     :type    (-> *ns* ns-name str)
                     :schedule (get-in config [:jobs :information-request-reminder-job-cron])})
