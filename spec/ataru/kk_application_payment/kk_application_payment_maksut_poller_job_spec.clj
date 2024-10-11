(ns ataru.kk-application-payment.kk-application-payment-maksut-poller-job-spec
  (:require [speclj.core :refer [it describe tags should= after before]]
            [clojure.java.jdbc :as jdbc]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.kk-application-payment.kk-application-payment-maksut-poller-job :as poller-job]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [clojure.string :as str]
            [ataru.background-job.job :as job]
            [com.stuartsierra.component :as component]))

(def ^:private comparison-state-ids (atom nil))
(def ^:private latest-state-id (atom nil))

(defn reference-to-order-id
  [reference]
  (let [[oid term year] (str/split reference #"[-]")
        aid (last (str/split oid #"[.]"))
        term (str/upper-case (last (str/split term #"[_]")))]
    (str "KKHA" aid term year)))

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
                                            (when (str/includes? key "2025")
                                              {:order_id (reference-to-order-id key)
                                               :reference key
                                               :status (cond
                                                         (str/includes? key "123456") :paid
                                                         (str/includes? key "654321") :overdue
                                                         :else                        :active)
                                               :origin "kkhakemusmaksu"})))
                                     (remove nil?)))
  (list-laskut-by-application-key [_ _] []))

(def mock-maksut-service (->MockMaksutService))

(def runner
  (map->FakeJobRunner {:maksut-service mock-maksut-service}))

(defn create-awaiting-status [state-data]
  (let [[oid term year] state-data
        id (payment/set-application-fee-required oid term year nil nil)
        reference (str/join "-" [oid term year])]
    (reset! latest-state-id id)
    reference))

(defn create-not-required-status [state-data]
  (let [[oid term year] state-data
        id (payment/set-application-fee-not-required oid term year nil nil)
        reference (str/join "-" [oid term year])]
    (reset! latest-state-id id)
    reference))

(defn- clean! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :kk_application_payment_events
                                          ["kk_application_payment_state_id = ?" id])
                            (jdbc/delete! conn :kk_application_payment_states
                                          ["id = ?" id])))

(def state-with-paid-status    ["1.2.246.562.24.123456" "kausi_s" 2025])
(def state-with-overdue-status ["1.2.246.562.24.654321" "kausi_s" 2025])
(def state-with-active-status  ["1.2.246.562.24.111111" "kausi_s" 2025])
(def state-with-no-status      ["1.2.246.562.24.111222" "kausi_k" 2026])

(def comparison-states
  "Some states that should hold after each and every job run."
  [["1.2.246.562.24.333333"  "kausi_s" 2025 (:not-required payment/all-states) 1 1]
   ["1.2.246.562.24.444444"  "kausi_s" 2025 (:overdue payment/all-states) 1 1]
   ["1.2.246.562.24.555555"  "kausi_s" 2025 (:paid payment/all-states) 1 1]
   ["1.2.246.562.24.5123456" "kausi_s" 2025 (:paid payment/all-states) 1 2]])

(defn check-state-and-event [oid term year state-name state-count event-count]
  (let [states (payment/get-raw-payment-states [oid] term year)
        state-data (first states)
        events (payment/get-raw-payment-events [(:id state-data)])]
    (should= state-count (count states))
    (should= event-count (count events))
    (should= state-name (:state state-data))))

(defn check-comparison-states []
  (doseq [[oid term year state-name state-count event-count] comparison-states]
    (check-state-and-event oid term year state-name state-count event-count)))

(describe "kk-application-payment-maksut-poller-job"
          (tags :unit)

          ; Add some other states that are checked during every test
          (before
            ; The first three ones should always stay as is
            (let [not-required-id (payment/set-application-fee-not-required "1.2.246.562.24.333333" "kausi_s" 2025 nil nil)
                  overdue-id (payment/set-application-fee-overdue "1.2.246.562.24.444444" "kausi_s" 2025 nil nil)
                  paid-id (payment/set-application-fee-paid "1.2.246.562.24.555555" "kausi_s" 2025 nil nil)
                  ; The fourth one should change to paid after each job run because it's in awaiting state
                  ; and mock maksut returns paid for the oid pattern.
                  should-be-paid-id (payment/set-application-fee-required "1.2.246.562.24.5123456" "kausi_s" 2025 nil nil)]
              (reset! comparison-state-ids [not-required-id overdue-id paid-id should-be-paid-id])))

          (after
            (doseq [id @comparison-state-ids]
              (clean! id))
            (clean! @latest-state-id))

          (it "does not change status if payment not in awaiting state"
              (let [[oid term year] state-with-no-status
                    _ (create-not-required-status [oid term year])
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-event oid term year (:not-required payment/all-states) 1 1)
                (check-comparison-states)))

          (it "does not change status if yet active payment returned from maksut"
              (let [[oid term year] state-with-active-status
                    _ (create-awaiting-status [oid term year])
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-event oid term year (:awaiting payment/all-states) 1 1)
                (check-comparison-states)))

          (it "changes the status of paid payment as paid"
              (let [[oid term year] state-with-paid-status
                    _ (create-awaiting-status [oid term year])
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-event oid term year (:paid payment/all-states) 1 2)
                (check-comparison-states)))

          (it "changes the status of overdue payment as overdue"
              (let [[oid term year] state-with-overdue-status
                    _ (create-awaiting-status [oid term year])
                    _ (poller-job/poll-kk-payments-handler {} runner)]
                (check-state-and-event oid term year (:overdue payment/all-states) 1 2)
                (check-comparison-states))))
