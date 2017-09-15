(ns ataru.hakija.hakija-routes-spec
  (:require [ataru.applications.application-store :as store]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.hakija.application-email-confirmation :as application-email]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.hakija.hakija-routes :as routes]
            [ataru.hakija.hakija-application-service :as application-service]
            [cheshire.core :as json]
            [ataru.db.db :as ataru-db]
            [ring.mock.request :as mock]
            [speclj.core :refer :all]
            [yesql.core :as sql]))

(sql/defqueries "sql/application-queries.sql")

(def ^:private form (atom nil))

(def form-blank-required-field (assoc-in application-fixtures/person-info-form-application [:answers 0 :value] ""))
(def form-invalid-email-field (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "invalid@email@foo.com"))
(def form-invalid-phone-field (assoc-in application-fixtures/person-info-form-application [:answers 5 :value] "invalid phone number"))
(def form-invalid-ssn-field (assoc-in application-fixtures/person-info-form-application [:answers 8 :value] "010101-123M"))
(def form-invalid-postal-code (assoc-in application-fixtures/person-info-form-application [:answers 11 :value] "0001"))
(def form-invalid-dropdown-value (assoc-in application-fixtures/person-info-form-application [:answers 13 :value] "kuikka"))
(def form-edited-email (assoc-in application-fixtures/person-info-form-application-for-hakukohde [:answers 2 :value] "edited@foo.com"))

(def handler (-> (routes/new-handler)
                 (assoc :tarjonta-service (tarjonta-service/new-tarjonta-service))
                 .start
                 :routes))

(defn- parse-body
  [resp]
  (if-not (nil? (:body resp))
    (update resp :body (comp #(json/parse-string % true) slurp))
    resp))

(defmacro with-response
  [method resp application & body]
  `(let [~resp (-> (mock/request ~method "/hakemus/api/application" (json/generate-string ~application))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defmacro with-get-response
  [secret resp application & body]
  `(let [~resp (-> (mock/request :get (str "/hakemus/api/application?secret=" ~secret))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defn- have-any-application-in-db
  []
  (let [app-count
        (+ (count (ataru-db/exec :db
                                 yesql-get-application-list-by-hakukohde
                                 {:hakukohde_oid (:hakukohde @form)
                                  :query_type "ALL"
                                  :authorized_organization_oids [""]}))
           (count (ataru-db/exec :db yesql-get-application-list-by-form {:form_key (:key @form)})))]
    (< 0 app-count)))

(defmacro add-spec
  [desc fixture]
  `(it ~desc
     (with-response :post resp# ~fixture
       (should= 400 (:status resp#))
       (should-not (have-any-application-in-db)))))

(defn- get-application-by-id [id]
  (first (ataru-db/exec :db yesql-get-application-by-id {:application_id id})))

(defn- have-application-in-db
  [application-id]
  (when-let [actual (get-application-by-id application-id)]
    (= (:form application-fixtures/person-info-form-application) (:form actual))))

(defn- have-application-for-hakukohde-in-db
  [application-id]
  (when-let [actual (get-application-by-id application-id)]
    (= (:form application-fixtures/person-info-form-application-for-hakukohde) (:form actual))))

(defn- cannot-edit? [answer] (true? (:cannot-edit answer)))

(defn- cannot-view? [answer] (true? (:cannot-view answer)))

(defn- get-answer
  [application key]
  (->> application
       :content
       :answers
       (filter #(= (:key %) key))
       first
       :value))

(defn- hakuaika-ended-within-10-days
  [_ _]
  {:on    false
   :start (- (System/currentTimeMillis) (* 20 24 3600 1000))
   :end   (- (System/currentTimeMillis) (* 5 24 3600 1000))})

(defn- hakuaika-ended-within-20-days
  [_ _]
  {:on    false
   :start (- (System/currentTimeMillis) (* 30 24 3600 1000))
   :end   (- (System/currentTimeMillis) (* 20 24 3600 1000))})



(describe "/application"
  (tags :unit :hakija-routes)

  (describe "POST application"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])
                    hakuaika/get-hakuaika-info                            (fn [_ _] {:on true})]
        (spec)))

    (before
      (reset! form (db/init-db-fixture)))

    (it "should validate application"
      (with-response :post resp application-fixtures/person-info-form-application
        (should= 200 (:status resp))
        (should (have-application-in-db (get-in resp [:body :id])))))

    (it "should validate application for hakukohde"
      (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
        (should= 200 (:status resp))
        (should (have-application-for-hakukohde-in-db (get-in resp [:body :id])))))

    (add-spec "should not validate form with blank required field" form-blank-required-field)

    (add-spec "should not validate form with invalid email field" form-invalid-email-field)

    (add-spec "should not validate form with invalid phone field" form-invalid-phone-field)

    (add-spec "should not validate form with invalid ssn field" form-invalid-ssn-field)

    (add-spec "should not validate form with invalid postal code field" form-invalid-postal-code)

    (add-spec "should not validate form with invalid dropdown field" form-invalid-dropdown-value))

  (describe "GET application"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])]
        (spec)))

    (before-all
      (reset! form (db/init-db-fixture)))

    (it "should create"
      (with-response :post resp application-fixtures/person-info-form-application
        (should= 200 (:status resp))
        (should (have-application-in-db (get-in resp [:body :id])))))

    (it "should not get application with wrong secret"
      (with-get-response "asdfasfas" resp
        (should= 400 (:ststus resp))))

    (it "should get application"
      (with-get-response "asdfgh" resp
        (should= 200 (:status resp))
        (let [answers (-> resp :body :answers)]
          (should= 1 (count (filter cannot-edit? answers)))
          (should= 1 (count (filter cannot-view? answers))))))

    (it "should get application with hakuaika ended"
      (with-redefs [hakuaika/get-hakuaika-info hakuaika-ended-within-10-days]
        (with-get-response "asdfgh" resp
          (should= 200 (:status resp))
          (let [answers (-> resp :body :answers)]
            (should= (count answers)
                     (count (filter cannot-edit? answers)))
            (should= 1 (count (filter cannot-view? answers))))))))

    (describe "PUT application"
      (around [spec]
        (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])
                      application-email/start-email-edit-confirmation-job (fn [_])
                      hakuaika/get-hakuaika-info hakuaika-ended-within-10-days]
          (spec)))

      (before-all
        (reset! form (db/init-db-fixture)))

      (it "should create"
        (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id])))))

      (it "should allow application edit after hakuaika within 10 days"
        (with-response :put resp form-edited-email
          (should= 200 (:status resp))
          (let [id (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "edited@foo.com" (get-answer application "email")))))

      (it "should not allow application edit after hakuaika"
        (with-redefs [hakuaika/get-hakuaika-info hakuaika-ended-within-20-days]
          (with-response :put resp application-fixtures/person-info-form-application-for-hakukohde
            (should= 400 (:status resp))
            (should= {:failures ["Not allowed to apply (not within hakuaika or review state is in complete states)"]} (:body resp)))))))
