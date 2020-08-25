(ns ataru.virkailija.virkailija-routes-spec
  (:require [ataru.applications.application-service :as application-service]
            [ataru.db.db :as ataru-db]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.fixtures.form :as fixtures]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.log.audit-log :as audit-log]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.test-utils :refer [login should-have-header]]
            [ataru.virkailija.editor.form-diff :as form-diff]
            [ataru.virkailija.virkailija-routes :as v]
            [cheshire.core :as json]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [ring.mock.request :as mock]
            [speclj.core :refer [before describe it run-specs should should-not-be-nil should= tags with]]))

(def virkailija-routes
  (delay (->
          (v/new-handler)
          (assoc :organization-service (org-service/->FakeOrganizationService))
          (assoc :tarjonta-service (tarjonta-service/->MockTarjontaService))
          (assoc :person-service (person-service/->FakePersonService))
          (assoc :kayttooikeus-service (kayttooikeus-service/->FakeKayttooikeusService))
          (assoc :application-service (application-service/new-application-service))
          (assoc :audit-logger (audit-log/new-dummy-audit-logger))
          (assoc :session-store (create-session-store (ataru-db/get-datasource :db)))
          .start
          :routes)))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (update-in [:headers] assoc "cookie" (login @virkailija-routes))
                   ((deref virkailija-routes)))))

