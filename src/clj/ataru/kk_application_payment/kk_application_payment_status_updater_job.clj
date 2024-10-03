(ns ataru.kk-application-payment.kk-application-payment-status-updater-job
  (:require [ataru.applications.application-store :as application-store]
            [ataru.background-job.job :as job]
            [ataru.db.db :as db]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(defn update-kk-payment-status-handler
  "Updates payment requirement status for a single (person oid, term, year). Creates payments and
  sends e-mails when necessary. Marking status as paid/overdue is done separately via
  kk-application-payment-maksut-poller-job, never here."
  [{:keys [person_oid term year]}
   {:keys [person-service tarjonta-service koodisto-cache haku-cache]}]
  (let [{:keys [old-state new-state]} (payment/update-payment-status person-service tarjonta-service
                                                                     koodisto-cache haku-cache
                                                                     person_oid term year nil)]
    (when-not (= old-state new-state)
      (cond
        (= "awaiting-payment" new-state) ()                 ; TODO: Trigger payment + payment email
        (= "payment-ok-via-linked-oid" new-state) ()        ; TODO: Do we need any further actions here?
        (= "payment-not-required" new-state) ()             ; TODO: Do we need any further actions here?
        ))))

(declare conn)                                              ; To keep linter happy
(defn update-statuses-for-haku
  "Queues kk payment status updates for all persons with active applications in haku."
  [haku job-runner]
  (log/info "Processing haku" (:oid haku) "kk application payment statuses.")
  (let [haku-oid    (:oid haku)
        term        (:alkamiskausi haku)
        year        (:alkamisvuosi haku)
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

(def scheduler-job-definition {:handler update-kk-payment-status-scheduler-handler
                               :type    "kk-application-payment-status-update-scheduler-job"
                               :schedule "0 6 * * *"})
