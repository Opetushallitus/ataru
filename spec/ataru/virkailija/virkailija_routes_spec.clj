(ns ataru.virkailija.virkailija-routes-spec
  (:require [ataru.applications.application-service :as application-service]
            [ataru.applications.field-deadline :as field-deadline]
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
            [ataru.kk-application-payment.kk-application-payment-status-updater-job :as kk-application-payment-status-updater-job]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service]
            [ataru.organization-service.organization-service :as org-service]
            [ataru.person-service.person-service :as person-service]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.tarjonta-service.mock-tarjonta-service :as tarjonta-service]
            [ataru.test-utils :refer [audit-entries-for login new-capturing-audit-logger
                                      new-fake-maksut-service new-fake-valinta-tulos-service
                                      fake-lasku fake-vts-response should-have-header]]
            [ataru.virkailija.background-jobs.virkailija-jobs :as virkailija-jobs]
            [ataru.virkailija.editor.form-diff :as form-diff]
            [ataru.virkailija.virkailija-routes :as v]
            [cheshire.core :as json]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [ring.mock.request :as mock]
            [clojure.string :as clj-string]
            [speclj.core :refer [after-all around before before-all describe
                                 it run-specs should should-be-nil should-contain
                                 should-not-be-nil should-not-contain should= tags with]]
            [ataru.time :as time]
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

;; Auditlokimerkinnät kerätään testien tarkastettaviksi. Järjestelmä rakennetaan delayn takana
;; kerran, joten atomi tyhjennetään testikohtaisesti (before).
(def audit-log-capture (new-capturing-audit-logger))
(def audit-entries (first audit-log-capture))

(def vts-capture (new-fake-valinta-tulos-service))
(def vts-calls (:calls vts-capture))

(def maksut-capture (new-fake-maksut-service))
(def maksut-calls (:calls maksut-capture))

(defn- reset-fakes!
  "Palauttaa kaikki jaetut keruuatomit ja fake-vastaukset lähtötilaan."
  []
  (reset! audit-entries [])
  (reset! vts-calls [])
  (reset! maksut-calls [])
  (reset! (:response vts-capture) fake-vts-response)
  (reset! (:laskut maksut-capture) [fake-lasku]))

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
          :tarjonta-service (tarjonta-service/->MockTarjontaKoutaService)
          :session-store (create-session-store (ataru-db/get-datasource :db))
          :kayttooikeus-service (kayttooikeus-service/->FakeKayttooikeusService)
          :person-service (person-service/->FakePersonService)
          :audit-logger (second audit-log-capture)
          :valinta-tulos-service (:service vts-capture)
          :maksut-service (:service maksut-capture)
          :job-runner (job/new-job-runner virkailija-jobs/job-definitions)
          :application-service (component/using
                                 (application-service/new-application-service)
                                 [:organization-service
                                  :tarjonta-service
                                  :audit-logger
                                  :koodisto-cache
                                  :person-service
                                  :ohjausparametrit-service
                                  :job-runner])
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
                                :valinta-tulos-service
                                :maksut-service
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

