(ns ataru.hakija.hakija-application-service
  (:require
    [taoensso.timbre :as log]
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.hakija.application-email-confirmation :as application-email]
    [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.person-service.person-integration :as person-integration]
    [ataru.tarjonta-service.hakuaika :as hakuaika]
    [ataru.tarjonta-service.hakukohde :as hakukohde]
    [ataru.tarjonta-service.tarjonta-protocol :refer [get-hakukohde get-haku]]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.validator :as validator]
    [ataru.application.review-states :refer [complete-states]]
    [ataru.applications.application-store :as application-store]
    [ataru.hakija.person-info-fields :refer [viewing-forbidden-person-info-field-ids
                                             editing-forbidden-person-info-field-ids
                                             editing-allowed-person-info-field-ids]]
    [ataru.application.field-types :as types]
    [ataru.util :as util]
    [ataru.files.file-store :as file-store]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.virkailija.authentication.virkailija-edit :refer [invalidate-virkailija-credentials virkailija-secret-valid?]]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [ataru.config.core :refer [config]]
    [clj-time.core :as time]
    [clj-time.coerce :as t]
    [ataru.applications.application-service :as application-service]))

(defn- store-and-log [application store-fn]
  (let [application-id (store-fn application)]
    (log/info "Stored application with id: " application-id)
    {:passed?        true
     :id application-id
     :application application}))

