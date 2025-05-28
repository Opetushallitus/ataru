(ns ataru.background-job.maksut-poller-job
  "Polls Maksut-services for paid and overdue invoices, linked to applications."
  (:require [clojure.core.match :refer [match]]
            [ataru.applications.application-service :as application-service]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
            [clojure.string :refer [ends-with?]]
            [taoensso.timbre :as log]))

(defn poll-maksut [application-service maksut-service job-runner apps]
    (let [keys      (map :key apps)
          key-state (into {} (map (fn [{:keys [key state]}] [key state]) apps))
          maksut    (maksut-protocol/list-lasku-statuses maksut-service keys)]
      (log/info "Received statuses for" (count maksut) "invoices")

      (let [terminal (filter #(some #{(:status %)} '(:paid :overdue)) maksut)
            raw      (map (fn [{:keys [reference status order_id origin]}]
                            (if-let [type (cond
                                              (= origin "astu") :decision
                                              (ends-with? order_id "-1") :processing
                                              (ends-with? order_id "-2") :decision
                                              :else nil)]
                              (if-let [key-match (find key-state reference)]
                                {:reference reference
                                 :maksu-status (name status)
                                 :type type
                                 :app-status (val key-match)
                                 :origin origin}
                                (log/warn "Key-match not found" reference order_id origin status))
                              (log/warn "Unknown type" reference order_id origin status)))
                          terminal)
            items    (filter some? raw)]
        (log/info "Out of which in terminal-state are" (count terminal) "invoices")
        (log/info (pr-str "Invoices" items))
        (when (not= (count terminal) (count items))
          (log/warn "Terminal invoices" terminal))
        (doseq [item items]
          (let [{:keys [reference origin type app-status maksu-status]} item
                toggle   #(application-service/payment-poller-processing-state-change application-service reference %)
                response (match origin
                               "tutu" (match [type app-status maksu-status]
                                             [:processing nil "paid"] (do
                                                                        (toggle "processing-fee-paid")
                                                                        (tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-send-job
                                                                          job-runner
                                                                          reference))
                                             [:processing nil "overdue"] (toggle "processing-fee-overdue")
                                             [:processing "unprocessed" "paid"] (do (toggle "processing-fee-paid")
                                                                                  (tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-send-job
                                                                                    job-runner
                                                                                    reference))
                                             [:processing "unprocessed" "overdue"] (toggle "processing-fee-overdue")
                                             [:decision   "decision-fee-outstanding" "paid"] (toggle "decision-fee-paid")
                                             [:decision   "decision-fee-outstanding" "overdue"] (toggle "decision-fee-overdue")
                                             :else (log/warn "Invalid application&payment state combo, will not do anything" item))
                               "astu" (match [type app-status maksu-status]
                                             [:decision "decision-fee-outstanding" "paid"] (toggle "processed")
                                             [:decision "decision-fee-outstanding" "overdue"] (toggle "decision-fee-overdue")
                                             :else (log/warn "Invalid application&payment state combo, will not do anything" item))
                               :else (log/warn "Not a tutu or astu invoice, will not do anything" item))]
              (when response (log/info "Process result:" response))
            )))))