(defn- get-omatsivut-applications-query [person query]
  (-> (mock/request :get (str "/lomake-editori/api/external/omatsivut/applications/" person) query)
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

(defn- post-siirto-application-query-with-params [query query-params]
  (-> (mock/request :post "/lomake-editori/api/external/siirto"
                    (json/generate-string query))
      (mock/query-string query-params)
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

(defn- post-valintalaskenta-application-oids-query [query]
  (-> (mock/request :post "/lomake-editori/api/external/valintalaskenta/application-oids"
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

(defn- put-field-deadline [application-key field-id deadline]
  (-> (mock/request :put (str "/lomake-editori/api/applications/" application-key
                              "/field-deadline/" field-id)
                    (json/generate-string {:deadline deadline}))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (update-in [:headers] assoc "if-none-match" "*")
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

(defn- post-review-notes [query]
  (-> (mock/request :post "/lomake-editori/api/applications/mass-notes"
                    (json/generate-string query))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes))
      (mock/content-type "application/json")
      ((deref virkailija-routes))))

(defn- post-review-note
  ([note] (post-review-note note nil))
  ([note user]
   (-> (mock/request :post (str "/lomake-editori/api/applications/notes/" (:application-key note))
                     (json/generate-string note))
       (update-in [:headers] assoc "cookie" (login @virkailija-routes user))
       (mock/content-type "application/json")
       ((deref virkailija-routes))
       parse-body)))

(defn- delete-review-note
  ([note-id] (delete-review-note note-id nil))
  ([note-id user]
   (-> (mock/request :delete (str "/lomake-editori/api/applications/notes/" note-id))
       (update-in [:headers] assoc "cookie" (login @virkailija-routes user))
       ((deref virkailija-routes))
       parse-body)))

(defn- update-payment-info [key payment-info]
  (-> (mock/request :put (str "/lomake-editori/api/forms/" key "/update-payment-info")
                    (json/generate-string payment-info))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes "SUPERUSER"))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      (update :body (comp (fn [content] (json/parse-string content true)) slurp))))

(defn- update-review [review]
  (-> (mock/request :put (str "/lomake-editori/api/applications/review")
                    (json/generate-string review))
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
            _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
            resp         (post-applications-list application-fixtures/applications-list-query)
            status       (:status resp)
            body         (:body resp)
            applications (:applications body)]
        (should= 200 status)
        (should= 1 (count applications))
        (should= application-id (:id (first applications)))
        (should= (:not-required payment/all-states)
                 (get-in (first applications) [:kk-payment-state]))))

  (it "Should accept created-time sort offset as ISO string"
      (let [application-id (db/init-db-fixture
                             fixtures/minimal-form
                             (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
                             [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
                              {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
            application    (application-store/get-application application-id)
            query          (-> application-fixtures/applications-list-query-matching-everything
                               (assoc :sort {:order-by "created-time"
                                             :order    "asc"
                                             :offset   {:key          (:key application)
                                                        :created-time (str (:created-time application))}}))
            resp           (post-applications-list query)]
        (should= 200 (:status resp))))

  (it "Should accept created-time sort without offset"
      (db/init-db-fixture
        fixtures/minimal-form
        (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))
        [{:hakukohde "1.2.246.562.20.49028196523" :review-requirement "processing-state" :review-state "processing"}
         {:hakukohde "1.2.246.562.20.49028196524" :review-requirement "processing-state" :review-state "information-request"}])
      (let [query (assoc application-fixtures/applications-list-query-matching-everything
                         :sort {:order-by "created-time"
                                :order    "asc"})
            resp  (post-applications-list query)]
        (should= 200 (:status resp))))

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

(describe "Field deadline"
          (tags :unit :api-applications)

  (it "Should accept field deadline as ISO string"
      (with-redefs [ataru.applications.field-deadline/put-field-deadline
                    (fn [_ _ _ _ _ field-id deadline _]
                      {:field-id      field-id
                       :deadline      deadline
                       :last-modified (java.time.ZonedDateTime/parse "2026-05-06T09:00:00Z")})]
        (let [resp (put-field-deadline
                     "1.2.246.562.11.00000000000003492391"
                     "d9017916-b9f4-440b-a083-0773fa960eea"
                     "2026-05-07T09:00:00.000Z")]
          (should= 200 (:status resp))
          (should= "d9017916-b9f4-440b-a083-0773fa960eea" (get-in resp [:body :field-id]))
          (should= "2026-05-07T09:00:00Z" (get-in resp [:body :deadline]))))))

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

(defn- set-review-note-author!
  "Asettaa muistiinpanon tekijän suoraan kantaan. Reitin kautta tekijäksi tulee aina kirjautunut
   käyttäjä, joten omistajuutta koskevia tapauksia (tekijätön rivi, toisen käyttäjän omistama
   hakukohteellinen muistiinpano) ei voi muuten rakentaa."
  [note-id virkailija-oid]
  (jdbc/with-db-transaction [conn {:datasource (ataru-db/get-datasource :db)}]
    (jdbc/execute! conn ["UPDATE application_review_notes SET virkailija_oid = ? WHERE id = ?"
                         virkailija-oid note-id])))

;; auth_routes.clj:n fake-kirjautumisen henkiloOid, josta tulee istunnon :oid
(def ^:private view-only-user-oid "1.2.246.562.11.11111111015")

;; application-review-notes-with-hakukohde-fixtuurin hakukohde, johon oletuskäyttäjällä on oikeus
(def ^:private authorized-hakukohde "1.2.246.562.29.93102260101")

(defn- init-application-keys
  "Luo annetun määrän hakemuksia samalla lomakkeella ja palauttaa niiden avaimet."
  [n]
  (->> (db/init-db-fixture
         fixtures/minimal-form
         (repeat n (assoc application-fixtures/bug2139-application :form (:id fixtures/minimal-form))))
       (map get-application-by-id)
       (map :key)))

(describe "Review note audit logging"
          (tags :unit :review-note-audit)

          (before (reset-fakes!))

          (it "Should write one audit entry when a review note is added"
              (let [application-key (first (init-application-keys 1))
                    resp            (post-review-note {:application-key application-key
                                                       :notes           "Muistiinpano hakijasta"})
                    note-id         (get-in resp [:body :id])
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                (should= 200 (:status resp))
                (should-not-be-nil note-id)
                (should= 1 (count entries))
                (should= (str note-id) (get-in (first entries) [:target :noteId]))
                (should-contain "Muistiinpano hakijasta" (pr-str (:changes (first entries))))))

          (it "Should record state-name as requirement in the audit target"
              (let [application-key (first (init-application-keys 1))
                    resp            (post-review-note {:application-key application-key
                                                       :notes           "Käsittelymerkintä"
                                                       :state-name      "processing-state"})
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should= "processing-state" (get-in (first entries) [:target :requirement]))))

          ;; Tämä testi kiinnittää mapv-korjauksen: laiskalla map:llä tallennus ja lokitus
          ;; tapahtuisivat vasta vastausta serialisoitaessa, jolloin merkintöjä olisi nolla.
          (it "Should write one audit entry per application for mass review notes"
              (let [application-keys (init-application-keys 3)
                    resp             (post-review-notes {:application-keys application-keys
                                                         :notes            "Massamuistiinpano"})]
                (should= 200 (:status resp))
                (should= 3 (count (audit-entries-for audit-entries "lisäys")))
                (doseq [application-key application-keys]
                  (should= 1 (count (audit-entries-for audit-entries "lisäys" :applicationOid application-key))))))

          (it "Should write a delete audit entry containing the removed note text"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Poistettava muistiinpano"})
                                            [:body :id])
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id)
                    entries         (audit-entries-for audit-entries "poisto" :applicationOid application-key)]
                (should= 200 (:status resp))
                (should= note-id (get-in resp [:body :id]))
                (should= 1 (count entries))
                (should-contain "Poistettava muistiinpano" (pr-str (:changes (first entries))))
                (should= 0 (count (application-store/get-application-review-notes application-key)))))

          (it "Should not remove the note or write a delete entry for an unauthorized user"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Toisen organisaation muistiinpano"})
                                            [:body :id])
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id "USER-WITH-HAKUKOHDE-ORGANIZATION")]
                (should= 401 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "poisto")))
                (should= 1 (count (audit-entries-for audit-entries "epäonnistunut")))
                (should= 1 (count (application-store/get-application-review-notes application-key)))))

          ;; Katseluoikeus riittää oman muistiinpanon poistoon: samalla oikeudella se on voitu
          ;; lisätäkin, joten lisäys ilman poistomahdollisuutta olisi epäsymmetrinen.
          (it "Should allow a view-only user to delete their own note"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Oma muistiinpano"}
                                                              "VIEW-ONLY-USER")
                                            [:body :id])
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id "VIEW-ONLY-USER")]
                (should= 200 (:status resp))
                (should= 1 (count (audit-entries-for audit-entries "poisto" :applicationOid application-key)))
                (should= 0 (count (application-store/get-application-review-notes application-key)))))

          ;; Muistiinpanolla ei ole hakukohdetta, jolloin oikeustarkistus kohdistuu hakemukseen.
          ;; Toisen tekemän muistiinpanon poisto on käsittelytoimenpide, johon katseluoikeus ei riitä.
          (it "Should not allow a view-only user to delete another user's note"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Toisen muistiinpano"})
                                            [:body :id])
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id "VIEW-ONLY-USER")]
                (should= 401 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "poisto")))
                (should= 1 (count (audit-entries-for audit-entries "epäonnistunut")))
                (should= 1 (count (application-store/get-application-review-notes application-key)))))

          ;; Tekijätön muistiinpano ei ole kenenkään oma, joten siihen vaaditaan muokkausoikeus.
          (it "Should not treat a note with no author as the view-only user's own note"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Tekijätön muistiinpano"}
                                                              "VIEW-ONLY-USER")
                                            [:body :id])
                    _               (set-review-note-author! note-id nil)
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id "VIEW-ONLY-USER")]
                (should= 401 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "poisto")))
                (should= 1 (count (application-store/get-application-review-notes application-key)))))

          ;; Hakukohteellinen muistiinpano on osa hakukohteen käsittelyä, joten omistajuus ei
          ;; kevennä vaatimusta: poistoon tarvitaan muokkausoikeus vaikka muistiinpano olisi oma.
          (it "Should require edit rights to delete an own note that has a hakukohde"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :hakukohde       authorized-hakukohde
                                                               :notes           "Oma hakukohteellinen muistiinpano"})
                                            [:body :id])
                    _               (set-review-note-author! note-id view-only-user-oid)
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id "VIEW-ONLY-USER")]
                (should= 401 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "poisto")))
                (should= 1 (count (audit-entries-for audit-entries "epäonnistunut")))
                (should= 1 (count (application-store/get-application-review-notes application-key)))))

          ;; Massapassivointi ja -palautus luovat muistiinpanon store-kerroksessa suoraan
          ;; (application_store.clj inactivate-application / reactivate-application), eivät
          ;; muistiinpanoreitin kautta. Varmistetaan että myös nämä auditlokitetaan.
          (it "Should write a lisäys entry for the note created by mass inactivate"
              (let [message          "Hakemuksilta puuttuu pakollisia tietoja"
                    application-keys (init-application-keys 2)
                    _                (reset! audit-entries [])
                    resp             (post-mass-inactivate-applications application-keys message)]
                (should= 200 (:status resp))
                (doseq [application-key application-keys]
                  (let [entries (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                    (should= 1 (count entries))
                    (should-contain message (pr-str (:changes (first entries))))))))

          (it "Should write a lisäys entry for the note created by mass reactivate"
              (let [application-keys (init-application-keys 2)
                    _                (post-mass-inactivate-applications application-keys "Passivoidaan")
                    _                (reset! audit-entries [])
                    resp             (post-mass-reactivate-applications application-keys "Palautetaan käsittelyyn")]
                (should= 200 (:status resp))
                (doseq [application-key application-keys]
                  (let [entries (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                    (should= 1 (count entries))
                    (should-contain "Palautetaan käsittelyyn" (pr-str (:changes (first entries))))))))

          ;; Huom: kirjautuminen tuottaa oman "kirjautuminen"-merkintänsä, joten tarkastellaan
          ;; vain muistiinpanoon liittyviä operaatioita.
          (it "Should return 404 and write no note audit entry for an unknown note"
              (let [resp (delete-review-note 999999)]
                (should= 404 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "poisto")))
                (should= 0 (count (audit-entries-for audit-entries "epäonnistunut")))))

          ;; Poisto on idempotentti: toinen kutsu onnistuu, mutta ei tuota uutta merkintää.
          (it "Should not write a second delete entry when a note is removed twice"
              (let [application-key (first (init-application-keys 1))
                    note-id         (get-in (post-review-note {:application-key application-key
                                                               :notes           "Kahdesti poistettava"})
                                            [:body :id])
                    _               (delete-review-note note-id)
                    _               (reset! audit-entries [])
                    resp            (delete-review-note note-id)]
                (should= 200 (:status resp))
                (should= note-id (get-in resp [:body :id]))
                (should= 0 (count (audit-entries-for audit-entries "poisto"))))))

(defn- raw-get
  "GET ilman body-parsintaa: nämä reitit vastaavat 307-uudelleenohjauksella."
  ([path] (raw-get path nil))
  ([path user]
   (-> (mock/request :get path)
       (update-in [:headers] assoc "cookie" (login @virkailija-routes user))
       ((deref virkailija-routes)))))

(defn- secret-from-redirect
  "Poimii virkailija-secretin Location-otsakkeesta."
  [resp]
  (some-> (get-in resp [:headers "Location"])
          (clj-string/split #"virkailija-secret=")
          second
          (clj-string/split #"&")
          first))

(defn- select-organization
  "Valitsee organisaation annetuilla oikeuksilla. Samaa evästettä käyttämällä valinta säilyy
   istunnossa seuraaviin pyyntöihin."
  [oid rights cookie]
  (-> (mock/request :post (str "/lomake-editori/api/organization/user-organization/" oid
                               "?rights=" (clj-string/join "&rights=" rights)))
      (update-in [:headers] assoc "cookie" cookie)
      ((deref virkailija-routes))
      parse-body))

(defn- post-form-with-cookie [form cookie]
  (-> (mock/request :post "/lomake-editori/api/forms" (json/generate-string form))
      (update-in [:headers] assoc "cookie" cookie)
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

;; Oletuskäyttäjällä ja VIEW-ONLY-USERilla on sama organisaatio mutta eri oikeustaso
;; (auth_routes.clj: EDITORI_CRUD vs HAKEMUS_READ). Kun lomake kiinnitetään tähän
;; organisaatioon, testissä eroaa vain oikeus, ei organisaatiojäsenyys.
(def ^:private shared-organization "1.2.246.562.10.0439845")

(defn- user-info [cookie]
  (-> (mock/request :get "/lomake-editori/api/user-info")
      (update-in [:headers] assoc "cookie" cookie)
      ((deref virkailija-routes))
      parse-body))

(defn- rights-for-organization [cookie oid]
  (->> (get-in (user-info cookie) [:body :organizations])
       (filter #(= oid (:oid %)))
       first
       :rights
       set))

(defn- resend-maksu-link [application-key]
  (-> (mock/request :post "/lomake-editori/api/maksut/resend-maksu-link"
                    (json/generate-string {:application-key application-key
                                           :locale          "fi"}))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes nil))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

(defn- resend-modify-link [application-key]
  (-> (mock/request :post (str "/lomake-editori/api/applications/" application-key "/resend-modify-link"))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes nil))
      ((deref virkailija-routes))
      parse-body))

(describe "Secret minting audit logging"
          (tags :unit :secret-audit)

          (before (reset-fakes!))

          ;; Ohitetaan sähköpostijobi, koska testien kohde on auditlokitus.
          (around [spec]
                  (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)]
                    (spec)))

          (it "Should write a lisäys entry when a create secret is minted for a haku"
              (let [resp    (raw-get "/lomake-editori/api/preview/haku/1.2.246.562.29.1?lang=fi")
                    entries (audit-entries-for audit-entries "lisäys")]
                (should= 307 (:status resp))
                (should= 1 (count entries))
                (should= "1.2.246.562.29.1" (get-in (first entries) [:target :hakuOid]))
                (should-contain "virkailija-create" (pr-str (:changes (first entries))))))

          (it "Should write a lisäys entry when a create secret is minted for a form"
              (let [resp    (raw-get "/lomake-editori/api/preview/form/some-form-key?lang=sv")
                    entries (audit-entries-for audit-entries "lisäys")]
                (should= 307 (:status resp))
                (should= 1 (count entries))
                (should= "some-form-key" (get-in (first entries) [:target :formKey]))
                (should-contain "sv" (pr-str (:changes (first entries))))))

          ;; Itse salaisuus ei saa päätyä auditlokille. Merkinnästä käy ilmi vain mihin ja millainen tunniste luotiin.
          (it "Should never write the minted secret into the audit entry"
              (let [resp   (raw-get "/lomake-editori/api/preview/haku/1.2.246.562.29.1?lang=fi")
                    secret (secret-from-redirect resp)
                    entry  (first (audit-entries-for audit-entries "lisäys"))]
                (should-not-be-nil secret)
                (should-not-contain secret (pr-str entry))))

          (it "Should write a lisäys entry when an update secret is minted for an application"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (raw-get (str "/lomake-editori/api/applications/" application-key "/modify"))
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                (should= 307 (:status resp))
                (should= 1 (count entries))
                (should-contain "virkailija-update" (pr-str (:changes (first entries))))
                (should-not-be-nil (secret-from-redirect resp))
                (should-not-contain (secret-from-redirect resp) (pr-str (first entries)))))

          (it "Should write a lisäys entry when a rewrite secret is minted for an application"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (raw-get (str "/lomake-editori/api/applications/" application-key "/rewrite-modify")
                                             "SUPERUSER")
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                (should= 307 (:status resp))
                (should= 1 (count entries))
                (should-contain "virkailija-rewrite" (pr-str (:changes (first entries))))
                (should-not-be-nil (secret-from-redirect resp))
                (should-not-contain (secret-from-redirect resp) (pr-str (first entries)))))

          ;; Rewrite-secret vaatii pääkäyttäjäoikeudet; ilman niitä salaisuutta ei luoda eikä
          ;; merkintää synny.
          (it "Should not mint a rewrite secret or write an entry for a non-superuser"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (raw-get (str "/lomake-editori/api/applications/" application-key "/rewrite-modify"))]
                (should= 400 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "lisäys" :applicationOid application-key)))))

          ;; Linkin uudelleenlähetys kierrättää hakijan salaisuuden, joten se on muutos.
          ;; Ennen tätä lokiin jäi vain oikeustarkistuksen "luku"-merkintä.
          ;;
          (it "Should write a muutos entry when a modify link is resent"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (resend-modify-link application-key)
                    entries         (audit-entries-for audit-entries "muutos" :applicationOid application-key)]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should-contain "secret-rotated" (pr-str (:changes (first entries))))))

          ;; Ilman aktiivista laskua linkkiä ei lähetetä eikä salaisuutta kierrätetä.
          (it "Should not write an entry when the application has no active lasku"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! (:laskut maksut-capture) [])
                    resp            (resend-maksu-link application-key)]
                (should= 404 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "muutos"
                                                     :applicationOid application-key)))))

          ;; Maksu-linkin uudelleenlähetys kierrättää salaisuuden samalla tavalla kuin
          ;; muokkauslinkki.
          (it "Should write a muutos entry when a maksu link is resent"
              (let [application-key (first (init-application-keys 1))
                    resp            (resend-maksu-link application-key)
                    entries         (audit-entries-for audit-entries "muutos" :applicationOid application-key)
                    entry           (pr-str (first entries))]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should-contain "secret-rotated" entry)
                (should-contain "maksu" entry)
                ;; Maksu-url rakennetaan laskun salaisuudesta, joten salaisuus ei saa vuotaa
                ;; merkintään sitäkään kautta.
                (should-not-contain "lasku-secret-1" entry))))

