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
   [ataru.tarjonta-service.tarjonta-protocol :refer [get-hakukohde get-haku]]
   [ataru.forms.form-store :as form-store]
   [ataru.hakija.validator :as validator]
   [ataru.application.review-states :refer [complete-states]]
   [ataru.applications.application-store :as application-store]
   [ataru.hakija.editing-forbidden-fields :refer [viewing-forbidden-person-info-field-ids
                                                  editing-forbidden-person-info-field-ids]]
   [ataru.application.field-types :as types]
   [ataru.util :as util]
   [ataru.files.file-store :as file-store]
   [ataru.tarjonta-service.tarjonta-parser :as tarjonta-parser]
   [ataru.virkailija.authentication.virkailija-edit :refer [invalidate-virkailija-credentials virkailija-secret-valid?]]
   [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
   [clj-time.core :as time]
   [clj-time.coerce :refer [from-long]]))

(defn- store-and-log [application store-fn]
  (let [application-id (store-fn application)]
    (log/info "Stored application with id: " application-id)
    {:passed?        true
     :application-id application-id}))

(defn- get-hakukohteet [application]
  (or (->> application
           :answers
           (filter #(= (:key %) "hakukohteet"))
           first
           :value)
      (:hakukohde application)))

(defn- get-hakuaikas
  [tarjonta-service application]
  (let [application-hakukohde (-> application get-hakukohteet first) ; TODO check apply times for each hakukohde separately?
        hakukohde             (when application-hakukohde (get-hakukohde tarjonta-service application-hakukohde))
        haku-oid              (:hakuOid hakukohde)
        haku                  (when haku-oid (get-haku tarjonta-service haku-oid))]
    (hakuaika/get-hakuaika-info hakukohde haku)))

(defn- after-apply-end-within-10-days?
  [apply-end-long]
  (when apply-end-long
    (let [now            (time/now)
          apply-end      (from-long apply-end-long)
          days-after-end (time/plus apply-end (time/days 10))]
      (and (time/after? now apply-end)
           (time/after? days-after-end now)))))

(defn- allowed-to-apply?
  "If there is a hakukohde the user is applying to, check that hakuaika is on"
  [tarjonta-service application]
  (let [hakukohteet (get-hakukohteet application)]
    (if (empty? hakukohteet)
      true ;; plain form, always allowed to apply
      (let [hakuaikas (get-hakuaikas tarjonta-service application)]
        (or (:on hakuaikas)
            (after-apply-end-within-10-days? (:end hakuaikas)))))))

(def not-allowed-reply {:passed? false
                        :failures ["Not allowed to apply (not within hakuaika or review state is in complete states)"]})

(defn- in-complete-state? [application-key]
  (let [state (:state (application-store/get-application-review application-key))]
    (boolean (some #{state} complete-states))))

(defn processing-in-jatkuva-haku? [application-key tarjonta-info]
  (let [state (:state (application-store/get-application-review application-key))]
    (and (= state "processing")
         (:is-jatkuva-haku? (:tarjonta tarjonta-info)))))

(defn- get-hakuaika-end
  [application tarjonta-service]
  (when tarjonta-service
    (:end (get-hakuaikas tarjonta-service application))))

(defn- only-attachments-editable?
  [answer application tarjonta-service]
  (let [hakuaika-end (get-hakuaika-end application tarjonta-service)]
    (and (when hakuaika-end
           (after-apply-end-within-10-days? hakuaika-end))
         (not= (:fieldType answer) "attachment"))))

(defn- dummy-answer-to-unanswered-question
  [{:keys [id fieldType label]}]
  {:key       id
   :fieldType fieldType
   :label     label
   :value     ""})

(defn- filter-questions-without-answers
  [answers-by-key form-fields]
  (filter (fn [answer]
            (and (not (util/in? (keys answers-by-key) (keyword (:id answer))))
                 (not (util/in? (:validators answer) "required"))
                 (some #{(:fieldType answer)} types/form-fields)
                 (not (:followup? answer)) ; make sure followup answers don't show when parent not selected
                 (not (:exclude-from-answers answer))
                 (not (:exclude-from-answers-if-hidden answer)))) form-fields))

(defn- get-questions-without-answers
  "This function serves to get dummy answers and their editability for fields that were not required and thus were left
   editable in the 10 day attachment grace period. This happened due to the fact that they had no answer in db to which
   make uneditable in flag-uneditable-answers."
  [application]
  (let [form-fields               (-> application
                                      (:form)
                                      (form-store/fetch-by-id)
                                      :content
                                      (util/flatten-form-fields))
        answers-by-key            (util/answers-by-key (:answers application))
        questions-without-answers (filter-questions-without-answers answers-by-key form-fields)]
    (map dummy-answer-to-unanswered-question questions-without-answers)))

(defn flag-uneditable-answers
  [{:keys [answers] :as application} tarjonta-service]
  (assoc application
    :answers
    (map
      (fn [answer]
        (let [answer-kw (keyword (:key answer))]
          (cond-> answer
            (contains? viewing-forbidden-person-info-field-ids answer-kw)
            (merge {:cannot-view true :value nil})

            (or (contains? editing-forbidden-person-info-field-ids answer-kw)
                (only-attachments-editable? answer application tarjonta-service))
            (merge {:cannot-edit true}))))
      (apply conj answers (get-questions-without-answers application)))))

(defn- uneditable-answers-with-labels-from-new
  [uneditable-answers new-answers old-answers]
  ; the old (persisted) answers do not include labels for all languages, so they are taken from new answers instead
  (keep (fn [answer]
         (let [answer-key (:key answer)
               answer-with-key #(= (:key %) answer-key)
               old-answer (->> old-answers
                               (filter answer-with-key)
                               (first))
               new-label  (->> new-answers
                               (filter answer-with-key)
                               (first)
                               :label)]
           (when old-answer
             ;Sometimes old an answer doesn't exist: old application, new question in form (flag-uneditable-answers)
             (merge old-answer {:label new-label}))))
       uneditable-answers))

(defn- merge-uneditable-answers-from-previous
  [old-application new-application tarjonta-service]
  (let [new-answers                 (-> new-application
                                        (flag-uneditable-answers tarjonta-service)
                                        :answers)
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
                             (merge-uneditable-answers-from-previous latest-application application tarjonta-service)
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

(defn handle-application-submit [tarjonta-service application]
  (log/info "Application submitted:" application)
  (let [{passed?        :passed?
         application-id :application-id
         :as            result}
        (validate-and-store tarjonta-service application application-store/add-application false)]
    (if passed?
      (start-submit-jobs application-id)
      result)))

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
