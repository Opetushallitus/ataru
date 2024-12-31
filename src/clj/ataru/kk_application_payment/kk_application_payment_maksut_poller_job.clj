(ns ataru.kk-application-payment.kk-application-payment-maksut-poller-job
  "Polls Maksut-services for paid and overdue kk payment invoices, linked to persons."
  (:require [ataru.background-job.job :as job]
            [ataru.db.db :as db]
            [clojure.core.match :refer [match]]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [ataru.kk-application-payment.kk-application-payment-store :as store]
            [ataru.config.core :refer [config]]
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as updater-job]))

(defn poll-payments
  "Polls maksut service for any open payment statuses, updates kk payment status to paid or overdue when necessary.
   Triggers a confirmation e-mail and full payment status update for person whenever an application is marked paid."
  [job-runner maksut-service payments]
  (let [keys-states (into {}
                          (map (fn [state] [(payment/payment->maksut-reference state) state])
                               payments))
        ; TODO: the amount of open payments may be quite large at a given moment, should we partition the API queries here?
        maksut    (maksut-protocol/list-lasku-statuses maksut-service (keys keys-states))]
    (log/info "Received statuses for" (count maksut) "kk payment invoices")
    (let [terminal (filter #(some #{(:status %)} '(:paid :overdue)) maksut)
          raw      (map (fn [{:keys [reference status origin]}]
                          (when-let [key-match (get keys-states reference)]
                            {:maksut-status (name status)
                             :ataru-status (:state key-match)
                             :ataru-data key-match
                             :origin origin}))
                        terminal)
          items    (filter some? raw)]
      (log/info "Out of which in terminal-state are" (count terminal) "invoices")
      (log/debug (pr-str "Invoices" items))
      (doseq [item items]
        (let [{:keys [origin ataru-status maksut-status ataru-data]} item
              {:keys [application-key]} ataru-data
              awaiting-status (:awaiting payment/all-states)
              response (if (= payment/kk-application-payment-origin origin)
                         (match [ataru-status maksut-status]
                                [awaiting-status "paid"]
                                (do
                                  (log/info "Set kk application payment paid for application key" application-key)
                                  (payment/set-application-fee-paid application-key ataru-data)
                                  (log/info "Starting kk application payment jobs for application key" application-key)
                                  (updater-job/start-update-kk-payment-status-for-application-key-job
                                    job-runner application-key))

                                [awaiting-status "overdue"]
                                (do
                                  (log/info "Set kk application payment overdue for application key" application-key)
                                  (payment/set-application-fee-overdue application-key ataru-data))

                                :else (log/debug "Invalid kk payment state combo, will not do anything" item))
                         (log/debug "Invalid origin, will not do anything" item))]
          (when response (log/info "Process result:" response)))))))

(defn get-payments-and-poll [{:keys [maksut-service] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (try
      ; TODO: we should probably also handle awaiting payments without maksut information (in case something has gone wrong)
      (if-let [payments (seq (store/get-awaiting-kk-application-payments))]
        (do
          (log/info "Found " (count payments) " open kk application payments, checking maksut status")
          (poll-payments job-runner maksut-service payments))
        (log/info "No kk application payments in need of maksut polling found"))
      (catch Exception e
        (log/error e "Maksut polling failed")))))

(defn poll-kk-payments-handler
  [_ job-runner]
  (when (get-in config [:kk-application-payments :maksut-poller-enabled?])
    (log/info "Poll kk application payments step starting")
    (get-payments-and-poll job-runner)
    (log/info "Poll kk application payments step finished")))

(defn start-kk-application-payment-maksut-poller-job
  [job-runner]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-maksut-poller-job"
                                             {}))))


(def job-definition {:handler poll-kk-payments-handler
                     :type    "kk-application-payment-maksut-poller-job"
                     :schedule "*/15 * * * *"})