(defn- patch-valinnan-tulos [valintatapajono-oid body]
  (-> (mock/request :patch (str "/lomake-editori/api/valinta-tulos-service/valinnan-tulos/"
                                valintatapajono-oid)
                    (json/generate-string body))
      (update-in [:headers] assoc
                 "cookie" (login @virkailija-routes nil)
                 "if-unmodified-since" "Mon, 1 Jan 2026 00:00:00 GMT")
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

(defn- put-hyvaksynnan-ehto [hakukohde-oid application-key ehto if-unmodified-since]
  (-> (mock/request :put (str "/lomake-editori/api/valinta-tulos-service/hyvaksynnan-ehto"
                              "/hakukohteessa/" hakukohde-oid "/hakemus/" application-key)
                    (json/generate-string ehto))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes nil))
      (cond-> if-unmodified-since
              (update-in [:headers] assoc "if-unmodified-since" if-unmodified-since))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

(defn- delete-hyvaksynnan-ehto [hakukohde-oid application-key]
  (-> (mock/request :delete (str "/lomake-editori/api/valinta-tulos-service/hyvaksynnan-ehto"
                                 "/hakukohteessa/" hakukohde-oid "/hakemus/" application-key))
      (update-in [:headers] assoc
                 "cookie" (login @virkailija-routes nil)
                 "if-unmodified-since" "Mon, 1 Jan 2026 00:00:00 GMT")
      ((deref virkailija-routes))
      parse-body))

(defn- post-maksupyynto [lasku user]
  (-> (mock/request :post "/lomake-editori/api/maksut/maksupyynto"
                    (json/generate-string lasku))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes user))
      (mock/content-type "application/json")
      ((deref virkailija-routes))
      parse-body))

