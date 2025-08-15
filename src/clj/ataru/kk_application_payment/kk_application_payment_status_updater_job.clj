(ns ataru.kk-application-payment.kk-application-payment-status-updater-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.job :as job]
            [ataru.config.url-helper :as url-helper]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [ataru.tarjonta-service.tarjonta-protocol :as tarjonta]
            [clj-time.core :as time]
            [clj-time.format :as f]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [ataru.config.core :refer [config]]
            [ataru.email.application-email :as application-email]
            [ataru.kk-application-payment.kk-application-payment-email-job :as email-job]
            [ataru.kk-application-payment.utils :as utils])
  (:import java.util.Locale))

(def tz (time/time-zone-for-id "Europe/Helsinki"))

(def formatters
  {:fi (-> (f/formatters :rfc822 tz)
           (f/with-locale (Locale. "fi"))
           (f/with-zone tz))
   :sv (-> (f/formatters :rfc822 tz)
           (f/with-locale (Locale. "sv"))
           (f/with-zone tz))
   :en (-> (f/formatters :rfc822 tz)
           (f/with-locale (Locale. "en"))
           (f/with-zone tz))})

(def remind-days-before 2)

(defn- local-date-today []
  (let [time-local (time/to-time-zone (time/now) (time/time-zone-for-id "Europe/Helsinki"))]
    (time/local-date (time/year time-local) (time/month time-local) (time/day time-local))))

(defn- due-date-to-printable-datetime
  [lang due-date]
  (let [due-at             (payment/parse-due-date due-date)
        due-datetime       (time/date-time (time/year due-at) (time/month due-at) (time/day due-at) 23 59)
        due-datetime-in-tz (time/from-time-zone due-datetime (time/time-zone-for-id "Europe/Helsinki"))
        formatter          (lang formatters)]
    (f/unparse formatter due-datetime-in-tz)))

(defn- payment-reminder-email-params
  [lang suffix]
  {:subject-suffix suffix
   :subject-key :email-kk-payment-reminder-subject
   :template-path (str "templates/email_kk_payment_reminder_" (name lang) ".html")})

(defn- payment-link-email-params
  [lang suffix]
  {:subject-suffix suffix
   :subject-key :email-kk-payment-link-subject
   :template-path (str "templates/email_kk_payment_link_" (name lang) ".html")})

(defn- start-payment-email-job [{:keys [tarjonta-service ohjausparametrit-service koodisto-cache organization-service]
                                 :as job-runner}
                                application secret params-fn type-str]
  (let [application-key   (:key application)
        job-type          (:type email-job/job-definition)
        lang              (utils/get-application-language application)
        email-address     (utils/get-application-email application)
        payment-url       (url-helper/resolve-url :maksut-service.hakija-get-by-secret secret (name lang))
        payment           (first (payment/get-raw-payments [application-key]))
        haku              (tarjonta/get-haku tarjonta-service (:haku application))
        haku-name         (get-in haku [:name lang] (get-in haku [:name :fi]))
        due-date-str      (due-date-to-printable-datetime lang (:due-date payment))
        tarjonta-info     (try
                            (application-email/get-tarjonta-info koodisto-cache tarjonta-service organization-service
                                                                 ohjausparametrit-service application)
                            (catch Exception e
                              (log/warn e "Error getting tarjonta info")))
        organization-oids (application-email/organization-oids tarjonta-info application)
        mail-content      (utils/payment-email lang email-address {:payment-url       payment-url
                                                                   :due-date-time     due-date-str
                                                                   :haku-name         haku-name
                                                                   :person-oid        (:person-oid application)
                                                                   :organization-oids organization-oids
                                                                   :application-key   (:key application)}
                                               (params-fn lang due-date-str))]
    (log/info "Generate kk application payment " type-str " for email" email-address
              "URL" payment-url "application-key" application-key)
    (if mail-content
      (let [job-id (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                     (job/start-job job-runner conn job-type mail-content))]
        (log/info "Created kk application payment" type-str "email job" job-id "for application" application-key))
      (log/warn "Creating kk application payment" type-str "mail to application" application-key "failed"))))

