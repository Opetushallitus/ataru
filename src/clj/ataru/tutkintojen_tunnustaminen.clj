(ns ataru.tutkintojen-tunnustaminen
  (:require [ataru.background-job.job :as job]
            [clojure.java.io :as io]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.files.file-store :as file-store]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.util :as util]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.data.xml :as xml]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]])
  (:import [java.io
            ByteArrayOutputStream
            OutputStreamWriter
            PipedInputStream
            PipedOutputStream]
           [java.util.concurrent
            TimeoutException
            TimeUnit]
           java.util.Base64))

(defqueries "sql/tutkintojen-tunnustaminen-queries.sql")

(defn- ->property-string
  [id value]
  (xml/element :propertyString {:propertyDefinitionId id}
               (xml/element :value {} value)))

(defn- ->case
  [application]
  (when (or (not (string? (:country application)))
            (string/blank? (:country application)))
    (throw (new RuntimeException
                (str "Application " (:id application)
                     " has invalid country: " (:country application)))))
  (let [application-key (:key application)
        name            (:name application)
        country         (:country application)
        submitted       (f/unparse (f/formatter :date-time-no-ms (t/time-zone-for-id "Europe/Helsinki"))
                                   (:submitted application))]
    (xml/element :createFolder {}
                 (xml/element :properties {}
                              (->property-string "ams_studypathid" application-key)
                              (->property-string "ams_orignator" name)
                              (->property-string "ams_applicantcountry" country)
                              (->property-string "ams_registrationdate" submitted)
                              (->property-string "ams_title" "Hakemus"))
                 (xml/element :folderType {} "ams_case"))))

(defn- ->action
  [title task-id]
  (xml/element :createFolder {}
               (xml/element :properties {}
                            (->property-string "ams_title" title)
                            (->property-string "ams_processtaskid" task-id))
               (xml/element :folderType {} "ams_action")))

