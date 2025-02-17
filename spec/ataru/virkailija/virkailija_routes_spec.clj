(ns ataru.virkailija.virkailija-routes-spec
  (:require [ataru.applications.application-service :as application-service]
            [ataru.background-job.job :as job]
            [ataru.cache.cache-service :as cache-service]
            [ataru.config.core :refer [config]]
            [ataru.db.db :as ataru-db]
            [ataru.email.application-email-jobs :as application-email]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.fixtures.form :as fixtures]
            [ataru.fixtures.synthetic-application :as synthetic-application-fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.applications.application-store :as application-store]
            [ataru.kayttooikeus-service.kayttooikeus-service :as kayttooikeus-service]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.log.audit-log :as audit-log]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.test-utils :refer [login should-have-header]]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.virkailija.editor.form-diff :as form-diff]
            [ataru.virkailija.virkailija-routes :as v]
            [cheshire.core :as json]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [com.stuartsierra.component :as component]
            [ring.mock.request :as mock]
            [speclj.core :refer [after-all around before before-all describe
                                 it run-specs should should-be-nil should-not-be-nil should=
                                 tags with]]
            [yesql.core :as sql]))

(declare yesql-get-latest-application-by-key)
(declare yesql-get-application-by-id)
(sql/defqueries "sql/application-queries.sql")

(defn- parse-body
  [resp]
  (if-not (nil? (:body resp))
    (assoc resp :body (cond-> (:body resp)
                        (not (string? (:body resp)))
                        slurp
                        true
                        (json/parse-string true)))
    resp))

(defn- get-latest-application-by-key [key]
  (first (ataru-db/exec :db yesql-get-latest-application-by-key {:application_key key})))

(defn- get-application-by-id [id]
  (first (ataru-db/exec :db yesql-get-application-by-id {:application_id id})))

(defn- hakuaika-ongoing
  [_ _ _ _]
  (hakuaika/hakuaika-with-label {:on                                  true
                                 :start                               (- (System/currentTimeMillis) (* 2 24 3600 1000))
                                 :end                                 (+ (System/currentTimeMillis) (* 2 24 3600 1000))
                                 :hakukierros-end                     nil
                                 :jatkuva-haku?                       false
                                 :joustava-haku?                      false
                                 :jatkuva-or-joustava-haku?           false
                                 :attachment-modify-grace-period-days (-> config :public-config :attachment-modify-grace-period-days)}))

(def virkailija-routes
  (delay
    (-> (component/system-map
          :form-by-id-cache (reify cache-service/Cache
                             (get-from [_ key]
                               (form-store/fetch-by-id (Integer/valueOf key)))
                             (get-many-from [_ _])
                             (remove-from [_ _])
                             (clear-all [_]))
          :koodisto-cache     (reify cache-service/Cache
                               (get-from [_ _])
                               (get-many-from [_ _])
                               (remove-from [_ _])
                               (clear-all [_]))
          :organization-service (org-service/->FakeOrganizationService)
          :ohjausparametrit-service (ohjausparametrit-service/new-ohjausparametrit-service)
          :tarjonta-service (tarjonta-service/->MockTarjontaService)
          :session-store (create-session-store (ataru-db/get-datasource :db))
          :kayttooikeus-service (kayttooikeus-service/->FakeKayttooikeusService)
          :person-service (person-service/->FakePersonService)
          :audit-logger (audit-log/new-dummy-audit-logger)
          :job-runner (job/new-job-runner virkailija-jobs/job-definitions)
          :application-service (component/using
                                 (application-service/new-application-service)
                                 [:organization-service
                                  :tarjonta-service
                                  :audit-logger
                                  :koodisto-cache
                                  :person-service
                                  :ohjausparametrit-service])
          :virkailija-routes (component/using
                               (v/new-handler)
                               [:organization-service
                                :tarjonta-service
                                :session-store
                                :kayttooikeus-service
                                :person-service
                                :application-service
                                :audit-logger
                                :ohjausparametrit-service
                                :form-by-id-cache
                                :koodisto-cache
                                :job-runner]))
      component/start
      :virkailija-routes
      :routes)))