(defn- create-payment-and-send-email
  [{:keys [tarjonta-service] :as job-runner} maksut-service payment-data]
  (let [application-key (:application-key payment-data)
        application     (application-store/get-latest-application-by-key application-key)
        invoice-data    (payment/generate-invoicing-data tarjonta-service payment-data application)
        invoice         (maksut-protocol/create-kk-application-payment-lasku maksut-service invoice-data)]
    (when invoice
      (log/info "Kk application payment invoice details" invoice)
      (log/info "Store kk application payment maksut secret for reference " (:reference invoice))
      (payment/set-maksut-secret application-key (:secret invoice))
      (start-payment-email-job job-runner application (:secret invoice) payment-link-email-params "maksut-link"))))

(defn resend-payment-email [job-runner application-key session]
  (let [application     (application-store/get-latest-application-by-key application-key)
        payment         (first (payment/get-raw-payments [application-key]))]
    (start-payment-email-job job-runner application (:maksut-secret payment) payment-link-email-params "maksut-link")
    (application-store/add-application-event
      {:application-key application-key
       :event-type "kk-application-payment-email-resent"}
      session)))

(defn- send-reminder-email-and-mark-sent
  [job-runner payment-data]
  (let [application-key (:application-key payment-data)
        application     (application-store/get-latest-application-by-key application-key)]
    (log/info "Scheduling kk application payment reminder e-mail for application" application-key)
    (start-payment-email-job job-runner application (:maksut-secret payment-data) payment-reminder-email-params "reminder")
    (payment/mark-reminder-sent application-key)
    (application-store/add-application-event
      {:application-key (:application-key payment-data)
       :event-type "kk-application-payment-reminder-email-sent"}
      nil)))

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
    (if (some? application-id)
      (payment/get-valid-payment-info-for-application-id tarjonta-service application-id)
      (payment/get-valid-payment-info-for-application-key tarjonta-service application-key))))

