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

(defn- get-raw-payment-data
  [keys-states {:keys [reference status origin]}]
  (when-let [key-match (get keys-states reference)]
    {:maksut-status       (name status)
     :ataru-status        (:state key-match)
     :ataru-maksut-secret (:maksut-secret key-match)
     :ataru-data          key-match
     :origin              origin}))

(defn- handle-terminal-payments
  "Updates kk payment status to paid or overdue when necessary"
  [job-runner maksut keys-states]
  (let [terminal       (filter #(some #{(:status %)} '(:paid :overdue)) maksut)
        raw-terminal   (map (partial get-raw-payment-data keys-states) terminal)
        items-terminal (filter some? raw-terminal)]
    (log/info "Out of which in terminal state are" (count terminal) "invoices")
    (log/debug (pr-str "Invoices" items-terminal))
    (doseq [item items-terminal]
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
        (when response (log/info "Process result:" response))))))

(defn- handle-active-payments
  "Updates missing secrets for awaiting payments"
  [{:keys [maksut-service]} maksut keys-states]
  (let [active                (filter #(some #{(:status %)} '(:active)) maksut)
        raw-active            (map (partial get-raw-payment-data keys-states) active)
        items-active          (filter some? raw-active)
        items-missing-secrets (remove #(some? (:ataru-maksut-secret %)) items-active)]
    (log/info "kk-application-payment invoices missing secrets:" (count items-missing-secrets))
    (doseq [item items-missing-secrets]
      (let [application-key (get-in item [:ataru-data :application-key])
            invoice (first (maksut-protocol/list-laskut-by-application-key
                            maksut-service application-key))
            maksut-secret (:secret invoice)]
        (if maksut-secret
          (payment/set-maksut-secret application-key maksut-secret)
          (log/error "No maksut secret found for application key" application-key))))))

(defn poll-payments
  "- Polls maksut service for any open payment statuses
   - Updates kk payment status to paid or overdue when necessary
   - Triggers a full payment status update for person whenever an application is marked paid
   - Updates missing secrets for awaiting payments"
  [job-runner maksut-service payments]
  (let [keys-states (into {}
                          (map (fn [state] [(payment/payment->maksut-reference state) state])
                               payments))
        maksut    (maksut-protocol/list-lasku-statuses maksut-service (keys keys-states))]
    (log/info "Received statuses for" (count maksut) "kk payment invoices")
    (handle-terminal-payments job-runner maksut keys-states)
    (handle-active-payments job-runner maksut keys-states )))

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
