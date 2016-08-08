(ns ataru.hakija.email-store
  (:require [oph.soresu.common.db :refer [exec]]))

(defqueries "sql/email-queries.sql")

(defn store-email-verification
  [application application-id]
  (let [recipient (-> (filter #(= "email" (:key %)) (:answers application))
                      first
                      :value)]
    ; TODO: remove old rows with same recipient and application-id?
    (exec :db yesql-add-application-confirmation-email<! {:application-id application-id :recipient recipient})))