(defn- needs-tuition-fee?
  [hakukohde]
  (let [codes (->> (:opetuskieli-koodi-urit hakukohde)
                   (map #(first (str/split % #"#")))
                   set)]
    (and (seq codes)
         (not (contains? codes "oppilaitoksenopetuskieli_1"))    ; fi
         (not (contains? codes "oppilaitoksenopetuskieli_2"))    ; sv
         (not (contains? codes "oppilaitoksenopetuskieli_3"))))) ; fi/sv

(defn- mark-tuition-fee-obligated
  "Marks tuition fee (lukuvuosimaksu) obligation for application key for every hakukohde that does not organize
  studies in Finnish and/or Swedish."
  [{:keys [tarjonta-service]} application-key]
  (let [application            (application-store/get-latest-application-by-key application-key)
        hakukohde-oids         (:hakukohde application)
        hakukohteet            (tarjonta/get-hakukohteet
                                 tarjonta-service
                                 (remove nil? hakukohde-oids))
        tuition-hakukohde-oids (remove nil?
                                       (map #(when (needs-tuition-fee? %) (:oid %)) hakukohteet))]
    (doseq [hakukohde-oid tuition-hakukohde-oids]
      (log/info "Marking tuition payment obligation due to kk application fee eligibility for application key"
                application-key "and hakukohde oid" hakukohde-oid)
      (application-store/save-payment-obligation-automatically-changed
        application-key
        hakukohde-oid
        "payment-obligation"
        "obligated"))))

(defn- invalidate-maksut-payments-if-needed
  "Whenever a payment for a term is made, other payment invoices for the person and term
  should be invalidated to avoid accidental double payments."
  [maksut-service modified-payments]
  (let [proxy-state-application-keys (->> modified-payments
                                          (filter #(= (:ok-by-proxy payment/all-states) (:state %)))
                                          (map :application-key))]
    (when (seq proxy-state-application-keys)
      (log/info "Invalidating ok-by-proxy kk payment applications with keys" proxy-state-application-keys)
      (maksut-protocol/invalidate-laskut maksut-service proxy-state-application-keys))))

(defn update-kk-payment-status-for-person-handler
  "Updates payment requirement status for a single (person oid, term, year) either directly or
  via an application id/key. Creates payments and sends e-mails when necessary. Also marks tuition fee obligation
  if necessary. Marking status as paid/overdue is done separately via kk-application-payment-maksut-poller-job,
  never here."
  [{:keys [person_oid term year application_id application_key]}
   {:keys [ohjausparametrit-service person-service tarjonta-service
           koodisto-cache get-haut-cache maksut-service] :as job-runner}]
  (when (get-in config [:kk-application-payments :enabled?])
    (let [[person-oid application-term application-year]
          (resolve-term-data tarjonta-service person_oid term year application_id application_key)]
      (if (and person-oid application-term application-year)
        (let [{:keys [modified-payments existing-payments]}
              (payment/update-payments-for-person-term-and-year ohjausparametrit-service
                                                                person-service tarjonta-service
                                                                koodisto-cache get-haut-cache
                                                                person-oid application-term application-year)]
          (if (or (some? modified-payments) (some? existing-payments))
            (do
              (log/info "Update kk application payment status handler for"
                        person-oid application-term application-year
                        "returned" (count existing-payments) "created or modified payments and"
                        (count modified-payments) "existing payments before creating / modifying.")
              (doseq [payment modified-payments]
                (let [new-state (:state payment)]
                  (cond
                    (= (:awaiting payment/all-states) new-state)
                    (do
                      (create-payment-and-send-email job-runner maksut-service payment)
                      ; If application payment is required, tuition fee will be always required as well.
                      (mark-tuition-fee-obligated job-runner (:application-key payment)))

                    (= (:ok-by-proxy payment/all-states) new-state)
                    (mark-tuition-fee-obligated job-runner (:application-key payment)))))

              (doseq [application-payment existing-payments]
                (let [{:keys [payment]} application-payment]
                  (cond
                    (needs-reminder-sent? payment)
                    (send-reminder-email-and-mark-sent job-runner payment))))

              (invalidate-maksut-payments-if-needed maksut-service modified-payments)
              (log/info "Update kk payment status handler for" person-oid application-term application-year "finished."))

            ; Here we've already established there should be a person with at least one application, but for the first
            ; applications in haku, get-haku-cache may still be refreshing so it doesn't return any applications for
            ; the specific haku. That's a temporary error, and quite short-lived, so let's try again later.
            (throw (ex-info "Could not find or create a payment status for person" {:person-oid person-oid
                                                                                    :term application-term
                                                                                    :year application-year}))))
        (log/warn "Update kk payment status handler not run for params"
                  person_oid term year application_id application_key
                  "because no valid payment info was found.")))))

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

; Called only from the scheduler job.
(defn start-periodical-update-kk-payment-status-for-person-job
  [job-runner person-oid term year]
  (when (get-in config [:kk-application-payments :enabled?])
    (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
      (job/start-job job-runner
                     conn
                     "kk-application-payment-periodical-person-status-update-job"
                     {:person_oid person-oid :term term :year year}))))

; TODO: we might be updating status of a single person multiple times if they have applications in multiple hakus.
(defn update-statuses-for-haku
  "Queues kk payment status updates for all persons with active applications in haku."
  [haku job-runner]
  (log/info "Processing haku" (:oid haku) "kk application payment statuses.")
  (let [haku-oid (:oid haku)
        term (:alkamiskausi haku)
        year (:alkamisvuosi haku)
        person-oids (distinct (application-store/get-application-person-oids-for-haku haku-oid))]
    (log/info "Found" (count person-oids) "distinct oids for haku" haku-oid "- updating kk application payment statuses.")
    (doseq [person-oid person-oids]
      (start-periodical-update-kk-payment-status-for-person-job job-runner person-oid term year))))

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

; This uses the same handler as the person updater job, but is only called from the twice a day scheduler job.
; This way we get separate queues for the twice a day updates so that the person updater job is not blocked by
; the twice a day updates.
(def periodical-updater-job-definition {:handler update-kk-payment-status-for-person-handler
                                        :type    "kk-application-payment-periodical-person-status-update-job"})

(def scheduler-job-definition {:handler  update-kk-payment-status-for-all-handler
                               :type     "kk-application-payment-status-update-scheduler-job"
                               :schedule "0 6/12 * * *"})
