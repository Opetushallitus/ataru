(ns ataru.hakija.hakija-application-service
  (:require
   [taoensso.timbre :as log]
   [ataru.background-job.job :as job]
   [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
   [ataru.hakija.application-email-confirmation :as application-email]
   [ataru.person-service.person-integration :as person-integration]
   [ataru.tarjonta-service.hakuaika :as hakuaika]
   [ataru.forms.form-store :as form-store]
   [ataru.hakija.validator :as validator]
   [ataru.applications.application-store :as application-store]))

(defn- store-and-log [application store-fn]
  (let [application-id (store-fn application)]
    (log/info "Stored application with id: " application-id)
    {:passed?        true
     :application-id application-id}))

(defn- allowed-to-apply?
  "If there is a hakukohde the user is applying to, check that hakuaika is on"
  [tarjonta-service application]
  (if (not (:hakukohde application))
    true ;; plain form, always allowed to apply
    (let [hakukohde         (.get-hakukohde tarjonta-service (:hakukohde application))
          haku-oid          (:hakuOid hakukohde)
          haku              (when haku-oid (.get-haku tarjonta-service haku-oid))
          {hakuaika-on :on} (hakuaika/get-hakuaika-info hakukohde haku)]
      hakuaika-on)))

(def not-allowed-reply {:passed? false :failures ["Not allowed to apply (probably hakuaika is not on)"]})

(defn- validate-and-store [tarjonta-service application store-fn]
  (let [form              (form-store/fetch-by-id (:form application))
        allowed           (allowed-to-apply? tarjonta-service application)
        validation-result (validator/valid-application? application form)]
    (cond
      (not (:passed? validation-result))
      validation-result

      (not allowed)
      {:passed? false :failures ["Not allowed to apply (probably hakuaika is not on)"]}

      :else
      (store-and-log application store-fn))))

(defn- start-submit-jobs [application-id]
  (let [person-service-job-id (job/start-job hakija-jobs/job-definitions
                                             (:type person-integration/job-definition)
                                             {:application-id application-id})]
    (application-email/start-email-submit-confirmation-job application-id)
    (log/info "Started person creation job (to person service) with job id" person-service-job-id)
    {:passed? true :id application-id}))

(defn handle-application-submit [tarjonta-service application]
  (log/info "Application submitted:" application)
  (if (allowed-to-apply? tarjonta-service application)
    (let [{passed? :passed?
           application-id :application-id
           :as result}
          (validate-and-store tarjonta-service application application-store/add-application)]
      (if passed?
        (start-submit-jobs application-id)
        result))
    not-allowed-reply))

(defn handle-application-edit [tarjonta-service application]
  (log/info "Application edited:" application)
  (let [{passed? :passed?
         application-id :application-id
         :as validation-result}
        (validate-and-store tarjonta-service application application-store/update-application)]
    (if passed?
      (do
        (application-email/start-email-edit-confirmation-job application-id)
        {:passed? true :id application-id})
      validation-result)))
