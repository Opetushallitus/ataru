(ns ataru.fixtures.db.browser-test-db
  "Database fixture, insert test-data to DB"
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.db.db :as db]
            [ataru.virkailija.component-data.component :as component]
            [ataru.virkailija.component-data.person-info-module :as person-info-module]
            [ataru.fixtures.db.test-form :refer [test-form]]
            [ataru.fixtures.db.test-form-application :refer [test-form-application]]))

(defqueries "sql/form-queries.sql")

(def form1 {:id 1,
            :key "foobar1",
            :name "Selaintestilomake1",
            :created-by "DEVELOPER"
            :organization-oid "1.2.246.562.10.0439845"
            :content
            [{:fieldClass "wrapperElement",
              :id "G__31",
              :fieldType "fieldset",
              :children
                          [{:label {:fi "Kengännumero", :sv ""},
                            :fieldClass "formField",
                            :id "c2e4536c-1cdb-4450-b019-1b38856296ae",
                            :params {},
                            :fieldType "textField"}],
              :label {:fi "Jalat", :sv "Avsnitt namn"}}]})

(def form2 {:id 2,
            :key "foobar2",
            :name "Selaintestilomake2",
            :created-by "DEVELOPER"
            :organization-oid "1.2.246.562.10.0439845"
            :content
            [{:fieldClass "wrapperElement",
              :id "d5cd3c63-02a3-4c19-a61e-35d85e46602f",
              :fieldType "fieldset",
              :children
                          [{:label {:fi "Pään ympärys", :sv ""},
                            :fieldClass "formField",
                            :id "e257afce-ff30-40e1-ad6f-c224a1537d01",
                            :params {},
                            :fieldType "textField"}],
              :label {:fi "Pää", :sv "Avsnitt namn"}}]})

(def form3 {:id               3,
            :key              "41101b4f-1762-49af-9db0-e3603adae3ad",
            :name             "Selaintestilomake3",
            :created-by       "DEVELOPER"
            :organization-oid "1.2.246.562.10.0439845"
            :languages        ["fi", "en"]
            :content
                              [(component/hakukohteet)
                               (person-info-module/person-info-module)
                               {:fieldClass "wrapperElement",
                                :id         "d5cd3c63-02a3-4c19-a61e-35d85e46602f",
                                :fieldType  "fieldset",
                                :children
                                            [{:label      {:fi "Pään ympärys", :sv ""},
                                              :fieldClass "formField",
                                              :id         "e257afce-ff30-40e1-ad6f-c224a1537d01",
                                              :params     {},
                                              :fieldType  "textField"}],
                                :label      {:fi "Pää", :sv "Avsnitt namn"}}]})

(def ssn-testform {:id               4,
            :key              "41101b4f-1762-49af-9db0-e3603adae656",
            :name             "SSN_testilomake",
            :created-by       "DEVELOPER"
            :organization-oid "1.2.246.562.10.0439845"
            :languages        ["fi"]
            :content
                              [(component/hakukohteet)
                               (person-info-module/person-info-module)]})

(def application1 {:form 1,
                   :lang "fi",
                   :key "application-key1",
                   :answers
                         [{:key "c2e4536c-1cdb-4450-b019-1b38856296ae",
                           :value "47",
                           :fieldType "textField",}
                          {:fieldType "textField",
                           :key "preferred-name",
                           :value "Seija Susanna"}
                          {:fieldType "textField",
                           :key "last-name",
                           :value "Kuikeloinen"}
                          {:fieldType "textField",
                           :key "ssn",
                           :value "020202A0202"}
                          {:fieldType "textField",
                           :key "email",
                           :value "seija.kuikeloinen@gmail.com"}]})

(def application2 {:form 1,
                   :lang "fi",
                   :key "application-key2",
                   :answers
                         [{:key "c2e4536c-1cdb-4450-b019-1b38856296ae",
                           :value "39",
                           :fieldType "textField",}
                          {:fieldType "textField",
                           :key "preferred-name",
                           :value "Ari"}
                          {:fieldType "textField",
                           :key "last-name",
                           :value "Vatanen"}
                          {:fieldType "textField",
                           :key "ssn",
                           :value "141196-933S"}
                          {:fieldType "textField",
                           :key "email",
                           :value "ari.vatanen@iki.fi"}]})

(def application3 {:form 1,
                   :lang "fi",
                   :key "application-key3",
                   :answers
                         [{:key "c2e4536c-1cdb-4450-b019-1b38856296ae",
                           :value "47",
                           :fieldType "textField",}
                          {:fieldType "textField",
                           :key "preferred-name",
                           :value "Johanna Irmeli"}
                          {:fieldType "textField",
                           :key "last-name",
                           :value "Tyrni"}
                          {:fieldType "textField",
                           :key "ssn",
                           :value "020202A0202"}
                          {:fieldType "textField",
                           :key "email",
                           :value "seija.kuikeloinen@gmail.com"}]})

(defn init-db-fixture []
  (form-store/create-new-form! form1 (:key form1))
  (form-store/create-new-form! form2 (:key form2))
  (form-store/create-new-form! form3 (:key form3))
  (form-store/create-new-form! form3 "41101b4f-1762-49af-9db0-e3603adae3ae")
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (application-store/add-application application1)
    (application-store/add-application application2)
    (application-store/add-application application3)))

(defn- insert-form-and-return [form]
  (form-store/create-new-form! form (:key form))
  (->> (form-store/get-all-forms)
       (filter #(= (:name %) (:name form)))
       (first)))

(defn insert-test-form [form-name]
  (condp = form-name
  "Testilomake" (insert-form-and-return test-form)
  "SSN_testilomake" (insert-form-and-return ssn-testform)
  nil))

(defn insert-test-application [form-id]
  (application-store/add-application (merge test-form-application
                                            {:form form-id})))