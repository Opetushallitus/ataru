(ns ataru.hakija.hakija-application-service
  (:require
    [taoensso.timbre :as log]
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [ataru.applications.automatic-eligibility :as automatic-eligibility]
    [ataru.background-job.job :as job]
    [ataru.forms.hakukohderyhmat :as hakukohderyhmat]
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
    [ataru.util :as util]
    [ataru.files.file-store :as file-store]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [ataru.config.core :refer [config]]
    [clj-time.core :as time]
    [clj-time.coerce :as t]
    [ataru.applications.application-service :as application-service]
    [ataru.log.audit-log :as audit-log]
    [clj-time.format :as f]))

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

(defn- merge-unviewable-answers-from-previous
  [new-application
   old-application
   form]
  (let [fields-by-key      (->> (:content form)
                                util/flatten-form-fields
                                (util/group-by-first :id))
        old-answers-by-key (util/group-by-first :key (:answers old-application))]
    (update new-application :answers
            (partial keep (fn [answer]
                            (if (:cannot-view (fields-by-key (:key answer)))
                              (when-let [old-answer (old-answers-by-key (:key answer))]
                                (assoc old-answer :label (:label answer)))
                              answer))))))

(defn- edited-cannot-edit-questions
  [new-application old-application form]
  (let [new-answers (util/group-by-first :key (:answers new-application))
        old-answers (util/group-by-first :key (:answers old-application))]
    (->> (:content form)
         util/flatten-form-fields
         (keep (fn [{:keys [id cannot-edit]}]
                 (when (and cannot-edit
                            (not= (:value (new-answers id))
                                  (:value (old-answers id))))
                   id))))))

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

(def tz (time/time-zone-for-id "Europe/Helsinki"))

(def ^:private modified-time-format
  (f/formatter "dd.MM.yyyy HH:mm:ss" tz))

(defn- log-late-submitted-application [application submitted-at]
  (audit-log/log {:new       (format "Hakija yritti palauttaa hakemuksen hakuajan päätyttyä: %s. Hakemus: %s"
                                     (f/unparse modified-time-format submitted-at)
                                     (cheshire.core/generate-string application))
                  :operation audit-log/operation-new
                  :id        (util/extract-email application)}))

