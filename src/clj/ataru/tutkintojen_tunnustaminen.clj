(ns ataru.tutkintojen-tunnustaminen
  (:require [ataru.background-job.job :as job]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.files.file-store :as file-store]
            [ataru.person-service.person-service :as person-service]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.data.xml :as xml]
            [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]])
  (:import [java.io
            BufferedWriter
            ByteArrayInputStream
            ByteArrayOutputStream
            OutputStreamWriter]
           java.util.Base64
           [com.jcraft.jsch
            JSch
            Logger]))

(defqueries "sql/tutkintojen-tunnustaminen-queries.sql")

(defn- ->property-string
  [id value]
  (xml/element :propertyString {:propertyDefinitionId id}
               (xml/element :value {} value)))

(defn- ->case
  [application person]
  (let [application-key (:key application)
        name            (str (:etunimet person) " " (:sukunimi person))
        country         (:country application)
        created-time    (f/unparse (f/formatter :date-time-no-ms (t/time-zone-for-id "Europe/Helsinki"))
                                   (:created-time application))]
    (xml/element :createFolder {}
                 (xml/element :properties {}
                              (->property-string "ams_opintopolkuid" application-key)
                              (->property-string "ams_originator" name)
                              (->property-string "ams_applicantcountry" country)
                              (->property-string "ams_registrationdate" created-time)
                              (->property-string "ams_title" "Hakemus"))
                 (xml/element :folderType {} "ams_case"))))

(defn- ->action
  [title task-id]
  (xml/element :createFolder {}
               (xml/element :properties {}
                            (->property-string "ams_title" title)
                            (->property-string "ams_processtaskid" task-id))
               (xml/element :folderType {} "ams_action")))

(defn- ->documents
  [application attachments]
  (let [lang (:lang application)]
    (map (fn [{:keys [filename data]}]
           (xml/element :createDocument {}
                        (xml/element :properties {}
                                     (->property-string "ams_language" lang))
                        (xml/element :contentStream {}
                                     (xml/element :filename {} filename)
                                     (xml/element :stream {} data))))
         attachments)))

(defn- ->application-submitted
  [application person attachments]
  (apply
   xml/element :message {}
   (->case application person)
   (->action "Hakemuksen saapuminen" "TODO")
   (->documents application attachments)))

(defn- ->application-edited
  [application person attachments]
  (apply
   xml/element :message {}
   (->case application person)
   (->action "Hakemuksen muokkaus" "TODO")
   (->documents application attachments)))

(defn- ->application-inactivated
  [application person]
  (xml/element :message {}
               (->case application person)
               (->action "Hakemuksen peruutus" "TODO")))

(defn- get-application
  [country-question-id application-id]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [application (first (yesql-get-application {:country_question_id country-question-id
                                                     :id                  application-id}
                                                    {:connection connection}))]
      (when (nil? application)
        (throw (new RuntimeException (str "Application " application-id
                                          " not found"))))
      (when (or (not (string? (:country application)))
                (clojure.string/blank? (:country application)))
        (throw (new RuntimeException (str "Application " application-id
                                          " has invalid country: " (:country application)
                                          " as value for question " country-question-id))))
      application)))

(defn- get-application-by-event-id
  [country-question-id event-id]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (let [application (first (yesql-get-application-by-event-id {:country_question_id country-question-id
                                                                 :id                  event-id}
                                                                {:connection connection}))]
      (when (nil? application)
        (throw (new RuntimeException (str "Application by event id " event-id
                                          " not found"))))
      (when (or (not (string? (:country application)))
                (clojure.string/blank? (:country application)))
        (throw (new RuntimeException (str "Application " (:id application)
                                          " has invalid country: " (:country application)
                                          " as value for question " country-question-id))))
      application)))

(defn- get-person
  [person-service application]
  (person-service/get-person person-service (:person-oid application)))

(defn- attachment-as-base64
  [key]
  (if-let [response (file-store/get-file key)]
    (.toString
     (with-open [in  (:body response)
                 out (new ByteArrayOutputStream)
                 enc (.wrap (Base64/getEncoder) out)]
       (clojure.java.io/copy in enc)
       out)
     "UTF-8")
    (throw (new RuntimeException (str "Attachment " key " not found")))))

(defn- get-attachments
  [size-limit application]
  (let [attachment-metadata   (file-store/get-metadata (:attachment-keys application))
        attachment-total-size (reduce + 0 (map :size attachment-metadata))]
    (if (< size-limit attachment-total-size)
      (do (log/error "Application" (:id application)
                     "contains" attachment-total-size "bytes"
                     "of attachments which is over the limit" size-limit
                     ", skipping attachments")
          [])
      (map (fn [{:keys [key filename]}]
             {:filename filename
              :data     (attachment-as-base64 key)})
           attachment-metadata))))

