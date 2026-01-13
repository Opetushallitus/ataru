(ns ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-service
  (:require [clojure.java.io :as io]
            [ataru.files.file-store :as file-store]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-store :as tutkintojen-tunnustaminen-store]
            [ataru.util :as util]
            [ataru.tutkintojen-tunnustaminen.tutkintojen-tunnustaminen-utils :refer [get-configuration get-form tutu-form?]]
            [clojure.string :as string]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.data.xml :as xml]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log])
  (:import [java.io
            ByteArrayOutputStream
            OutputStreamWriter
            PipedInputStream
            PipedOutputStream]
           [java.util.concurrent
            TimeoutException
            TimeUnit]
           java.util.Base64))

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
  [config filename message application-id]
  (let [stdin (new PipedInputStream)
        ftp-debug-log-file (str "/tmp/asha-ftp-debug-log-" application-id ".txt")
        ftp-transfer-log-file (str "/tmp/asha-ftp-debug-log-" application-id ".txt")
        emit  (future
                (with-open [w (new OutputStreamWriter (new PipedOutputStream stdin) "UTF-8")]
                  (xml/emit message w)))
        lftp  (future
                (sh "lftp" "-vvv" "-c" (str (format "set log:file/xfer %s" ftp-transfer-log-file)
                                     "&& set xfer:log 1"
                                     (format "&& debug -t -o %s" ftp-debug-log-file)
                                     "&& set ftp:nop-interval 5"
                                     (format "&& open --user %s --env-password %s:%d" (:user config) (:host config) (:port config))
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
                  {:exit 1 :err (str "Writing timed out after " (:timeout-seconds config) " seconds")}))
        debug-log (slurp ftp-debug-log-file)
        transfer-log (slurp ftp-transfer-log-file)]
    (log/info (str "FTP transfer output for application-id " application-id " | exit-code: " (:exit r) " | out: " (:out r) " | err: " (:err r)))
    (log/info (str "FTP transfer log for application-id " application-id " | transfer log: \r\n" transfer-log))
    (log/info (str "FTP debug log for application-id " application-id " | debug log: \r\n" debug-log))
    (io/delete-file ftp-debug-log-file true)
    (io/delete-file ftp-transfer-log-file true)
    (when-not (zero? (:exit r))
      (throw (new RuntimeException (str "Writing file " filename " failed: "
                                        (:err r)))))))

(defn- application-job-step
  [liiteri-cas-client form-by-id-cache koodisto-cache attachment-deadline-service application-id edit?]
  (let [{:keys [country-question-id
                attachment-total-size-limit
                ftp]} (get-configuration)
        application   (tutkintojen-tunnustaminen-store/get-application country-question-id application-id)
        form          (get-form form-by-id-cache koodisto-cache attachment-deadline-service application)]
    (if (tutu-form? form)
      (let [attachments (get-attachments liiteri-cas-client attachment-total-size-limit application)
            message     (if edit?
                          (->application-edited application form attachments)
                          (->application-submitted application form attachments))]
        (log/info "Sending application"
                  (if edit? "edited" "submitted")
                  "message to ASHA for application"
                  application-id)
        (transfer ftp
                  (str (:key application) "_" application-id ".xml")
                  message
                  application-id)
        (log/info "Sent application"
                  (if edit? "edited" "submitted")
                  "message to ASHA for application"
                  application-id)
        {:transition {:id :final}})
      {:transition {:id :final}})))

(defn tutkintojen-tunnustaminen-submit-job-handler
  [{:keys [application-id]} {:keys [liiteri-cas-client form-by-id-cache koodisto-cache attachment-deadline-service]}]
  (application-job-step liiteri-cas-client form-by-id-cache koodisto-cache attachment-deadline-service application-id false))

(defn tutkintojen-tunnustaminen-edit-job-handler
  [{:keys [application-id]} {:keys [liiteri-cas-client form-by-id-cache koodisto-cache attachment-deadline-service]}]
  (application-job-step liiteri-cas-client form-by-id-cache koodisto-cache attachment-deadline-service application-id true))

(defn tutkintojen-tunnustaminen-review-state-changed-job-step
  [{:keys [event-id]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service]}]
  (let [{:keys [country-question-id
                ftp]}         (get-configuration)
        application-and-state (tutkintojen-tunnustaminen-store/get-application-by-event-id country-question-id event-id)
        application           (:application application-and-state)
        application-id        (:id application)
        form                  (get-form form-by-id-cache koodisto-cache attachment-deadline-service application)]
    (if (and (tutu-form? form)
             (nil? (:review-key application-and-state))
             (= "inactivated" (:state application-and-state)))
      (let [message (->application-inactivated application)]
        (log/info "Sending application inactivated message to ASHA for application"
                  application-id)
        (transfer ftp
                  (str (:key application) "_" (:id application) "_" event-id ".xml")
                  message
                  application-id)
        (log/info "Sent application inactivated message to ASHA for application"
                  application-id)
        {:transition {:id :final}})
      {:transition {:id :final}})))

(defn timestamp [] (System/currentTimeMillis))

(defn tutkintojen-tunnustaminen-information-request-sent-job-step
  [{:keys [information-request]} {:keys [form-by-id-cache koodisto-cache attachment-deadline-service]}]
  (let [{:keys [country-question-id ftp]} (get-configuration)
        application-id (:id (tutkintojen-tunnustaminen-store/get-latest-application-id (:application-key information-request)))
        application    (tutkintojen-tunnustaminen-store/get-application country-question-id application-id)
        form           (get-form form-by-id-cache koodisto-cache attachment-deadline-service application)]
    (if (and (tutu-form? form)
             (= "information-request" (:message-type information-request)))
      (let [ts (timestamp)
            message (->application-information-request-sent application information-request ts)]
        (log/info "Sending application information request sent message to ASHA for application"
                  (:id application))
        (transfer ftp
                  (str (:key application) "_" (:id application) "_taydennyspyynto_" ts ".xml")
                  message
                  (:id application))
        (log/info "Sent application information request sent message to ASHA for application"
                  (:id application))
        {:transition {:id :final}})
      {:transition {:id :final}})))
