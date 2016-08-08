(ns ataru.hakija.email-store
  (:require [oph.soresu.common.db :refer [exec]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/email-queries.sql")

(defn store-email-verification
  [application application-id]
  (let [recipient (-> (filter #(= "email" (:key %)) (:answers application))
                      first
                      :value)]
    ; TODO: remove old rows with same recipient and application-id?
    (exec :db yesql-add-application-confirmation-email<! {:application_id application-id :recipient recipient})))

(defn get-unsent-emails
  []
  (exec :db yesql-get-unsent-application-confirmation-emails {}))

(defn mark-email-delivered
  [confirmation-id]
  (exec :db yesql-increment-delivery-attempt-count-and-mark-delivered {:id confirmation-id}))

(defn increment-delivery-attempt-count
  [confirmation-id]
  (exec :db yesql-increment-delivery-attempt-count {:id confirmation-id}))