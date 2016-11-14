(ns ataru.fixtures.db.browser-test-db
  "Database fixture, insert test-data to DB"
  (:require [yesql.core :refer [defqueries]]
            [clojure.java.jdbc :as jdbc]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.fixtures.application :as app-fixture]
            [oph.soresu.common.db :as db]))

(defqueries "sql/form-queries.sql")

(def form1 {:id 1,
            :key "foobar1",
            :name "Selaintestilomake1",
            :created-by "DEVELOPER",
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
            :created-by "DEVELOPER",
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

(def application1 {:form 1,
                   :lang "fi",
                   :key "application-key",
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
                           :value "020202A0202"}]})

(defn init-db-fixture []
  (form-store/create-form-or-increment-version! form1 "1.2.246.562.10.0439845")
  (form-store/create-form-or-increment-version! form2 "1.2.246.562.10.0439845")
  (jdbc/with-db-transaction [conn {:datasource (db/get-datasource :db)}]
    (application-store/add-new-application application1 conn)))
