(ns ataru.person-service.person-integration
  (:require
   [clojure.core.match :refer [match]]
   [ataru.component-data.person-info-module :as person-info-module]
   [ataru.forms.form-store :as form-store]
   [clojure.java.jdbc :as jdbc]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as log]
   [ataru.applications.application-store :as application-store]
   [ataru.aws.sqs :as sqs]
   [ataru.aws.sns :as sns]
   [ataru.background-job.job :as job]
   [ataru.cache.cache-service :as cache]
   [ataru.db.db :as db]
   [ataru.person-service.person-service :as person-service]
   [ataru.kk-application-payment.kk-application-payment-status-updater-job :as kk-payment-job]
   [yesql.core :refer [defqueries]])
  (:import [java.util.concurrent Executors TimeUnit]
           (software.amazon.awssdk.services.sqs.model Message)))

(declare yesql-update-person-info-as-in-person!)
(declare yesql-update-person-info-as-in-application!)
(defqueries "sql/person-integration-queries.sql")

(defn- start-jobs-for-kk-application-payments [job-runner application-id]
  (kk-payment-job/start-update-kk-payment-status-for-application-id-job job-runner application-id))

(defn- start-jobs-for-person [job-runner person-oid]
  (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
    (job/start-job job-runner
      connection
      "update-person-info-job"
      {:person-oid person-oid})
    (job/start-job job-runner
      connection
      "automatic-payment-obligation-job"
      {:person-oid person-oid})))

(defn muu-person-info-module? [application]
  (person-info-module/muu-person-info-module?
   (form-store/fetch-by-id (:form application))))

(defn- upsert-and-log-person [job-runner person-service application-id application]
  (log/info "Adding applicant from application"
            application-id
            "to oppijanumerorekisteri")
  (try
    (let [{:keys [status oid]} (person-service/create-or-find-person
                                 person-service
                                 application)]
      (match status
             :created
             (log/info "Added person" oid "to oppijanumerorekisteri")
             :exists
             (log/info "Person" oid "already existed in oppijanumerorekisteri")
             :found-matching
             (do
               (log/info "Found person" oid "with matching email, date of birth and gender")
               (application-store/add-application-event {:application-key (:key application)
                                                         :event-type      "person-found-matching"}
                                                        nil))
             :dob-or-gender-conflict
             (do
               (log/info "Found person with matching email, but conflicting date of birth or gender. Created new person" oid)
               (application-store/add-application-event {:application-key (:key application)
                                                         :event-type      "person-dob-or-gender-conflict"}
                                                        nil))
             :created-with-email-id
             (log/info "Added person" oid "with email identification to oppijanumerorekisteri"))
      (application-store/add-person-oid application-id oid)
      (log/info "Added person" oid "to application" application-id)
      (start-jobs-for-person job-runner oid)
      (start-jobs-for-kk-application-payments job-runner application-id)
      (log/info "Started person info update job for application" application-id)
      oid)
    (catch IllegalArgumentException e
      (log/error e "Failed to create-or-find person for application"
        application-id))))

(defn upsert-person-synchronized
  [job-runner person-service application-id]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))
         (not (nil? job-runner))]}
    (let [application (application-store/get-application application-id)]
    (if (muu-person-info-module? application)
      (log/info "Not adding applicant from application"
                application-id
                "to oppijanumerorekisteri")
      (upsert-and-log-person job-runner person-service application-id application))))

(defn upsert-person
  [{:keys [application-id]}
   {:keys [person-service] :as job-runner}]
  {:pre [(not (nil? application-id))
         (not (nil? person-service))]}
  (let [application (application-store/get-application application-id)]
    (log/info "About to add applicant from application id"
              application-id "key" (:key application) "form-id" (:form application))
    (if (muu-person-info-module? application)
      (log/info "Not adding applicant from application"
                application-id
                "to oppijanumerorekisteri")
      (upsert-and-log-person job-runner person-service application-id application))))

(defn- update-person-info-as-in-person
  [person-oid person]
  (pos? (db/exec :db yesql-update-person-info-as-in-person!
                 {:preferred_name (:kutsumanimi person)
                  :last_name      (:sukunimi person)
                  :ssn            (:hetu person)
                  :dob            (:syntymaaika person)
                  :person_oid     person-oid})))

(defn- update-person-info-as-in-application
  [person-oid]
  (pos? (db/exec :db yesql-update-person-info-as-in-application!
                 {:person_oid person-oid})))

(defn- update-person-info
  [henkilo-cache person-service person-oid]
  (log/info "Checking person info of" person-oid)
  (cache/remove-from henkilo-cache person-oid)
  (let [person (person-service/get-person person-service person-oid)]
    (if (or (:yksiloity person)
            (:yksiloityVTJ person))
      (when (update-person-info-as-in-person person-oid person)
        (log/info "Updated person info of" person-oid
                  "to that on oppijanumerorekisteri"))
      (when (update-person-info-as-in-application person-oid)
        (log/info "Updated person info of" person-oid
                  "to that on application")))))

(defn update-person-info-job-handler
  [{:keys [person-oid]}
   {:keys [henkilo-cache person-service]}]
  (update-person-info henkilo-cache person-service person-oid))

(def job-type (str (ns-name *ns*)))

(def job-definition {:handler upsert-person
                     :type  job-type})

(defn- parse-henkilo-modified-message
  [message]
  (if-let [oid (:oidHenkilo message)]
    oid
    (throw (new RuntimeException
                (str "Could not find key oidHenkilo from message '" message "'")))))

(defn- try-handle-message
  [job-runner drain-failed? message]
  (try
    (some->> ^Message message
             (.body)
             (sns/handle-message)
             (parse-henkilo-modified-message)
             (start-jobs-for-person job-runner))
    message
    (catch Exception e
      (if drain-failed?
        (do (log/error e "Handling henkilö modified message failed, deleting" message)
            message)
        (log/warn e "Handling henkilö modified message failed")))))

(defn- try-handle-messages
  [amazon-sqs
   job-runner
   drain-failed?
   queue-url
   receive-wait]
  (try
    (->> (repeatedly #(sqs/batch-receive amazon-sqs queue-url receive-wait))
         (take-while not-empty)
         (map (partial keep (partial try-handle-message
                                     job-runner
                                     drain-failed?)))
         (map (partial sqs/batch-delete amazon-sqs queue-url))
         dorun)
    (catch Exception e
      (log/warn e "Handling henkilö modified messages failed"))))

(defrecord UpdatePersonInfoWorker [amazon-sqs
                                   job-runner
                                   enabled?
                                   drain-failed?
                                   queue-url
                                   receive-wait
                                   executor]
  component/Lifecycle
  (start [this]
    (when (not enabled?)
      (log/warn "UpdatePersonInfoWorker disabled"))
    (if (and enabled? (nil? executor))
      (let [executor (Executors/newSingleThreadScheduledExecutor)]
        (.scheduleAtFixedRate
         executor
         (partial try-handle-messages
                  amazon-sqs
                  job-runner
                  drain-failed?
                  queue-url
                  receive-wait)
         0 20 TimeUnit/SECONDS)
        (assoc this :executor executor))
      this))
  (stop [this]
    (when (some? executor)
      (.shutdown executor))
    (assoc this :executor nil)))