(defn- validate-and-store [koodisto-cache tarjonta-service organization-service ohjausparametrit-service application is-modify?]
  (let [now                           (time/now)
        tarjonta-info                 (when (:haku application)
                                        (tarjonta-parser/parse-tarjonta-info-by-haku
                                         koodisto-cache
                                         tarjonta-service
                                         organization-service
                                         ohjausparametrit-service
                                         (:haku application)))
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
        priorisoivat-and-rajaavat     (fn [form]
                                          (merge form
                                                 (when (:haku application)
                                                       {:priorisoivat-hakukohderyhmat (:ryhmat (hakukohderyhmat/priorisoivat-hakukohderyhmat tarjonta-service (:haku application)))
                                                        :tarjonta-hakukohteet         hakukohteet
                                                        :rajaavat-hakukohderyhmat     (:ryhmat (hakukohderyhmat/rajaavat-hakukohderyhmat (:haku application)))})))
        form                          (-> application
                                          (:form)
                                          (form-store/fetch-by-id)
                                          (hakukohde/populate-hakukohde-answer-options tarjonta-info)
                                          (hakija-form-service/populate-can-submit-multiple-applications tarjonta-info)
                                          (priorisoivat-and-rajaavat)
                                          (hakija-form-service/flag-uneditable-and-unviewable-fields
                                           hakukohteet
                                           form-roles
                                           (util/application-in-processing? application-hakukohde-reviews)))
        final-application             (if is-modify?
                                        (-> application
                                            (merge-unviewable-answers-from-previous
                                             latest-application
                                             form)
                                            (assoc :person-oid (:person-oid latest-application)))
                                        application)
        validation-result             (validator/valid-application?
                                       koodisto-cache
                                       has-applied
                                       (set-original-values latest-application final-application)
                                       form
                                       applied-hakukohderyhmat
                                       (some? virkailija-secret))
        edited-cannot-edit-questions  (when is-modify?
                                        (edited-cannot-edit-questions
                                         final-application
                                         latest-application
                                         form))]
    (cond
      (and (some? (:virkailija-secret application))
           (nil? virkailija-secret))
      {:passed? false
       :failures ["Tried to edit application with invalid virkailija secret."]
       :code :internal-server-error}

      (and (:secret application)
           virkailija-secret)
      {:passed? false
       :failures ["Tried to edit hakemus with both virkailija and hakija secret."]
       :code :internal-server-error}

      (and (:haku application)
           (empty? (:hakukohde application)))
      {:passed? false
       :failures ["Hakukohde must be specified"]
       :code :internal-server-error}

      (and (not is-modify?)
           (nil? virkailija-secret)
           (some #(not (:on (:hakuaika %))) applied-hakukohteet))
      (do
        (log-late-submitted-application application now)
        {:passed? false
         :failures ["Application period is not open."]
         :code :application-period-closed})

      (not-empty edited-cannot-edit-questions)
      {:passed?  false
       :failures (into {} (map #(vector % "Cannot edit answer to question")
                               edited-cannot-edit-questions))
       :code :internal-server-error}

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

(defn- start-submit-jobs [tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (application-email/start-email-submit-confirmation-job tarjonta-service
                                                         organization-service
                                                         ohjausparametrit-service
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

(defn- start-hakija-edit-jobs [tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (application-email/start-email-edit-confirmation-job tarjonta-service organization-service ohjausparametrit-service
                                                       job-runner
                                                       application-id)
  (start-attachment-finalizer-job job-runner application-id))

(defn handle-application-submit
  [koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   application]
  (log/info "Application submitted:" application)
  (let [{:keys [passed? id]
         :as   result}
        (validate-and-store koodisto-cache tarjonta-service organization-service ohjausparametrit-service application false)
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (do
        (when virkailija-secret
          (virkailija-edit/invalidate-virkailija-create-secret virkailija-secret))
        (start-submit-jobs tarjonta-service organization-service ohjausparametrit-service job-runner id))
      (do
        (audit-log/log {:new       application
                        :operation audit-log/operation-failed
                        :id        (util/extract-email application)})
        (log/warn "Application failed verification" result)))
    result))

(defn handle-application-edit
  [koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   application]
  (log/info "Application edited:" application)
  (let [{:keys [passed? id application]
         :as   result}
        (validate-and-store koodisto-cache tarjonta-service organization-service ohjausparametrit-service application true)
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (if virkailija-secret
        (start-virkailija-edit-jobs job-runner
          virkailija-secret
          id
          application)
        (start-hakija-edit-jobs tarjonta-service organization-service ohjausparametrit-service job-runner id))
      (do
        (audit-log/log {:new       application
                        :operation audit-log/operation-failed
                        :id        (util/extract-email application)})
        (log/warn "Application edit failed verification" result)))
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

(defn is-inactivated? [application]
  (cond (nil? application)
        false
        (-> (application-store/get-application-review (:key application))
            :state (= "inactivated"))
        true
        :else
        false))

(defn get-latest-application-by-secret
  [secret tarjonta-service koodisto-cache organization-service ohjausparametrit-service person-client]
  (let [[actor-role secret] (match [secret]
                              [{:virkailija s}]
                              [:virkailija s]

                              [{:hakija s}]
                              [:hakija s]

                              :else
                              [:hakija nil])
        application                (cond
                                     (and (= actor-role :virkailija) (virkailija-edit/virkailija-update-secret-valid? secret))
                                     (application-store/get-latest-application-for-virkailija-edit secret)

                                     (and (= actor-role :hakija) (some? secret))
                                     (application-store/get-latest-application-by-secret secret))
        form-roles                 (cond-> [actor-role]
                                     (some? (:person-oid application))
                                     (conj :with-henkilo))
        secret-expired?            (when (nil? application)
                                     (application-store/application-exists-with-secret? secret))
        lang-override              (when secret-expired? (application-store/get-application-language-by-secret secret))
        application-in-processing? (util/application-in-processing? (:application-hakukohde-reviews application))
        inactivated?               (is-inactivated? application)
        form                       (cond (some? (:haku application)) (hakija-form-service/fetch-form-by-haku-oid
                                                                       tarjonta-service
                                                                       koodisto-cache
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
                                                                       koodisto-cache
                                                                       nil
                                                                       application-in-processing?))
        person                     (if (= actor-role :virkailija)
                                     (application-service/get-person application person-client)
                                     (some-> application
                                             (application-service/get-person person-client)
                                             (dissoc :ssn :birth-date)))
        full-application           (some-> application
                                           (remove-unviewable-answers form)
                                           attachments-metadata->answers
                                           (dissoc :person-oid :application-hakukohde-reviews)
                                           (assoc :cannot-edit-because-in-processing (and
                                                                                      (not= actor-role :virkailija)
                                                                                      (in-processing-state? application form))))]
    [(when full-application
       {:application full-application
        :person      person
        :form        form})
     secret-expired?
     lang-override
     inactivated?]))

(defn create-new-secret-and-send-link
  [tarjonta-service organization-service ohjausparametrit-service job-runner old-secret]
  (let [application-id (application-store/add-new-secret-to-application-by-old-secret old-secret)]
    (application-email/start-email-refresh-secret-confirmation-job tarjonta-service
                                                                   organization-service ohjausparametrit-service
                                                                   job-runner
                                                                   application-id)))
