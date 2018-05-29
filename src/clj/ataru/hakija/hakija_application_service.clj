(ns ataru.hakija.hakija-application-service
  (:require
    [taoensso.timbre :as log]
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [ataru.applications.automatic-eligibility :as automatic-eligibility]
    [ataru.background-job.job :as job]
    [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
    [ataru.email.application-email-confirmation :as application-email]
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
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [ataru.config.core :refer [config]]
    [clj-time.core :as time]
    [clj-time.coerce :as t]
    [ataru.applications.application-service :as application-service]))

(defn- store-and-log [application applied-hakukohteet form is-modify?]
  {:pre [(boolean? is-modify?)]}
  (let [store-fn (if is-modify? application-store/update-application application-store/add-application)
        application-id (store-fn application applied-hakukohteet form)]
    (log/info "Stored application with id: " application-id)
    {:passed?        true
     :id application-id
     :application application}))

(defn in-processing-state?
  [application form]
  (let [applied-hakukohteet        (filter #(contains? (set (:hakukohde application)) (:oid %))
                                           (-> form :tarjonta :hakukohteet))
        bare-form-haku?            (nil? (:haku application))
        application-in-processing? (util/application-in-processing? (:application-hakukohde-reviews application))]
    (boolean (if bare-form-haku?
               application-in-processing?
               (and (some #(get-in % [:hakuaika :jatkuva-haku?]) applied-hakukohteet)
                    application-in-processing?)))))

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

(defn- valid-virkailija-update-secret [{:keys [virkailija-secret]}]
  (when (virkailija-edit/virkailija-update-secret-valid? virkailija-secret)
    virkailija-secret))

(defn- valid-virkailija-create-secret [{:keys [virkailija-secret]}]
  (when (virkailija-edit/virkailija-create-secret-valid? virkailija-secret)
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

(defn- validate-and-store [tarjonta-service organization-service ohjausparametrit-service application is-modify?]
  (let [tarjonta-info                 (when (:haku application)
                                        (tarjonta-parser/parse-tarjonta-info-by-haku
                                         tarjonta-service
                                         organization-service
                                         ohjausparametrit-service
                                         (:haku application)))
        haku-oid                      (get-in tarjonta-info [:tarjonta :haku-oid])
        hakukohteet                   (get-in tarjonta-info [:tarjonta :hakukohteet])
        applied-hakukohteet           (filter #(contains? (set (:hakukohde application)) (:oid %))
                                              hakukohteet)
        applied-hakukohderyhmat       (mapcat :hakukohderyhmat applied-hakukohteet)
        virkailija-secret             (if is-modify?
                                        (valid-virkailija-update-secret application)
                                        (valid-virkailija-create-secret application))
        latest-application            (application-store/get-latest-version-of-application-for-edit application)
        form-roles                    (cond-> []
                                        (some? virkailija-secret)
                                        (conj :virkailija)
                                        (nil? virkailija-secret)
                                        (conj :hakija)
                                        (some? (:person-oid latest-application))
                                        (conj :with-henkilo))
        application-hakukohde-reviews (some-> latest-application
                                              :key
                                              application-store/get-application-hakukohde-reviews)
        form                          (-> application
                                          (:form)
                                          (form-store/fetch-by-id)
                                          (hakukohde/populate-hakukohde-answer-options tarjonta-info)
                                          (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info)
                                          (hakija-form-service/flag-uneditable-and-unviewable-fields
                                           hakukohteet
                                           form-roles
                                           (util/application-in-processing? application-hakukohde-reviews)))
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
                                       form
                                       applied-hakukohderyhmat)]
    (cond
      (and (some? (:virkailija-secret application))
           (nil? virkailija-secret))
      {:passed? false :failures ["Tried to edit application with invalid virkailija secret."]}

      (and (:secret application)
           virkailija-secret)
      {:passed? false :failures ["Tried to edit hakemus with both virkailija and hakija secret."]}

      (and (:haku application)
           (empty? (:hakukohde application)))
      {:passed? false :failures ["Hakukohde must be specified"]}

      (and (not is-modify?)
           (nil? virkailija-secret)
           (some #(not (:on (:hakuaika %))) applied-hakukohteet))
      {:passed? false :failures ["Application period is not open."]}

      (not (:passed? validation-result))
      validation-result

      :else
      (do
        (remove-orphan-attachments final-application latest-application)
        (store-and-log final-application applied-hakukohteet form is-modify?)))))

(defn- start-person-creation-job [job-runner application-id]
  (log/info "Started person creation job (to person service) with job id"
            (job/start-job job-runner
                           (:type person-integration/job-definition)
                           {:application-id application-id})))

(defn- start-attachment-finalizer-job [job-runner application-id]
  (log/info "Started attachment finalizer job (to Liiteri) with job id"
            (job/start-job job-runner
                           (:type attachment-finalizer-job/job-definition)
                           {:application-id application-id})))

(defn- start-submit-jobs [tarjonta-service job-runner application-id]
  (application-email/start-email-submit-confirmation-job tarjonta-service
                                                         job-runner
                                                         application-id)
  (start-person-creation-job job-runner application-id)
  (start-attachment-finalizer-job job-runner application-id)
  (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
   job-runner
   application-id))

(defn- start-virkailija-edit-jobs
  [job-runner virkailija-secret application-id application]
  (virkailija-edit/invalidate-virkailija-update-secret virkailija-secret)
  (when (nil? (:person-oid application))
    (start-person-creation-job job-runner application-id))
  (start-attachment-finalizer-job job-runner application-id))

(defn- start-hakija-edit-jobs [tarjonta-service job-runner application-id]
  (application-email/start-email-edit-confirmation-job tarjonta-service
                                                       job-runner
                                                       application-id)
  (start-attachment-finalizer-job job-runner application-id))

(defn handle-application-submit
  [tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   application]
  (log/info "Application submitted:" application)
  (let [{:keys [passed? id]
         :as   result}
        (validate-and-store tarjonta-service organization-service ohjausparametrit-service application false)
        virkailija-secret (:virkailija-secret application)]
    (when passed?
      (when virkailija-secret
        (virkailija-edit/invalidate-virkailija-create-secret virkailija-secret))
      (start-submit-jobs tarjonta-service job-runner id))
    result))

(defn handle-application-edit
  [tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   application]
  (log/info "Application edited:" application)
  (let [{:keys [passed? id application]
         :as   result}
        (validate-and-store tarjonta-service organization-service ohjausparametrit-service application true)
        virkailija-secret (:virkailija-secret application)]
    (when passed?
      (if virkailija-secret
        (start-virkailija-edit-jobs job-runner
                                    virkailija-secret
                                    id
                                    application)
        (start-hakija-edit-jobs tarjonta-service job-runner id)))
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

(defn get-latest-application-by-secret
  [secret tarjonta-service organization-service ohjausparametrit-service person-client]
  (let [[actor-role secret] (match [secret]
                                   [{:virkailija s}]
                                   [:virkailija s]

                                   [{:hakija s}]
                                   [:hakija s]

                                   :else
                                   [:hakija nil])
        application      (cond
                           (and (= actor-role :virkailija) (virkailija-edit/virkailija-update-secret-valid? secret))
                           (application-store/get-latest-application-for-virkailija-edit secret)

                           (and (= actor-role :hakija) (some? secret))
                           (application-store/get-latest-application-by-secret secret))
        form-roles       (cond-> [actor-role]
                                 (some? (:person-oid application))
                                 (conj :with-henkilo))
        secret-expired?  (when (nil? application)
                           (application-store/application-exists-with-secret? secret))
        lang-override    (when secret-expired? (application-store/get-application-language-by-secret secret))
        application-in-processing? (util/application-in-processing? (:application-hakukohde-reviews application))
        form             (cond (some? (:haku application)) (hakija-form-service/fetch-form-by-haku-oid
                                                             tarjonta-service
                                                             organization-service
                                                             ohjausparametrit-service
                                                             (:haku application)
                                                             application-in-processing?
                                                             form-roles)
                               (some? (:form application)) (hakija-form-service/fetch-form-by-key-with-flagged-fields
                                                             (->> application
                                                                  :form
                                                                  form-store/fetch-by-id
                                                                  :key)
                                                             form-roles
                                                             nil
                                                             application-in-processing?))
        person           (some-> application
                                 (application-service/get-person person-client)
                                 (dissoc :ssn :birth-date))
        full-application (some-> application
                                 (remove-unviewable-answers form)
                                 attachments-metadata->answers
                                 (dissoc :person-oid :application-hakukohde-reviews)
                                 (assoc :cannot-edit-because-in-processing (in-processing-state? application form)))]
    [(when full-application
       {:application full-application
        :person      person
        :form        form})
     secret-expired?
     lang-override]))

(defn create-new-secret-and-send-link
  [tarjonta-service job-runner old-secret]
  (let [application-id (application-store/add-new-secret-to-application-by-old-secret old-secret)]
    (application-email/start-email-refresh-secret-confirmation-job
     tarjonta-service
     job-runner
     application-id)))
