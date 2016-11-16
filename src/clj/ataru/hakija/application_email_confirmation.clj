(ns ataru.hakija.application-email-confirmation
  "Application-specific email confirmation init logic"
  (:require
    [taoensso.timbre :as log]
    [selmer.parser :as selmer]
    [ataru.applications.application-store :as application-store]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.hakija.background-jobs.email-job :as email-job]
    [oph.soresu.common.config :refer [config]]
    [clojure.java.io :as io]
    [clojure.edn :as edn])
  (:import [java.io FileReader PushbackReader]))

(defn- get-translations [lang]
  (with-open [reader (-> "translations/email_confirmation.edn"
                         io/resource
                         io/file
                         FileReader.
                         PushbackReader.)]
    (let [data (edn/read reader)]
      (clojure.walk/prewalk (fn [x]
                              (cond-> x
                                (and (map? x)
                                     (contains? x lang))
                                (get lang)))
                            data))))

(defn create-email [application-id]
  (let [application  (application-store/get-application application-id)
        translations (get-translations (keyword (:lang application)))
        subject      (:subject translations)
        recipient    (-> (filter #(= "email" (:key %)) (:answers application)) first :value)
        template     "templates/email_confirmation_template.html"
        service-url  (get-in config [:public-config :applicant :service_url])
        body         (selmer/render-file template (merge {:service-url service-url
                                                          :secret      (:secret application)}
                                                         (dissoc translations :subject)))]
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
