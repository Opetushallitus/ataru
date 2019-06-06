(ns ataru.tutkintojen-tunnustaminen
  (:require [ataru.config.core :refer [config]]
            [ataru.db.db :as db]
            [ataru.dob :as dob]
            [clojure.data.xml :as xml]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer :all]
            [yesql.core :refer [defqueries]])
  (:import java.io.ByteArrayInputStream))

(defqueries "sql/form-queries.sql")
(defqueries "sql/application-queries.sql")

(defn- get-file
  [filename]
  (let [config (get-in config [:tutkintojen-tunnustaminen :ftp])]
    (let [r (sh "lftp" "-c" (str (format "open --user %s --env-password %s:%d" (:user config) (:host config) (:port config))
                                 (format "&& set ssl:verify-certificate %b" (:verify-certificate config true))
                                 "&& set ftp:ssl-protect-data true"
                                 (format "&& cd %s && cat %s" (:path config) filename))
                :env {"LFTP_PASSWORD" (:password config)})]
      (when (zero? (:exit r))
        (:out r)))))

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
  {"hakemus"        {:filename "hakemus.json"}
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
  [keys]
  (let [ms      (keep attachment-metadata keys)
        found   (set (map :key ms))
        missing (remove found keys)]
    (when (not-empty missing)
      (throw (new RuntimeException (str "no files "
                                        (clojure.string/join ", " missing)
                                        " found"))))
    (map #(select-keys % [:size :filename :key]) ms)))

(defn- get-attachment
  [key]
  (if-let [data (get-in attachment-metadata [key :data])]
    {:body (new ByteArrayInputStream (.getBytes data))}
    (throw (new RuntimeException (str "no file " key " found")))))

(def ^:dynamic *form-id*)
(def ^:dynamic *wrong-form-id*)
(def ^:dynamic *application-id*)
(def ^:dynamic *edited-application-id*)
(def ^:dynamic *event-id*)
(def ^:dynamic *application-key*)
(def ^:dynamic *application-submitted*)

(describe "Tutkintojen tunnustaminen integration"
  (tags :unit)

  (around [it]
    (let [form-id       (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id (yesql-add-form<! {:name             {:fi "Lomake"}
                                                  :content          {:content []}
                                                  :created_by       "testi"
                                                  :key              (get-in config [:tutkintojen-tunnustaminen :form-key])
                                                  :languages        {:languages ["fi"]}
                                                  :organization_oid "1.2.246.562.10.00000000001"
                                                  :deleted          false
                                                  :locked           nil
                                                  :locked_by        nil}
                                                 {:connection connection})))
          wrong-form-id (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id(yesql-add-form<! {:name             {:fi "Lomake"}
                                                 :content          {:content []}
                                                 :created_by       "testi"
                                                 :key              (str (get-in config [:tutkintojen-tunnustaminen :form-key]) "-asd")
                                                 :languages        {:languages ["fi"]}
                                                 :organization_oid "1.2.246.562.10.00000000001"
                                                 :deleted          false
                                                 :locked           nil
                                                 :locked_by        nil}
                                                {:connection connection})))
          application   (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (yesql-add-application<! {:form_id        form-id
                                                    :content        {:answers [{:key       (get-in config [:tutkintojen-tunnustaminen :country-question-id])
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
                                                                                            ["liite-3-2-1-id" "liite-3-2-2-id"]]
                                                                                :fieldType "attachment"}
                                                                               {:key       "first-name"
                                                                                :value     "Etunimi Toinenetunimi"
                                                                                :fieldType "textField"}
                                                                               {:key       "last-name"
                                                                                :value     "Sukunimi"
                                                                                :fieldType "textField"}]}
                                                    :lang           "fi"
                                                    :preferred_name "Testi"
                                                    :last_name      "Testi"
                                                    :hakukohde      []
                                                    :haku           nil
                                                    :person_oid     nil
                                                    :ssn            nil
                                                    :dob            (dob/str->dob "24.09.1989")
                                                    :email          "test@example.com"}
                                                   {:connection connection}))
          _             (Thread/sleep 1000) ;; avoid equal created_time
          event-id      (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (:id (yesql-add-application-event<! {:application_key  (:key application)
                                                               :event_type       "review-state-change"
                                                               :new_review_state "inactivated"
                                                               :review_key       nil
                                                               :hakukohde        nil
                                                               :virkailija_oid   nil}
                                                              {:connection connection})))
          _             (Thread/sleep 1000) ;; avoid equal created_time
          edited        (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
                          (yesql-add-application-version<! (assoc application :content
                                                                  {:answers [{:key       (get-in config [:tutkintojen-tunnustaminen :country-question-id])
                                                                              :value     "028"
                                                                              :fieldType "dropdown"}
                                                                             {:key       "liite-1"
                                                                              :value     ["liite-1-id"]
                                                                              :fieldType "attachment"}
                                                                             {:key       "liite-2"
                                                                              :value     ["liite-2-1-id" "liite-2-2-id"]
                                                                              :fieldType "attachment"}
                                                                             {:key       "liite-3"
                                                                              :value     [["liite-3-1-2-id"]]
                                                                              :fieldType "attachment"}
                                                                             {:key       "first-name"
                                                                              :value     "Etunimi Toinenetunimi"
                                                                              :fieldType "textField"}
                                                                             {:key       "last-name"
                                                                              :value     "Sukunimi"
                                                                              :fieldType "textField"}]})
                                                           {:connection connection}))]
      (binding [*form-id*               form-id
                *wrong-form-id*         wrong-form-id
                *application-id*        (:id application)
                *event-id*              event-id
                *edited-application-id* (:id edited)
                *application-key*       (:key application)
                *application-submitted* (f/unparse (f/formatter :date-time-no-ms (t/time-zone-for-id "Europe/Helsinki"))
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
                               WHERE id IN (?, ?)"
                              (:id application)
                              (:id edited)])
              (jdbc/execute! connection
                             ["DELETE FROM forms
                               WHERE id IN (?, ?)"
                              form-id
                              wrong-form-id])))))))

  (it "should send submit message to ASHA SFTP server"
    (let [r       (tutkintojen-tunnustaminen-submit-job-step
                   {:application-id *application-id*}
                   {})
          message (xml/parse-str (get-file (str *application-key* "_" *application-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Etunimi Toinenetunimi Sukunimi" (property-value "ams_orignator" case))
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
    (let [r       (tutkintojen-tunnustaminen-edit-job-step
                   {:application-id *edited-application-id*}
                   {})
          message (xml/parse-str (get-file (str *application-key* "_" *edited-application-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Etunimi Toinenetunimi Sukunimi" (property-value "ams_orignator" case))
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
                   {})
          message (xml/parse-str (get-file (str *application-key* "_" *application-id* "_" *event-id* ".xml")))]
      (should= {:transition {:id :final}} r)
      (should= :message (:tag message))
      (let [case (create-folder-by-type "ams_case" message)]
        (should= *application-key* (property-value "ams_studypathid" case))
        (should= "Etunimi Toinenetunimi Sukunimi" (property-value "ams_orignator" case))
        (should= "024" (property-value "ams_applicantcountry" case))
        (should= *application-submitted* (property-value "ams_registrationdate" case))
        (should= "Hakemus" (property-value "ams_title" case)))
      (let [action (create-folder-by-type "ams_action" message)]
        (should= "Hakemuksen peruutus" (property-value "ams_title" action))
        (should= "03.01" (property-value "ams_processtaskid" action)))
      (let [attachments (by-tag :createDocument (:content message))]
        (should-be empty? attachments))))

  (it "should not do anything if hakemus in wrong form"
    (jdbc/with-db-transaction [connection {:datasource (db/get-datasource :db)}]
      (jdbc/execute! connection
                     ["UPDATE applications
                       SET form_id = ?
                       WHERE id = ?"
                      *wrong-form-id*
                      *application-id*]))
    (should= {:transition {:id :final}}
             (tutkintojen-tunnustaminen-submit-job-step
              {:application-id *application-id*}
              {}))
    (should= nil
             (get-file (str *application-key* "_" *application-id* ".xml")))))