(defn- check-for-db-application-with-haku-and-person
  [application-id person-oid]
  (let [application (get-latest-application-by-key application-id)]
    (should-not-be-nil application)
    (should= (:id fixtures/synthetic-application-test-form) (:form application))
    (should= person-oid (:person_oid application))))

(defmacro with-synthetic-response
  [method resp applications & body]
  `(let [~resp (-> (mock/request ~method "/lomake-editori/api/synthetic-applications" (json/generate-string ~applications))
                   (mock/content-type "application/json")
                   (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
                   ((deref virkailija-routes))
                   parse-body)]
     ~@body))

(defmacro with-static-resource
  [name path]
  `(with ~name (-> (mock/request :get ~path)
                   (update-in [:headers] assoc "cookie" (login @virkailija-routes))
                   ((deref virkailija-routes)))))

(defn- get-valintapiste-application-query [query]
  (-> (mock/request :get "/lomake-editori/api/external/valintapiste" query)
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- get-tilastokeskus-application-query [query]
  (-> (mock/request :get "/lomake-editori/api/external/tilastokeskus" query)
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- get-valinta-ui-application-query [query]
  (-> (mock/request :get "/lomake-editori/api/external/valinta-ui" query)
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-vts-application-query [query]
  (-> (mock/request :post "/lomake-editori/api/external/valinta-tulos-service"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-sure-application-query [query]
  (-> (mock/request :post "/lomake-editori/api/external/suoritusrekisteri"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-siirto-application-query [query]
  (-> (mock/request :post "/lomake-editori/api/external/siirto"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-valintalaskenta-application-query [query]
  (-> (mock/request :post "/lomake-editori/api/external/valintalaskenta"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- get-application-details [application-key]
  (-> (mock/request :get (str "/lomake-editori/api/applications/" application-key))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- get-haku [form-key]
  (-> (mock/request :get (str "/lomake-editori/api/tarjonta/haku") {:form-key form-key})
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

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

(defn- post-review-notes [query]
  (-> (mock/request :post "/lomake-editori/api/applications/mass-notes"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))))

(defn- update-payment-info [key payment-info]
  (-> (mock/request :put (str "/lomake-editori/api/forms/" key "/update-payment-info")
                    (json/generate-string payment-info))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-mass-inactivate-applications [application-keys reason-of-inactivation]
  (-> (mock/request :post "/lomake-editori/api/applications/mass-inactivate"
                    (json/generate-string {:application-keys application-keys
                                           :message reason-of-inactivation}))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- post-mass-reactivate-applications [application-keys reason-of-reactivation]
  (-> (mock/request :post "/lomake-editori/api/applications/mass-reactivate"
                    (json/generate-string {:application-keys application-keys
                                           :message reason-of-reactivation}))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
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
                        (assoc :option-answers [{:key "country-of-residence" :options ["123"]}]))]
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
                      (assoc :option-answers [{:key "country-of-residence" :options ["246"]}]))]
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
                        (assoc :option-answers [{:key "nationality" :options ["246"]}]))]
          (db/init-db-fixture
            fixtures/person-info-form-with-more-questions
            (assoc application-fixtures/person-info-form-application-with-more-answers
              :form (:id fixtures/person-info-form-with-more-questions))
            [])
          (let [resp         (post-applications-list query)
                status       (:status resp)
                body         (:body resp)
                applications (:applications body)]
            (should= 200 status)
            (should= 1 (count applications)))))

  (it "Should fetch payment status with application"
      (let [application-id (db/init-db-fixture
                             fixtures/minimal-form
                             (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                             [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
                              {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
            application (application-store/get-application application-id)
            _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
            resp         (post-applications-list application-fixtures/applications-list-query)
            status       (:status resp)
            body         (:body resp)
            applications (:applications body)]
        (should= 200 status)
        (should= 1 (count applications))
        (should= application-id (:id (first applications)))
        (should= (:not-required payment/all-states)
                 (get-in (first applications) [:kk-payment-state]))))

  (it "Should include application with matching payment state"
      (let [query (-> application-fixtures/applications-list-query-matching-everything
                      (assoc-in [:states-and-filters :filters :kk-application-payment :awaiting] true))
            application-id (db/init-db-fixture
                             fixtures/minimal-form
                             (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                             [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
                              {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
            application (application-store/get-application application-id)
            _ (payment/set-application-fee-required (:key application) nil)
            resp         (post-applications-list query)
            status       (:status resp)
            body         (:body resp)
            applications (:applications body)]
        (should= 200 status)
        (should= 1 (count applications))
        (should= application-id (:id (first applications)))
        (should= (:awaiting payment/all-states)
                 (get-in (first applications) [:kk-payment-state]))))

  (it "Should filter out application with non-matching payment state"
      (let [query (-> application-fixtures/applications-list-query-matching-everything
                      (assoc-in [:states-and-filters :filters :kk-application-payment :not-required] true))
            application-id (db/init-db-fixture
                             fixtures/minimal-form
                             (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                             [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
                              {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
            application (application-store/get-application application-id)
            _ (payment/set-application-fee-required (:key application) nil)
            resp         (post-applications-list query)
            status       (:status resp)
            body         (:body resp)
            applications (:applications body)]
        (should= 200 status)
        (should= 0 (count applications)))))

(describe "Submitting mass review notes"
          (tags :unit :mass-notes)

          (it "Should accept mass review notes without hakukohde"
              (let [resp             (post-review-notes application-fixtures/application-review-notes-without-hakukohde)
                    status           (:status resp)]
                (should= 200 status)))

          (it "Should accept mass review notes with hakukohde"
              (let [resp             (post-review-notes application-fixtures/application-review-notes-with-hakukohde)
                    status           (:status resp)]
                (should= 200 status)))

          (it "Should return http 400 for invalid mass review notes"
              (let [resp             (post-review-notes application-fixtures/invalid-application-review-notes)
                    status           (:status resp)]
                (should= 400 status)))

          (it "Should return http 400 for invalid mass review notes state"
              (let [resp             (post-review-notes application-fixtures/application-review-notes-with-invalid-state)
                    status           (:status resp)]
                (should= 400 status)))

          (it "Should return http 200 for valid mass review notes state"
              (let [resp             (post-review-notes application-fixtures/application-review-notes-with-valid-state)
                    status           (:status resp)]
                (should= 200 status))))

(describe "Mass inactivate applications"
          (tags :unit :api-applications)

          (it "Should mass inactivate valid applications"
              (let [message         "Applications are missing mandatory information"
                    ; Here we just create two applications with the same form - the fixtures used bear no significance
                    application-ids (db/init-db-fixture
                                     fixtures/minimal-form
                                     [(assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                                      (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))])
                    application-keys (->> application-ids
                                          (map get-application-by-id)
                                          (map :key))
                    resp (post-mass-inactivate-applications application-keys message)
                    not-inactivated (get-in resp [:body :not-inactivated-keys])
                    application-reviews (->> application-keys
                                            (map application-store/get-application-review))
                    application-notes (->> application-keys
                                           (map application-store/get-application-review-notes)
                                           (map first))
                    status (:status resp)]
                (should= 200 status)
                (should= 2 (count application-notes))
                (should= 2 (count application-reviews))
                (should= message (-> application-notes first :notes))
                (should= message (-> application-notes second :notes))
                (should= "inactivated" (-> application-reviews first :state))
                (should= "inactivated" (-> application-reviews second :state))
                (should= [] not-inactivated)))

          (it "Should not mass inactivate applications that are already inactive"
              (let [message         "Applications are missing mandatory information"
                    application-ids (db/init-db-fixture
                                     fixtures/minimal-form
                                     [(assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                                      (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))])
                    application-keys (->> application-ids
                                          (map get-application-by-id)
                                          (map :key))
                    _ (post-mass-inactivate-applications application-keys message)
                    resp (post-mass-inactivate-applications application-keys message)
                    not-inactivated (get-in resp [:body :not-inactivated-keys])
                    application-reviews (->> application-keys
                                             (map application-store/get-application-review))
                    status (:status resp)]
                (should= 200 status)
                (should= "inactivated" (-> application-reviews first :state))
                (should= "inactivated" (-> application-reviews second :state))
                (should-not-be-nil not-inactivated)
                (should= 2 (count not-inactivated)))))

(describe "Mass reactivate applications"
          (tags :unit :api-applications)

          (it "Should mass reactivate valid applications"
              (let [message         "Applications were mistakenly inactivated"
                    application-ids (db/init-db-fixture
                                     fixtures/minimal-form
                                     [(assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                                      (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))])
                    application-keys (->> application-ids
                                          (map get-application-by-id)
                                          (map :key))
                    _ (post-mass-inactivate-applications application-keys message)
                    resp (post-mass-reactivate-applications application-keys message)
                    not-reactivated (get-in resp [:body :not-reactivated-keys])
                    application-reviews (->> application-keys
                                            (map application-store/get-application-review))
                    application-notes (->> application-keys
                                           (map application-store/get-application-review-notes)
                                           (map first))
                    status (:status resp)]
                (should= 200 status)
                (should= [] not-reactivated)
                (should= 2 (count application-notes))
                (should= 2 (count application-reviews))
                (should= message (-> application-notes first :notes))
                (should= message (-> application-notes second :notes))
                (should= "active" (-> application-reviews first :state))
                (should= "active" (-> application-reviews second :state))))

          (it "Should not reactivate applications that are already active"
              (let [message         "Applications were mistakenly inactivated"
                    application-ids (db/init-db-fixture
                                     fixtures/minimal-form
                                     [(assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                                      (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))])
                    application-keys (->> application-ids
                                          (map get-application-by-id)
                                          (map :key))
                    resp (post-mass-reactivate-applications application-keys message)
                    not-reactivated (get-in resp [:body :not-reactivated-keys])
                    application-reviews (->> application-keys
                                            (map application-store/get-application-review))
                    status (:status resp)]
                (should= 200 status)
                (should-not-be-nil not-reactivated)
                (should= 2 (count not-reactivated))
                (should= "active" (-> application-reviews first :state))
                (should= "active" (-> application-reviews second :state)))))

(describe "/synthetic-application"
          (tags :unit :api-applications)

          (defn check-synthetic-applications [resp expected-count expected-failing-indices]
            (let [applications (:body resp)
                  failures-exist (not-empty expected-failing-indices)]
              (if failures-exist
                (should= 400 (:status resp))
                (should= 200 (:status resp)))
              (should= expected-count (count applications))
              (doall
               (map-indexed (fn [idx application]
                              (if (contains? expected-failing-indices idx)
                                (do
                                  (should-be-nil (:hakemusOid application))
                                  (should-not-be-nil (:failures application))
                                  (should-not-be-nil (:code application)))
                                (do
                                  (should-be-nil (:failures application))
                                  (should-be-nil (:code application))
                                  (if failures-exist
                                    (should-be-nil (:hakemusOid application))
                                    (do
                                      (should-not-be-nil (:hakemusOid application))
                                      (should= "1.2.3.4.5.6" (:personOid application))
                                      (check-for-db-application-with-haku-and-person (:hakemusOid application) "1.2.3.4.5.6"))))))
                            applications))))

          (describe "POST synthetic application"
                    (around [spec]
                            (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                                          hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                                          koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                                                  (case uri
                                                                                                    "maatjavaltiot2"
                                                                                                    #{"246", "840"}
                                                                                                    "kunta"
                                                                                                    #{"273"}
                                                                                                    "sukupuoli"
                                                                                                    #{"1" "2"}
                                                                                                    "kieli"
                                                                                                    #{"FI" "SV" "EN"}))]
                              (spec)))
                    (before-all
                     (db/init-db-fixture fixtures/synthetic-application-test-form))

                    (after-all
                     (db/init-db-fixture fixtures/synthetic-application-test-form))

                    (it "should validate and store synthetic application for hakukohde"
                        (with-synthetic-response :post resp [synthetic-application-fixtures/synthetic-application-basic]
                          (check-synthetic-applications resp 1 #{})))

                    (it "should validate and store synthetic application for person with non-finnish ssn"
                        (with-synthetic-response :post resp [synthetic-application-fixtures/synthetic-application-foreign]
                          (check-synthetic-applications resp 1 #{})))

                    (it "should validate and store more than one synthetic applications in batch"
                        (with-synthetic-response :post resp [synthetic-application-fixtures/synthetic-application-basic
                                                             synthetic-application-fixtures/synthetic-application-foreign]
                          (check-synthetic-applications resp 2 #{})))

                    (it "should not validate and store synthetic application for haku that doesn't have synthetic applications enabled"
                        (with-synthetic-response :post resp [synthetic-application-fixtures/synthetic-application-with-disabled-haku]
                          (check-synthetic-applications resp 1 #{0})))

                    (it "should not store anything when one or more applications fail validation"
                        (with-synthetic-response :post resp [synthetic-application-fixtures/synthetic-application-basic
                                                             synthetic-application-fixtures/synthetic-application-malformed
                                                             synthetic-application-fixtures/synthetic-application-foreign]
                          (check-synthetic-applications resp 3 #{1})))))

(describe "update-payment-info"
          (tags :unit :api-forms)

          (around [spec]
                  (db/init-db-fixture fixtures/payment-properties-test-form)
                  (spec)
                  (db/nuke-old-fixture-forms-with-key (:key fixtures/payment-properties-test-form)))

          (defn check-for-db-form-payment-info
            [form-key payment-info]
            (let [form (form-store/fetch-by-key form-key)
                  properties (:properties form)]
              (should-not-be-nil form)
              (should= payment-info properties)))

          (defn update-and-check
            [updated-payment-info expected-payment-info expected-status]
            (let [response (update-payment-info
                             (:key fixtures/payment-properties-test-form)
                             updated-payment-info)
                  status (:status response)]
              (should= expected-status status)
              (check-for-db-form-payment-info
                (:key fixtures/payment-properties-test-form) expected-payment-info)))

          (it "should fail trying to set a bird fee (sanity check)"
              (update-and-check
                {:paymentType :payment-type-astu :decisionFee "bird"}
                {} 400))

          (it "should set TUTU payment information"
              (update-and-check
                {:paymentType :payment-type-tutu :processingFee "100.00"}
                {:payment {:type "payment-type-tutu" :processing-fee "100.00" :decision-fee nil}}
                200))

          (it "should fail when trying to set a fixed decision fee for TUTU"
              (update-and-check
                {:paymentType :payment-type-tutu :processingFee "100.00" :decisionFee "100.00"}
                {} 400))

          (it "should fail when trying to set a processing fee for ASTU"
              (update-and-check
                {:paymentType :payment-type-astu :processingFee "100.00" :decisionFee "100.00"}
                {} 400))

          (it "should fail when trying to set a fixed decision fee for ASTU"
              (update-and-check
                {:paymentType :payment-type-astu :decisionFee "150.00"}
                {} 400))

          (it "should not allow setting hakemusmaksu / kk payment information manually"
              (update-and-check
                {:paymentType :payment-type-kk :processingFee "1234.00"}
                {} 400))

          (it "should fail setting payment information when payment type is not valid"
              (update-and-check
                {:paymentType :payment-type-foobar :processingFee "1234.00"}
                {} 400))

          (it "should fail trying to set a negative fee"
              (update-and-check
                {:paymentType :payment-type-tutu :processingFee "-1.00"}
                {} 400))

          (it "should fail trying to set a zero fee"
              (update-and-check
                {:paymentType :payment-type-tutu :processingFee "0.00"}
                {} 400))

          (it "should successfully set a fractional fee"
              (update-and-check
                {:paymentType :payment-type-tutu :processingFee "1.9"}
                {:payment {:type "payment-type-tutu" :processing-fee "1.9" :decision-fee nil}}
                200)))

(describe "GET /tarjonta/haku payment info"
          (tags :unit)

          (it "should return admission-payment-required? true for matching higher education admission"
              (let [resp (get-haku "payment-info-test-kk-form")
                    status (:status resp)
                    body (:body resp)]
                (should= 200 status)
                (should= 1 (count body))
                (should= true (:admission-payment-required? (first body)) )))

          (it "should return admission-payment-required? false for non higher education admission"
              (let [resp (get-haku "payment-info-test-non-kk-form")
                    status (:status resp)
                    body (:body resp)]
                (should= 200 status)
                (should= 1 (count body))
                (should= false (:admission-payment-required? (first body))))))

(describe "GET kk application payment info"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return payment information for an application"
              (let [application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                                       application-fixtures/application-without-hakemusmaksu-exemption
                                                       nil)
                    application (get-application-by-id application-id)
                    _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
                    _ (payment/set-application-fee-required (:key application) nil)
                    _ (payment/set-application-fee-paid (:key application) nil)
                    resp (get-application-details (:key application))
                    status (:status resp)
                    body (:body resp)
                    payment-data (:kk-payment body)
                    form-payment (get-in body [:form :properties :payment])
                    state (get-in payment-data [:payment :state])]
                (should= 200 status)
                (should-not-be-nil payment-data)
                (should= {:type "payment-type-kk", :processing-fee "100.00", :decision-fee nil} form-payment)
                (should= (:paid payment/all-states) state))))

(defn- init-and-get-kk-fixtures []
  (let [person-oid "1.2.3.4.5.303"
        term "kausi_s"
        year 2025
        application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                           application-fixtures/application-without-hakemusmaksu-exemption
                                           nil)
        application (get-application-by-id application-id)
        haku-oid (:haku application-fixtures/application-without-hakemusmaksu-exemption)]
    [person-oid term year application haku-oid]))

(describe "valintalaskenta"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    resp (post-valintalaskenta-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
                    resp (post-valintalaskenta-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (post-valintalaskenta-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (post-valintalaskenta-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "siirto"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    resp (post-siirto-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
                    resp (post-siirto-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (post-siirto-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with-overdue kk payment"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (post-siirto-application-query [(:key application)])
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "suoritusrekisteri"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (post-sure-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
                    resp (post-sure-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (post-sure-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (post-sure-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "valinta-tulos-service"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (post-vts-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-eu-citizen (:key application) nil)
                    resp (post-vts-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (post-vts-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (post-vts-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (get-in resp [:body :applications])]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "valinta-ui"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (get-valinta-ui-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-exemption (:key application) nil)
                    resp (get-valinta-ui-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (get-valinta-ui-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (get-valinta-ui-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "tilastokeskus"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (get-tilastokeskus-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-exemption (:key application) nil)
                    resp (get-tilastokeskus-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (get-tilastokeskus-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (get-tilastokeskus-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications)))))

(describe "valintapiste"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (get-valintapiste-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-exemption (:key application) nil)
                    resp (get-valintapiste-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))))

          (it "should not return an application awaiting kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    resp (get-valintapiste-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications))))

          (it "should not return an application with overdue kk payment"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-overdue (:key application) nil)
                    resp (get-valintapiste-application-query {:hakuOid haku-oid})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 0 (count applications)))))

(run-specs)
