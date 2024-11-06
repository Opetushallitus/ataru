(ns ataru.kk-application-payment.kk-application-payment-status-updater-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.job :as job]
            [ataru.config.url-helper :as url-helper]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.translations.translation-util :as translations]
            [clojure.java.jdbc :as jdbc]
            [selmer.parser :as selmer]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.kk-application-payment.kk-application-payment-email-job :as email-job]))

(declare conn)

(defn- payment-link-email-template-filename
  [lang]
  (str "templates/email_kk_payment_link_" (name lang) ".html"))

(defn- payment-link-email [application email payment-url]
  (let [lang             (-> application (get :lang "fi") keyword)
        template-name    (payment-link-email-template-filename lang)
        translations     (translations/get-translations lang)
        emails           [email]
        subject          (:email-kk-payment-link-subject translations)
        body             (selmer/render-file template-name
                                             (merge {:payment-url payment-url}
                                                    translations))]
    (when (not-empty emails)
      {:from       "no-reply@opintopolku.fi"
       :recipients emails
       :body       body
       :subject    subject})))

(defn- start-payment-link-email-job [job-runner application email payment-url]
  (let [application-key (:key application)
        job-type        (:type email-job/job-definition)
        mail-content    (payment-link-email application email payment-url)]
    (if mail-content
      (let [job-id (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                             (job/start-job job-runner conn job-type mail-content))]
        (log/info (str "Created kk application payment link email job " job-id " for application " application-key)))
      (log/warn "Creating kk application payment link mail to application" application-key "failed"))))

(defn- create-payment-and-send-email
  [job-runner maksut-service payment-data person]
  (let [application-key (:application-key payment-data)
        application     (application-store/get-latest-application-by-key application-key)
        lang            (-> application (get :lang "fi") keyword)
        email           (:email payment-data)
        invoice-data    (payment/generate-invoicing-data payment-data person)
        invoice         (maksut-protocol/create-kk-application-payment-lasku maksut-service invoice-data)
        url             (url-helper/resolve-url :maksut-service.hakija-get-by-secret (:secret invoice) lang)]
    (when invoice
      (log/info "Kk application payment invoice details" invoice)
      (log/info "Store kk application payment maksut secret for reference " (:reference invoice))
      (payment/set-maksut-secret application-key (:secret invoice))
      (log/info "Generate kk application payment maksut-link for email" email "URL" url)
      (start-payment-link-email-job job-runner application email url))))

(defn update-kk-payment-status-for-person-handler
  "Updates payment requirement status for a single (person oid, term, year). Creates payments and
  sends e-mails when necessary. Marking status as paid/overdue is done separately via
  kk-application-payment-maksut-poller-job, never here."
  [{:keys [person_oid term year]}
   {:keys [person-service tarjonta-service koodisto-cache haku-cache maksut-service] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [{:keys [person modified-payments existing-payments]}
          (payment/update-payments-for-person-term-and-year person-service tarjonta-service
                                                            koodisto-cache haku-cache
                                                            person_oid term year)]
      (doseq [payment modified-payments]
        (let [new-state (:state payment)]
          (cond
            ; Freshly awaiting payments need a maksut link created and notification e-mail sent
            (= (:awaiting payment/all-states) new-state)
            (create-payment-and-send-email job-runner maksut-service payment person))))

      ; TODO: reminder e-mails for applications not paid yet 2 days before due date
      (doseq [application-payment existing-payments]
        (let [{:keys [application payment]} application-payment]
          (cond
            (and (= (:awaiting payment/all-states) (:state payment))
                 (nil? (:reminder-sent-at payment))
                 (; due date is in 2 days or less
                   )
                 ; Send reminder and mark sent
                 )))))))

(defn start-update-kk-payment-status-for-person-job
  [job-runner person-oid term year]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                              (job/start-job job-runner
                                             conn
                                             "kk-application-payment-status-update-job"
                                             {:person_oid person-oid :term term :year year}))))

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
  [{:keys [tarjonta-service haku-cache] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [hakus (payment/get-haut-for-update haku-cache tarjonta-service)]
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
