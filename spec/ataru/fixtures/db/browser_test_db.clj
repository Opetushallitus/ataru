(ns ataru.fixtures.db.browser-test-db
  "Database fixture, insert test-data to DB"
  (:require [yesql.core :refer [defqueries]]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.fixtures.application :as app-fixture]))

(defqueries "sql/form-queries.sql")

(def form1 {:id 1,
            :name "Selaintestilomake1",
            :modified-by "DEVELOPER",
            :content
            [{:fieldClass "wrapperElement",
              :id "G__31",
              :fieldType "fieldset",
              :children
                          [{:label {:fi "Kengännumero", :sv ""},
                            :fieldClass "formField",
                            :id "c2e4536c-1cdb-4450-b019-1b38856296ae",
                            :params {},
                            :required false,
                            :fieldType "textField"}],
              :label {:fi "Jalat", :sv "Avsnitt namn"}}]})

(def form2 {:id 2,
            :name "Selaintestilomake2",
            :modified-by "DEVELOPER",
            :content
            [{:fieldClass "wrapperElement",
              :id "d5cd3c63-02a3-4c19-a61e-35d85e46602f",
              :fieldType "fieldset",
              :children
                          [{:label {:fi "Pään ympärys", :sv ""},
                            :fieldClass "formField",
                            :id "e257afce-ff30-40e1-ad6f-c224a1537d01",
                            :params {},
                            :required false,
                            :fieldType "textField"}],
              :label {:fi "Pää", :sv "Avsnitt namn"}}]})

(def application1 {:form 1,
                   :lang "fi",
                   :answers
                         [{:key "c2e4536c-1cdb-4450-b019-1b38856296ae",
                           :value "47",
                           :fieldType "textField",
                           :label {:fi "Kengännumero", :sv ""}}],
                   :state :received})

(defn init-db-fixture []
  (form-store/upsert-form form1)
  (form-store/upsert-form form2)
  (application-store/insert-application application1))
