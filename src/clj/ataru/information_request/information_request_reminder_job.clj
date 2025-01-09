(ns ataru.information-request.information-request-reminder-job
  (:require [ataru.db.db :as db]
            [ataru.information-request.information-request-store :as ir-store]
            [ataru.information-request.information-request-service :as ir-service]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn- handle-reminder [information-request job-runner]
  (log/info "Handling reminder for information request" (:id information-request))
  (if (.isBefore (:created-time information-request) (:application-updated-time information-request))
    (log/info "Application has been modified, reminder" (:id information-request) "not sent")
    (jdbc/with-db-transaction
      [connection {:datasource (db/get-datasource :db)}]
      (ir-service/start-email-job
        job-runner
        (assoc information-request :reminder? true)
        connection))))

(defn- handler [_ job-runner]
  (let [information-requests (ir-store/get-information-requests-to-remind)]
    (doseq [information-request information-requests]
      (handle-reminder information-request job-runner)
      (ir-store/set-information-request-reminder-processed-time-by-id! (:id information-request)))))

(def job-definition {:handler handler
                     :type    (-> *ns* ns-name str)
                     :schedule "0 12,16 * * *"})
