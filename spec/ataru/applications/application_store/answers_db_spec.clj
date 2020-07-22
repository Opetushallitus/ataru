(ns ataru.applications.application-store.answers-db-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.db.db :as db]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :as util]
            [clj-time.core :as clj-time]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [after describe it should= tags]]))

(def ^:private test-application-key (atom nil))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- find-application-key-by-id [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/query conn ["select key from applications where id = ?"
                                                   id])
                                 first
                                 :key)))

(defn- delete-application! [key]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (->> (jdbc/delete! conn :application_hakukohde_attachment_reviews ["application_key = ?" key]))
                            (->> (jdbc/delete! conn :applications ["key = ?" key]))))

(defn- reset-database! []
  (println "Tyhjennetään hakemuksia tietokannasta.")
  (delete-application! @test-application-key))

(def test-form
  {})

(def test-application
  {:key          "attachments"
   :lang         "fi"
   :last-name    "Liittäjä"
   :created-time (clj-time/date-time 2016 6 16 7 15 0)
   :submitted    (clj-time/date-time 2016 6 16 7 15 0)
   :state        "unprocessed"
   :form_id      110001
   :id           110101
   :hakukohde    ["1.2.246.562.29.123454321" "1.2.246.562.29.123454322" "1.2.246.562.29.123454323"]
   :answers      []})

(defn- add-application-with-answers [answers]
  (let [test-application (-> test-application
                             (dissoc :key)
                             (assoc :answers answers))
        application-key  (-> (store/add-application
                               test-application
                               []
                               test-form
                               {}
                               audit-logger)
                             (find-application-key-by-id))]
    (reset! test-application-key application-key)
    application-key))

(defn- poista-label [answers]
  (mapv #(dissoc % :label)
       answers))

(defn- create-answer [with-properties]
  (merge {:key       (util/component-id)
          :label     {:fi "Tekstikenttä"}
          :value     ""
          :fieldType "textField"}
         with-properties))

(describe "get application answers:"
  (tags :unit :database)

  (after
    (reset-database!))

  (describe "basic fields:"
    (it "should get answers containing a string"
      (let [answers           [(create-answer {:value "vastaus"})]
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label answers) (poista-label (:answers found-application)))))

    (it "should get answers containing a null value"
      (let [answers           [(create-answer {:value nil})]
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label answers) (poista-label (:answers found-application))))))

  (describe "question group:"
    (it "should get answers containing a null value"
      (let [answers           [(create-answer {:value [[nil] ["liite-id"]]})]
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label answers) (poista-label (:answers found-application)))))

    (it "should get answers containing an empty value as nil"
      (let [answers           [(create-answer {:value [[] ["liite-id"]]})]
            expected          (mapv #(assoc % :value [[nil] ["liite-id"]])
                                    answers)
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label expected) (poista-label (:answers found-application))))))

  (describe "repeatable fields:"
    (it "should get answers"
      (let [answers           [(create-answer {:value ["vastaus 1" "vastaus 2"]})]
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label answers) (poista-label (:answers found-application)))))

    (it "should get a null answer value"
      (let [answers           [(create-answer {:value [nil]})]
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label answers) (poista-label (:answers found-application)))))

    (it "should get an empty answer value as nil"
      (let [answers           [(create-answer {:value []})]
            expected          (mapv #(assoc % :value [nil])
                                    answers)
            application-key   (add-application-with-answers answers)
            found-application (store/get-latest-application-by-key application-key)]
        (should= (poista-label expected) (poista-label (:answers found-application)))))))