(defn in-processing-state-in-jatkuva-haku?
  [application-hakukohde-reviews applied-hakukohteet]
  (and (some #(get-in % [:hakuaika :jatkuva-haku?]) applied-hakukohteet)
       (util/application-in-processing? application-hakukohde-reviews)))

(defn remove-unviewable-answers
  [application form]
  (let [fields-by-key (->> (:content form)
                           util/flatten-form-fields
                           (util/group-by-first :id))]
    (update application :answers
            (partial map (fn [answer]
                           (cond-> answer
                                   (:cannot-view (fields-by-key (:key answer)))
                                   (assoc :value nil :cannot-view true)))))))

(defn- merge-uneditable-answers-from-previous
  [new-application
   old-application
   form]
  (let [fields-by-key      (->> (:content form)
                                util/flatten-form-fields
                                (util/group-by-first :id))
        old-answers-by-key (util/group-by-first :key (:answers old-application))]
    (update new-application :answers
            (partial keep (fn [answer]
                            (if (:cannot-edit (fields-by-key (:key answer)))
                              (when-let [old-answer (old-answers-by-key (:key answer))]
                                (assoc old-answer :label (:label answer)))
                              answer))))))

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

(defn- set-original-value
  [old-values-by-key new-answer]
  (assoc new-answer :original-value (get old-values-by-key (:key new-answer))))

(defn- set-original-values
  [old-application new-application]
  (let [old-values-by-key (into {} (map (juxt :key :value)
                                        (:answers old-application)))]
    (update new-application :answers
      (partial map (partial set-original-value old-values-by-key)))))

(defn- has-applied
  [haku-oid identifier]
  (async/go
    (if (contains? identifier :ssn)
      (:has-applied (application-store/has-ssn-applied haku-oid (:ssn identifier)))
      (:has-applied (application-store/has-email-applied haku-oid (:email identifier))))))

(defn- validate-and-store [tarjonta-service ohjausparametrit-service application store-fn is-modify?]
  (let [tarjonta-info                 (when (:haku application)
                                        (tarjonta-parser/parse-tarjonta-info-by-haku
                                         tarjonta-service
                                         ohjausparametrit-service
                                         (:haku application)))
        haku-oid                      (get-in tarjonta-info [:tarjonta :haku-oid])
        hakukohteet                   (get-in tarjonta-info [:tarjonta :hakukohteet])
        applied-hakukohteet           (filter #(contains? (set (:hakukohde application)) (:oid %))
                                              hakukohteet)
        virkailija-secret             (valid-virkailija-secret application)
        latest-application            (application-store/get-latest-version-of-application-for-edit application)
        form-roles                    (cond-> []
                                        (some? virkailija-secret)
                                        (conj :virkailija)
                                        (nil? virkailija-secret)
                                        (conj :hakija)
                                        (some? (:person-oid latest-application))
                                        (conj :with-henkilo))
        form                          (-> application
                                          (:form)
                                          (form-store/fetch-by-id)
                                          (hakija-form-service/inject-hakukohde-component-if-missing)
                                          (hakukohde/populate-hakukohde-answer-options tarjonta-info)
                                          (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info)
                                          (hakija-form-service/flag-uneditable-and-unviewable-fields
                                           hakukohteet
                                           form-roles))
        application-hakukohde-reviews (some-> latest-application
                                              :key
                                              application-store/get-application-hakukohde-reviews)
        final-application             (if is-modify?
                                        (-> application
                                            (merge-uneditable-answers-from-previous
                                             latest-application
                                             form)
                                            (assoc :person-oid (:person-oid latest-application)))
                                        application)
        validation-result             (validator/valid-application?
                                       has-applied
                                       (set-original-values latest-application final-application)
                                       form)]
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

      (and (not is-modify?)
           (some #(not (:on (:hakuaika %))) applied-hakukohteet))
      {:passed? false :failures ["Application period is not open."]}

      (and is-modify?
           (not virkailija-secret)
           (in-processing-state-in-jatkuva-haku? application-hakukohde-reviews
                                                 applied-hakukohteet))
      {:passed false :failures ["Application is in review state and cannot be modified."]}

      (not (:passed? validation-result))
      validation-result

      :else
      (do
        (remove-orphan-attachments final-application latest-application)
        (store-and-log final-application store-fn)))))

(defn- start-person-creation-job [application-id]
  (log/info "Started person creation job (to person service) with job id"
            (job/start-job hakija-jobs/job-definitions
                           (:type person-integration/job-definition)
                           {:application-id application-id})))

(defn- start-attachment-finalizer-job [application-id]
  (log/info "Started attachment finalizer job (to Liiteri) with job id"
            (job/start-job hakija-jobs/job-definitions
                           (:type attachment-finalizer-job/job-definition)
                           {:application-id application-id})))

(defn- start-submit-jobs [tarjonta-service application-id]
  (application-email/start-email-submit-confirmation-job tarjonta-service
                                                         application-id)
  (start-person-creation-job application-id)
  (start-attachment-finalizer-job application-id))

(defn- start-virkailija-edit-jobs [virkailija-secret application-id application]
  (invalidate-virkailija-credentials virkailija-secret)
  (when (nil? (:person-oid application))
    (start-person-creation-job application-id))
  (start-attachment-finalizer-job application-id))

(defn- start-hakija-edit-jobs [tarjonta-service application-id]
  (application-email/start-email-edit-confirmation-job tarjonta-service
                                                       application-id)
  (start-attachment-finalizer-job application-id))

(defn handle-application-submit [tarjonta-service ohjausparametrit-service application]
  (log/info "Application submitted:" application)
  (let [{:keys [passed? id]
         :as   result}
        (validate-and-store tarjonta-service ohjausparametrit-service application application-store/add-application false)]
    (when passed?
      (start-submit-jobs tarjonta-service id))
    result))

(defn handle-application-edit [tarjonta-service ohjausparametrit-service application]
  (log/info "Application edited:" application)
  (let [{:keys [passed? id application]
         :as   result}
        (validate-and-store tarjonta-service ohjausparametrit-service application application-store/update-application true)
        virkailija-secret (:virkailija-secret application)]
    (when passed?
      (if virkailija-secret
        (start-virkailija-edit-jobs virkailija-secret
                                    id
                                    application)
        (start-hakija-edit-jobs tarjonta-service id)))
    result))

(defn save-application-feedback
  [feedback]
  (log/info "Saving feedback" feedback)
  (application-store/add-application-feedback feedback))

(defn- attachment-metadata->answer [{:keys [fieldType] :as answer}]
  (cond-> answer
          (= fieldType "attachment")
          (update :value (fn [value]
                           (if (and (vector? value)
                                    (not (empty? value))
                                    (every? vector? value))
                             (map file-store/get-metadata value)
                             (file-store/get-metadata value))))))

(defn attachments-metadata->answers [application]
  (update application :answers (partial map attachment-metadata->answer)))

(defn get-latest-application-by-secret [secret
                                        tarjonta-service
                                        ohjausparametrit-service
                                        person-client]
  (let [[actor-role
         hakija-secret] (match [secret]
                          [{:virkailija s}]
                          [:virkailija
                           (when (virkailija-edit/virkailija-secret-valid? s)
                             (application-store/get-hakija-secret-by-virkailija-secret s))]
                          [{:hakija s}]
                          [:hakija s]
                          :else
                          [:hakija nil])
        application     (when (some? hakija-secret)
                          (application-store/get-latest-application-by-secret hakija-secret))
<<<<<<< HEAD
        form-roles      (cond-> [actor-role]
                          (some? (:person-oid application))
                          (conj :with-henkilo))
=======
        secret-expired? (when (nil? application)
                          (application-store/application-exists-with-secret? hakija-secret))
>>>>>>> Always create new secret in separate table when editing application
        form            (cond (some? (:haku application)) (hakija-form-service/fetch-form-by-haku-oid
                                                           tarjonta-service
                                                           ohjausparametrit-service
                                                           (:haku application)
                                                           form-roles)
                              (some? (:form application)) (hakija-form-service/fetch-form-by-key
                                                           (->> application
                                                                :form
                                                                form-store/fetch-by-id
                                                                :key)
                                                           form-roles)
                              :else                       nil)
        person          (some-> application
                                (application-service/get-person person-client)
                                (dissoc :ssn :birth-date))
        full-application (some-> application
                                 (remove-unviewable-answers form)
                                 attachments-metadata->answers
                                 (assoc :person person)
                                 (dissoc :person-oid))]
    [full-application secret-expired?]))

(defn create-new-secret-and-send-link
  [tarjonta-service old-secret]
  (let [application-id (application-store/add-new-secret-to-application-by-old-secret old-secret)]
    (application-email/start-email-refresh-secret-confirmation-job tarjonta-service application-id)))
