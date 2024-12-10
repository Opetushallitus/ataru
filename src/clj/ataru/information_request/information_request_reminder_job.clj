(ns ataru.information-request.information-request-reminder-job
  (:require [ataru.background-job.email-job :as email-job]
            [ataru.background-job.job :as job]
            [ataru.db.db :as db]
            [ataru.information-request.information-request-store :as ir-store]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn- start-email-job [job-runner email]
  (let [job-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                         (job/start-job job-runner
                                                        connection
                                                        (:type email-job/job-definition)
                                                        email))]
    (log/info "Started information request reminder email job (to viestintäpalvelu) with job id" job-id)))



(defn- handle-reminder [information-request job-runner]
  (log/info "Handling reminder for information request" (:id information-request))
  (if (.isBefore (:created-time information-request) (:application-updated-time information-request))
    (log/info "Application has been modified, reminder" (:id information-request) "not sent")
    (start-email-job job-runner {:subject ""
                                 :from ""})))

(defn- handler [job-runner]
  (let [information-requests (ir-store/get-information-requests-to-remind)]
    (doseq [information-request information-requests]
      (handle-reminder information-request job-runner)
      (ir-store/set-information-request-reminder-processed-time-by-id! (:id information-request)))))

(def job-definition {:handler handler
                     :type    (-> *ns* ns-name str)
                     :schedule "0 12 * * *"})
