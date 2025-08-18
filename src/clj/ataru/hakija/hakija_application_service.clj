(ns ataru.hakija.hakija-application-service
  (:require
    [ataru.applications.application-service :as application-service]
    [ataru.applications.application-store :as application-store]
    [ataru.applications.automatic-eligibility :as automatic-eligibility]
    [ataru.applications.field-deadline :as field-deadline]
    [ataru.background-job.job :as job]
    [ataru.cache.cache-service :as cache]
    [ataru.config.core :refer [config]]
    [ataru.config.url-helper :as url-helper]
    [ataru.db.db :as db]
    [ataru.email.application-email-jobs :as application-email]
    [ataru.files.file-store :as file-store]
    [ataru.forms.form-store :as form-store]
    [ataru.hakija.background-jobs.attachment-finalizer-job :as attachment-finalizer-job]
    [ataru.hakija.hakija-form-service :as hakija-form-service]
    [ataru.hakija.validator :as validator]
    [ataru.koski.koski-service :as koski-service]
    [ataru.koski.koski-json-parser :refer [parse-koski-tutkinnot]]
    [ataru.log.audit-log :as audit-log]
    [ataru.maksut.maksut-protocol :as maksut-protocol]
    [ataru.person-service.person-integration :as person-integration]
    [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
    [ataru.tarjonta-service.tarjonta-protocol :as tarjonta-service]
    [ataru.tutkinto.tutkinto-util :as tutkinto-util]
    [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
    [ataru.util :as util]
    [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
    [cheshire.core :as json]
    [clj-time.core :as time]
    [clj-time.format :as f]
    [clojure.core.async :as async]
    [clojure.core.match :refer [match]]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [taoensso.timbre :as log]
    [ataru.hakija.toisen-asteen-yhteishaku-logic :as toisen-asteen-yhteishaku-logic]
    [ataru.harkinnanvaraisuus.harkinnanvaraisuus-process-store :as harkinnanvaraisuus-store]
    [ataru.tarjonta.haku :as h]
    [ataru.kk-application-payment.kk-application-payment :as kk-application-payment]))

(defn- store-and-log [application applied-hakukohteet form is-modify? session audit-logger harkinnanvaraisuus-process-fn oppija-session]
  {:pre [(boolean? is-modify?)]}
  (let [store-fn (if is-modify? application-store/update-application application-store/add-application)
        key-and-id (store-fn application applied-hakukohteet form session audit-logger oppija-session)]
    (log/info "Stored application with id: " (:id key-and-id))
    (when harkinnanvaraisuus-process-fn
      (harkinnanvaraisuus-process-fn (:id key-and-id) (:key key-and-id)))
    {:passed?        true
     :id (:id key-and-id)
     :application application}))

(defn in-processing-state?
  [application form]
  (let [applied-hakukohteet        (filter #(contains? (set (:hakukohde application)) (:oid %))
                                           (-> form :tarjonta :hakukohteet))
        bare-form-haku?            (nil? (:haku application))
        application-in-processing? (util/application-in-processing? (:application-hakukohde-reviews application))]
    (boolean (if bare-form-haku?
               application-in-processing?
               (and (some #(get-in % [:hakuaika :jatkuva-or-joustava-haku?]) applied-hakukohteet)
                    application-in-processing?)))))

(defn remove-unviewable-answers
  [application form]
  (let [fields-by-key (->> (:content form)
                           util/flatten-form-fields
                           (util/group-by-first :id))]
    (update application :answers
            (partial map (fn [answer]
                           (let [original-question-field  (fields-by-key (:original-question answer))
                                 field                    (fields-by-key (:key answer))
                                 original-followup-field  (fields-by-key (:original-followup answer))]
                             (cond-> answer
                                     (or (:cannot-view field) (:cannot-view original-question-field) (:cannot-view original-followup-field))
                                     (assoc :value nil))))))))

(defn- is-unviewable-parent-of-per-hakukohde-old-followup-in-new-answers?
  [form-fields new-answers followup followup-answer]
  (let [parent-field (form-fields (:followup-of followup))
        option-value (:option-value followup)
        parent-answer (->> new-answers
                        (filter #(= (:original-question %) (:id parent-field)))
                        (filter #(= (:duplikoitu-kysymys-hakukohde-oid %) (:duplikoitu-followup-hakukohde-oid followup-answer)))
                        first)]
    (or (and
          (some? parent-answer)
          (nil? (:value parent-answer))
          (some #(= "required" %) (:validators parent-field))
          (:cannot-view parent-field))
        (= option-value (:value parent-answer)))))

(defn merge-unviewable-answers-from-previous
  [new-application
   old-application
   form]
  (let [fields-by-key (->> (:content form)
                           util/flatten-form-fields
                           (util/group-by-first :id))
        hakukohde-oids-in-new-application (set (:hakukohde new-application))
        old-answers-by-key (util/group-by-first :key (:answers old-application))
        new-answers-by-key (util/group-by-first :key (:answers new-application))
        original-unviewable-followups-not-in-new (filter #(and (seq (:original-followup %))
                                                    (nil? (get new-answers-by-key (:key %)))
                                                    (contains? hakukohde-oids-in-new-application (:duplikoitu-followup-hakukohde-oid %))
                                                    (:cannot-view (fields-by-key (:original-followup %)))
                                                    (is-unviewable-parent-of-per-hakukohde-old-followup-in-new-answers?
                                                      fields-by-key
                                                      (:answers new-application)
                                                      (fields-by-key (:original-followup %))
                                                      %))
                                              (:answers old-application))
        if-cannot-view-use-old (fn [answer]
                                 (let [original-question-field (fields-by-key (:original-question answer))
                                       field (fields-by-key (:key answer))
                                       original-followup-field (fields-by-key (:original-followup answer))]
                                   (if (and
                                         (or (:cannot-view field) (:cannot-view original-question-field) (:cannot-view original-followup-field))
                                         (or (:cannot-edit field) (nil? (:value answer))))
                                     (or (old-answers-by-key (:key answer)) answer)
                                     answer)))]
    (assoc new-application :answers
            (concat (keep if-cannot-view-use-old (:answers new-application))
                    original-unviewable-followups-not-in-new))))

(defn- merge-uneditable-answers-from-previous
  [new-application
   old-application
   form
   cannot-edit-fields]
  (let [cannot-edit-fields      (set cannot-edit-fields)
        fields-by-key           (->> (:content form)
                                     util/flatten-form-fields
                                     (util/group-by-first :id))
        old-answers-by-key      (util/group-by-first :key (:answers old-application))]
    (update new-application :answers
            (partial keep (fn [answer]
                            (if (or (:cannot-view (fields-by-key (:key answer)))
                                    (cannot-edit-fields (:key answer)))
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
                           hakukohderyhma-settings-cache
                           audit-logger
                           application
                           is-modify?
                           session
                           oppija-session]
  (let [strict-warnings-on-unchanged-edits? (if (nil? (:strict-warnings-on-unchanged-edits? application))
                                              true
                                              (:strict-warnings-on-unchanged-edits? application))
        now                           (time/now)
        haku                          (when (:haku application)
                                        (tarjonta-service/get-haku
                                        tarjonta-service
                                        (:haku application)))
        tarjonta-info                 (when (:haku application)
                                        (tarjonta-parser/parse-tarjonta-info-by-haku
                                         koodisto-cache
                                         tarjonta-service
                                         organization-service
                                         ohjausparametrit-service
                                         (:haku application)))
        hakukohteet                   (get-in tarjonta-info [:tarjonta :hakukohteet])
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
        use-toisen-asteen-yhteishaku-restrictions? (toisen-asteen-yhteishaku-logic/use-toisen-asteen-yhteishaku-restrictions?
                                                     form-roles
                                                     rewrite?
                                                     haku)
        kk-payment                 (kk-application-payment/get-kk-payment-state application false)
        has-overdue-payment?       (= (get-in kk-payment [:payment :state])
                                      (:overdue kk-application-payment/all-states))
        form                          (cond (some? (:haku application))
                                            (hakija-form-service/fetch-form-by-haku-oid-and-id
                                             form-by-id-cache
                                             tarjonta-service
                                             koodisto-cache
                                             organization-service
                                             ohjausparametrit-service
                                             hakukohderyhma-settings-cache
                                             (:haku application)
                                             (:form application)
                                             (util/application-in-processing? application-hakukohde-reviews)
                                             field-deadlines
                                             form-roles
                                             use-toisen-asteen-yhteishaku-restrictions?
                                             has-overdue-payment?)
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
        edited-cannot-edit-questions  (when is-modify?
                                        (edited-cannot-edit-questions
                                          final-application
                                          latest-application
                                          form))
        cannot-edit-attachment        (when (not-empty edited-cannot-edit-questions)
                                        (->> (:content form)
                                             util/flatten-form-fields
                                             (filter #((set edited-cannot-edit-questions) (:id %)))
                                             (keep (fn [{:keys [id fieldType]}]
                                                     (when (= fieldType "attachment")
                                                       id)))))
        cannot-edit-fields            (when (not-empty edited-cannot-edit-questions)
                                        (->> (:content form)
                                             util/flatten-form-fields
                                             (filter #((set edited-cannot-edit-questions) (:id %)))
                                             (keep (fn [{:keys [id fieldType]}]
                                                     (when-not (= fieldType "attachment")
                                                       id)))))
        final-application             (if (and is-modify? (not-empty cannot-edit-fields))
                                        (merge-uneditable-answers-from-previous
                                          final-application
                                          latest-application
                                          form
                                          cannot-edit-fields)
                                        final-application)
        ; halutaan että kahdessa kohtaa (juuressa, answers-osioissa) olevien hakukohteiden järjestys on aina synkassa
        ; koska answer-osion muutokset validoidaan käytetään sitä masterina
        final-answer-hakukohteet      (:value (first (filter #(= (:key %) "hakukohteet") (:answers final-application))))
        hakukohteet-are-valid?        (fn [kohteet real-kohteet]
                                        (let [real-oids (set (map :oid real-kohteet))]
                                          (every? #(contains? real-oids %) kohteet)))
        applied-hakukohteet           (filter #(contains? (set final-answer-hakukohteet) (:oid %))
                                              hakukohteet)
        applied-hakukohderyhmat       (set (mapcat :hakukohderyhmat applied-hakukohteet))
        final-application             (if final-answer-hakukohteet
                                        (merge final-application {:hakukohde final-answer-hakukohteet})
                                        (dissoc final-application :hakukohde))
        hakeminen-tunnistautuneena-validation-errors (validator/validate-tunnistautunut-oppija-fields (util/answers-by-key (:answers application)) oppija-session)
        validation-result             (validator/valid-application?
                                       koodisto-cache
                                       has-applied
                                       (set-original-values latest-application final-application)
                                       form
                                       applied-hakukohderyhmat
                                       (some? virkailija-secret)
                                       (get latest-application :id "NEW_APPLICATION_ID")
                                       (get latest-application :key "NEW_APPLICATION_KEY"))
        harkinnanvaraisuus-process-fn (when (and haku (h/toisen-asteen-yhteishaku? haku))
                                        (fn [application-id application-key]
                                          (harkinnanvaraisuus-store/upsert-harkinnanvaraisuus-process application-id application-key (:haku application))))]
    (when (not-empty cannot-edit-fields)
      (log/warnf "Skipping uneditable updated answers in application %s: %s" (:key latest-application) (str (vec cannot-edit-fields))))
    (when (not-empty hakeminen-tunnistautuneena-validation-errors)
      (log/error "Error(s) when validating fields from oppija-session" oppija-session ":" (pr-str hakeminen-tunnistautuneena-validation-errors)))
    (cond
      (not-empty hakeminen-tunnistautuneena-validation-errors)
      {:passed? false
       :failures ["Supplied application answers do not match fields from oppija-session."]
       :key  nil
       :code :internal-server-error}

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

      (and (seq (:hakukohde application))
           (empty? (:haku application)))
      {:passed? false
       :failures ["Haku must be specified also when hakukohde is"]
       :key  (:key latest-application)
       :code :internal-server-error}

      (and (:haku application)
           (:hakukohde application)
           (not (hakukohteet-are-valid? final-answer-hakukohteet hakukohteet)))
      {:passed? false
       :failures ["Hakukohteet contains invalid members"]
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

      (and strict-warnings-on-unchanged-edits?
           (not-empty cannot-edit-fields))
      {:passed?  false
       :failures (into {} (map #(vector % "Cannot edit answer to question")
                               edited-cannot-edit-questions))
       :key  (:key latest-application)
       :code :internal-server-error}

      (true? (get-in form [:properties :closed] false))
      {:passed? false
       :failures ["Form is closed"]
       :key (:key latest-application)
       :code :form-closed}

      (not-empty cannot-edit-attachment)
      {:passed?  false
       :failures (into {} (map #(vector % "Deadline passed in attachments")
                               cannot-edit-attachment))
       :key  (:key latest-application)
       :code :internal-server-error}

      (not (:passed? validation-result))
      (assoc validation-result :key (:key latest-application))

      :else
      (do
        (remove-orphan-attachments liiteri-cas-client final-application latest-application)
        (assoc (store-and-log final-application applied-hakukohteet form is-modify? session audit-logger harkinnanvaraisuus-process-fn oppija-session)
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

(defn start-submit-jobs [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id payment-url]
  (application-email/start-email-submit-confirmation-job koodisto-cache tarjonta-service
                                                         organization-service
                                                         ohjausparametrit-service
                                                         job-runner
                                                         application-id
                                                         payment-url)
  (start-person-creation-job job-runner application-id)
  (start-attachment-finalizer-job job-runner application-id)
  (tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-submit-job
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

(defn- start-hakija-edit-jobs [koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner application-id _]
  (application-email/start-email-edit-confirmation-job koodisto-cache tarjonta-service organization-service ohjausparametrit-service
                                                       job-runner
                                                       application-id)
  (tutkintojen-tunnustaminen-store/start-tutkintojen-tunnustaminen-edit-job
   job-runner
   application-id)
  (start-attachment-finalizer-job job-runner application-id)
  (automatic-eligibility/start-automatic-eligibility-if-ylioppilas-job
   job-runner
   application-id))

(defn- tutu-form? [form]
  (or (= "payment-type-tutu" (get-in form [:properties :payment :type]))
      (let [tutu-keys (string/split (-> config :tutkintojen-tunnustaminen :maksut :form-keys) #",")]
        (boolean
          (and (some? tutu-keys) (some #(= (:key form) %) tutu-keys))))))

(defn- handle-tutu-form [form-by-id-cache id application]
  (let [app-key    (-> (application-store/get-application id) :key)
        form-id    (:form application)
        form       (when (some? form-id)
                     (-> (cache/get-from form-by-id-cache (str form-id))))
        get-field  (fn [key] (->> (:answers application)
                                  (filter #(= key (:key %)))
                                  (map :value)
                                  first))
        amount     (or (get-in form [:properties :payment :processing-fee])
                       (-> config :tutkintojen-tunnustaminen :maksut :decision-amount))
        req-fn     (fn [] {:reference app-key
                           :origin "tutu"
                           :due-days 14
                           :first-name (get-field "first-name")
                           :last-name (get-field "last-name")
                           :email (get-field "email")
                           :amount amount})]
    {:tutu-form? (tutu-form? form)
     :req-fn     req-fn
     :lang       (:lang application)
     :app-key    app-key}))

(defn remove-empty-arrays [answers]
  (filter #(not= (:value %) [[]]) answers))

(defn remove-arrays-with-quotations-only [answers]
  (filter #(not= (:value %)  [[""]] ) answers))

(defn remove-empty-answers [answers]
  (-> answers
      (remove-empty-arrays)
      (remove-arrays-with-quotations-only)))

(defn save-koski-tutkinnot [form-id app-key form-by-id-cache oppija-session koski-service]
  (when-let [tutkinnot (some->> (get-in oppija-session [:data :person-oid])
                                (koski-service/get-tutkinnot-for-oppija koski-service true)
                                :opiskeluoikeudet
                                (parse-koski-tutkinnot (->> form-id
                                                            (str)
                                                            (cache/get-from form-by-id-cache)
                                                            (tutkinto-util/koski-tutkinto-levels-in-form))))]
    (application-store/add-new-koski-tutkinnot-for-application app-key tutkinnot)))

(defn handle-application-submit
  [form-by-id-cache
   koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   hakukohderyhma-settings-cache
   audit-logger
   application
   session
   liiteri-cas-client
   maksut-service
   oppija-session
   koski-service]
  (log/info "Application submitted:" application)
  (let [answers-empty-removed (remove-empty-answers (:answers application))
        application-empty-answers-removed (assoc application :answers answers-empty-removed)
        {:keys [passed? id]
         :as   result}
        (validate-and-store liiteri-cas-client
                            form-by-id-cache
                            koodisto-cache
                            tarjonta-service
                            organization-service
                            ohjausparametrit-service
                            hakukohderyhma-settings-cache
                            audit-logger
                            application-empty-answers-removed
                            false
                            session
                            oppija-session)
        {:keys [tutu-form? req-fn lang app-key]} (handle-tutu-form form-by-id-cache id application-empty-answers-removed)
        virkailija-secret (:virkailija-secret application-empty-answers-removed)]

    (if passed?
      (do
        (when virkailija-secret
          (virkailija-edit/invalidate-virkailija-create-secret virkailija-secret))

        (when (:save-koski-tutkinnot application-empty-answers-removed)
          (save-koski-tutkinnot
            (:form application-empty-answers-removed)
            app-key
            form-by-id-cache
            oppija-session
            koski-service))

        (let [invoice (when tutu-form? (maksut-protocol/create-kasittely-lasku maksut-service (req-fn)))
              url (when tutu-form? (url-helper/resolve-url :maksut-service.hakija-get-by-secret (:secret invoice) lang))]
          (when invoice
            (log/info "Invoice details" invoice)
            (log/info "Generate maksut-link for email" url))

          (start-submit-jobs koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner id url)
          (-> result
              (cond-> tutu-form? (assoc :payment {:url url})))))
      (do
        (audit-log/log audit-logger
                       {:new       application-empty-answers-removed
                        :operation audit-log/operation-failed
                        :session   session
                        :id        {:email (util/extract-email application-empty-answers-removed)}})
        (log/warn "Application failed verification" result)
        result))
    ))

(defn handle-application-edit
  [form-by-id-cache
   koodisto-cache
   tarjonta-service
   job-runner
   organization-service
   ohjausparametrit-service
   hakukohderyhma-settings-cache
   audit-logger
   input-application
   session
   liiteri-cas-client
   _
   ]
  (log/info "Application edited:" input-application)
  (let [answers-empty-removed (remove-empty-answers (:answers input-application))
        application-empty-answers-removed (assoc input-application :answers answers-empty-removed)
        {:keys [passed? id application key]
         :as   result}
        (validate-and-store liiteri-cas-client
                            form-by-id-cache
                            koodisto-cache
                            tarjonta-service
                            organization-service
                            ohjausparametrit-service
                            hakukohderyhma-settings-cache
                            audit-logger
                            application-empty-answers-removed
                            true
                            session
                            {});fixme, miten oppija-sessio toimii muokkauksessa? Ei tällä hetkellä mitenkään?
        virkailija-secret (:virkailija-secret application)]
    (if passed?
      (if virkailija-secret
        (start-virkailija-edit-jobs job-runner
          virkailija-secret
          id
          application)
        (start-hakija-edit-jobs koodisto-cache tarjonta-service organization-service ohjausparametrit-service job-runner id nil))
      (do
        (audit-log/log audit-logger
                       {:new       application-empty-answers-removed
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
                            (if (nil? keys)
                                nil
                                (mapv (fn [key] (first (filter #(= (:key %) key) attachment-metadata))) keys)))]
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
  (let [all-attachment-keys (filter some? (mapcat attachment-metadata->keys (:answers application)))
        attachment-metadata (file-store/get-metadata liiteri-cas-client (vec all-attachment-keys))]
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
   application-service
   organization-service
   tarjonta-service
   hakukohderyhma-settings-cache
   secret
   liiteri-cas-client
   koski-service]
  (let [[actor-role secret] (match [secret]
                              [{:virkailija s}]
                              [:virkailija s]

                              [{:hakija s}]
                              [:hakija s]

                              :else
                              [:hakija nil])
        virkailija-oid-with-rewrite-secret (when (= actor-role :virkailija)
                                             (virkailija-edit/virkailija-oid-with-rewrite-secret secret))
        virkailija-oid-with-update-secret  (when (and (nil? virkailija-oid-with-rewrite-secret)
                                                      (= actor-role :virkailija))
                                             (virkailija-edit/virkailija-oid-with-update-secret secret))
        application                (cond
                                     virkailija-oid-with-rewrite-secret
                                     (application-store/get-latest-application-for-virkailija-rewrite-edit secret)

                                     virkailija-oid-with-update-secret
                                     (application-store/get-latest-application-for-virkailija-edit secret)

                                     (and (= actor-role :hakija) (some? secret))
                                     (application-store/get-latest-application-by-secret secret))
        form-roles                 (cond-> [actor-role]
                                     (some? (:person-oid application))
                                     (conj :with-henkilo))
        secret-expired?            (when (nil? application)
                                     (application-store/application-exists-with-secret? secret))
        virkailija-oid             (or virkailija-oid-with-rewrite-secret virkailija-oid-with-update-secret)
        application-in-processing? (util/application-in-processing? (:application-hakukohde-reviews application))
        inactivated?               (is-inactivated? application)
        kk-payment                 (kk-application-payment/get-kk-payment-state application false)
        has-overdue-payment?       (= (get-in kk-payment [:payment :state])
                                      (:overdue kk-application-payment/all-states))
        lang-override              (when (or secret-expired? inactivated?) (application-store/get-application-language-by-secret secret))
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
                                                                      hakukohderyhma-settings-cache
                                                                      (:haku application)
                                                                      application-in-processing?
                                                                      field-deadlines
                                                                      form-roles
                                                                      (some? virkailija-oid-with-rewrite-secret)
                                                                      has-overdue-payment?)
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
        requested-tutkinto-levels  (tutkinto-util/koski-tutkinto-levels-in-form form)
        koski-tutkinnot            (future (when requested-tutkinto-levels
                                             (if (tutkinto-util/save-koski-tutkinnot? form)
                                              (application-store/koski-tutkinnot-for-application (:key application))
                                              (some->> (:person-oid application)
                                                       (koski-service/get-tutkinnot-for-oppija koski-service true)
                                                       :opiskeluoikeudet
                                                       (parse-koski-tutkinnot requested-tutkinto-levels)))))
        new-person (application-service/get-person-for-securelink application-service application)
        filtered-person (if (= actor-role :virkailija)
                          new-person
                          (dissoc new-person :ssn :birth-date))
        full-application (merge (some-> application
                                        (remove-unviewable-answers form)
                                        (attachments-metadata->answers liiteri-cas-client)
                                        (dissoc :person-oid :application-hakukohde-reviews)
                                        (assoc :cannot-edit-because-in-processing (and
                                                                                   (not= actor-role :virkailija)
                                                                                   (in-processing-state? application form))))
                                (when (some? (:key application))
                                  {:application-identifier (application-service/mask-application-key (:key application))}))]
    [(when full-application
       (cond-> {:application full-application
                :person      filtered-person
                :form        form
                :kk-payment  kk-payment}
               @koski-tutkinnot
               (assoc :koski-tutkinnot @koski-tutkinnot)))
     secret-expired?
     lang-override
     inactivated?
     virkailija-oid]))

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