(defn- resend-hakemusmaksu-email [hakemus-oid user]
  (-> (mock/request :post (str "/lomake-editori/api/maksut/hakemusmaksu/email/laheta/" hakemus-oid))
      (update-in [:headers] assoc "cookie" (login @virkailija-routes user))
      ((deref virkailija-routes))
      parse-body))

(defn- lasku-for [application-key]
  {:first-name "Aku"
   :last-name  "Ankka"
   :email      "aku@ankkalinna.com"
   :amount     "100"
   :due-date   "2026-12-31"
   :origin     "tutu"
   :reference  application-key
   :locale     "fi"
   :message    "Maksupyyntö"})

(describe "Maksut audit logging"
          (tags :unit :maksut-audit)

          (before (reset-fakes!))

          (it "Should write a lisäys entry when an invoice is created"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (post-maksupyynto (lasku-for application-key) nil)
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should= "ORDER-1" (get-in (first entries) [:target :orderId]))
                (should-contain "tutu" (pr-str (:changes (first entries))))))

          ;; Laskua (tai ylipäänsä kutsua maksut-palveluun) ei saa syntyä oikeudettomasta kutsusta.
          (it "Should not create an invoice when the caller is not authorized"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    _               (reset! maksut-calls [])
                    resp            (post-maksupyynto (lasku-for application-key) "VIEW-ONLY-USER")]
                (should= 401 (:status resp))
                (should= 0 (count (filter #(= :create-paatos-lasku (:op %)) @maksut-calls)))
                (should= 0 (count (audit-entries-for audit-entries "lisäys" :applicationOid application-key)))))

          ;; Maksupyynnölle on aina löydyttävä hakemus johon se liittyy. Jos ei löydy, tarkistetaan
          ;; että rajapinta vastaa 404 ja että kutsua maksut-palveluun ei tehdä.
          (it "Should not create an invoice for a reference that matches no application"
              (let [_    (reset! audit-entries [])
                    _    (reset! maksut-calls [])
                    resp (post-maksupyynto (lasku-for "ei-olemassa-olevaa-hakemusta") nil)]
                (should= 404 (:status resp))
                (should= 0 (count (filter #(= :create-paatos-lasku (:op %)) @maksut-calls)))
                (should= 0 (count (audit-entries-for audit-entries "lisäys")))))

          ;; Salaisuus ja sen sisältävä maksu-url eivät kuulu lokille, kuten eivät myöskään hakijan
          ;; henkilötiedot. Hakemus yksilöidään target-kentässä.
          (it "Should not write the invoice secret or the applicant's personal data"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    _               (post-maksupyynto (lasku-for application-key) nil)
                    entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)
                    entry           (pr-str (first entries))]
                (should= 1 (count entries))
                (should= application-key (get-in (first entries) [:target :applicationOid]))
                (should-not-contain "lasku-secret-1" entry)
                (should-not-contain "virkailija-secret" entry)
                (should-not-contain "aku@ankkalinna.com" entry)
                (should-not-contain "Ankka" entry)))

          ;; Sähköpostijobi ohitetaan: start-payment-email-job tarvitsee tarjonta-servicen
          ;; job-runnerin riippuvuutena, eikä testin job-runnerilla ole riippuvuuksia. Testin
          ;; kohde on merkintä, ei sähköpostikoneisto — sama ohitus kuin muokkauslinkkitestissä.
          (it "Should write a lisäys entry when a hakemusmaksu email is resent"
              (with-redefs [kk-application-payment-status-updater-job/resend-payment-email
                            (constantly nil)]
                (let [application-key (first (init-application-keys 1))
                      _               (reset! audit-entries [])
                      resp            (resend-hakemusmaksu-email application-key nil)
                      entries         (audit-entries-for audit-entries "lisäys" :applicationOid application-key)]
                  (should= 200 (:status resp))
                  (should= 1 (count entries))
                  (should-contain "hakemusmaksu" (pr-str (:changes (first entries)))))))

          ;; Tämä reitti tarkistaa oikeudet ennen toimintaa, joten estetystä kutsusta ei synny
          ;; merkintää eikä sivuvaikutuksia.
          (it "Should not write an entry when the email resend is unauthorized"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (resend-hakemusmaksu-email application-key "VIEW-ONLY-USER")]
                (should= 401 (:status resp))
                (should= 0 (count (audit-entries-for audit-entries "lisäys" :applicationOid application-key))))))

