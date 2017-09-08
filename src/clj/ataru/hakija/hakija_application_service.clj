(ns ataru.hakija.hakija-application-service
  (:require
    [taoensso.timbre :as log]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.hakija.application-email-confirmation :as application-email]
    [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.person-service.person-integration :as person-integration]
    [ataru.tarjonta-service.hakuaika :as hakuaika]
    [ataru.tarjonta-service.hakukohde :as hakukohde]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.validator :as validator]
    [ataru.application.review-states :refer [complete-states]]
    [ataru.applications.application-store :as application-store]
    [ataru.hakija.editing-forbidden-fields :refer [viewing-forbidden-person-info-field-ids
                                                   editing-forbidden-person-info-field-ids]]
    [ataru.util :as util]
    [ataru.files.file-store :as file-store]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.virkailija.authentication.virkailija-edit :refer [invalidate-virkailija-credentials virkailija-secret-valid?]]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]))

(defn- store-and-log [application store-fn]
  (let [application-id (store-fn application)]
    (log/info "Stored application with id: " application-id)
    {:passed?        true
     :application-id application-id}))

(defn- get-hakukohteet [application]
  (->> application
       :answers
       (filter #(= (:key %) "hakukohteet"))
       first
       :value))

(defn- allowed-to-apply?
  "If there is a hakukohde the user is applying to, check that hakuaika is on"
  [tarjonta-service application]
  (println "hakukohteet" (filter #(= (:key %) "hakukohteet") (:answers application)))
  (let [hakukohteet (get-hakukohteet application)]
    (if (empty? hakukohteet)
      true ;; plain form, always allowed to apply
      (let [hakukohde         (.get-hakukohde tarjonta-service (first hakukohteet)) ; TODO check apply times for each hakukohde separately?
            haku-oid          (:hakuOid hakukohde)
            haku              (when haku-oid (.get-haku tarjonta-service haku-oid))
            {hakuaika-on :on} (hakuaika/get-hakuaika-info hakukohde haku)]
        hakuaika-on))))

(def not-allowed-reply {:passed? false
                        :failures ["Not allowed to apply (not within hakuaika or review state is in complete states)"]})

(defn- in-complete-state? [application-key]
  (let [state (:state (application-store/get-application-review application-key))]
    (boolean (some #{state} complete-states))))

(defn processing-in-jatkuva-haku? [application-key tarjonta-info]
  (let [state (:state (application-store/get-application-review application-key))]
    (and (= state "processing")
         (:is-jatkuva-haku? (:tarjonta tarjonta-info)))))

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

(defn- flatten-attachment-keys [application]
  (->> (:answers application)
       (filter (comp (partial = "attachment") :fieldType))
       (map :value)
       (flatten)))

(defn- remove-orphan-attachments [new-application old-application]
  (let [new-attachments    (set (flatten-attachment-keys new-application))
        orphan-attachments (->> (flatten-attachment-keys old-application)
                                (filter (comp not (partial contains? new-attachments))))]
    (doseq [attachment-key orphan-attachments]
      (file-store/delete-file (name attachment-key)))
    (log/info (str "Updated application " (:key old-application) ", removed old attachments: " (clojure.string/join ", " orphan-attachments)))))

(defn- valid-virkailija-secret [{:keys [virkailija-secret]}]
  (when (virkailija-edit/virkailija-secret-valid? virkailija-secret)
    virkailija-secret))

(defn- validate-and-store [tarjonta-service application store-fn is-modify?]
  (let [tarjonta-info      (when (:haku application)
                             (tarjonta-parser/parse-tarjonta-info-by-haku tarjonta-service (:haku application)))
        form               (-> application
                               (:form)
                               (form-store/fetch-by-id)
                               (hakija-form-service/inject-hakukohde-component-if-missing)
                               (hakukohde/populate-hakukohde-answer-options tarjonta-info))
        allowed            (allowed-to-apply? tarjonta-service application)
        latest-application (application-store/get-latest-version-of-application-for-edit application)
        final-application  (if is-modify?
                             (merge-uneditable-answers-from-previous latest-application application)
                             application)
        validation-result  (validator/valid-application? final-application form)
        virkailija-secret  (valid-virkailija-secret application)]
    (cond
      (and (not (nil? virkailija-secret))
           (not (virkailija-secret-valid? virkailija-secret)))
      {:passed? false :failures ["Tried to edit application with invalid virkailija secret."]}

      (and (:secret application)
           virkailija-secret)
      {:passed? false :failures ["Tried to edit hakemus with both virkailija and hakija secret."]}

      (and (:haku application)
           (empty? (:hakukohde application)))
      {:passed? false :failures ["Hakukohde must be specified"]}

      (not allowed)
      not-allowed-reply

      (and is-modify?
           (not virkailija-secret)
           (or (in-complete-state? (:key latest-application))
               (processing-in-jatkuva-haku? (:key latest-application) tarjonta-info)))
      not-allowed-reply

      (not (:passed? validation-result))
      validation-result

      :else
      (do
        (remove-orphan-attachments final-application latest-application)
        (store-and-log final-application store-fn)))))

(defn- start-submit-jobs [application-id]
  (let [person-service-job-id (job/start-job hakija-jobs/job-definitions
                                             (:type person-integration/job-definition)
                                             {:application-id application-id})
        attachment-finalizer-job-id (job/start-job hakija-jobs/job-definitions
                                                   (:type attachment-finalizer-job/job-definition)
                                                   {:application-id application-id})]
    (application-email/start-email-submit-confirmation-job application-id)
    (log/info "Started person creation job (to person service) with job id" person-service-job-id)
    (log/info "Started attachment finalizer job (to Liiteri) with job id" attachment-finalizer-job-id)
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
        (validate-and-store tarjonta-service application application-store/update-application true)
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (do
        (if virkailija-secret
          (invalidate-virkailija-credentials virkailija-secret)
          (application-email/start-email-edit-confirmation-job application-id))
        {:passed? true :id application-id})
      validation-result)))

(defn save-application-feedback
  [feedback]
  (log/info "Saving feedback" feedback)
  (application-store/add-application-feedback feedback))
