(ns ataru.tutkintojen-tunnustaminen-spec
  (:require [ataru.applications.application-store :as application-store]
            [ataru.cache.cache-service :as cache-service]
            [ataru.files.file-store :as file-store]
            [clojure.java.shell :refer [sh]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.tutkintojen-tunnustaminen :refer [tutkintojen-tunnustaminen-edit-job-handler
                                                     tutkintojen-tunnustaminen-submit-job-handler
                                                     tutkintojen-tunnustaminen-review-state-changed-job-step
                                                     tutkintojen-tunnustaminen-information-request-sent-job-step]]
            [ataru.forms.form-store :as form-store]
            [clojure.string :as string]
            [ataru.log.audit-log :as audit-log]
            [clojure.data.xml :as xml]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [around should-contain should-be should= it describe tags]]
            [yesql.core :refer [defqueries]])
  (:import java.io.ByteArrayInputStream
           java.util.Base64))

(defqueries "sql/form-queries.sql")
(defqueries "sql/application-queries.sql")

(defn- get-file
  [filename]
  (let [config (get-in config [:tutkintojen-tunnustaminen :ftp])
        r (sh "lftp" "-c" (str (format "open --user %s --env-password %s:%d" (:user config) (:host config) (:port config))
                               (format "&& set ssl:verify-certificate %b" (:verify-certificate config true))
                               "&& set ftp:ssl-protect-data true"
                               (format "&& cd %s && cat %s" (:path config) filename))
              :env {"LFTP_PASSWORD" (:password config)})]
    (when (zero? (:exit r))
      (:out r))))

(defn- delete-file
  [filename]
  (let [config (get-in config [:tutkintojen-tunnustaminen :ftp])]
    (sh "lftp" "-c" (str (format "open --user %s --env-password %s:%d" (:user config) (:host config) (:port config))
                         (format "&& set ssl:verify-certificate %b" (:verify-certificate config true))
                         "&& set ftp:ssl-protect-data true"
                         (format "&& cd %s && rm %s" (:path config) filename))
        :env {"LFTP_PASSWORD" (:password config)})))

(defn- by-tag
  [tag elements]
  (get (group-by :tag elements) tag))

