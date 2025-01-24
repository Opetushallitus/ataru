(ns ataru.kk-application-payment.kk-application-payment-store
  (:require [ataru.db.db :as db]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [yesql.core :refer [defqueries]]
            [clj-time.core :as time]))

(defqueries "sql/kk-application-payment-queries.sql")
(defqueries "sql/field-deadline-queries.sql")

(declare yesql-get-awaiting-kk-application-payments)
(declare yesql-get-kk-application-payments-for-application-keys)
(declare yesql-get-kk-application-payments-history-for-application-keys)
(declare yesql-upsert-kk-application-payment<!)
(declare yesql-update-maksut-secret!)
(declare yesql-mark-reminder-sent!)
(declare yesql-get-field-deadlines)

(def ^:private ->kebab-case-kw (partial transform-keys ->kebab-case-keyword))

; Yesql converts PostgreSQL dates to datetimes automatically - and in that case the
; true due datetime is the last minute of the day in Helsinki time zone. This also
; helps to sidestep various automatic UTC conversion issues in the frontend...
(defn due-date-to-full-time-in-finnish-tz [payment]
  (if-let [due-date (:due-date payment)]
    (assoc payment :due-date
                   (time/to-time-zone
                       (time/plus due-date (time/hours 23) (time/minutes 59))
                       (time/time-zone-for-id "Europe/Helsinki")))
    payment))

(defn- exec-db
  [ds-key query params]
  (db/exec ds-key query params))

; This is here to avoid messy cyclic dependency conflicts. If kk-application-payment was refactored into a service
; we might do better using dependency injections and ataru.applications.field-deadline
(defn get-field-deadlines
  ([application-key]
   (db/exec :db yesql-get-field-deadlines {:application_key application-key})))

(defn mark-reminder-sent!
  [application-key]
  (exec-db :db yesql-mark-reminder-sent! {:application_key application-key}))

(defn update-maksut-secret!
  [application-key maksut-secret]
  (exec-db :db yesql-update-maksut-secret! {:application_key application-key
                                            :maksut_secret   maksut-secret}))

(defn get-awaiting-kk-application-payments
  []
  (->> (exec-db :db yesql-get-awaiting-kk-application-payments {})
       (map ->kebab-case-kw)
       (map due-date-to-full-time-in-finnish-tz)))

(defn get-kk-application-payments-history
  [application-keys]
  (->> (exec-db :db yesql-get-kk-application-payments-history-for-application-keys {:application_keys application-keys})
       (map ->kebab-case-kw)
       (map due-date-to-full-time-in-finnish-tz)))

(defn get-kk-application-payments
  [application-keys]
  (->> (exec-db :db yesql-get-kk-application-payments-for-application-keys {:application_keys application-keys})
       (map ->kebab-case-kw)
       (map due-date-to-full-time-in-finnish-tz)))

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
       (->kebab-case-kw)
       (due-date-to-full-time-in-finnish-tz)))
