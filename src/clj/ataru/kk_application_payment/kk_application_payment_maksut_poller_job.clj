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
            [ataru.kk-application-payment.kk-application-payment-email-job :as email-job]
            [ataru.kk-application-payment.utils :as utils]))

(defn- payment-confirmation-email-params
  [lang]
  {:subject-key :email-kk-payment-confirmation-subject
   :template-path (str "templates/email_kk_payment_confirmation_" (name lang) ".html")})

(defn- start-confirmation-email-job [job-runner application]
  (let [application-key (:key application)
        job-type        (:type email-job/job-definition)
        email           (utils/get-application-email application)
        lang            (utils/get-application-language application)
        params          (payment-confirmation-email-params lang)
        mail-content    (utils/payment-email lang email {} params)]
    (if mail-content
      (let [job-id (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                             (job/start-job job-runner conn job-type mail-content))]
        (log/info (str "Created kk application payment confirmation email job " job-id " for application " application-key)))
      (log/warn "Creating kk application payment confirmation mail to application" application-key "failed"))))

(defn poll-payments [job-runner maksut-service payments]
  (let [keys-states (into {}
                          (map (fn [state] [(payment/payment->maksut-reference state) state])
                               payments))
        ; TODO: the amount of open payments may be quite large at a given moment, should we partition the API queries here?
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
                                (do
                                  (log/info "Set kk application payment paid for application key" application-key)
                                  (payment/set-application-fee-paid application-key ataru-status)
                                  (start-confirmation-email-job job-runner application-key))

                                [awaiting-status "overdue"]
                                (do
                                  (log/info "Set kk application payment overdue for application key" application-key)
                                  (payment/set-application-fee-overdue application-key ataru-status))

                                :else (log/debug "Invalid kk payment state combo, will not do anything" item))
                         (log/debug "Invalid origin, will not do anything" item))]
          (when response (log/info "Process result:" response)))))))

(defn get-payments-and-poll [job-runner maksut-service]
  (when (get-in config [:kk-application-payments :enabled?])
    (try
      ; TODO: we have to also handle awaiting payments without maksut information (in case something has gone wrong)
      (if-let [payments (seq (store/get-awaiting-kk-application-payments))]
        (do
          (log/debug "Found " (count payments) " open kk application payments, checking maksut status")
          (poll-payments job-runner maksut-service payments))
        (log/debug "No kk application payments in need of maksut polling found"))
      (catch Exception e
        (log/error e "Maksut polling failed")))))

(defn poll-kk-payments-handler
  [_ {:keys [maksut-service] :as job-runner}]
  (when (get-in config [:kk-application-payments :maksut-poller-enabled?])
    (log/info "Poll kk application payments step starting")
    (get-payments-and-poll job-runner maksut-service)
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
