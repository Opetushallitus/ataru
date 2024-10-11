(ns ataru.background-job.maksut-poller-job
  "Polls Maksut-services for paid and overdue invoices, linked to applications."
  (:require [clojure.core.match :refer [match]]
            [ataru.applications.application-service :as application-service]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [clojure.string :refer [ends-with?]]
            [taoensso.timbre :as log]))

(defn poll-maksut [application-service maksut-service apps]
    (let [keys      (map :key apps)
          key-state (into {} (map (fn [{:keys [key state]}] [key state]) apps))
          maksut    (maksut-protocol/list-lasku-statuses maksut-service keys)]

      (log/debug "Received statuses for" (count maksut) "invoices")

      (let [terminal (filter #(some #{(:status %)} '(:paid :overdue)) maksut)
            raw      (map (fn [{:keys [reference status order_id origin]}]
                            (when-let [type (cond
                                            (ends-with? order_id "-1") :processing
                                            (ends-with? order_id "-2") :decision
                                          :else nil)]
                              (when-let [key-match (find key-state reference)]
                                {:reference reference
                                 :maksu-status (name status)
                                 :type type
                                 :app-status (val key-match)
                                 :origin origin})))
                          terminal)
            items    (filter some? raw)]
        (log/debug "Out of which in terminal-state are" (count terminal) "invoices")
        (log/debug (pr-str "Invoices" items))
        (doseq [item items]
          (let [{:keys [reference origin type app-status maksu-status]} item
                toggle   #(application-service/payment-poller-processing-state-change application-service reference %)
                response (match origin
                               "tutu" (match [type app-status maksu-status]
                                             [:processing nil "paid"] (toggle "processing-fee-paid")
                                             [:processing nil "overdue"] (toggle "processing-fee-overdue")
                                             [:processing "unprocessed" "paid"] (toggle "processing-fee-paid")
                                             [:processing "unprocessed" "overdue"] (toggle "processing-fee-overdue")
                                             [:decision   "decision-fee-outstanding" "paid"] (toggle "decision-fee-paid")
                                             [:decision   "decision-fee-outstanding" "overdue"] (toggle "decision-fee-overdue")
                                             :else (log/debug "Invalid application&payment state combo, will not do anything" item))
                               "astu" (match [type app-status maksu-status]
                                             [:decision   "decision-fee-outstanding" "paid"] (toggle "processed")
                                             [:decision   "decision-fee-outstanding" "overdue"] (toggle "decision-fee-overdue")
                                             :else (log/debug "Invalid application&payment state combo, will not do anything" item))
                               :else (log/debug "Invalid origin, will not do anything" item))
                ]
              (when response (log/info "Process result:" response))
            )))))