;; Varsinaisen muutoksen lokittaa valinta-tulos-service itse. Nämä merkinnät kertovat kuka
;; muutosta yritti: Ataru kutsuu VTS:ää palvelutunnuksella eikä välitä loppukäyttäjän
;; identiteettiä, joten VTS:n omasta merkinnästä tekijä ei selviä. Vastausta ei tarkisteta.
(describe "Valinta-tulos-service change audit logging"
          (tags :unit :valinta-audit)

          (before (reset-fakes!))

          (it "Should write a muutos entry for a kevyt valinta patch"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (patch-valinnan-tulos
                                      "1.2.246.562.20.1"
                                      [{:hakemusOid {:s application-key}}])
                    entries         (audit-entries-for audit-entries "muutos")]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should= "1.2.246.562.20.1"
                         (get-in (first entries) [:target :valintatapajonoOid]))
                (should-contain application-key (pr-str (:changes (first entries))))
                ;; Kutsu meni myös perille.
                (should= 1 (count @vts-calls))))

          ;; Hakemus-oidit eivät saa päätyä Target-kenttään: rajaamaton lista ylittäisi kentän
          ;; kokorajan ja merkintä katoaisi lokin vastaanotossa.
          (it "Should keep a large kevyt valinta patch out of the target fields"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    body            (vec (repeat 2000 {:hakemusOid {:s application-key}}))
                    _               (patch-valinnan-tulos "1.2.246.562.20.1" body)
                    entry           (first (audit-entries-for audit-entries "muutos"))]
                (should= 1 (count (audit-entries-for audit-entries "muutos")))
                (doseq [[_ v] (:target entry)]
                  (should (< (count (.getBytes (str v) "UTF-8")) 32766)))))

          (it "Should write a lisäys entry when a hyvaksynnan ehto is created"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (put-hyvaksynnan-ehto "1.2.246.562.20.1" application-key
                                                          {:ehto "Ehdollinen"} nil)
                    entries         (audit-entries-for audit-entries "lisäys")]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should= application-key (get-in (first entries) [:target :applicationOid]))
                (should= "1.2.246.562.20.1" (get-in (first entries) [:target :hakukohdeOid]))))

          ;; if-unmodified-since erottaa muokkauksen luonnista, kuten VTS-asiakaskin tekee.
          (it "Should write a muutos entry when a hyvaksynnan ehto is updated"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (put-hyvaksynnan-ehto "1.2.246.562.20.1" application-key
                                                          {:ehto "Ehdollinen"}
                                                          "Mon, 1 Jan 2026 00:00:00 GMT")]
                (should= 200 (:status resp))
                (should= 1 (count (audit-entries-for audit-entries "muutos")))
                (should= 0 (count (audit-entries-for audit-entries "lisäys")))))

          (it "Should write a poisto entry when a hyvaksynnan ehto is deleted"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! audit-entries [])
                    resp            (delete-hyvaksynnan-ehto "1.2.246.562.20.1" application-key)
                    entries         (audit-entries-for audit-entries "poisto")]
                (should= 200 (:status resp))
                (should= 1 (count entries))
                (should= application-key (get-in (first entries) [:target :applicationOid]))
                ;; Poiston arvo kuuluu :old-kenttään kuten muissakin poistoissa, jotta
                ;; operaatiosuodatus poimii sen. :old päätyy changes-taulukkoon oldValue-kenttänä.
                (should-contain :oldValue (first (:changes (first entries))))))

          ;; Yritys kirjataan vastauksesta riippumatta — merkintä kertoo kuka yritti, ei mitä
          ;; VTS:ssä lopulta tapahtui.
          (it "Should write the entry even when valinta-tulos-service refuses the change"
              (let [application-key (first (init-application-keys 1))
                    _               (reset! (:response vts-capture) {:status 409 :headers {} :body "{}"})
                    resp            (delete-hyvaksynnan-ehto "1.2.246.562.20.1" application-key)]
                (should= 409 (:status resp))
                (should= 1 (count (audit-entries-for audit-entries "poisto"))))))

