(ns ataru.hakija.email-store
  (:require [oph.soresu.common.db :refer [exec]]
            [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :refer [info]]
            [oph.soresu.common.db :as db]
            [manifold.deferred :as deferred]))

(defqueries "sql/email-queries.sql")

(defn store-email-verification
  [application application-id]
  (let [recipient (-> (filter #(= "email" (:key %)) (:answers application))
                      first
                      :value)]
    (exec :db yesql-add-application-confirmation-email<! {:application_id application-id :recipient recipient})))

(defn deliver-emails
  [send-email-fn]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (let [connection {:connection conn}
          emails (yesql-get-unsent-application-confirmation-emails {} connection)
          undelivered-emails (filter #(nil? (:delivered-at %)) emails)]
      (when (< 0 (count undelivered-emails))
        (info "Attempting to deliver" (count undelivered-emails) "application confirmation emails")
        (doseq [email undelivered-emails]
          (let [application-id (:application_id email)
                email-id (:id email)
                {:keys [status error]} (send-email-fn email)]
            (info "sent email with status" status (or error ""))
            (if (or error (not (= status 200)))
              (do
                (yesql-increment-delivery-attempt-count! {:id email-id} connection)
                (info "Sending email" email-id "to viestintäpalvelu failed for application" application-id)
                (info "error details:")
                (info error))
              (do
                (yesql-increment-delivery-attempt-count-and-mark-delivered! {:id email-id} connection)
                (info "Successfully sent email" email-id "to viestintäpalvelu for application" application-id)))))))))