(defn- by-attr-value
  [attr value elements]
  (filter #(= value (get-in % [:attrs attr])) elements))

(defn- create-folder-by-type
  [folder-type message]
  (->> (:content message)
       (by-tag :createFolder)
       (filter (fn [{:keys [content]}]
                 (->> content
                      (by-tag :folderType)
                      (some (fn [{:keys [content]}]
                              (= [folder-type] content))))))
       first))

(defn- property-value
  [id element]
  (->> (:content element)
       (by-tag :properties)
       first
       :content
       (by-attr-value :propertyDefinitionId id)
       first
       :content
       (by-tag :value)
       first
       :content
       first))

(def attachment-metadata
  {"hakemus"        {:filename "hakemus.txt"}
   "liite-1-id"     {:size     10
                     :filename "liite-1"
                     :key      "liite-1-id"
                     :data     "liite-1-data"}
   "liite-2-1-id"   {:size     10
                     :filename "liite-2-1"
                     :key      "liite-2-1-id"
                     :data     "liite-2-1-data"}
   "liite-2-2-id"   {:size     10
                     :filename "liite-2-2"
                     :key      "liite-2-2-id"
                     :data     "liite-2-2-data"}
   "liite-3-1-1-id" {:size     10
                     :filename "liite-3-1-1"
                     :key      "liite-3-1-1-id"
                     :data     "liite-3-1-1-data"}
   "liite-3-2-1-id" {:size     10
                     :filename "liite-3-2-1"
                     :key      "liite-3-2-1-id"
                     :data     "liite-3-2-1-data"}
   "liite-3-2-2-id" {:size     10
                     :filename "liite-3-2-2"
                     :key      "liite-3-2-2-id"
                     :data     "liite-3-2-2-data"}
   "liite-3-1-2-id" {:size     10
                     :filename "liite-3-1-2"
                     :key      "liite-3-1-2-id"
                     :data     "liite-3-1-2-data"}})

(defn get-metadata
  [_ keys]
  (let [ms      (keep attachment-metadata keys)
        found   (set (map :key ms))
        missing (remove found keys)]
    (when (not-empty missing)
      (throw (new RuntimeException (str "no files "
                                        (string/join ", " missing)
                                        " found"))))
    (map #(select-keys % [:size :filename :key]) ms)))

(defn- get-attachment
  [_ key]
  (if-let [data (get-in attachment-metadata [key :data])]
    {:body (new ByteArrayInputStream (.getBytes data))}
    (throw (new RuntimeException (str "no file " key " found")))))

(def maatjavaltiot2-024
  {:uri     "maatjavaltiot2_024"
   :version 1
   :value   "024"
   :label   {:fi "Angola"
             :sv "Angola"
             :en "Angola"}})

(def maatjavaltiot2-028
  {:uri     "maatjavaltiot2_028"
   :version 1
   :value   "028"
   :label   {:fi "Antigua ja Barbuda"
             :sv "Antigua och Barbuda"
             :en "Antigua and Barbuda"}})

(def koodisto-cache-mock
  (reify cache-service/Cache
    (get-from [_ _]
      [maatjavaltiot2-024 maatjavaltiot2-028])
    (get-many-from [_ _])
    (remove-from [_ _])
    (clear-all [_])))

(def form-by-id-cache-mock
  (reify cache-service/Cache
    (get-from [_ key]
      (form-store/fetch-by-id (Integer/valueOf key)))
    (get-many-from [_ _])
    (remove-from [_ _])
    (clear-all [_])))

(def ^:dynamic *form-id* nil)
(def ^:dynamic *wrong-form-id* nil)
(def ^:dynamic *application-id* nil)
(def ^:dynamic *edited-application-id* nil)
(def ^:dynamic *in-wrong-form-application-id* nil)
(def ^:dynamic *in-wrong-form-application-key* nil)
(def ^:dynamic *event-id* nil)
(def ^:dynamic *application-key* nil)
(def ^:dynamic *application-submitted* nil)

(def liiteri-cas-client nil)

(describe "Tutkintojen tunnustaminen integration"
  (tags :unit)

  (around [it]
    (let [audit-logger  (audit-log/new-dummy-audit-logger)
          form-id       (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id (yesql-add-form<! {:name                 {:fi "Lomake"}
                                                  :content              {:content [{:id              (get-in config [:tutkintojen-tunnustaminen :country-question-id])
                                                                                    :fieldType       "dropdown"
                                                                                    :koodisto-source {:uri     "maatjavaltiot2"
                                                                                                      :version 1}
                                                                                    :label           {:fi "FI: Suoritusmaa"
                                                                                                      :sv "SV: Suoritusmaa"
                                                                                                      :en "EN: Suoritusmaa"}}
                                                                                   {:id        "liite-1"
                                                                                    :fieldType "attachment"
                                                                                    :label     {:fi "FI: Liite 1"
                                                                                                :sv "SV: Liite 1"
                                                                                                :en "EN: Liite 1"}}
                                                                                   {:id        "liite-2"
                                                                                    :fieldType "attachment"
                                                                                    :label     {:fi "FI: Liite 2"
                                                                                                :sv "SV: Liite 2"
                                                                                                :en "EN: Liite 2"}}
                                                                                   {:id         "ryhma-1"
                                                                                    :fieldClass "questionGroup"
                                                                                    :fieldType  "fieldset"
                                                                                    :label      {:fi ""
                                                                                                 :sv ""
                                                                                                 :en ""}
                                                                                    :children   [{:id        "liite-3"
                                                                                                  :fieldType "attachment"
                                                                                                  :label     {:fi "FI: Liite 3"
                                                                                                              :sv "SV: Liite 3"
                                                                                                              :en "EN: Liite 3"}}]}
                                                                                   {:id        "first-name"
                                                                                    :fieldType "textField"
                                                                                    :label     {:fi "FI: Etunimet"
                                                                                                :sv "SV: Etunimet"
                                                                                                :en "EN: Etunimet"}}
                                                                                   {:id        "last-name"
                                                                                    :fieldType "textField"
                                                                                    :label     {:fi "FI: Sukunimi"
                                                                                                :sv "SV: Sukunimi"
                                                                                                :en "EN: Sukunimi"}}]}
                                                  :created_by           "testi"
                                                  :key                  (get-in config [:tutkintojen-tunnustaminen :form-key])
                                                  :languages            {:languages ["fi"]}
                                                  :organization_oid     "1.2.246.562.10.00000000001"
                                                  :deleted              false
                                                  :locked               nil
                                                  :locked_by            nil
                                                  :used_hakukohderyhmas []
                                                  :properties           {}}
                                {:connection connection})))
          form          (form-store/fetch-by-id form-id)
          wrong-form-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id(yesql-add-form<! {:name                 {:fi "Lomake"}
                                                 :content              {:content []}
                                                 :created_by           "testi"
                                                 :key                  (str (get-in config [:tutkintojen-tunnustaminen :form-key]) "-asd")
                                                 :languages            {:languages ["fi"]}
                                                 :organization_oid     "1.2.246.562.10.00000000001"
                                                 :deleted              false
                                                 :locked               nil
                                                 :locked_by            nil
                                                 :used_hakukohderyhmas []
                                                 :properties           {}}
                                                {:connection connection})))
          application   (application-store/get-application
                          (:id (application-store/add-application
                            {:form      form-id
                             :answers   [{:key       (get-in config [:tutkintojen-tunnustaminen :country-question-id])
                                          :value     "024"
                                          :fieldType "dropdown"}
                                         {:key       "liite-1"
                                          :value     ["liite-1-id"]
                                          :fieldType "attachment"}
                                         {:key       "liite-2"
                                          :value     ["liite-2-1-id" "liite-2-2-id"]
                                          :fieldType "attachment"}
                                         {:key       "liite-3"
                                          :value     [["liite-3-1-1-id"]
                                                      []
                                                      ["liite-3-2-1-id" "liite-3-2-2-id"]]
                                          :fieldType "attachment"}
                                         {:key       "first-name"
                                          :value     "Etunimi Toinenetunimi"
                                          :fieldType "textField"}
                                         {:key       "last-name"
                                          :value     "Sukunimi"
                                          :fieldType "textField"}
                                         {:key       "birth-date"
                                          :fieldType "textField"
                                          :value     "24.09.1989"}
                                         {:key       "email"
                                          :fieldType "textField"
                                          :value     "test@example.com"}]
                             :lang      "fi"
                             :hakukohde []
                             :haku      nil}
                            []
                            form
                            {}
                            audit-logger
                            nil)))
          _             (Thread/sleep 1000) ;; avoid equal created_time
          event-id      (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id (yesql-add-application-event<! {:application_key          (:key application)
                                                               :event_type               "review-state-change"
                                                               :new_review_state         "inactivated"
                                                               :review_key               nil
                                                               :hakukohde                nil
                                                               :virkailija_oid           nil
                                                               :virkailija_organizations nil}
                                                              {:connection connection})))
          _             (Thread/sleep 1000) ;; avoid equal created_time
          edited        (:id (jdbc/with-db-transaction [_ {:datasource (db/get-datasource :db)}]
                          (application-store/update-application
                           (update application :answers #(map (fn [answer]
                                                                (cond (= (get-in config [:tutkintojen-tunnustaminen :country-question-id]) (:key answer))
                                                                      (assoc answer :value "028")
                                                                      (= "liite-3" (:key answer))
                                                                      (assoc answer :value [["liite-3-1-2-id"]])
                                                                      :else
                                                                      answer)) %))
                           []
                           form
                           {}
                           audit-logger
                           nil)))
          wrong-form-application
          (application-store/get-application
            (:id (application-store/add-application
                  {:form      wrong-form-id
                   :answers   [{:key       (get-in config [:tutkintojen-tunnustaminen :country-question-id])
                                :value     "024"
                                :fieldType "dropdown"}
                               {:key       "liite-1"
                                :value     ["liite-1-id"]
                                :fieldType "attachment"}
                               {:key       "liite-2"
                                :value     ["liite-2-1-id" "liite-2-2-id"]
                                :fieldType "attachment"}
                               {:key       "liite-3"
                                :value     [["liite-3-1-1-id"]
                                            []
                                            ["liite-3-2-1-id" "liite-3-2-2-id"]]
                                :fieldType "attachment"}
                               {:key       "first-name"
                                :value     "Etunimi Toinenetunimi"
                                :fieldType "textField"}
                               {:key       "last-name"
                                :value     "Sukunimi"
                                :fieldType "textField"}
                               {:key       "birth-date"
                                :fieldType "textField"
                                :value     "24.09.1989"}
                               {:key       "email"
                                :fieldType "textField"
                                :value     "test@example.com"}]
                    :lang      "fi"
                    :hakukohde []
                    :haku      nil}
                  []
                  form
                  {}
                  audit-logger
                  nil)))]
      (binding [*form-id*                      form-id
                *wrong-form-id*                wrong-form-id
                *application-id*               (:id application)
                *event-id*                     event-id
                *edited-application-id*        edited
                *in-wrong-form-application-id* (:id wrong-form-application)
                *in-wrong-form-application-key* (:key wrong-form-application)
                *application-key*              (:key application)
                *application-submitted*        (f/unparse (f/formatter :date-time-no-ms (t/time-zone-for-id "Europe/Helsinki"))
                                                          (:submitted application))]
        (try
          (with-redefs [file-store/get-metadata get-metadata
                        file-store/get-file     get-attachment]
            (it))
          (finally
            (delete-file (str *application-key* "_" *application-id* ".xml"))
            (delete-file (str *application-key* "_" *edited-application-id* ".xml"))
            (delete-file (str *application-key* "_" *application-id* "_" *event-id* ".xml"))
            (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
              (jdbc/execute! connection
                             ["DELETE FROM application_events
                               WHERE id = ?"
                              event-id])
              (jdbc/execute! connection
                             ["DELETE FROM applications
                               WHERE id IN (?, ?, ?)"
                              (:id application)
                              edited
                              *in-wrong-form-application-id*])
              (jdbc/execute! connection
                             ["DELETE FROM forms
                               WHERE id IN (?, ?)"
                              form-id
                              wrong-form-id])))))))
  (it "should send information request sent message to ASHA SFTP server"
    (with-redefs [ataru.tutkintojen-tunnustaminen/timestamp (fn [] 1)]
      (let [r (tutkintojen-tunnustaminen-information-request-sent-job-step
                {:information-request {:application-key *application-key*
                                       :subject "Täydennyspyyntö otsikko"
                                       :message "Hello world!"
                                       :message-type "information-request"}}
                {:form-by-id-cache form-by-id-cache-mock
                 :koodisto-cache koodisto-cache-mock})
            message (xml/parse-str
                      (get-file (str *application-key* "_" *edited-application-id* "_taydennyspyynto_1.xml")))]
        (should= {:transition {:id :final}} r)
        (should= :message (:tag message))
        (let [case (create-folder-by-type "ams_case" message)]
          (should= *application-key* (property-value "ams_studypathid" case))
          (should= "Sukunimi Etunimi Toinenetunimi" (property-value "ams_orignator" case))
          (should= "028" (property-value "ams_applicantcountry" case))
          (should= *application-submitted* (property-value "ams_registrationdate" case))
          (should= "Hakemus" (property-value "ams_title" case)))
        (let [action (create-folder-by-type "ams_action" message)]
          (should= "Täydennyspyyntö" (property-value "ams_title" action))
          (should= "02.02" (property-value "ams_processtaskid" action)))
        (let [documents (by-tag :createDocument (:content message))]
          (should= 1 (count documents))
          (let [doc (first documents)
                decoder (Base64/getDecoder)]
            (should= "fi" (property-value "ams_language" doc))
            (should= "Täydennyspyyntö" (property-value "ams_documenttype" doc))
            (should= "taydennyspyynto_1.html" (->> (:content doc)
                                                   (by-tag :contentStream)
                                                   first
                                                   :content
                                                   (by-tag :filename)
                                                   first
                                                   :content
                                                   first))
            (should-contain "<p>Hello world!</p>" (->> (:content doc)
                                                       (by-tag :contentStream)
                                                       first
                                                       :content
                                                       (by-tag :stream)
                                                       first
                                                       :content
                                                       first
                                                       (.decode decoder)
                                                       (String.))))))))

  (it "should send submit message to ASHA SFTP server"
    (let [r       (tutkintojen-tunnustaminen-submit-job-handler
                   {:application-id *application-id*}
                   {:form-by-id-cache form-by-id-cache-mock
                    :koodisto-cache koodisto-cache-mock})
          message (xml/parse-str (get-file (str *application-key* "_" *application-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Sukunimi Etunimi Toinenetunimi" (property-value "ams_orignator" case))
        (should= "024" (property-value "ams_applicantcountry" case))
        (should= *application-submitted* (property-value "ams_registrationdate" case))
        (should= "Hakemus" (property-value "ams_title" case)))
      (let [action (create-folder-by-type "ams_action" message)]
        (should= "Hakemuksen saapuminen" (property-value "ams_title" action))
        (should= "01.01" (property-value "ams_processtaskid" action)))
      (let [attachments (by-tag :createDocument (:content message))]
        (should= 7 (count attachments))
        (doseq [attachment attachments]
          (let [filename (->> (:content attachment)
                              (by-tag :contentStream)
                              first
                              :content
                              (by-tag :filename)
                              first
                              :content
                              first)
                lang     (property-value "ams_language" attachment)]
            (should-contain filename (set (map (comp :filename second) attachment-metadata)))
            (should= "fi" lang))))))

  (it "should send edit message to ASHA SFTP server"
    (let [r       (tutkintojen-tunnustaminen-edit-job-handler
                   {:application-id *edited-application-id*}
                   {:form-by-id-cache form-by-id-cache-mock
                    :koodisto-cache koodisto-cache-mock})
          message (xml/parse-str (get-file (str *application-key* "_" *edited-application-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Sukunimi Etunimi Toinenetunimi" (property-value "ams_orignator" case))
        (should= "028" (property-value "ams_applicantcountry" case))
        (should= *application-submitted* (property-value "ams_registrationdate" case))
        (should= "Hakemus" (property-value "ams_title" case)))
      (let [action (create-folder-by-type "ams_action" message)]
        (should= "Täydennys" (property-value "ams_title" action))
        (should= "01.02" (property-value "ams_processtaskid" action)))
      (let [attachments (by-tag :createDocument (:content message))]
        (should= 5 (count attachments))
        (doseq [attachment attachments]
          (let [filename (->> (:content attachment)
                              (by-tag :contentStream)
                              first
                              :content
                              (by-tag :filename)
                              first
                              :content
                              first)
                lang     (property-value "ams_language" attachment)]
            (should-contain filename (set (map (comp :filename second) attachment-metadata)))
            (should= "fi" lang))))))

  (it "should send inactivated message to ASHA SFTP server"
    (let [r       (tutkintojen-tunnustaminen-review-state-changed-job-step
                   {:event-id *event-id*}
                   {:form-by-id-cache form-by-id-cache-mock
                    :koodisto-cache koodisto-cache-mock})
          message (xml/parse-str (get-file (str *application-key* "_" *application-id* "_" *event-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Sukunimi Etunimi Toinenetunimi" (property-value "ams_orignator" case))
        (should= "024" (property-value "ams_applicantcountry" case))
        (should= *application-submitted* (property-value "ams_registrationdate" case))
        (should= "Hakemus" (property-value "ams_title" case)))
      (let [action (create-folder-by-type "ams_action" message)]
        (should= "Hakemuksen peruutus" (property-value "ams_title" action))
        (should= "03.01" (property-value "ams_processtaskid" action)))
      (let [attachments (by-tag :createDocument (:content message))]
        (should-be empty? attachments))))

  (it "should not do anything if hakemus in wrong form"
    (should= {:transition {:id :final}}
             (tutkintojen-tunnustaminen-submit-job-handler
              {:application-id *in-wrong-form-application-id*}
              {:form-by-id-cache form-by-id-cache-mock
               :koodisto-cache koodisto-cache-mock}))
    (should= nil
             (get-file (str *in-wrong-form-application-key* "_" *in-wrong-form-application-id* ".xml")))))
