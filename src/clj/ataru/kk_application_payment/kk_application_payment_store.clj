(ns ataru.kk-application-payment.kk-application-payment-store
  (:require [ataru.db.db :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/kk-application-payment-queries.sql")

(declare yesql-get-awaiting-kk-application-payments)
(declare yesql-get-kk-application-payments-for-application-keys)
(declare yesql-get-kk-application-payments-history-for-application-keys)
(declare yesql-upsert-kk-application-payment<!)

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

(defn get-awaiting-kk-application-payments
  []
  (->> (exec-db :db yesql-get-awaiting-kk-application-payments {})
       (map ->kebab-case-kw)))

(defn get-kk-application-payments-history
  [application-keys]
  (->> (exec-db :db yesql-get-kk-application-payments-history-for-application-keys {:application_keys application-keys})
       (map ->kebab-case-kw)))

(defn get-kk-application-payments
  [application-keys]
  (->> (exec-db :db yesql-get-kk-application-payments-for-application-keys {:application_keys application-keys})
       (map ->kebab-case-kw)))

(defn create-or-update-kk-application-payment!
  [{:keys [application-key state reason due-date total-sum maksut-secret
           required-at reminder-sent-at approved-at]}]
  (->> (exec-db :db yesql-upsert-kk-application-payment<! {:application_key      application-key
                                                           :state                state
                                                           :reason               reason
                                                           :due_date             due-date
                                                           :total_sum            total-sum
                                                           :maksut_secret        maksut-secret
                                                           :required_at          required-at
                                                           :reminder_sent_at     reminder-sent-at
                                                           :approved_at          approved-at})
       (->kebab-case-kw)))
