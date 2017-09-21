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

(def application-blank-required-field (assoc-in application-fixtures/person-info-form-application [:answers 0 :value] ""))
(def application-invalid-email-field (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "invalid@email@foo.com"))
(def application-invalid-phone-field (assoc-in application-fixtures/person-info-form-application [:answers 5 :value] "invalid phone number"))
(def application-invalid-ssn-field (assoc-in application-fixtures/person-info-form-application [:answers 8 :value] "010101-123M"))
(def application-invalid-postal-code (assoc-in application-fixtures/person-info-form-application [:answers 11 :value] "0001"))
(def application-invalid-dropdown-value (assoc-in application-fixtures/person-info-form-application [:answers 13 :value] "kuikka"))
(def application-edited-email (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "edited@foo.com"))
(def application-edited-ssn (assoc-in application-fixtures/person-info-form-application [:answers 8 :value] "020202A0202"))
(def application-for-hakukohde-edited (-> application-fixtures/person-info-form-application-for-hakukohde
                                          (assoc-in [:answers 2 :value] "edited@foo.com")
                                          (assoc-in [:answers 14 :value] ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"])))

(def application-with-extra-answers (update application-fixtures/person-info-form-application
                                      :answers
                                      conj
                                      {:key       "j1jk2h121lkh",
                                       :value     "Extra stuff!",
                                       :fieldType "textField",
                                       :label     {:fi "exxxtra", :sv ""}}))

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

(defmacro add-failing-post-spec
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

    (it "should not validate application with extra answers"
      (with-response :post resp application-with-extra-answers
        (should= 400 (:status resp))
        (should= {:failures {:extra-answers ["j1jk2h121lkh"]}} (:body resp))))

    (add-failing-post-spec "should not validate form with blank required field" application-blank-required-field)

    (add-failing-post-spec "should not validate form with invalid email field" application-invalid-email-field)

    (add-failing-post-spec "should not validate form with invalid phone field" application-invalid-phone-field)

    (add-failing-post-spec "should not validate form with invalid ssn field" application-invalid-ssn-field)

    (add-failing-post-spec "should not validate form with invalid postal code field" application-invalid-postal-code)

    (add-failing-post-spec "should not validate form with invalid dropdown field" application-invalid-dropdown-value))

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
            (should= 1 (count (filter #(not (cannot-edit? %)) answers)))
            ;; Take -1 here since the form has one more quesiton than this application has answers to, and the extra
            ;; question on the form happens to be an attachment
            (should= (- (count answers) 1 ) (count (filter cannot-edit? answers)))
            (should= 1 (count (filter cannot-view? answers))))))))

    (describe "PUT application"
      (around [spec]
        (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])
                      application-email/start-email-edit-confirmation-job (fn [_])]
          (spec)))

      (before-all
        (reset! form (db/init-db-fixture)))

      (it "should create"
        (with-response :post resp application-fixtures/person-info-form-application
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id])))))

      (it "should edit application"
        (with-response :put resp application-edited-email
          (should= 200 (:status resp))
          (let [id (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "edited@foo.com" (get-answer application "email")))))

      (it "should not allow editing ssn"
        (with-response :put resp application-edited-ssn
          (should= 200 (:status resp))
          (let [id (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "010101A123N" (get-answer application "ssn"))))))

    (describe "PUT application after hakuaika ended"
      (around [spec]
        (with-redefs [application-email/start-email-submit-confirmation-job (fn [_])
                      application-email/start-email-edit-confirmation-job (fn [_])
                      hakuaika/get-hakuaika-info hakuaika-ended-within-10-days
                      application-service/remove-orphan-attachments (fn [_ _])]
          (spec)))

      (before-all
        (reset! form (db/init-db-fixture)))

      (it "should create"
        (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id])))))

      (it "should allow application edit after hakuaika within 10 days and only changes to attachments"
        (with-response :put resp application-for-hakukohde-edited
          (should= 200 (:status resp))
          (let [id (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "aku@ankkalinna.com" (get-answer application "email"))
            (should= ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"] (get-answer application "164954b5-7b23-4774-bd44-dee14071316b")))))

      (it "should not allow application edit after hakuaika"
        (with-redefs [hakuaika/get-hakuaika-info hakuaika-ended-within-20-days]
          (with-response :put resp application-fixtures/person-info-form-application-for-hakukohde
            (should= 400 (:status resp))
            (should= {:failures ["Not allowed to apply (not within hakuaika or review state is in complete states)"]} (:body resp)))))))
