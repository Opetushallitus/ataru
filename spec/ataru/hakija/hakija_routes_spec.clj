(ns ataru.hakija.hakija-routes-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.hakija.application-email-confirmation :as application-email]
            [ataru.hakija.hakija-routes :as routes]
            [cheshire.core :as json]
            [oph.soresu.common.db :as soresu-db]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [yesql.core :as sql]))

(sql/defqueries "sql/application-queries.sql")

(def ^:private form (atom nil))

(def form-blank-required-field (assoc-in application-fixtures/person-info-form-application [:answers 0 :value] ""))
(def form-invalid-email-field (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "invalid@email@foo.com"))
(def form-invalid-phone-field (assoc-in application-fixtures/person-info-form-application [:answers 5 :value] "invalid phone number"))
(def form-invalid-ssn-field (assoc-in application-fixtures/person-info-form-application [:answers 7 :value] "010101-123M"))
(def form-invalid-postal-code (assoc-in application-fixtures/person-info-form-application [:answers 10 :value] "0001"))
(def form-invalid-dropdown-value (assoc-in application-fixtures/person-info-form-application [:answers 12 :value] "kuikka"))

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

(defn- have-any-application-in-db
  []
  (let [app-count
        (+ (count (soresu-db/exec :db yesql-get-application-list-by-hakukohde {:form_key (:key @form) :hakukohde_oid (:hakukohde @form)}))
           (count (soresu-db/exec :db yesql-get-application-list-by-form {:form_key (:key @form)})))]
    (< 0 app-count)))

(defmacro add-spec
  [desc fixture]
  `(it ~desc
     (with-response resp# ~fixture
       (should= 400 (:status resp#))
       (should-not (have-any-application-in-db)))))

(defn- have-application-in-db
  [application-id]
  (when-let [actual (first (soresu-db/exec :db yesql-get-application-by-id {:application_id application-id}))]
    (= (:form application-fixtures/person-info-form-application) (:form actual))))

(defn- have-application-for-hakukohde-in-db
  [application-id]
  (when-let [actual (first (soresu-db/exec :db yesql-get-application-by-id {:application_id application-id}))]
    (= (:form application-fixtures/person-info-form-application-for-hakukohde) (:form actual))))

(describe "POST /application"
  (tags :unit)

  (around [spec]
    (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])]
      (spec)))

  (before
    (reset! form (db/init-db-fixture)))

  (it "should validate application"
    (with-response resp application-fixtures/person-info-form-application
      (should= 200  (:status resp))
      (should (have-application-in-db (get-in resp [:body :id])))))

  (it "should validate application for hakukohde"
      (with-response resp application-fixtures/person-info-form-application-for-hakukohde
                     (should= 200 (:status resp))
                     (should (have-application-for-hakukohde-in-db (get-in resp [:body :id])))))

  (add-spec "should not validate form with blank required field" form-blank-required-field)

  (add-spec "should not validate form with invalid email field" form-invalid-email-field)

  (add-spec "should not validate form with invalid phone field" form-invalid-phone-field)

  (add-spec "should not validate form with invalid ssn field" form-invalid-ssn-field)

  (add-spec "should not validate form with invalid postal code field" form-invalid-postal-code)

  (add-spec "should not validate form with invalid dropdown field" form-invalid-dropdown-value))
