(ns ataru.kk-application-payment.kk-application-payment-maksut-poller-job
  "Polls Maksut-services for paid and overdue kk payment invoices, linked to persons."
  (:require [clojure.core.match :refer [match]]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.kk-application-payment.kk-application-payment-store :as store]))

(defn- payment-state-to-key
  "Payment references for hakemusmaksu are like 1.2.246.562.24.123456-kausi_s-2025"
  [{:keys [person-oid start-term start-year]}]
  (str/join "-" [person-oid start-term start-year]))

(defn poll-payments [maksut-service payment-states]
  (let [keys-states (into {}
                          (map (fn [state] [(payment-state-to-key state) state]) payment-states))
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
              {:keys [person-oid start-term start-year]} ataru-data
              response (if (= "kkhakemusmaksu" origin)
                         (match [ataru-status maksut-status]
                                ; TODO can an overdue payment still be paid?
                                ["awaiting-payment" "paid"]
                                (payment/set-application-fee-paid person-oid start-term start-year nil nil)

                                ["awaiting-payment" "overdue"]
                                (payment/set-application-fee-overdue person-oid start-term start-year nil nil)

                                :else (log/debug "Invalid kk payment state combo, will not do anything" item))
                         (log/debug "Invalid origin, will not do anything" item))]
          (when response (log/info "Process result:" response)))))))

(defn poll-kk-payments-handler
  [_ {:keys [maksut-service]}]
  (log/info "Poll kk application payments step starting")
  (try
    (if-let [payment-states (seq (store/get-open-kk-application-payment-states))]
      (do
        (log/debug "Found " (count payment-states) " open kk application payments, checking maksut status")
        (poll-payments maksut-service payment-states))
      (log/debug "No kk application payments in need of maksut polling found"))
    (catch Exception e
      (log/error e "Maksut polling failed"))))

(def job-definition {:handler poll-kk-payments-handler
                     :type    "kk-application-payment-maksut-poller-job"
                     :schedule "*/15 * * * *"})
