(ns ataru.hakija.hakija-application-service
  (:require
    [ataru.applications.application-service :as application-service]
    [ataru.applications.application-store :as application-store]
    [ataru.applications.automatic-eligibility :as automatic-eligibility]
    [ataru.applications.field-deadline :as field-deadline]
    [ataru.background-job.job :as job]
    [ataru.db.db :as db]
    [ataru.email.application-email-confirmation :as application-email]
    [ataru.files.file-store :as file-store]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.hakija.validator :as validator]
    [ataru.log.audit-log :as audit-log]
    [ataru.person-service.person-integration :as person-integration]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.tutkintojen-tunnustaminen :as tutkintojen-tunnustaminen]
    [ataru.util :as util]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [cheshire.core :as json]
    [clj-time.core :as time]
    [clj-time.format :as f]
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [taoensso.timbre :as log]))

(defn- store-and-log [application applied-hakukohteet form is-modify? session audit-logger]
  {:pre [(boolean? is-modify?)]}
  (let [store-fn (if is-modify? application-store/update-application application-store/add-application)
        application-id (store-fn application applied-hakukohteet form session audit-logger)]
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
                                   (assoc :value nil)))))))

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
                              (old-answers-by-key (:key answer))
                              answer))))))

(defn- edited-cannot-edit-questions
  [new-application old-application form]
  (let [new-answers (util/group-by-first :key (:answers new-application))
        old-answers (util/group-by-first :key (:answers old-application))]
    (->> (:content form)
         util/flatten-form-fields
         (keep (fn [{:keys [id cannot-edit]}]
                 (when (and cannot-edit
                            (if (contains? old-answers id)
                              (not= (:value (new-answers id))
                                    (:value (old-answers id)))
                              (not-every? string/blank?
                                          (flatten (vector (:value (new-answers id)))))))
                   id))))))

(defn- flatten-attachment-keys [application]
  (->> (:answers application)
       (filter (comp (partial = "attachment") :fieldType))
       (mapcat (fn [answer]
                 (let [value (:value answer)]
                   (if (or (vector? (first value))
                           (nil? (first value)))
                     (mapcat identity value)
                     value))))))

(defn- remove-orphan-attachments [liiteri-cas-client new-application old-application]
  (let [new-attachments    (->> new-application
                                flatten-attachment-keys
                                set)
        orphan-attachments (->> old-application
                                flatten-attachment-keys
                                (remove new-attachments))]
    (doseq [attachment-key orphan-attachments]
      (file-store/delete-file liiteri-cas-client attachment-key))
    (log/info (str "Updated application " (:key old-application) ", removed old attachments: " (string/join ", " orphan-attachments)))))

(defn- valid-virkailija-update-secret [{:keys [virkailija-secret]}]
  (when (virkailija-edit/virkailija-update-secret-valid? virkailija-secret)
    virkailija-secret))

(defn- valid-virkailija-rewrite-secret [{:keys [virkailija-secret]}]
  (when (virkailija-edit/virkailija-rewrite-secret-valid? virkailija-secret)
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

(defn- log-late-submitted-application [application submitted-at session audit-logger]
  (audit-log/log audit-logger
                 {:new       {:late-submitted-application (format "Hakija yritti palauttaa hakemuksen hakuajan päätyttyä: %s. Hakemus: %s"
                                                     (f/unparse modified-time-format submitted-at)
                                                     (json/generate-string application))}
                  :operation audit-log/operation-failed
                  :session   session
                  :id        {:email (util/extract-email application)}}))

(defn- validate-and-store [liiteri-cas-client
                           form-by-id-cache
                           koodisto-cache
                           tarjonta-service
                           organization-service
                           ohjausparametrit-service
                           audit-logger
                           application
                           is-modify?
                           session]
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
        applied-hakukohderyhmat       (set (mapcat :hakukohderyhmat applied-hakukohteet))
        [rewrite? virkailija-secret] (if is-modify?
                                       (if-let [rewrite-secret (valid-virkailija-rewrite-secret application)]
                                         [true rewrite-secret]
                                         [false (valid-virkailija-update-secret application)])
                                       [false (valid-virkailija-create-secret application)])
        latest-application            (application-store/get-latest-version-of-application-for-edit rewrite? application)
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
        field-deadlines               (or (some->> latest-application
                                                   :key
                                                   field-deadline/get-field-deadlines
                                                   (map #(dissoc % :last-modified))
                                                   (util/group-by-first :field-id))
                                          {})
        form                          (cond (some? (:haku application))
                                            (hakija-form-service/fetch-form-by-haku-oid-and-id
                                             form-by-id-cache
                                             tarjonta-service
                                             koodisto-cache
                                             organization-service
                                             ohjausparametrit-service
                                             (:haku application)
                                             (:form application)
                                             (util/application-in-processing? application-hakukohde-reviews)
                                             field-deadlines
                                             form-roles)
                                            (some? (:form application))
                                            (hakija-form-service/fetch-form-by-id
                                             (:form application)
                                             form-roles
                                             form-by-id-cache
                                             koodisto-cache
                                             nil
                                             (util/application-in-processing? application-hakukohde-reviews)
                                             field-deadlines))
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
       :key  nil
       :code :internal-server-error}

      (and (:secret application)
           virkailija-secret)
      {:passed? false
       :failures ["Tried to edit hakemus with both virkailija and hakija secret."]
       :key  (:key latest-application)
       :code :internal-server-error}

      (and (:haku application)
           (empty? (:hakukohde application)))
      {:passed? false
       :failures ["Hakukohde must be specified"]
       :key  (:key latest-application)
       :code :internal-server-error}

      (and (not is-modify?)
           (nil? virkailija-secret)
           (some #(not (:on (:hakuaika %))) applied-hakukohteet))
      (do
        (log-late-submitted-application application now session audit-logger)
        {:passed? false
         :failures ["Application period is not open."]
         :key  (:key latest-application)
         :code :application-period-closed})

      (not-empty edited-cannot-edit-questions)
      {:passed?  false
       :failures (into {} (map #(vector % "Cannot edit answer to question")
                               edited-cannot-edit-questions))
       :key  (:key latest-application)
       :code :internal-server-error}

      (not (:passed? validation-result))
      (assoc validation-result :key (:key latest-application))

      :else
      (do
        (remove-orphan-attachments liiteri-cas-client final-application latest-application)
        (assoc (store-and-log final-application applied-hakukohteet form is-modify? session audit-logger)
          :key (:key latest-application))))))

(defn- start-person-creation-job [job-runner application-id]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (log/info "Started person creation job (to person service) with job id"
              (job/start-job job-runner
                             connection
                             (:type person-integration/job-definition)
                             {:application-id application-id}))))

(defn- start-attachment-finalizer-job [job-runner application-id]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (log/info "Started attachment finalizer job (to Liiteri) with job id"
              (job/start-job job-runner
                             connection
                             (:type attachment-finalizer-job/job-definition)
                             {:application-id application-id}))))

