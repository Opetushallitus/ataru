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

(def ^:private viewing-forbidden-person-info-field-ids #{:ssn :birth-date})
(def ^:private editing-forbidden-person-info-field-ids #{:nationality :have-finnish-ssn})

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

(defn- uneditable-answers-with-labels-from-new
  [uneditable-answers new-answers old-answers]
  ; the old (persisted) answers do not include labels for all languages, so they are taken from new answers instead
  (map (fn [answer]
         (let [answer-key (:key answer)
               answer-with-key #(= (:key %) answer-key)
               old-answer (->> old-answers
                               (filter answer-with-key)
                               (first))
               new-label  (->> new-answers
                               (filter answer-with-key)
                               (first)
                               :label)]
           (merge old-answer {:label new-label})))
       uneditable-answers))

(defn- merge-uneditable-answers-from-previous
  [old-application new-application]
  (let [new-answers                 (:answers new-application)
        uneditable-or-unviewable    #(or (:cannot-edit %) (:cannot-view %))
        uneditable-answers          (filter uneditable-or-unviewable new-answers)
        editable-answers            (remove uneditable-or-unviewable new-answers)
        merged-answers              (into editable-answers
                                          (uneditable-answers-with-labels-from-new
                                            uneditable-answers
                                            new-answers
                                            (:answers old-application)))]
    (assoc new-application :answers merged-answers)))

(defn- validate-and-store [tarjonta-service application store-fn is-modify?]
  (let [form              (form-store/fetch-by-id (:form application))
        allowed           (allowed-to-apply? tarjonta-service application)
        final-application (if is-modify?
                            (merge-uneditable-answers-from-previous (application-store/get-latest-application-by-secret (:secret application)) application)
                            application)
        validation-result (validator/valid-application? final-application form)]
    (cond
      (not allowed)
      not-allowed-reply

      (not (:passed? validation-result))
      validation-result

      :else
      (store-and-log final-application store-fn))))

(defn- start-submit-jobs [application-id]
  (let [person-service-job-id (job/start-job hakija-jobs/job-definitions
                                             (:type person-integration/job-definition)
                                             {:application-id application-id})]
    (application-email/start-email-submit-confirmation-job application-id)
    (log/info "Started person creation job (to person service) with job id" person-service-job-id)
    {:passed? true :id application-id}))

(defn- flag-uneditable-answers
  [{:keys [answers] :as application} cannot-view-field-ids cannot-edit-field-ids]
  (assoc application
    :answers
    (map
      (fn [answer]
        (let [answer-kw (keyword (:key answer))]
          (cond
            (contains? cannot-view-field-ids answer-kw) (merge answer {:cannot-view true :value nil})
            (contains? cannot-edit-field-ids answer-kw) (merge answer {:cannot-edit true})
            :else answer)))
      answers)))

(defn remove-person-info-module-from-application-answers
  [application]
  (when application
    (flag-uneditable-answers application viewing-forbidden-person-info-field-ids editing-forbidden-person-info-field-ids)))

(defn handle-application-submit [tarjonta-service application]
  (log/info "Application submitted:" application)
  (if (allowed-to-apply? tarjonta-service application)
    (let [{passed? :passed?
           application-id :application-id
           :as result}
          (validate-and-store tarjonta-service application application-store/add-application false)]
      (if passed?
        (start-submit-jobs application-id)
        result))
    not-allowed-reply))

(defn handle-application-edit [tarjonta-service application]
  (log/info "Application edited:" application)
  (let [{passed? :passed?
         application-id :application-id
         :as validation-result}
        (validate-and-store tarjonta-service application application-store/update-application true)]
    (if passed?
      (do
        (application-email/start-email-edit-confirmation-job application-id)
        {:passed? true :id application-id})
      validation-result)))