(defn- with-session
  [jsch {:keys [host port user password host-key-type]} f]
  (let [session (doto (.getSession jsch user host port)
                  (.setTimeout 600000)
                  (.setConfig "server_host_key" host-key-type)
                  (.setPassword password)
                  (.connect))]
    (try (f session) (finally (.disconnect session)))))

(defn- with-channel
  [session f]
  (let [channel (doto (.openChannel session "sftp")
                  (.connect))]
    (try (f channel) (finally (.disconnect channel)))))

(defn- with-file-writer
  [channel filename f]
  (let [out (-> (.put channel filename)
                (OutputStreamWriter. "UTF-8")
                (BufferedWriter.))]
    (try (f out) (finally (.close out)))))

(def jsch-logger
  (reify Logger
    (isEnabled [_ level] (< Logger/INFO level))
    (log [_ level message]
      (case level
        0 (log/debug message)
        1 (log/info message)
        2 (log/warn message)
        3 (log/error message)
        4 (log/fatal message)))))

(defn- transfer
  [{:keys [known-host] :as config} filename message]
  (let [jsch (doto (new JSch)
               (.setKnownHosts (new ByteArrayInputStream (.getBytes known-host))))]
    (JSch/setLogger jsch-logger)
    (with-session jsch config
      (fn [session]
        (with-channel session
          (fn [channel]
            (with-file-writer channel (str filename ".part")
              (fn [writer] (xml/emit message writer)))
            (.rename channel (str filename ".part") filename)))))))

(defn start-tutkintojen-tunnustaminen-submit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen submit job with job id"
              (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-submit-job"
                               {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-edit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen edit job with job id"
              (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-edit-job"
                               {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-review-state-changed-job
  [job-runner event-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen review state changed job with job id"
              (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-review-state-changed-job"
                               {:event-id event-id})))))

(defn- get-configuration
  []
  (let [cfg (:tutkintojen-tunnustaminen config)]
    (when (clojure.string/blank? (:form-key cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen form key not set")))
    (when (clojure.string/blank? (:country-question-id cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen country question id not set")))
    (when (not (integer? (:attachment-total-size-limit cfg)))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen attachment size limit not set")))
    cfg))

(defn- application-job-step
  [person-service application-id edit?]
  (let [{:keys [form-key
                country-question-id
                attachment-total-size-limit
                sftp]} (get-configuration)
        application    (get-application country-question-id application-id)]
    (cond (and (some? (:person-oid application))
               (= form-key (:form-key application)))
          (let [person      (get-person person-service application)
                attachments (get-attachments attachment-total-size-limit application)
                message     (if edit?
                              (->application-edited application person attachments)
                              (->application-submitted application person attachments))]
            (log/info "Sending application"
                      (if edit? "edited" "submitted")
                      "message to ASHA for application"
                      application-id)
            (transfer sftp
                      (str (:key application) "_" application-id ".xml")
                      message)
            (log/info "Sent application"
                      (if edit? "edited" "submitted")
                      "message to ASHA for application"
                      application-id)
            {:transition {:id :final}})
          (= form-key (:form-key application))
          {:transition {:id :retry}}
          :else
          {:transition {:id :final}})))

(defn tutkintojen-tunnustaminen-submit-job-step
  [{:keys [application-id]} {:keys [person-service]}]
  (application-job-step person-service application-id false))

(defn tutkintojen-tunnustaminen-edit-job-step
  [{:keys [application-id]} {:keys [person-service]}]
  (application-job-step person-service application-id true))

(defn tutkintojen-tunnustaminen-review-state-changed-job-step
  [{:keys [event-id]} {:keys [person-service]}]
  (let [{:keys [form-key
                country-question-id
                sftp]} (get-configuration)
        application    (get-application-by-event-id country-question-id event-id)]
    (cond (and (some? (:person-oid application))
               (= form-key (:form-key application))
               (= "inactivated" (:state application)))
          (let [person  (get-person person-service application)
                message (->application-inactivated application person)]
            (log/info "Sending application inactivated message to ASHA for application"
                      (:id application))
            (transfer sftp
                      (str (:key application) "_" (:id application) "_" event-id ".xml")
                      message)
            (log/info "Sent application inactivated message to ASHA for application"
                      (:id application))
            {:transition {:id :final}})
          (and (= form-key (:form-key application))
               (= "inactivated" (:state application)))
          {:transition {:id :retry}}
          :else
          {:transition {:id :final}})))
