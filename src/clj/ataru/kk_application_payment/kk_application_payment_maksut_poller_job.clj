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
            [ataru.config.core :refer [config]]))

(defn poll-payments [maksut-service payments]
  (let [keys-states (into {}
                          (map (fn [state] [(payment/payment->maksut-reference state) state])
                               payments))
        maksut    (maksut-protocol/list-lasku-statuses maksut-service (keys keys-states))]
    (log/debug "Received statuses for" (count maksut) "kk payment invoices")
    (let [terminal (filter #(some #{(:status %)} '(:paid :overdue)) maksut)
          raw      (map (fn [{:keys [reference status origin]}]
                          (when-let [key-match (get keys-states reference)]
                            {:maksut-status (name status)
                             :ataru-status (:state key-match)
                             :ataru-data key-match
                             :origin origin}))
                        terminal)
          items    (filter some? raw)]
      (log/debug "Out of which in terminal-state are" (count terminal) "invoices")
      (log/debug (pr-str "Invoices" items))
      (doseq [item items]
        (let [{:keys [origin ataru-status maksut-status ataru-data]} item
              {:keys [application-key]} ataru-data
              awaiting-status (:awaiting payment/all-states)
              response (if (= payment/kk-application-payment-origin origin)
                         (match [ataru-status maksut-status]
                                [awaiting-status "paid"]
                                (payment/set-application-fee-paid application-key ataru-status)

                                [awaiting-status "overdue"]
                                (payment/set-application-fee-overdue application-key ataru-status)

                                :else (log/debug "Invalid kk payment state combo, will not do anything" item))
                         (log/debug "Invalid origin, will not do anything" item))]
          (when response (log/info "Process result:" response)))))))

(defn get-payments-and-poll [maksut-service]
  (when (get-in config [:kk-application-payments :enabled?])
    (try
      (if-let [payments (seq (store/get-awaiting-kk-application-payments))]
        (do
          (log/debug "Found " (count payments) " open kk application payments, checking maksut status")
          (poll-payments maksut-service payments))
        (log/debug "No kk application payments in need of maksut polling found"))
      (catch Exception e
        (log/error e "Maksut polling failed")))))

(defn poll-kk-payments-handler
  [_ {:keys [maksut-service]}]
  (when (get-in config [:kk-application-payments :maksut-poller-enabled?])
    (log/info "Poll kk application payments step starting")
    (get-payments-and-poll maksut-service)
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
