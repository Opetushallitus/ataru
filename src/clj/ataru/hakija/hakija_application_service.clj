(ns ataru.hakija.hakija-application-service
  (:require
   [taoensso.timbre :as log]
   [ataru.log.audit-log :as audit-log]
   [ataru.background-job.job :as job]
   [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
   [ataru.hakija.application-email-confirmation :as application-email]
   [ataru.person-service.person-integration :as person-integration]
   [ataru.forms.form-store :as form-store]
   [ataru.hakija.validator :as validator]
   [ataru.applications.application-store :as application-store]))

(def ^:private is-ssn? (partial = "ssn"))

(defn- extract-ssn [application]
  (->> (:answers application)
       (filter (comp is-ssn? :key))
       (first)
       :value))

(defn- do-audit-log [application]
  (let [id (extract-ssn application)]
    (audit-log/log {:new       application
                    :id        id
                    :operation audit-log/operation-new})))

(defn- store-and-log [application store-fn]
  (let [application-id (store-fn application)]
    (log/info "Stored application with id: " application-id)
    (do-audit-log application)
    {:passed?        true
     :application-id application-id}))

(defn- validate-and-store [application store-fn]
  (let [form              (form-store/fetch-by-id (:form application))
        validation-result (validator/valid-application? application form)]
    (if (:passed? validation-result)
      (store-and-log application store-fn)
      validation-result)))

(defn- start-submit-jobs [application-id]
  (let [person-service-job-id (job/start-job hakija-jobs/job-definitions
                                             (:type person-integration/job-definition)
                                             {:application-id application-id})]
    (application-email/start-email-submit-confirmation-job application-id)
    (log/info "Started person creation job (to person service) with job id" person-service-job-id)
    {:passed? true :id application-id}))

(defn handle-application-submit [application]
  (log/info "Application submitted:" application)
  (let [{passed? :passed?
         application-id :application-id
         :as validation-result}
        (validate-and-store application application-store/add-application)]
    (if passed?
      (start-submit-jobs application-id)
      validation-result)))

(defn handle-application-edit [application]
  (log/info "Application edited:" application)
  (let [{passed? :passed?
         application-id :application-id
         :as validation-result}
        (validate-and-store application application-store/update-application)]
    (if passed?
      (do
        (application-email/start-email-edit-confirmation-job application-id)
        {:passed? true :id application-id})
      validation-result)))
