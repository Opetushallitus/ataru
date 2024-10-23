(ns ataru.kk-application-payment.kk-application-payment-maksut-poller-job
  "Polls Maksut-services for paid and overdue kk payment invoices, linked to persons."
  (:require [clojure.core.match :refer [match]]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [taoensso.timbre :as log]
            [ataru.kk-application-payment.kk-application-payment-store :as store]))

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

(defn poll-kk-payments-handler
  [_ {:keys [maksut-service]}]
  (log/info "Poll kk application payments step starting")
  (try
    (if-let [payments (seq (store/get-awaiting-kk-application-payments))]
      (do
        (log/debug "Found " (count payments) " open kk application payments, checking maksut status")
        (poll-payments maksut-service payments))
      (log/debug "No kk application payments in need of maksut polling found"))
    (catch Exception e
      (log/error e "Maksut polling failed"))))

(def job-definition {:handler poll-kk-payments-handler
                     :type    "kk-application-payment-maksut-poller-job"
                     :schedule "*/15 * * * *"})
