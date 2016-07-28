(ns ataru.hakija.hakija-routes-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.hakija.email :as email]
            [ataru.hakija.hakija-routes :as routes]
            [cheshire.core :as json]
            [oph.soresu.common.db :as soresu-db]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [yesql.core :as sql]))

(sql/defqueries "sql/application-queries.sql")

(def form-blank-required-field (assoc-in application-fixtures/person-info-form-application [:answers 0 :value] ""))

(def handler (-> (routes/new-handler) .start :routes))

(defn- parse-body
  [resp]
  (if-not (nil? (:body resp))
    (update resp :body (comp #(json/parse-string % true) slurp))
    resp))

(defmacro with-response
  [resp application & body]
  `(let [~resp (-> (mock/request :post "/hakemus/api/application" (json/generate-string ~application))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defn- have-application-in-db
  [application-id]
  (when-let [actual (first (soresu-db/exec :db yesql-get-application-by-id {:application_id application-id}))]
    (= (:form application-fixtures/person-info-form-application) (:form actual))))

(defn- have-any-application-in-db
  []
  (let [app-count (count (soresu-db/exec :db yesql-get-application-list {:form_id 15}))]
    (< 0 app-count)))

(describe "POST /application"
  (tags :hakija)

  (around [spec]
    (with-redefs [email/send-email-verification (fn [_])]
      (spec)))

  (before (db/init-db-fixture))

  (after (db/clear-database))

  (it "should validate application"
    (with-response resp application-fixtures/person-info-form-application
      (should= 200 (:status resp))
      (should (have-application-in-db (get-in resp [:body :id])))))

  (it "should not validate form with blank required field"
    (with-response resp form-blank-required-field
      (should= 400 (:status resp))
      (should-not (have-any-application-in-db)))))
