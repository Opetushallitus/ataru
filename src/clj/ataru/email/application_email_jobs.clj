(ns ataru.email.application-email-jobs
  "Application-specific email confirmation init logic"
  (:require [ataru.background-job.email-job :as email-job]
            [ataru.background-job.job :as job]
            [ataru.db.db :as db]
            [ataru.email.email-store :as email-store]
            [clojure.java.jdbc :as jdbc]
            [clojure.set]
            [taoensso.timbre :as log]
            [ataru.email.application-email :as application-email]))

(defn ->safe-html
  [content]
  (application-email/->safe-html content))

(defn get-email-templates
  [form-key form-allows-ht?]
  (application-email/get-email-templates form-key form-allows-ht?))

(defn preview-submit-emails [previews form-allows-ht]
  (map
   #(let [lang           (:lang %)
          subject        (:subject %)
          content        (:content %)
          content-ending (:content-ending %)
          signature      (:signature %)]
      (application-email/preview-submit-email lang subject content content-ending signature form-allows-ht)) previews))

(defn start-email-job [job-runner email]
  (let [job-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                 (job/start-job job-runner
                                connection
                                (:type email-job/job-definition)
                                email))]
    (log/info "Started application confirmation email job (to viestinvälityspalvelu) with job id" job-id ":")
    (log/info email)))

(defn start-email-submit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id payment-url]
  (dorun
    (for [email (application-email/create-submit-email koodisto-cache tarjonta-service
                  organization-service
                  ohjausparametrit-service
                  application-id
                  true
                  payment-url)]
        (start-email-job job-runner email))))

(defn start-email-edit-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (dorun
    (for [email (application-email/create-edit-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                       application-id
                       true)]
           (start-email-job job-runner email))))

(defn start-email-refresh-secret-confirmation-job
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (dorun
    (for [email (application-email/create-refresh-secret-email koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                  application-id)]
      (start-email-job job-runner email))))

(defn start-decision-email-job
  [job-runner email-params]
  (log/info "start-decision-email-job" (:application-id email-params) (:payment-url email-params))
  (dorun
    (for [email (application-email/create-decision-email email-params)]
      (do
        (log/info "Before email job" email)
        (start-email-job job-runner email)))))

(defn store-email-templates
  [form-key session templates form-allows-ht?]
  (let [stored-templates (mapv #(email-store/create-or-update-email-template
                                  form-key
                                  (:lang %)
                                  (-> session :identity :oid)
                                  (:subject %)
                                  (:content %)
                                  (:content-ending %)
                                  (:signature %))
                           templates)]
    (map #(application-email/preview-submit-email (:lang %) (:subject %) (:content %) (:content_ending %) (:signature %) form-allows-ht?) stored-templates)))