;; Organisaation valinta ei ole auditlokitettu, koska se vain kaventaa toimivaltaa (pääkäyttäjä valitsee itselleen
;; pääkäyttäjän oikeuksia kapeammat oikeudet johonkin organisaatioon).
(describe "Organization selection rights"
          (tags :unit :organization-selection)

          (it "Should not attach requested rights to a non-superuser's selected organization"
              (let [cookie (login @virkailija-routes "VIEW-ONLY-USER")
                    resp   (select-organization shared-organization
                                                ["form-edit" "edit-applications"]
                                                cookie)
                    rights (set (get-in resp [:body :rights]))]
                (should= 200 (:status resp))
                ;; VIEW-ONLY-USERilla on vain ATARU_HAKEMUS_READ, eikä pyydettyjä oikeuksia
                ;; kirjoiteta istuntoon.
                (should-contain "view-applications" rights)
                (should-not-contain "form-edit" rights)
                (should-not-contain "edit-applications" rights)))

          (it "Should deny a form edit on the user's own organization when the right is missing"
              (let [cookie (login @virkailija-routes "VIEW-ONLY-USER")
                    form   (assoc fixtures/form-with-content :organization-oid shared-organization)
                    rights (rights-for-organization cookie shared-organization)
                    resp   (post-form-with-cookie form cookie)]
                ;; Käyttäjä kuuluu organisaatioon, mutta vain katseluoikeudella.
                (should-contain "view-applications" rights)
                (should-not-contain "form-edit" rights)
                (should= 400 (:status resp))
                (should= "Käyttäjällä ei lomakkeen muokkausoikeutta" (-> resp :body :error))))

          ;; Varsinainen vuototesti. Ei-pääkäyttäjä ei saa asettaa itselleen laajempia oikeuksia omaan organisaatioon.
          (it "Should not let requested rights enable a form edit for a non-superuser"
              (let [cookie      (login @virkailija-routes "VIEW-ONLY-USER")
                    form        (assoc fixtures/form-with-content :organization-oid shared-organization)
                    before      (post-form-with-cookie form cookie)
                    _           (select-organization shared-organization ["form-edit"] cookie)
                    after       (post-form-with-cookie form cookie)
                    ;; Verrokki: samaan organisaatioon form-edit-oikeuden omaava käyttäjä pääsee
                    ;; samasta pyynnöstä läpi, joten esto johtuu oikeudesta eikä lomakkeesta.
                    with-rights (post-form-with-cookie form (login @virkailija-routes nil))]
                (should= 200 (:status with-rights))
                (should= 400 (:status before))
                (should= 400 (:status after))
                (should= "Käyttäjällä ei lomakkeen muokkausoikeutta" (-> before :body :error))
                (should= "Käyttäjällä ei lomakkeen muokkausoikeutta" (-> after :body :error))))

          ;; Muu kuin pääkäyttäjä ei pääse käsiksi koko organisaatiolistaan, joten vierasta
          ;; organisaatiota ei voi valita lainkaan.
          (it "Should not let a non-superuser select an organization they do not belong to"
              (let [cookie       (login @virkailija-routes "VIEW-ONLY-USER")
                    foreign-org  "1.2.246.562.10.22"
                    own-rights   (rights-for-organization cookie foreign-org)
                    resp         (select-organization foreign-org ["view-applications"] cookie)]
                ;; Organisaatio on olemassa (fake-org-by-oid: "Omnia") mutta ei käyttäjän omissa.
                (should= #{} own-rights)
                (should= 400 (:status resp))
                ;; Reitti palauttaa oman (bad-request {}) -haaransa, eli select-organization
                ;; palautti nil. Tyhjä body erottaa tämän user-feedback-exceptionista, joka
                ;; tuottaisi {:error ...} — eli esto ei tule poikkeuksesta vaan haun tuloksesta.
                (should= {} (:body resp)))))

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
                (should= true (:maksullinen-kk-haku? (first body)) )))

          (it "should return admission-payment-required? false for non higher education admission"
              (let [resp (get-haku "payment-info-test-non-kk-form")
                    status (:status resp)
                    body (:body resp)]
                (should= 200 status)
                (should= 1 (count body))
                (should= false (:maksullinen-kk-haku? (first body))))))

