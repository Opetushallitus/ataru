(ns ataru.anonymizer.anonymizer-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.anonymizer.core :as core]
            [ataru.db.db :as db]
            [ataru.forms.form-store :as form-store]
            [ataru.log.audit-log :as audit-log]
            [ataru.util :as util]
            [clojure.java.jdbc :as jdbc]
            [speclj.core :refer [before after describe it should should= should-not= tags]]))

(def ^:private test-form-id (atom nil))
(def ^:private test-application-id (atom nil))

(def audit-logger (audit-log/new-dummy-audit-logger))

(defn- delete-form [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :forms ["id = ?" id])))

(defn- delete-application! [id]
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
                            (jdbc/delete! conn :application_hakukohde_attachment_reviews
                                          ["application_key = (select key from applications where id = ?)" id])
                            (jdbc/delete! conn :applications
                                          ["id = ?" id])))

(defn- reset-database! []
  (println (str "Reseting database (test-application-id " @test-application-id ", test-form-id " @test-form-id ")."))
  (when @test-application-id
    (delete-application! @test-application-id))
  (when @test-form-id
    (delete-form @test-form-id)))

(def test-form
  {:name             {:fi "Lomake"}
   :content          []
   :created-by       "Testaaja"
   :languages        ["fi"]
   :organization-oid "1.2.246.562.10.00000000001"
   :deleted          nil
   :locked           nil
   :locked-by        nil})

(def test-application
  {:lang       "fi"
   :person-oid "1.2.246.562.24.25601805074"
   :answers    []})

(defn- add-application-with-answers [answers]
  (let [form-id        (-> (form-store/create-new-form! test-form)
                           :key
                           form-store/latest-id-by-key)
        application-id (-> test-application
                           (assoc :form form-id)
                           (assoc :answers answers)
                           (store/add-application
                             []
                             test-form
                             {}
                             audit-logger
                             {:data {:auth-type "strong"
                                     :person-oid "1.2.246.562.24.25601805074"}
                              :logged-in true})
                           :id)]
    (reset! test-form-id form-id)
    (reset! test-application-id application-id)
    application-id))

(defn- create-answer [with-properties]
  (merge
   {:key                               (util/component-id)
    :value                             ""
    :fieldType                         "textField"
    :original-question                 nil
    :duplikoitu-kysymys-hakukohde-oid  nil
    :original-followup                 nil
    :duplikoitu-followup-hakukohde-oid nil}
   with-properties))

(defn expected-anonymized-application
  []
  {:haku             nil
   :lang             "fi"
   :tunnistautuminen "strong"
   :id               @test-application-id
   :hakukohde        []
   :answers          (set
                       [{:key       "gender"
                         :value     "1"
                         :fieldType "textField"}
                        {:key       "first-name"
                         :value     "Måns Testi"
                         :fieldType "textField"}
                        {:key       "preferred-name"
                         :value     "Måns"
                         :fieldType "textField"}
                        {:key       "last-name"
                         :value     "Sarkkinen-Testi"
                         :fieldType "textField"}
                        {:key       "address"
                         :value     "Metelitie 393"
                         :fieldType "textField"}
                        {:key       "ssn"
                         :value     "090296-999D"
                         :fieldType "textField"}
                        {:key       "phone"
                         :value     "050 11581851"
                         :fieldType "textField"}
                        {:key       "email"
                         :value     "hakija-47904641@oph.fi"
                         :fieldType "textField"}
                        {:key       "postal-code"
                         :value     "00100"
                         :fieldType "textField"}
                        {:key       "birth-date"
                         :value     "09.02.1996"
                         :fieldType "textField"}
                        {:key       "postal-office"
                         :value     "HELSINKI"
                         :fieldType "textField"}
                        {:key       "home-town"
                         :value     "091"
                         :fieldType "textField"}
                        {:key       "abcd"
                         :value     "Lorem ipsum dolor sit amet, consec"
                         :fieldType "textArea"}
                        {:key       "guardian-firstname"
                         :value     ["Testi"]
                         :fieldType "textField"}
                        {:key       "guardian-lastname"
                         :value     ["Huoltaja"]
                         :fieldType "textField"}
                        {:key       "guardian-name"
                         :value     ["Testi Huoltaja"]
                         :fieldType "textField"}
                        {:key       "guardian-phone"
                         :value     ["0501234567"]
                         :fieldType "textField"}
                        {:key       "guardian-email"
                         :value     ["testi1.huoltaja@testiopintopolku.fi"]
                         :fieldType "textField"}
                        {:key       "guardian-firstname-secondary"
                         :value     ["Testi"]
                         :fieldType "textField"}
                        {:key       "guardian-lastname-secondary"
                         :value     ["Huoltaja"]
                         :fieldType "textField"}
                        {:key       "guardian-name-secondary"
                         :value     ["Testi Huoltaja"]
                         :fieldType "textField"}
                        {:key       "guardian-phone-secondary"
                         :value     ["0501234567"]
                         :fieldType "textField"}
                        {:key       "guardian-email-secondary"
                         :value     ["testi2.huoltaja@testiopintopolku.fi"]
                         :fieldType "textField"}])
   :person-oid       "1.2.246.562.24.25601805074"
   :form             @test-form-id})

