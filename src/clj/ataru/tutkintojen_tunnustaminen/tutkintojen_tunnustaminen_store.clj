(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store

  (:require [ataru.background-job.job :as job]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-send-job :as tutkintojen-tunnustaminen-send-job]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/tutkintojen-tunnustaminen-queries.sql")

(defn get-application
  [country-question-id application-id]
  (let [application (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                                             (first (yesql-get-application {:country_question_id country-question-id
                                                                            :id                  application-id}
                                                                           {:connection connection})))]
    (when (nil? application)
      (throw (new RuntimeException (str "Application " application-id
                                        " not found"))))
    application))
(defn get-application-by-event-id
  [country-question-id event-id]
  (let [id-and-state (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                                              (first (yesql-get-application-id-and-state-by-event-id {:id event-id}
                                                                                                     {:connection connection})))]
    (when (nil? id-and-state)
      (throw (new RuntimeException (str "Application id by event id " event-id
                                        " not found"))))
    {:review-key  (:review-key id-and-state)
     :state       (:state id-and-state)
     :application (get-application country-question-id (:id id-and-state))}))

(defn get-latest-application-id [application-key]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                           (first (yesql-get-latest-application-id {:key application-key} {:connection connection}))))

(defn start-tutkintojen-tunnustaminen-submit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen submit job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                        (job/start-job job-runner
                                                       connection
                                                       "tutkintojen-tunnustaminen-submit-job"
                                                       {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-edit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen edit job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                        (job/start-job job-runner
                                                       connection
                                                       "tutkintojen-tunnustaminen-edit-job"
                                                       {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-review-state-changed-job
  [job-runner event-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen review state changed job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                        (job/start-job job-runner
                                                       connection
                                                       "tutkintojen-tunnustaminen-review-state-changed-job"
                                                       {:event-id event-id})))))

(defn start-tutkintojen-tunnustaminen-information-request-sent-job
  [job-runner information-request]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen information request sent job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                        (job/start-job job-runner
                                                       connection
                                                       "tutkintojen-tunnustaminen-information-request-sent-job"
                                                       {:information-request information-request})))))

(defn- get-tutu-application [application-key]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                           (first (yesql-get-tutu-application {:key application-key} {:connection connection}))))

(defn start-tutkintojen-tunnustaminen-send-job [job-runner  application-key]
  (when (get-in config [:tutkintojen-tunnustaminen :tutu-send-enabled?])
    (let [job-type (:type tutkintojen-tunnustaminen-send-job/job-definition)
          tutu-application (get-tutu-application application-key)
          job-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                                           (job/start-job job-runner connection job-type tutu-application))]
      (log/info "Started tutkintojen tunnustaminen send job with job id" job-id))))
