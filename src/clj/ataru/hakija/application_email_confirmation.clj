(ns ataru.hakija.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require
   [taoensso.timbre :as log]
   [selmer.parser :as selmer]
   [ataru.applications.application-store :as application-store]
   [ataru.background-job.job :as job]
   [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
   [ataru.hakija.background-jobs.email-job :as email-job]))

(def ^:private subject
  {:fi "Opintopolku.fi - Hakemuksesi on vastaanotettu"
   :sv "Opintopolku.fi - Din ansökan har tagits emot"
   :en "Opintopolku.fi - Your application has been received"})

(defn create-email [application-id]
  (let [application    (application-store/get-application application-id)
        subject        (get subject (-> application :lang keyword))
        recipient      (-> (filter #(= "email" (:key %)) (:answers application)) first :value)
        template       (str "templates/email_confirmation_template_" (or (:lang application) "fi") ".txt")
        body           (selmer/render-file template {})]
    {:from       "no-reply@opintopolku.fi"
     :recipients [recipient]
     :subject    subject
     :body       body}))

(defn start-email-confirmation-job [application-id]
  (let [email  (create-email application-id)
        job-id (job/start-job hakija-jobs/job-definitions
                              (:type email-job/job-definition)
                              (create-email application-id))]
    (log/info "Started email sending job (to viestintäpalvelu) with job id" job-id ":")
    (log/info email)))
