(ns ataru.kk-application-payment.kk-application-payment-status-updater-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.job :as job]
            [ataru.config.url-helper :as url-helper]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [clj-time.core :as time]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.kk-application-payment.kk-application-payment-email-job :as email-job]
            [ataru.kk-application-payment.utils :as utils]))

(def remind-days-before 2)

(defn- local-date-today []
  (let [time-local (time/to-time-zone (time/now) (time/time-zone-for-id "Europe/Helsinki"))]
    (time/local-date (time/year time-local) (time/month time-local) (time/day time-local))))

(defn- payment-reminder-email-params
  [lang]
  {:subject-key :email-kk-payment-reminder-subject
   :template-path (str "templates/email_kk_payment_reminder_" (name lang) ".html")})

(defn- payment-link-email-params
  [lang]
  {:subject-key :email-kk-payment-link-subject
   :template-path (str "templates/email_kk_payment_link_" (name lang) ".html")})

(defn- start-payment-email-job [job-runner application email-address lang payment-url params-fn type-str]
  (let [application-key (:key application)
        job-type        (:type email-job/job-definition)
        mail-content    (utils/payment-email lang email-address {:payment-url payment-url} (params-fn lang))]
    (if mail-content
      (let [job-id (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                             (job/start-job job-runner conn job-type mail-content))]
        (log/info "Created kk application payment" type-str "email job" job-id "for application" application-key))
      (log/warn "Creating kk application payment" type-str "mail to application" application-key "failed"))))

(defn- create-payment-and-send-email
  [job-runner maksut-service payment-data]
  (let [application-key (:application-key payment-data)
        application     (application-store/get-latest-application-by-key application-key)
        lang            (utils/get-application-language application)
        email-address   (utils/get-application-email application)
        invoice-data    (payment/generate-invoicing-data payment-data application)
        invoice         (maksut-protocol/create-kk-application-payment-lasku maksut-service invoice-data)
        url             (url-helper/resolve-url :maksut-service.hakija-get-by-secret (:secret invoice) (name lang))]
    (when invoice
      (log/info "Kk application payment invoice details" invoice)
      (log/info "Store kk application payment maksut secret for reference " (:reference invoice))
      (payment/set-maksut-secret application-key (:secret invoice))
      (log/info "Generate kk application payment maksut-link for email" email-address
                "URL" url "application-key" application-key)
      (start-payment-email-job job-runner application email-address lang url payment-link-email-params "maksut-link"))))

(defn- send-reminder-email-and-mark-sent
  [job-runner payment-data application]
  (let [application-key (:application-key payment-data)
        lang            (utils/get-application-language application)
        email-address   (utils/get-application-email application)
        url             (url-helper/resolve-url :maksut-service.hakija-get-by-secret
                                                (:maksut-secret payment-data) (name lang))]
    (log/info "Generate kk application payment reminder for email" email-address
              "URL" url "application key" application-key)
    (start-payment-email-job job-runner application email-address lang url payment-reminder-email-params "reminder")
    (payment/mark-reminder-sent application-key)))

(defn needs-reminder-sent?
  [payment]
  (when (and (:due-date payment) (:maksut-secret payment) (nil? (:reminder-sent-at payment)))
    (let [now           (local-date-today)
          due-at        (payment/parse-due-date (:due-date payment))
          remind-at     (time/minus due-at (time/days remind-days-before))
          remind-at-met (or (time/after? now remind-at)
                            (time/equal? now remind-at))]
      (and (= (:awaiting payment/all-states) (:state payment))
           remind-at-met))))

(defn- resolve-term-data
  [tarjonta-service person-oid term year application-id application-key]
  (if (and person-oid term year)
    [person-oid term year]
    (payment/get-valid-payment-info-for-application-id-or-key
      tarjonta-service application-id application-key)))

(defn update-kk-payment-status-for-person-handler
  "Updates payment requirement status for a single (person oid, term, year) either directly or
  via an application id/key. Creates payments and sends e-mails when necessary. Marking status as paid/overdue
  is done separately via kk-application-payment-maksut-poller-job, never here."
  [{:keys [person_oid term year application_id application_key]}
   {:keys [person-service tarjonta-service koodisto-cache get-haut-cache maksut-service] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [[person-oid application-term application-year]
          (resolve-term-data tarjonta-service person_oid term year application_id application_key)]
      (if (and person-oid application-term application-year)
        (let [{:keys [modified-payments existing-payments]}
              (payment/update-payments-for-person-term-and-year person-service tarjonta-service
                                                                koodisto-cache get-haut-cache
                                                                person-oid application-term application-year)]
          (log/info "Update kk payment status hander for" person-oid application-term application-year)
          (doseq [payment modified-payments]
            (let [new-state (:state payment)]
              (cond
                (= (:awaiting payment/all-states) new-state)
                (create-payment-and-send-email job-runner maksut-service payment))))

          (doseq [application-payment existing-payments]
            (let [{:keys [application payment]} application-payment]
              (cond
                ; TODO: Check existing payments that were not updated:

                (needs-reminder-sent? payment)
                (send-reminder-email-and-mark-sent job-runner payment application)))))
        (log/debug "Application id" application_id "not in haku with kk application payments")))))

(defn start-update-kk-payment-status-for-person-job
  [job-runner person-oid term year]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-person-status-update-job"
                                             {:person_oid person-oid :term term :year year}))))

(defn start-update-kk-payment-status-for-application-key-job
  [job-runner application-key]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-person-status-update-job"
                                             {:application_key application-key}))))

(defn start-update-kk-payment-status-for-application-id-job
  [job-runner application-id]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-person-status-update-job"
                                             {:application_id application-id}))))

(defn start-update-kk-payment-status-for-all-job
  [job-runner]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-status-update-scheduler-job"
                                             {}))))

; TODO: we might be updating status of a single person multiple times if they have applications in multiple hakus.
(defn update-statuses-for-haku
  "Queues kk payment status updates for all persons with active applications in haku."
  [haku job-runner]
  (log/info "Processing haku" (:oid haku) "kk application payment statuses.")
  (let [haku-oid (:oid haku)
        term (:alkamiskausi haku)
        year (:alkamisvuosi haku)
        person-oids (application-store/get-application-person-oids-for-haku haku-oid)]
    (log/info "Found" (count person-oids) "oids for haku" haku-oid "- updating kk application payment statuses.")
    (doseq [person-oid person-oids]
      (start-update-kk-payment-status-for-person-job job-runner person-oid term year))))

(defn get-hakus-and-update
  "Finds active hakus that still need to have kk application payment statuses updated,
   queues updates for persons in hakus."
  [{:keys [tarjonta-service get-haut-cache] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [hakus (payment/get-haut-for-update get-haut-cache tarjonta-service)]
      (log/info "Found" (count hakus) "hakus for kk application payment status update.")
      (doseq [haku hakus]
        (update-statuses-for-haku haku job-runner)))))

(defn update-kk-payment-status-for-all-handler
  [_ job-runner]
  (when (get-in config [:kk-application-payments :status-updater-enabled?])
    (log/info "Update kk application payment status step starting")
    (get-hakus-and-update job-runner)
    (log/info "Update kk application payment status step finished")))

(def updater-job-definition {:handler update-kk-payment-status-for-person-handler
                             :type    "kk-application-payment-person-status-update-job"})

(def scheduler-job-definition {:handler  update-kk-payment-status-for-all-handler
                               :type     "kk-application-payment-status-update-scheduler-job"
                               :schedule "0 6 * * *"})