(describe "Anonymizer:"
  (tags :unit :database)
  (after (reset-database!))
  (before
    (println "Adding application.")
    (add-application-with-answers [(create-answer {:value "2" :key "gender"})
                                   (create-answer {:value "John" :key "first-name"})
                                   (create-answer {:value "John" :key "preferred-name"})
                                   (create-answer {:value "Smith" :key "last-name"})
                                   (create-answer {:value "1 First Av" :key "address"})
                                   (create-answer {:value "" :key "ssn"})
                                   (create-answer {:value "23456789" :key "phone"})
                                   (create-answer {:value "john@oph.fi" :key "email"})
                                   (create-answer {:value "10001" :key "postal-code"})
                                   (create-answer {:value "2000-01-01" :key "birth-date"})
                                   (create-answer {:value "New York" :key "postal-office"})
                                   (create-answer {:value "New York" :key "home-town"})
                                   (create-answer {:value "This is what I have always wanted!" :key "abcd" :fieldType "textArea"})
                                   (create-answer {:value ["X"] :key "guardian-firstname"})
                                   (create-answer {:value ["Huoltaja"] :key "guardian-lastname"})
                                   (create-answer {:value ["X Huoltaja"] :key "guardian-name"})
                                   (create-answer {:value ["1234"] :key "guardian-phone"})
                                   (create-answer {:value ["huoltaja@oph.fi"] :key "guardian-email"})
                                   (create-answer {:value ["Y"] :key "guardian-firstname-secondary"})
                                   (create-answer {:value ["Huoltaja"] :key "guardian-lastname-secondary"})
                                   (create-answer {:value ["Y Huoltaja"] :key "guardian-name-secondary"})
                                   (create-answer {:value ["12345"] :key "guardian-phone-secondary"})
                                   (create-answer {:value ["huoltajaY@oph.fi"] :key "guardian-email-secondary"})]))
  (it "should anonymize application"
    (let [initial-application (store/get-application @test-application-id)]
      (core/anonymize-data "dev-resources/anonymized-persons.json" "f996b389-2f36-4ba2-8139-6a7acefe0e3e" true)
      (let [anonymized-application (store/get-application @test-application-id)]
        (should= (expected-anonymized-application)
                 (-> anonymized-application
                     (dissoc  :key :submitted :created-time :secret)
                     (update :answers (fn [answers] (set (map #(select-keys % [:key :value :fieldType]) answers))))))
        (should (some? (:secret initial-application)))
        (should (some? (:secret anonymized-application)))
        (should-not= (:secret initial-application) (:secret anonymized-application))))))
