(ns ataru.kk-application-payment.kk-application-payment-maksut-poller-job-spec
  (:require [ataru.fixtures.db.unit-test-db :as unit-test-db]
            [speclj.core :refer [it describe tags should= after before]]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.kk-application-payment.kk-application-payment-maksut-poller-job :as poller-job]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [clojure.string :as str]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]))

(def key-with-paid-status    "1.2.246.562.8.00000000000022225100")
(def key-with-overdue-status "1.2.246.562.8.00000000000022225200")
(def key-with-active-status  "1.2.246.562.8.00000000000022225300")
(def key-with-no-status      "1.2.246.562.8.00000000000000005400")

(defrecord FakeJobRunner []
  component/Lifecycle

  job/JobRunner
  (start-job [_ _ _ _]))

(defrecord MockMaksutService []
  MaksutServiceProtocol

  (create-kasittely-lasku [_ _] {})
  (create-paatos-lasku [_ _] {})
  (list-lasku-statuses [_ keys] (->> keys
                                     (map (fn [key]
                                            (when (str/includes? key "2222")
                                              {:order_id (payment/maksut-reference->maksut-order-id key)
                                               :reference key
                                               :status (cond
                                                         (str/includes? key "5100") :paid
                                                         (str/includes? key "5200") :overdue
                                                         :else                      :active)
                                               :origin "kkhakemusmaksu"})))
                                     (remove nil?)))
  (list-laskut-by-application-key [_ _] []))

(def mock-maksut-service (->MockMaksutService))

(def runner
  (map->FakeJobRunner {:maksut-service mock-maksut-service}))

(defn create-awaiting-status [application-key]
  (payment/set-application-fee-required application-key nil)
  application-key)

(defn create-not-required-status [application-key]
  (payment/set-application-fee-not-required-for-eu-citizen application-key nil)
  application-key)

(defn check-state-and-history
  [application-key state-name payment-count history-count]
  (let [payments (payment/get-raw-payments [application-key])
        payment-data (first payments)
        history (payment/get-raw-payment-history [application-key])]
    (should= payment-count (count payments))
    (should= history-count (count history))
    (should= state-name (:state payment-data))))

(def comparison-payments
  "Some states that should hold after each and every job run."
  [["1.2.246.562.8.00000000000022225500" (:not-required payment/all-states) 1 0]
   ["1.2.246.562.8.00000000000022225600" (:overdue payment/all-states) 1 0]
   ["1.2.246.562.8.00000000000022225700" (:paid payment/all-states) 1 0]
   ["1.2.246.562.8.00000000001122225100" (:paid payment/all-states) 1 1]])

(defn check-comparison-payments []
  (doseq [[application-key state-name payment-count history-count] comparison-payments]
    (check-state-and-history application-key state-name payment-count history-count)))

(describe "kk-application-payment-maksut-poller-job"
          (tags :unit)

          ; Add some other states that are checked during every test
          (before
            ; The first three ones should always stay as is
            (payment/set-application-fee-not-required-for-eu-citizen
              "1.2.246.562.8.00000000000022225500" nil)
            (payment/set-application-fee-overdue
              "1.2.246.562.8.00000000000022225600" nil)
            (payment/set-application-fee-paid
              "1.2.246.562.8.00000000000022225700" nil)
            ; The fourth one should change to paid after each job run because it's in awaiting state
            ; and mock maksut returns paid for the oid pattern.
            (payment/set-application-fee-required
              "1.2.246.562.8.00000000001122225100" nil))

          (after
            (unit-test-db/nuke-kk-payment-data))

          (it "does not change status if payment not in awaiting state"
              (let [_ (create-not-required-status key-with-no-status)
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-history key-with-no-status (:not-required payment/all-states) 1 0)
                (check-comparison-payments)))

          (it "does not change status if yet active payment returned from maksut"
              (let [_ (create-awaiting-status key-with-active-status)
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-history key-with-active-status (:awaiting payment/all-states) 1 0)
                (check-comparison-payments)))

          (it "changes the status of newly paid awaiting payment as paid"
              (let [_ (create-awaiting-status key-with-paid-status)
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-history key-with-paid-status (:paid payment/all-states) 1 1)
                (check-comparison-payments)))

          (it "changes the status of newly overdue awaiting payment as overdue"
              (let [_ (create-awaiting-status key-with-overdue-status)
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-history key-with-overdue-status (:overdue payment/all-states) 1 1)
                (check-comparison-payments))))