(defn- start-submit-jobs [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (application-email/start-email-submit-confirmation-job koodisto-cache tarjonta-service
                                                         organization-service
                                                         ohjausparametrit-service
                                                         job-runner
                                                         application-id)
  (start-person-creation-job job-runner application-id)
  (start-attachment-finalizer-job job-runner application-id)
  (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-submit-job
   job-runner
   application-id)
  (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
   job-runner
   application-id))

(defn- start-virkailija-edit-jobs
  [job-runner virkailija-secret application-id application]
  (virkailija-edit/invalidate-virkailija-update-and-rewrite-secret virkailija-secret)
  (when (nil? (:person-oid application))
    (start-person-creation-job job-runner application-id))
  (start-attachment-finalizer-job job-runner application-id)
  (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
   job-runner
   application-id))

(defn- start-hakija-edit-jobs [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id]
  (application-email/start-email-edit-confirmation-job koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                       job-runner
                                                       application-id)
  (tutkintojen-tunnustaminen/start-tutkintojen-tunnustaminen-edit-job
   job-runner
   application-id)
  (start-attachment-finalizer-job job-runner application-id)
  (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
   job-runner
   application-id))

(defn handle-application-submit
  [form-by-id-cache
   koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   audit-logger
   application
   session
   liiteri-cas-client]
  (log/info "Application submitted:" application)
  (let [{:keys [passed? id]
         :as   result}
        (validate-and-store liiteri-cas-client
                            form-by-id-cache
                            koodisto-cache
                            tarjonta-service
                            organization-service
                            ohjausparametrit-service
                            audit-logger
                            application
                            false
                            session)
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (do
        (when virkailija-secret
          (virkailija-edit/invalidate-virkailija-create-secret virkailija-secret))
        (start-submit-jobs koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner id))
      (do
        (audit-log/log audit-logger
                       {:new       application
                        :operation audit-log/operation-failed
                        :session   session
                        :id        {:email (util/extract-email application)}})
        (log/warn "Application failed verification" result)))
    result))

(defn handle-application-edit
  [form-by-id-cache
   koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   audit-logger
   input-application
   session
   liiteri-cas-client]
  (log/info "Application edited:" input-application)
  (let [{:keys [passed? id application key]
         :as   result}
        (validate-and-store liiteri-cas-client
                            form-by-id-cache
                            koodisto-cache
                            tarjonta-service
                            organization-service
                            ohjausparametrit-service
                            audit-logger
                            input-application
                            true
                            session)
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (if virkailija-secret
        (start-virkailija-edit-jobs job-runner
          virkailija-secret
          id
          application)
        (start-hakija-edit-jobs koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner id))
      (do
        (audit-log/log audit-logger
                       {:new       input-application
                        :operation audit-log/operation-failed
                        :session   session
                        :id        {:applicationOid key}})
        (log/warn "Application" key "edit failed verification" result)))
    result))

(defn handle-application-attachment-post-process
  [koodisto-cache
   tarjonta-service
   organization-service
   ohjausparametrit-service
   application-key
   audit-logger
   session]
  (application-store/post-process-application-attachments
    koodisto-cache
    tarjonta-service
    organization-service
    ohjausparametrit-service
    application-key
    audit-logger
    session))

(defn save-application-feedback
  [feedback]
  (log/info "Saving feedback" feedback)
  (application-store/add-application-feedback feedback))

(defn- attachment-metadata->keys [{:keys [value fieldType]}]
  (when (= fieldType "attachment")
    (if (and (vector? value)
             (not-empty value)
             (or (nil? (first value))
                 (vector? (first value))))
      (flatten value)
      value)))

(defn- attachment-metadata->answer [attachment-metadata]
  (fn [{:keys [fieldType] :as answer}]
    (let [pick-metadata (fn [keys]
                          (let [ks (set keys)]
                            (filterv #(ks (:key %)) attachment-metadata)))]
      (cond-> answer
              (= fieldType "attachment")
              (update :value (fn [value]
                               (if (and (vector? value)
                                        (not-empty value)
                                        (or (nil? (first value))
                                            (vector? (first value))))
                                 (mapv pick-metadata value)
                                 (pick-metadata value))))))))

(defn attachments-metadata->answers [application liiteri-cas-client]
  (let [all-attachment-keys (mapcat attachment-metadata->keys (:answers application))
        attachment-metadata (file-store/get-metadata liiteri-cas-client all-attachment-keys)]
    (update application :answers (partial map (attachment-metadata->answer attachment-metadata)))))

(defn is-inactivated? [application]
  (cond (nil? application)
        false
        (-> (application-store/get-application-review (:key application))
            :state (= "inactivated"))
        true
        :else
        false))

(defn get-latest-application-by-secret
  [form-by-id-cache
   koodisto-cache
   ohjausparametrit-service
   organization-service
   application-service
   tarjonta-service
   secret
   liiteri-cas-client]
  (let [[actor-role secret] (match [secret]
                              [{:virkailija s}]
                              [:virkailija s]

                              [{:hakija s}]
                              [:hakija s]

                              :else
                              [:hakija nil])
        application                (cond
                                     (and (= actor-role :virkailija) (virkailija-edit/virkailija-rewrite-secret-valid? secret))
                                     (application-store/get-latest-application-for-virkailija-rewrite-edit secret)

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
        field-deadlines            (or (some->> application
                                                :key
                                                field-deadline/get-field-deadlines
                                                (map #(dissoc % :last-modified))
                                                (util/group-by-first :field-id))
                                       {})
        form                       (cond (some? (:haku application)) (hakija-form-service/fetch-form-by-haku-oid
                                                                      form-by-id-cache
                                                                      tarjonta-service
                                                                      koodisto-cache
                                                                      organization-service
                                                                      ohjausparametrit-service
                                                                      (:haku application)
                                                                      application-in-processing?
                                                                      field-deadlines
                                                                      form-roles)
                                         (some? (:form application)) (hakija-form-service/fetch-form-by-key
                                                                      (->> application
                                                                           :form
                                                                           form-store/fetch-by-id
                                                                           :key)
                                                                      form-roles
                                                                      form-by-id-cache
                                                                      koodisto-cache
                                                                      nil
                                                                      application-in-processing?
                                                                      field-deadlines))
        person                     (if (= actor-role :virkailija)
                                     (application-service/get-person application-service application)
                                     (when application
                                       (dissoc (application-service/get-person application-service application) :ssn :birth-date)))
        full-application           (merge (some-> application
                                                  (remove-unviewable-answers form)
                                                  (attachments-metadata->answers liiteri-cas-client)
                                                  (dissoc :person-oid :application-hakukohde-reviews)
                                                  (assoc :cannot-edit-because-in-processing (and
                                                                                             (not= actor-role :virkailija)
                                                                                             (in-processing-state? application form))))
                                          (when (and (:yksiloity person)
                                                     (some? (:key application)))
                                            {:application-identifier (application-service/mask-application-key (:key application))}))]
    [(when full-application
       {:application full-application
        :person      person
        :form        form})
     secret-expired?
     lang-override
     inactivated?]))

(defn create-new-secret-and-send-link
  [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner old-secret]
  (let [application-id (application-store/add-new-secret-to-application-by-old-secret old-secret)]
    (application-email/start-email-refresh-secret-confirmation-job koodisto-cache tarjonta-service
                                                                   organization-service ohjausparametrit-service
                                                                   job-runner
                                                                   application-id)))

(defn can-access-attachment?
  [secret virkailija-secret attachment-key]
  (when-let [application (cond (and (some? virkailija-secret) (virkailija-edit/virkailija-rewrite-secret-valid? virkailija-secret))
                               (application-store/get-latest-application-for-virkailija-rewrite-edit virkailija-secret)

                               (and (some? virkailija-secret) (virkailija-edit/virkailija-update-secret-valid? virkailija-secret))
                               (application-store/get-latest-application-for-virkailija-edit virkailija-secret)

                               (some? secret)
                               (application-store/get-latest-application-by-secret secret))]
    (some #(and (= "attachment" (:fieldType %))
                (some #{attachment-key} (flatten (:value %))))
          (:answers application))))