(describe "GET kk application payment info"
          (tags :unit)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return payment information for an application"
              (let [application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                                       application-fixtures/application-without-hakemusmaksu-exemption
                                                       nil)
                    application (get-application-by-id application-id)
                    _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
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
                    _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
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
                (should= 0 (count applications))))

          (it "should return application oids"
              (let [[_ _ _ application _] (init-and-get-kk-fixtures)
                    resp (post-valintalaskenta-application-oids-query [(first (:hakukohde application))])
                    status (:status resp)
                    oids (:body resp)]
                (should= 200 status)
                (should= 1 (count oids))
                (should= (:key application) (first oids))))

          (it "should not return an application oid"
              (let [[_ _ _ _ _] (init-and-get-kk-fixtures)
                    resp (post-valintalaskenta-application-oids-query ["hk"])
                    status (:status resp)
                    oids (:body resp)]
                (should= 200 status)
                (should= 0 (count oids)))))

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
                    _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
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
                (should= 0 (count applications))))

          (it "should return 409 conflict when there are yksiloimattomat and salliYksiloimattomat is not set"
              (let [application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                                       application-fixtures/application-yksiloimaton
                                                       nil)
                    application (get-application-by-id application-id)
                    resp (post-siirto-application-query [(:key application)])
                    status (:status resp)
                    body (:body resp)]
                (should= 409 status)
                (should= "Yksilöimättömiä hakijoita" (:error body))
                (should= ["1.2.3.4.5.6"] (:personOids body))))

          (it "should return applications with salliYksiloimattomat=true even when there are yksiloimattomat"
              (let [application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                                       application-fixtures/application-yksiloimaton
                                                       nil)
                    application (get-application-by-id application-id)
                    resp (post-siirto-application-query-with-params [(:key application)] {"salliYksiloimattomat" true})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications)))))

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
                    _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
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
                    applications (get-in resp [:body :applications])
                    application (first applications)]
                (should= 200 status)
                (should= 1 (count applications))
                (should= true (contains? application :jattoAjanhetki))
                (should= false (contains? application :lahiosoite))
                (should= false (contains? application :postinumero))
                (should= false (contains? application :postitoimipaikka))
                (should= false (contains? application :puhelinnumero))))

          (it "should return yhteystiedot when includeYhteystiedot is true"
              (let [[_ _ _ _ haku-oid] (init-and-get-kk-fixtures)
                    resp (post-vts-application-query {:hakuOid haku-oid :includeYhteystiedot true})
                    status (:status resp)
                    applications (get-in resp [:body :applications])
                    application (first applications)]
                (should= 200 status)
                (should= 1 (count applications))
                (should= "Paratiisitie 13" (:lahiosoite application))
                (should= "00013" (:postinumero application))
                (should= "Paikka" (:postitoimipaikka application))
                (should= "050123" (:puhelinnumero application))))

          (it "should return nil yhteystiedot fields when answers are missing"
              (let [stripped (update application-fixtures/application-without-hakemusmaksu-exemption
                                     :answers
                                     (fn [answers]
                                       (filterv #(not (#{"phone" "postal-office"} (:key %))) answers)))
                    _ (db/init-db-fixture fixtures/payment-exemption-test-form stripped nil)
                    haku-oid (:haku stripped)
                    resp (post-vts-application-query {:hakuOid haku-oid :includeYhteystiedot true})
                    status (:status resp)
                    applications (get-in resp [:body :applications])
                    application (first applications)]
                (should= 200 status)
                (should= 1 (count applications))
                (should-be-nil (:puhelinnumero application))
                (should-be-nil (:postitoimipaikka application))
                (should= "Paratiisitie 13" (:lahiosoite application))
                (should= "00013" (:postinumero application))))

          (it "should return an application with kk payment data"
              (let [[_ _ _ application haku-oid] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-not-required-for-eta-citizen (:key application) nil)
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

(defn- init-application
  ([]
   (init-application [{:hakukohde "payment-info-test-kk-hakukohde"
                       :review-requirement "selection-state"
                       :review-state "incomplete"}
                      {:hakukohde "payment-info-test-kk-hakukohde-2"
                       :review-requirement "selection-state"
                       :review-state "incomplete"}]))
  ([hakukohde-reviews]
   (let [application-id (db/init-db-fixture fixtures/payment-exemption-test-form
                                            (assoc application-fixtures/application-without-hakemusmaksu-exemption
                                                   :hakukohde
                                                   ["payment-info-test-kk-hakukohde"
                                                    "payment-info-test-kk-hakukohde-2"])
                                            hakukohde-reviews)
         application (application-store/get-application application-id)]
     {:application-id application-id
      :application-key (:key application)})))

(describe "update-review"
          (tags :unit :api-applications)

          (it "should sync value when there are no reviews of type kk-application-payment-obligation"
              (let [{:keys [application-id application-key]} (init-application)
                     review-update-response (update-review {:id             application-id
                                                           :application-key application-key
                                                           :state           "active"
                                                           :hakukohde-reviews
                                                           {:payment-info-test-kk-hakukohde
                                                            {:kk-application-payment-obligation "reviewed"
                                                             :selection-state "incomplete"}}})
                    reviews-after-update (application-store/get-application-hakukohde-reviews application-key)]
                (should= 200 (:status review-update-response))
                (should= true (:needs-refresh (:body review-update-response)))
                (should= #{{:requirement "selection-state"
                            :state "incomplete"
                            :hakukohde "payment-info-test-kk-hakukohde"}
                           {:requirement "selection-state"
                            :state "incomplete"
                            :hakukohde "payment-info-test-kk-hakukohde-2"}
                           {:requirement "kk-application-payment-obligation"
                            :state "reviewed"
                            :hakukohde "payment-info-test-kk-hakukohde"}
                           {:requirement "kk-application-payment-obligation"
                            :state "reviewed"
                            :hakukohde "payment-info-test-kk-hakukohde-2"}}
                         (set (map #(select-keys % [:requirement :state :hakukohde]) reviews-after-update)))))

            (it "should sync values on when there are existing reviews of type kk-application-payment-obligation"
                (let [{:keys [application-id application-key]} (init-application [{:hakukohde "payment-info-test-kk-hakukohde"
                                                                                   :review-requirement "selection-state"
                                                                                   :review-state "incomplete"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde"
                                                                                   :review-requirement "selection-state"
                                                                                   :review-state "incomplete"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde-2"
                                                                                   :review-requirement "kk-application-payment-obligation"
                                                                                   :review-state "reviewed"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde-2"
                                                                                   :review-requirement "kk-application-payment-obligation"
                                                                                   :review-state "reviewed"}])
                      review-update-response (update-review {:id              application-id
                                                             :application-key application-key
                                                             :state           "active"
                                                             :hakukohde-reviews
                                                             {:payment-info-test-kk-hakukohde-2
                                                              {:kk-application-payment-obligation "in-migri-review"
                                                               :selection-state "incomplete"}}})
                      reviews-after-update (application-store/get-application-hakukohde-reviews application-key)]
                  (should= 200 (:status review-update-response))
                  (should= true (:needs-refresh (:body review-update-response)))
                  (should= #{{:requirement "selection-state"
                              :state "incomplete"
                              :hakukohde "payment-info-test-kk-hakukohde"}
                             {:requirement "selection-state"
                              :state "incomplete"
                              :hakukohde "payment-info-test-kk-hakukohde-2"}
                             {:requirement "kk-application-payment-obligation"
                              :state "in-migri-review"
                              :hakukohde "payment-info-test-kk-hakukohde"}
                             {:requirement "kk-application-payment-obligation"
                              :state "in-migri-review"
                              :hakukohde "payment-info-test-kk-hakukohde-2"}}
                           (set (map #(select-keys % [:requirement :state :hakukohde]) reviews-after-update)))))

            (it "should not sync values when another type of review is updated"
                (let [{:keys [application-id application-key]} (init-application [{:hakukohde "payment-info-test-kk-hakukohde"
                                                                                   :review-requirement "selection-state"
                                                                                   :review-state "incomplete"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde-2"
                                                                                   :review-requirement "selection-state"
                                                                                   :review-state "incomplete"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde"
                                                                                   :review-requirement "kk-application-payment-obligation"
                                                                                   :review-state "in-migri-review"}
                                                                                  {:hakukohde "payment-info-test-kk-hakukohde-2"
                                                                                   :review-requirement "kk-application-payment-obligation"
                                                                                   :review-state "in-migri-review"}])
                      review-update-response (update-review {:id              application-id
                                                             :application-key application-key
                                                             :state           "active"
                                                             :hakukohde-reviews
                                                             {:payment-info-test-kk-hakukohde
                                                              {:kk-application-payment-obligation "in-migri-review"
                                                               :selection-state "selected"}}})
                      reviews-after-update (application-store/get-application-hakukohde-reviews application-key)]
                  (should= 200 (:status review-update-response))
                  (should= false (:needs-refresh (:body review-update-response)))
                  (should= #{{:requirement "selection-state"
                              :state "selected"
                              :hakukohde "payment-info-test-kk-hakukohde"}
                             {:requirement "selection-state"
                              :state "incomplete"
                              :hakukohde "payment-info-test-kk-hakukohde-2"}
                             {:requirement "kk-application-payment-obligation"
                              :state "in-migri-review"
                              :hakukohde "payment-info-test-kk-hakukohde"}
                             {:requirement "kk-application-payment-obligation"
                              :state "in-migri-review"
                              :hakukohde "payment-info-test-kk-hakukohde-2"}}
                           (set (map #(select-keys % [:requirement :state :hakukohde]) reviews-after-update))))))

(describe "omatsivut"
          (tags :unit :omatsivut)

          (after-all
            (db/nuke-kk-payment-data))

          (it "should return an application"
              (let [[person _ _ application _] (init-and-get-kk-fixtures)
                    _ (payment/set-application-fee-required (:key application) nil)
                    _ (payment/set-maksut-secret (:key application) "secret")
                    resp (get-omatsivut-applications-query person nil)
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))
                (should= "fi" (:asiointikieli (first applications)))
                (should= false (:processing (first applications)))
                (should= "awaiting" (:paymentState (first applications)))
                (should= (.plusDays (time/today (time/time-zone-for-id "Europe/Helsinki"))
                                    payment/kk-application-payment-due-days)
                         (-> (:paymentDueDate (first applications))
                             java.time.ZonedDateTime/parse
                             .toLocalDate))
                (should= "100.00" (:paymentSum (first applications)))
                (should-be-nil (:paymentReason (first applications)))
                (should= "https://toimimaton.hakija-host-arvo.test.edn-tiedostosta/maksut/fi?secret=secret" (:paymentLink (first applications)))
                (should-be-nil (:hakuaikaIsOn (first applications)))
                (should-be-nil (:hakuaikaEnds (first applications)))))

          (it "should return application with hakuaika"
              (let [[person _ _ _ _] (init-and-get-kk-fixtures)
                    resp (get-omatsivut-applications-query person {:with-haku-aika true})
                    status (:status resp)
                    applications (:body resp)]
                (should= 200 status)
                (should= 1 (count applications))
                (should= false (:processing (first applications)))
                (should= false (:hakuaikaIsOn (first applications)))
                (should= true (some? (:hakuaikaEnds (first applications)))))))

(run-specs)