(defn- value->text
  [attachments lang field value]
  (if (vector? value)
    (mapv (partial value->text attachments lang field) value)
    (let [option (some #(when (= value (:value %)) %) (:options field))]
      (cond (= "attachment" (:fieldType field))
            (get-in attachments [value :filename] value)
            (some? option)
            (get-in option [:label lang] value)
            :else
            value))))

(defn- field->label-value
  [answers attachments lang field]
  (when (contains? answers (:id field))
    [(get-in field [:label lang] (:id field))
     (value->text attachments lang field (get-in answers [(:id field) :value]))]))

(defn- pretty-print-value
  [value]
  (cond (vector? value)
        (mapcat (fn [v]
                  (let [[r & rs] (pretty-print-value v)]
                    (cons (str "- " r)
                          (map #(str "  " %) rs))))
                value)

        (string? value)
        (remove string/blank? (string/split value #"\n"))
        :else
        [""]))

(defn- pretty-print
  [[label value]]
  (str "- " label "\n  "
       (string/join "\n  " (pretty-print-value value))))

(defn- application->document
  [application form attachments]
  (let [attachments (util/group-by-first :key attachments)
        answers     (util/group-by-first :key (:answers (:content application)))
        lang        (keyword (:lang application))]
    {:filename "hakemus.txt"
     :data     (->> (:content form)
                    util/flatten-form-fields
                    (keep (partial field->label-value answers attachments lang))
                    (map pretty-print)
                    (string/join "\n\n")
                    ((fn [x] (.getBytes x "UTF-8"))))}))

(defn- ->documents
  [application form attachments]
  (let [lang    (:lang application)
        encoder (Base64/getEncoder)]
    (map (fn [{:keys [filename data]}]
           (xml/element :createDocument {}
                        (xml/element :properties {}
                                     (->property-string "ams_language" lang))
                        (xml/element :contentStream {}
                                     (xml/element :filename {} filename)
                                     (xml/element :stream {} (new String (.encode encoder data) "UTF-8")))))
         (cons (application->document application form attachments)
               attachments))))

(defn- ->information-request-document
  [application information-request timestamp]
  (let [lang    (:lang application)
        encoder (Base64/getEncoder)
        doc     (.getBytes (str "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body><h4>"
                                (:subject information-request)
                                "</h4><p>"
                                (:message information-request)
                                "</p></body></html>"))]
    [(xml/element :createDocument {}
                  (xml/element :properties {}
                               (->property-string "ams_language" lang)
                               (->property-string "ams_documenttype" "Täydennyspyyntö"))
                  (xml/element :contentStream {}
                               (xml/element :filename {} (str "taydennyspyynto_" timestamp ".html"))
                               (xml/element :stream {} (new String (.encode encoder doc) "UTF-8"))))]))

(defn- ->application-submitted
  [application form attachments]
  (apply
   xml/element :message {}
   (->case application)
   (->action "Hakemuksen saapuminen" "01.01")
   (->documents application form attachments)))

(defn- ->application-information-request-sent
  [application information-request timestamp]
  (apply
   xml/element :message {}
   (->case application)
   (->action "Täydennyspyyntö" "02.02")
   (->information-request-document application information-request timestamp)))

(defn- ->application-edited
  [application form attachments]
  (apply
   xml/element :message {}
   (->case application)
   (->action "Täydennys" "01.02")
   (->documents application form attachments)))

(defn- ->application-inactivated
  [application]
  (xml/element :message {}
               (->case application)
               (->action "Hakemuksen peruutus" "03.01")))

(defn- get-application
  [country-question-id application-id]
  (let [application (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                      (first (yesql-get-application {:country_question_id country-question-id
                                                     :id                  application-id}
                                                    {:connection connection})))]
    (when (nil? application)
      (throw (new RuntimeException (str "Application " application-id
                                        " not found"))))
    application))

(defn- get-form
  [form-by-id-cache koodisto-cache application]
  (let [form (hakija-form-service/fetch-form-by-id
              (:form-id application)
              [:hakija]
              form-by-id-cache
              koodisto-cache
              nil
              false
              {}
              false)]
    (when (nil? form)
      (throw (new RuntimeException (str "Form " (:form-id application)
                                        " not found"))))
    form))

(defn- get-application-by-event-id
  [country-question-id event-id]
  (let [id-and-state (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
                       (first (yesql-get-application-id-and-state-by-event-id {:id event-id}
                                                                              {:connection connection})))]
    (when (nil? id-and-state)
      (throw (new RuntimeException (str "Application id by event id " event-id
                                        " not found"))))
    {:review-key  (:review-key id-and-state)
     :state       (:state id-and-state)
     :application (get-application country-question-id (:id id-and-state))}))

(defn get-latest-application-id [application-key]
  (jdbc/with-db-connection [connection {:datasource (db/get-datasource :db)}]
    (first (yesql-get-latest-application-id {:key application-key} {:connection connection}))))

(defn- attachment-as-bytes
  [liiteri-cas-client key]
  (if-let [response (file-store/get-file liiteri-cas-client key)]
    (with-open [in (:body response)
                out (new ByteArrayOutputStream)]
      (io/copy in out)
      (.toByteArray out))
    (throw (new RuntimeException (str "Attachment " key " not found")))))

(defn- get-attachments
  [liiteri-cas-client size-limit application]
  (let [attachment-metadata   (file-store/get-metadata liiteri-cas-client (:attachment-keys application))
        attachment-total-size (reduce + 0 (map :size attachment-metadata))]
    (if (< size-limit attachment-total-size)
      (do (log/error "Application" (:id application)
                     "contains" attachment-total-size "bytes"
                     "of attachments which is over the limit" size-limit
                     ", skipping attachments")
          [])
      (map (fn [{:keys [key filename]}]
             {:key      key
              :filename filename
              :data     (attachment-as-bytes liiteri-cas-client key)})
           attachment-metadata))))

(defn- transfer
  [config filename message]
  (let [stdin (new PipedInputStream)
        emit  (future
                (with-open [w (new OutputStreamWriter (new PipedOutputStream stdin) "UTF-8")]
                  (xml/emit message w)))
        lftp  (future
                (sh "lftp" "-c" (str (format "open --user %s --env-password %s:%d" (:user config) (:host config) (:port config))
                                     (format "&& set ssl:verify-certificate %b" (:verify-certificate config true))
                                     "&& set ftp:ssl-protect-data true"
                                     (format "&& cd %s" (:path config))
                                     (format "&& put /dev/stdin -o %s.part" filename)
                                     (format "&& mv %s.part %s" filename filename))
                    :in stdin
                    :env {"LFTP_PASSWORD" (:password config)}))
        r     (try
                (.get lftp (:timeout-seconds config) TimeUnit/SECONDS)
                (catch TimeoutException _
                  (future-cancel emit)
                  (future-cancel lftp)
                  {:exit 1 :err (str "Writing timed out after " (:timeout-seconds config) " seconds")}))]
    (when-not (zero? (:exit r))
      (throw (new RuntimeException (str "Writing file " filename " failed: "
                                        (:err r)))))))

(defn start-tutkintojen-tunnustaminen-submit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen submit job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-submit-job"
                               {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-edit-job
  [job-runner application-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen edit job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-edit-job"
                               {:application-id application-id})))))

(defn start-tutkintojen-tunnustaminen-review-state-changed-job
  [job-runner event-id]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen review state changed job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-review-state-changed-job"
                               {:event-id event-id})))))

(defn start-tutkintojen-tunnustaminen-information-request-sent-job
  [job-runner information-request]
  (when (get-in config [:tutkintojen-tunnustaminen :enabled?])
    (log/info "Started tutkintojen tunnustaminen information request sent job with job id"
              (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                (job/start-job job-runner
                               connection
                               "tutkintojen-tunnustaminen-information-request-sent-job"
                               {:information-request information-request})))))

(defn- form-key-matches?
  [cfg-form-key test]
  (let [keys (string/split cfg-form-key #",")]
    (some #(= test %) keys)))

(defn- get-configuration
  []
  (let [cfg (:tutkintojen-tunnustaminen config)]
    (when (string/blank? (:form-key cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen form key not set")))
    (when (string/blank? (:country-question-id cfg))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen country question id not set")))
    (when (not (integer? (:attachment-total-size-limit cfg)))
      (throw (new RuntimeException
                  "Tutkintojen tunnustaminen attachment size limit not set")))
    cfg))

(defn- application-job-step
  [liiteri-cas-client form-by-id-cache koodisto-cache application-id edit?]
  (let [{:keys [form-key
                country-question-id
                attachment-total-size-limit
                ftp]} (get-configuration)
        application   (get-application country-question-id application-id)]
    (if (form-key-matches? form-key (:form-key application))
      (let [form        (get-form form-by-id-cache koodisto-cache application)
            attachments (get-attachments liiteri-cas-client attachment-total-size-limit application)
            message     (if edit?
                          (->application-edited application form attachments)
                          (->application-submitted application form attachments))]
        (log/info "Sending application"
                  (if edit? "edited" "submitted")
                  "message to ASHA for application"
                  application-id)
        (transfer ftp
                  (str (:key application) "_" application-id ".xml")
                  message)
        (log/info "Sent application"
                  (if edit? "edited" "submitted")
                  "message to ASHA for application"
                  application-id)
        {:transition {:id :final}})
      {:transition {:id :final}})))

(defn tutkintojen-tunnustaminen-submit-job-handler
  [{:keys [application-id]} {:keys [liiteri-cas-client form-by-id-cache koodisto-cache]}]
  (application-job-step liiteri-cas-client form-by-id-cache koodisto-cache application-id false))

(defn tutkintojen-tunnustaminen-edit-job-handler
  [{:keys [application-id]} {:keys [liiteri-cas-client form-by-id-cache koodisto-cache]}]
  (application-job-step liiteri-cas-client form-by-id-cache koodisto-cache application-id true))

(defn tutkintojen-tunnustaminen-review-state-changed-job-step
  [{:keys [event-id]} _]
  (let [{:keys [form-key
                country-question-id
                ftp]}         (get-configuration)
        application-and-state (get-application-by-event-id country-question-id event-id)
        application           (:application application-and-state)]
    (if (and (form-key-matches? form-key (:form-key application))
             (nil? (:review-key application-and-state))
             (= "inactivated" (:state application-and-state)))
      (let [message (->application-inactivated application)]
        (log/info "Sending application inactivated message to ASHA for application"
                  (:id application))
        (transfer ftp
                  (str (:key application) "_" (:id application) "_" event-id ".xml")
                  message)
        (log/info "Sent application inactivated message to ASHA for application"
                  (:id application))
        {:transition {:id :final}})
      {:transition {:id :final}})))

(defn timestamp [] (System/currentTimeMillis))

(defn tutkintojen-tunnustaminen-information-request-sent-job-step
  [{:keys [information-request]} _]
  (let [{:keys [form-key country-question-id ftp]} (get-configuration)
        application-id (:id (get-latest-application-id (:application-key information-request)))
        application (get-application country-question-id application-id)]
    (if (and (form-key-matches? form-key (:form-key application))
             (= "information-request" (:message-type information-request)))
      (let [ts (timestamp)
            message (->application-information-request-sent application information-request ts)]
        (log/info "Sending application information request sent message to ASHA for application"
                  (:id application))
        (transfer ftp
                  (str (:key application) "_" (:id application) "_taydennyspyynto_" ts ".xml")
                  message)
        (log/info "Sent application information request sent message to ASHA for application"
                  (:id application))
        {:transition {:id :final}})
      {:transition {:id :final}})))
