(ns ataru.background-job.maksut-poller-job
  "Polls Maksut-services for paid and overdue invoices, linked to applications."
  (:require [clojure.core.match :refer [match]]
            [ataru.applications.application-service :as application-service]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            ;[ataru.db.db :as db]
            ;[clj-time.core :as time]
            [clojure.string :refer [ends-with?]]
            [taoensso.timbre :as log]
            ;[clj-time.coerce :as coerce]
            ))

;(defqueries "sql/maksut-queries.sql")

(defn poll-maksut [application-service maksut-service apps]
    (let [keys      (map :key apps)
          key-state (into {} (map (fn [{:keys [key state]}] [key state]) apps))
          maksut    (maksut-protocol/list-lasku-statuses maksut-service keys)]

      (log/info "Received statuses for" (count maksut) "invoices")

      (let [terminal (filter #(some #{(:status %)} '("paid" "overdue")) maksut)
            raw      (map (fn [{:keys [reference status order_id]}]
                            (when-let [type (cond
                                            (ends-with? order_id "-1") :processing
                                            (ends-with? order_id "-2") :decision
                                          :else nil)]
                              ;(log/info (pr-str type reference (find key-state reference) (val (find key-state reference)) (= reference (val (find key-state reference)))))
                              (when-let [key-match (find key-state reference)]
                                {:reference reference
                                 :maksu-status status
                                 :type type
                                 :app-status (val key-match)})))
                          terminal)
            items    (filter some? raw)]
        (log/info "Out of which in terminal-state are" (count terminal) "invoices")
        (log/info (pr-str "Invoices" items))
        (doseq [item items]
          (let [{:keys [reference type app-status maksu-status]} item
                ;TODO also pass old app-status to the method so that this change is only done if status has not changed in between, as there is no locking
                toggle   #(application-service/payment-poller-processing-state-change application-service reference %)
                response (match [type app-status maksu-status]
                                [:processing nil "paid"] (toggle "processing-fee-paid")
                                [:processing nil "overdue"] (toggle "processing-fee-overdue")
                                [:processing "unprocessed" "paid"] (toggle "processing-fee-paid")
                                [:processing "unprocessed" "overdue"] (toggle "processing-fee-overdue")
                                [:decision   "decision-fee-outstanding" "paid"] (toggle "decision-fee-paid")
                                [:decision   "decision-fee-outstanding" "overdue"] (toggle "decision-fee-overdue")
                                :else (log/info "Invalid application&payment state combo, will not do anything" item))
                ]
              (log/info "Process result:" response)
            )))

        ;(prn "items" items)
      )
      ;(log/info "No applications in need of Maksut-polling found")
    )

;(defn check-maksut-status-step [state _]
;  (log/info "Checking Maksut statuses for following applications " (str keys))
;  (poll-maksut)
;  (log/info "Successfully polled Maksut")
;  (let [now (time/now)
;        next-activation (time/plus (time/with-time-at-start-of-day now)
;                                   (time/minutes 1)
;                                   ;(time/hours 1)
;                                   )]
;    {:transition      {:id :to-next :step :initial}
;     :updated-state   {:last-run-long (coerce/to-long now)}
;     :next-activation next-activation}))
;
;(def job-definition {:steps {:initial check-maksut-status-step}
;                     :type  (str (ns-name *ns*))})