(defn- get-form [id]
  (-> (mock/request :get (str "/lomake-editori/api/forms/" id))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-form [form]
  (-> (mock/request :post "/lomake-editori/api/forms"
        (json/generate-string form))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- update-form [id fragments]
  (-> (mock/request :put (str "/lomake-editori/api/forms/" id)
        (json/generate-string fragments))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-applications-list [query]
  (-> (mock/request :post "/lomake-editori/api/applications/list"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(declare resp)

(describe "GET /lomake-editori"
          (tags :unit)

  (with-static-resource resp "/lomake-editori")

  (it "should not return nil"
      (should-not-be-nil @resp))

  (it "should return HTTP 302"
      (should= 302 (:status @resp)))

  (it "should redirect to /lomake-editori/"
      (should-have-header "Location" "http://localhost:8350/lomake-editori/" @resp)))

(describe "GET /lomake-editori/"
          (tags :unit)

  (with-static-resource resp "/lomake-editori/")

  (it "should not return nil"
      (should-not-be-nil @resp))

  (it "should return HTTP 200"
      (should= 200 (:status @resp)))

  (it "should refer to the compiled app.js in response body"
      (let [body (:body @resp)]
        (should-not-be-nil (re-matches #"(?s).*<script src=\".*virkailija-app.js\?fingerprint=\d{13}\"></script>.*" body))))

  (it "should have text/html as content type"
      (should-have-header "Content-Type" "text/html; charset=utf-8" @resp))

  (it "should have Cache-Control: no-store header"
      (should-have-header "Cache-Control" "no-store" @resp)))

(describe "Getting a static resource"
          (tags :unit)

  (with-static-resource resp "/lomake-editori/js/compiled/virkailija-app.js")

  (it "should provide the resource found from the resources/ directory"
      (should-not-be-nil @resp))

  (it "should have Cache-Control: max-age  header"
      (should-have-header "Cache-Control" "public, max-age=2592000" @resp)))

(describe "Storing a form"
          (tags :unit :route-store-form)

  (with resp
    (post-form fixtures/form-with-content))

  (before
    (println (:body @resp)))

  (it "Should respond ok"
      (should= 200 (:status @resp)))

  (it "Should have an id"
      (should (some? (-> @resp :body :id))))

  (it "Should have :content with it"
      (should= (:content fixtures/form-with-content) (-> @resp :body :content))))

(defn- swap [v i1 i2]
  (assoc v i2 (v i1) i1 (v i2)))

(defn- get-structure-as-names [content]
  (map (fn [element] (if (= "questionGroup" (:fieldClass element))
                       (fixtures/get-names (:children element))
                       (get-in element [:label :fi]))) content))

(defn- get-content-from-response [response]
  (get-in response [:body :content]))

(defn- update-and-get-form [id operations]
  (update-form id operations)
  (get-form id))

(describe "Storing a fragment"
          (tags :unit :route-store-fragment)

  (it "Should handle delete"
      (let [resp        (post-form (fixtures/create-form (fixtures/create-element "A")
                                     (fixtures/create-element "B")
                                     (fixtures/create-element "C")))
            form        (:body resp)
            with-update (-> form
                            (update :content (fn [v] [(first v) (last v)])))
            operations  (form-diff/as-operations form with-update)
            new-content (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["A" "C"] (fixtures/get-names new-content))))

  (it "Should handle updates"
      (let [resp         (post-form (fixtures/create-form (fixtures/create-element "A")
                                      (fixtures/create-element "B")
                                      (fixtures/create-element "C")))
            form         (:body resp)
            with-updates (-> form
                             (update-in [:content 0 :label :fi] (fn [_] "AA"))
                             (update-in [:content 1 :label :fi] (fn [_] "BB")))
            operations   (form-diff/as-operations form with-updates)
            new-content  (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["AA" "BB" "C"] (fixtures/get-names new-content))))

  (it "Should handle (different users) update and relocate"
      (let [resp           (post-form (fixtures/create-form (fixtures/create-element "A")
                                        (fixtures/create-element "B")
                                        (fixtures/create-element "C")))
            form           (:body resp)
            with-updates   (-> form
                               (update-in [:content 0 :label :fi] (fn [_] "AA")))
            with-relocate  (-> form
                               (update :content (fn [v] (swap v 0 1))))
            _              (update-form (:id form) (form-diff/as-operations form with-updates))
            _              (update-form (:id form) (form-diff/as-operations form with-relocate))
            new-content    (get-content-from-response (get-form (:id form)))]
        (should= ["B" "AA" "C"] (fixtures/get-names new-content))))

  (it "Should handle relocation"
      (let [resp        (post-form (fixtures/create-form (fixtures/create-element "A")
                                     (fixtures/create-element "B")
                                     (fixtures/create-element "C")))
            form        (:body resp)
            with-update (-> form
                            (update :content (fn [v] (swap v 0 1))))
            operations  (form-diff/as-operations form with-update)
            new-content (get-content-from-response (update-and-get-form (:id form) operations))]
        (should= ["B" "A" "C"] (fixtures/get-names new-content))))

  (it "Should handle move out of wrapper element"
      (let [resp        (post-form (fixtures/create-form (fixtures/create-wrapper-element (fixtures/create-element "A1") (fixtures/create-element "A2"))
                                     (fixtures/create-element "B")
                                     (fixtures/create-element "C")))
            form        (:body resp)
            with-update (-> form
                            (update :content (fn [content]
                                                 (let [[wrapper & rest] content
                                                       a1    (get-in content [0 :children 0])
                                                       a2    (get-in content [0 :children 1])
                                                       wa2   (assoc wrapper :children [a2])
                                                       new-c (concat [a1 wa2] rest)]
                                                   new-c))))
            new-content (get-content-from-response (update-and-get-form (:id form) (form-diff/as-operations form with-update)))]
        (should= ["A1" ["A2"] "B" "C"] (get-structure-as-names new-content))))

  (it "Shouldn't allow conflicting updates"
      (let [resp                     (post-form (fixtures/create-form (fixtures/create-element "A")
                                                  (fixtures/create-element "B")
                                                  (fixtures/create-element "C")))
            form                     (:body resp)
            with-updates             (-> form
                                         (update-in [:content 0 :label :fi] (fn [_] "AA")))
            with-conflicting-updates (-> form
                                         (update-in [:content 0 :label :fi] (fn [_] "ABC")))
            success-response         (update-form (:id form) (form-diff/as-operations form with-updates))
            failure-response         (update-form (:id form) (form-diff/as-operations form with-conflicting-updates))]
        (should= 200 (:status success-response))
        (should= 400 (:status failure-response))))

  (it "Should allow updating form details"
      (let [resp             (post-form (fixtures/create-form (fixtures/create-element "A")
                                          (fixtures/create-element "B")
                                          (fixtures/create-element "C")))
            form             (:body resp)
            with-updates     (-> form (assoc :name {:fi "A" :en "B"}))
            operations       (form-diff/as-operations form with-updates)
            success-response (update-and-get-form (:id form) operations)
            new-content      (get-content-from-response success-response)]
        (should= ["A" "B" "C"] (fixtures/get-names new-content))
        (should= {:fi "A" :en "B"} (get-in success-response [:body :name]))))

  )

(describe "Fetching applications list"
          (tags :unit :api-applications)

  (it "Should fetch nothing when no review matches"
      (db/init-db-fixture
        fixtures/minimal-form
        (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
        [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "information-request"}
         {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "processing"}])
      (let [resp             (post-applications-list application-fixtures/applications-list-query)
            status           (:status resp)
            body             (:body resp)
            applications     (:applications body)]
        (should= 200 status)
        (should= 0 (count applications))))

  (it "Should fetch an application when review matches"
      (db/init-db-fixture
        fixtures/minimal-form
        (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
        [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
         {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
      (let [resp             (post-applications-list application-fixtures/applications-list-query)
            status           (:status resp)
            body             (:body resp)
            applications     (:applications body)]
        (should= 200 status)
        (should= 1 (count applications))))

  (it "Should fetch nothing when answer to a question does not match"
        (let [query (-> application-fixtures/applications-list-query-matching-everything
                        (assoc :option-answers {"country-of-residence" ["123"]}))]
          (db/init-db-fixture
            fixtures/person-info-form-with-more-questions
            (assoc application-fixtures/person-info-form-application-with-more-answers :form (:id fixtures/person-info-form-with-more-questions))
            [])
          (let [resp         (post-applications-list query)
                status       (:status resp)
                body         (:body resp)
                applications (:applications body)]
            (should= 200 status)
            (should= 0 (count applications)))))

  (it "Should fetch an application when answer to a question matches"
      (let [query (-> application-fixtures/applications-list-query-matching-everything
                      (assoc :option-answers {"country-of-residence" ["246"]}))]
        (db/init-db-fixture
          fixtures/person-info-form-with-more-questions
          (assoc application-fixtures/person-info-form-application-with-more-answers :form (:id fixtures/person-info-form-with-more-questions))
          [])
        (let [resp         (post-applications-list query)
              status       (:status resp)
              body         (:body resp)
              applications (:applications body)]
          (should= 200 status)
          (should= 1 (count applications)))))

  (it "Should fetch an application when answer to a question with multiple answers matches"
        (let [query (-> application-fixtures/applications-list-query-matching-everything
                        (assoc :option-answers {"nationality" ["246"]}))]
          (db/init-db-fixture
            fixtures/person-info-form-with-more-questions
            (assoc application-fixtures/person-info-form-application-with-more-answers :form (:id fixtures/person-info-form-with-more-questions))
            [])
          (let [resp         (post-applications-list query)
                status       (:status resp)
                body         (:body resp)
                applications (:applications body)]
            (should= 200 status)
            (should= 1 (count applications))))))

(run-specs)
