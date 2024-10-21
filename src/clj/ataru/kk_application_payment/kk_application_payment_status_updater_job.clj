(ns ataru.kk-application-payment.kk-application-payment-status-updater-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.job :as job]
            [ataru.config.url-helper :as url-helper]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.maksut.maksut-protocol :as maksut-protocol]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

; TODO finalize after big OK-687 changes
(defn- create-payment-and-send-email
  [maksut-service person term year]
  (let [lang (:language person)
        invoice-data (payment/generate-invoicing-data person term year)
        invoice (maksut-protocol/create-kk-application-payment-lasku maksut-service invoice-data)
        url (url-helper/resolve-url :maksut-service.hakija-get-by-secret (:secret invoice) lang)]
    (when invoice
      (log/info "Invoice details" invoice)
      (log/info "Generate maksut-link for email" url))))

(defn update-kk-payment-status-handler
  "Updates payment requirement status for a single (person oid, term, year). Creates payments and
  sends e-mails when necessary. Marking status as paid/overdue is done separately via
  kk-application-payment-maksut-poller-job, never here."
  [{:keys [person_oid term year]}
   {:keys [person-service tarjonta-service koodisto-cache haku-cache maksut-service]}]
  (let [{:keys [person old-state new-state]} (payment/update-payments-for-person-term-and-year person-service tarjonta-service
                                                                                               koodisto-cache haku-cache
                                                                                               person_oid term year)]
    (when-not (= old-state new-state)
      (cond
        (= (:awaiting payment/all-states) new-state) (create-payment-and-send-email maksut-service person term year)
        ; TODO: Which other states need eg. mails sent?
        ))))

(declare conn)                                              ; To keep linter happy
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
      (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                                (job/start-job job-runner
                                               conn
                                               "kk-application-payment-status-update-job"
                                               {:person_oid person-oid :term term :year year})))))

(defn update-kk-payment-status-scheduler-handler
  "Finds active hakus that still need to have kk application payment statuses updated,
   queues updates for persons in hakus."
  [_ {:keys [tarjonta-service haku-cache] :as job-runner}]
  (log/info "Update kk application payment status step starting")
  (let [hakus (payment/get-haut-for-update haku-cache tarjonta-service)]
    (log/info "Found" (count hakus) "hakus for kk application payment status update.")
    (doseq [haku hakus]
      (update-statuses-for-haku haku job-runner))))

(def updater-job-definition {:handler update-kk-payment-status-handler
                             :type    "kk-application-payment-status-update-job"})

(def scheduler-job-definition {:handler  update-kk-payment-status-scheduler-handler
                               :type     "kk-application-payment-status-update-scheduler-job"
                               :schedule "0 6 * * *"})
