(ns ataru.hakija.hakija-routes-spec
  (:require [ataru.applications.field-deadline :as field-deadline]
            [ataru.kk-application-payment.kk-application-payment :as payment]
            [ataru.util :as util]
            [ataru.log.audit-log :as audit-log]
            [ataru.koodisto.koodisto :as koodisto]
            [ataru.applications.application-store :as store]
            [ataru.background-job.job :as job]
            [ataru.files.file-store :as file-store]
            [ataru.fixtures.application :as application-fixtures]
            [ataru.fixtures.db.unit-test-db :as db]
            [ataru.forms.form-store :as form-store]
            [ataru.email.application-email-jobs :as application-email]
            [ataru.hakija.background-jobs.hakija-jobs :as hakija-jobs]
            [ataru.hakija.hakija-form-service :as hakija-form-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.hakuaika :as hakuaika]
            [ataru.cache.cache-service :as cache-service]
            [ataru.hakija.hakija-routes :as routes]
            [ataru.hakija.hakija-application-service :as application-service]
            [ataru.applications.application-service :as common-application-service]
            [ataru.config.core :refer [config]]
            [ataru.util.random :as crypto]
            [clj-time.core :as t]
            [cheshire.core :as json]
            [ataru.db.db :as ataru-db]
            [ring.mock.request :as mock]
            [speclj.core :refer [around before-all should-not before should= should it describe tags]]
            [yesql.core :as sql]
            [ataru.fixtures.form :as form-fixtures]
            [ataru.ohjausparametrit.ohjausparametrit-service :as ohjausparametrit-service])
  (:import org.joda.time.DateTime))

(declare resp)
(declare yesql-get-application-by-id)

(sql/defqueries "sql/application-queries.sql")

(def ^:private form (atom nil))

(def application-blank-required-field (assoc-in application-fixtures/person-info-form-application [:answers 0 :value] ""))
(def application-invalid-email-field (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "invalid@email@foo.com"))
(def application-invalid-phone-field (assoc-in application-fixtures/person-info-form-application [:answers 5 :value] "invalid phone number"))
(def application-invalid-ssn-field (assoc-in application-fixtures/person-info-form-application [:answers 8 :value] "010101-123M"))
(def application-invalid-postal-code (assoc-in application-fixtures/person-info-form-application [:answers 10 :value] "0001"))
(def application-invalid-dropdown-value (assoc-in application-fixtures/person-info-form-application [:answers 12 :value] "kuikka"))
(def application-edited-email (assoc-in application-fixtures/person-info-form-application [:answers 2 :value] "edited@foo.com"))
(def application-edited-ssn (assoc-in application-fixtures/person-info-form-application [:answers 8 :value] "020202A0202"))
(def application-for-hakukohde-edited (-> application-fixtures/person-info-form-application-for-hakukohde
                                          (assoc-in [:answers 2 :value] "edited@foo.com")
                                          (assoc-in [:answers 16 :value] ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"])))
(def application-for-hakukohde-email-edited (-> application-fixtures/person-info-form-application-for-hakukohde
                                                (assoc-in [:answers 2 :value] "aku@ankkalinna.com")
                                                (assoc-in [:answers 16 :value] ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"])))
(def application-for-hakukohde-hakukohde-order-edited (-> application-fixtures/person-info-form-application-for-hakukohde
                                                          (assoc :hakukohde [ "1.2.246.562.20.49028196524" "1.2.246.562.20.49028196523"])
                                                          (assoc-in [:answers 17 :value] [ "1.2.246.562.20.49028196524" "1.2.246.562.20.49028196523"])))

(def handler
  (let [form-by-id-cache                     (reify cache-service/Cache
                                               (get-from [_ key]
                                                 (form-store/fetch-by-id (Integer/valueOf key)))
                                               (get-many-from [_ _])
                                               (remove-from [_ _])
                                               (clear-all [_]))
        tarjonta-service                     (tarjonta-service/new-tarjonta-service)
        organization-service                 (organization-service/new-organization-service)
        ohjausparametrit-service             (ohjausparametrit-service/new-ohjausparametrit-service)
        application-service                  (common-application-service/new-application-service)
        audit-logger                         (audit-log/new-dummy-audit-logger)
        koodisto-cache                       (reify cache-service/Cache
                                               (get-from [_ _])
                                               (get-many-from [_ _])
                                               (remove-from [_ _])
                                               (clear-all [_]))
        hakukohderyhma-settings-cache         (reify cache-service/Cache
                                               (get-from [_ _])
                                               (get-many-from [_ _])
                                               (remove-from [_ _])
                                               (clear-all [_]))
        form-by-haku-oid-str-cache-loader    (hakija-form-service/map->FormByHakuOidStrCacheLoader
                                              {:form-by-id-cache         form-by-id-cache
                                               :koodisto-cache           koodisto-cache
                                               :ohjausparametrit-service ohjausparametrit-service
                                               :organization-service     organization-service
                                               :tarjonta-service         tarjonta-service
                                               :hakukohderyhma-settings-cache hakukohderyhma-settings-cache})]
    (-> (routes/new-handler)
        (assoc :tarjonta-service tarjonta-service)
        (assoc :job-runner (job/new-job-runner hakija-jobs/job-definitions))
        (assoc :organization-service organization-service)
        (assoc :ohjausparametrit-service ohjausparametrit-service)
        (assoc :application-service application-service)
        (assoc :form-by-id-cache form-by-id-cache)
        (assoc :form-by-haku-oid-str-cache (reify cache-service/Cache
                                             (get-from [_ key]
                                               (.load form-by-haku-oid-str-cache-loader key))
                                             (get-many-from [_ _])
                                             (remove-from [_ _])
                                             (clear-all [_])))
        (assoc :koodisto-cache koodisto-cache)
        (assoc :audit-logger audit-logger)
        .start
        :routes)))

(defn- parse-body
  [resp]
  (if-not (nil? (:body resp))
    (assoc resp :body (cond-> (:body resp)
                              (not (string? (:body resp)))
                              slurp
                              true
                              (json/parse-string true)))
    resp))

(defmacro with-response
  [method resp application & body]
  `(let [~resp (-> (mock/request ~method "/hakemus/api/application" (json/generate-string ~application))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defmacro logout-request-with-response
  [method resp logout-request & body]
  `(let [~resp (-> (mock/request ~method "/hakemus/auth/oppija")
                   (assoc-in [:params] {:logoutRequest ~logout-request})
                   handler
                   parse-body)]
     ~@body))

(defmacro with-get-response
  [secret resp & body]
  `(let [~resp (-> (mock/request :get (str "/hakemus/api/application?secret=" ~secret))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defmacro with-haku-form-response
  [haku-oid roles resp & body]
  `(let [~resp (-> (mock/request :get (str "/hakemus/api/haku/" ~haku-oid
                                           (when (not-empty ~roles)
                                             (str "?role=" (clojure.string/join
                                                            "&role="
                                                            (map name ~roles))))))
                   (mock/content-type "application/json")
                   handler
                   parse-body)]
     ~@body))

(defn- have-any-application-in-db
  []
  (let [app-count
        (+ (count (store/get-application-heading-list
                   {:hakukohde [(:hakukohde @form)]}
                   {:order-by "created-time"
                    :order    "desc"}))
           (count (store/get-application-heading-list
                   {:form (:key @form)}
                   {:order-by "created-time"
                    :order    "desc"})))]
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

(defn- cannot-edit? [field] (true? (:cannot-edit field)))

(defn- cannot-view? [field] (true? (:cannot-view field)))

(defn- get-answer
  [application key]
  (->> application
       :content
       :answers
       (filter #(= (:key %) key))
       first
       :value))


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

(defn- hakuaika-ended-within-grace-period
  [_ _ _ _]
  (let [edit-grace-period (-> config :public-config :attachment-modify-grace-period-days)
        start             (* 2 edit-grace-period)
        end               (quot edit-grace-period 2)]
    (hakuaika/hakuaika-with-label {:on                                  false
                                   :start                               (- (System/currentTimeMillis) (* start 24 3600 1000))
                                   :end                                 (- (System/currentTimeMillis) (* end 24 3600 1000))
                                   :hakukierros-end                     nil
                                   :jatkuva-haku?                       false
                                   :joustava-haku?                      false
                                   :jatkuva-or-joustava-haku?           false
                                   :attachment-modify-grace-period-days edit-grace-period})))

(defn- hakuaika-ended-within-grace-period-hakukierros-ongoing
  [_ _ _ _]
  (let [edit-grace-period (-> config :public-config :attachment-modify-grace-period-days)
        start             (* 2 edit-grace-period)
        end               (quot edit-grace-period 2)]
    (hakuaika/hakuaika-with-label {:on                                  false
                                   :start                               (- (System/currentTimeMillis) (* start 24 3600 1000))
                                   :end                                 (- (System/currentTimeMillis) (* end 24 3600 1000))
                                   :hakukierros-end                     (+ (System/currentTimeMillis) (* 2 24 3600 1000))
                                   :jatkuva-haku?                       false
                                   :joustava-haku?                      false
                                   :jatkuva-or-joustava-haku?           false
                                   :attachment-modify-grace-period-days edit-grace-period})))

(defn- hakuaika-ended-grace-period-passed-hakukierros-ongoing
  [_ _ _ _]
  (let [edit-grace-period (-> config :public-config :attachment-modify-grace-period-days)
        start             (* 2 edit-grace-period)
        end               (+ edit-grace-period 1)]
    (hakuaika/hakuaika-with-label {:on                                  false
                                   :start                               (- (System/currentTimeMillis) (* start 24 3600 1000))
                                   :end                                 (- (System/currentTimeMillis) (* end 24 3600 1000))
                                   :hakukierros-end                     (+ (System/currentTimeMillis) (* 2 24 3600 1000))
                                   :jatkuva-haku?                       false
                                   :joustava-haku?                      false
                                   :jatkuva-or-joustava-haku?           false
                                   :attachment-modify-grace-period-days edit-grace-period})))

(describe "/haku"
  (tags :unit :hakija-routes)

  (around [spec]
    (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                  koodisto/all-koodisto-values                          (constantly #{})]
      (spec)))

  (before
    (let [person-info-form-with-hidden-attachment (update form-fixtures/person-info-form :content concat form-fixtures/form-hidden-attachment)]
      (reset! form (db/init-db-fixture person-info-form-with-hidden-attachment))))

  (it "should get form"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:hakija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (filter cannot-edit? fields))
                   ["first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "language" "87834771-34da-40a4-a9f6-sensitive"])
           (should= (map :id (filter cannot-view? fields))
                   ["ssn" "birth-date" "87834771-34da-40a4-a9f6-sensitive"])))))

  (it "should get form as virkailija"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:virkailija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["hakukohteet" "birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city" "b0839467-a6e8-4294-b5cc-830756bbda8a" "164954b5-7b23-4774-bd44-dee14071316b" "87834771-34da-40a4-a9f6-sensitive" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-edit? fields))
                   ["first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "language"])
          (should= (map :id (filter cannot-view? fields))
                   [])))))

  (it "should get form as virkailija without henkilo"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:virkailija] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["hakukohteet" "first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city" "language" "b0839467-a6e8-4294-b5cc-830756bbda8a" "164954b5-7b23-4774-bd44-dee14071316b" "87834771-34da-40a4-a9f6-sensitive" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-edit? fields))
                   [])
          (should= (map :id (filter cannot-view? fields))
                   [])))))

  (it "should get form with dynamic kk payment info as hakija"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
        (with-haku-form-response "1.2.246.562.29.65950024186" [:hakija :with-henkilo] resp
           (should= 200 (:status resp))
           (let [payment-properties (->> resp :body :properties :payment)]
             (should-not (empty? payment-properties))
             (should= "payment-type-kk" (:type payment-properties))
             (should= "100.00" (:processing-fee payment-properties))
             (should= nil (:decision-fee payment-properties))))))

  (it "should get form with dynamic kk payment info as virkailija without henkilo"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
        (with-haku-form-response "1.2.246.562.29.65950024186" [:virkailija] resp
           (should= 200 (:status resp))
           (let [payment-properties (->> resp :body :properties :payment)]
             (should-not (empty? payment-properties))
             (should= "payment-type-kk" (:type payment-properties))
             (should= "100.00" (:processing-fee payment-properties))
             (should= nil (:decision-fee payment-properties))))))

  (it "should get application with hakuaika ended"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-within-grace-period]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:hakija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["164954b5-7b23-4774-bd44-dee14071316b" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-edit? fields))
                   ["hakukohteet" "first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city" "language" "b0839467-a6e8-4294-b5cc-830756bbda8a" "87834771-34da-40a4-a9f6-sensitive"])
          (should= (map :id (filter cannot-view? fields))
                   ["ssn" "birth-date" "87834771-34da-40a4-a9f6-sensitive"])))))

  (it "should get application with hakuaika ended as virkailija"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-within-grace-period]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:virkailija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["hakukohteet" "birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city" "b0839467-a6e8-4294-b5cc-830756bbda8a" "164954b5-7b23-4774-bd44-dee14071316b" "87834771-34da-40a4-a9f6-sensitive" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-edit? fields))
                   ["first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "language"])
          (should= (map :id (filter cannot-view? fields))
                   [])))))

  (it "should get application with hakuaika ended but hakukierros ongoing"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:hakija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city"])
          (should= (map :id (filter cannot-edit? fields))
                   ["hakukohteet" "first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "language" "b0839467-a6e8-4294-b5cc-830756bbda8a" "164954b5-7b23-4774-bd44-dee14071316b" "87834771-34da-40a4-a9f6-sensitive" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-view? fields))
                   ["ssn" "birth-date" "87834771-34da-40a4-a9f6-sensitive"])))))

  (it "should get application with hakuaika ended but hakukierros ongoing as virkailija"
    (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing]
      (with-haku-form-response "1.2.246.562.29.65950024186" [:virkailija :with-henkilo] resp
        (should= 200 (:status resp))
        (let [fields (->> resp :body :content util/flatten-form-fields (filter util/answerable?))]
          (should= (map :id (remove cannot-edit? fields))
                   ["hakukohteet" "birthplace" "passport-number" "national-id-number" "email" "phone" "country-of-residence" "address" "postal-code" "postal-office" "home-town" "city" "b0839467-a6e8-4294-b5cc-830756bbda8a" "164954b5-7b23-4774-bd44-dee14071316b" "87834771-34da-40a4-a9f6-sensitive" "164954b5-7b23-4774-bd44-hidden"])
          (should= (map :id (filter cannot-edit? fields))
                   ["first-name" "preferred-name" "last-name" "nationality" "have-finnish-ssn" "ssn" "birth-date" "gender" "language"])
          (should= (map :id (filter cannot-view? fields))
                   []))))))

(describe "/application"
  (tags :unit :hakija-routes)

  (describe "POST application"
            (around [spec]
              (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                            hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                            koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                                    (case uri
                                                                                      "maatjavaltiot2"
                                                                                      #{"246"}
                                                                                      "kunta"
                                                                                      #{"273"}
                                                                                      "sukupuoli"
                                                                                      #{"1" "2"}
                                                                                      "kieli"
                                                                                      #{"FI" "SV" "EN"}))]
                (spec)))

    (before
      (let [person-info-form-with-hidden-attachment (update form-fixtures/person-info-form :content concat form-fixtures/form-hidden-attachment)]
        (reset! form (db/init-db-fixture person-info-form-with-hidden-attachment))))

    (it "should validate application for hakukohde and do not contain hidden attachment"
        (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
          (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
            (should= 200 (:status resp))
            (should (have-application-for-hakukohde-in-db (get-in resp [:body :id]))))))

    (it "should validate application"
        (with-response :post resp application-fixtures/person-info-form-application
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id])))))

    (it "should validate application for hakukohde"
        (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
          (should= 200 (:status resp))
          (should (have-application-for-hakukohde-in-db (get-in resp [:body :id])))))

    (it "should not validate application with extra answers"
        (with-response :post resp application-fixtures/person-info-form-application-with-extra-answer
          (should= 400 (:status resp))
          (should= {:code "application-validation-failed-error"
                    :failures {:extra-answers ["extra-answer-key"]}} (:body resp))))

    (add-failing-post-spec "should not validate form with blank required field" application-blank-required-field)

    (add-failing-post-spec "should not validate form with invalid email field" application-invalid-email-field)

    (add-failing-post-spec "should not validate form with invalid phone field" application-invalid-phone-field)

    (add-failing-post-spec "should not validate form with invalid ssn field" application-invalid-ssn-field)

    (add-failing-post-spec "should not validate form with invalid postal code field" application-invalid-postal-code)

    (it "should not validate form with invalid dropdown field"
        (with-redefs [koodisto/all-koodisto-values (constantly #{"Some-koodi"})]
          (with-response :post resp application-invalid-dropdown-value
            (should= 400 (:status resp))
            (should-not (have-any-application-in-db)))))

    (it "should validate answers based on actual form options instead of koodisto-source if available"
        (with-redefs [koodisto/all-koodisto-values (constantly #{"Some-koodi"})]
          (db/init-db-fixture form-fixtures/form-with-koodisto-source)
          (with-response :post resp application-fixtures/application-with-koodisto-form
            (should= 200 (:status resp))
            (let [id (get-in resp [:body :id])
                  actual (get-application-by-id id)]
              (should= (:form application-fixtures/application-with-koodisto-form) (:form actual)))))))

  (describe "GET application"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                                    (case uri
                                                                                      "maatjavaltiot2"
                                                                                      #{"246"}
                                                                                      "kunta"
                                                                                      #{"273"}
                                                                                      "sukupuoli"
                                                                                      #{"1" "2"}
                                                                                      "kieli"
                                                                                      #{"FI" "SV" "EN"}))
                    file-store/get-metadata                               (fn [_ keys]
                                                                            (mapv #(hash-map
                                                                                    :key %
                                                                                    :content-type "plain/text"
                                                                                    :filename "tiedosto.txt"
                                                                                    :size 1
                                                                                    :virus-scan-status "done"
                                                                                    :final true
                                                                                    :uploaded (t/now))
                                                                                  keys))]
        (spec)))

    (before-all
      (reset! form (db/init-db-fixture form-fixtures/person-info-form)))

    (it "should create"
        (with-redefs [crypto/url-part (constantly "12345")]
          (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
                         (should= 200 (:status resp))
                         (should (have-application-in-db (get-in resp [:body :id]))))))

    (it "should not get application with wrong secret"
      (with-get-response "asdfasfas" resp
        (should= 404 (:status resp))))

    (it "should get application"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
        (with-get-response "12345" resp
          (should= 200 (:status resp)))))

    (it "should get application with hakuaika ended"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-within-grace-period]
        (with-get-response "12345" resp
          (should= 200 (:status resp)))))

    (it "should get application with hakuaika ended but hakukierros ongoing"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing]
        (with-get-response "12345" resp
          (should= 200 (:status resp)))))

    (it "should get application with hakuaika ended but field deadline extended"
      (with-redefs [hakuaika/hakukohteen-hakuaika      hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    field-deadline/get-field-deadlines (fn [_]
                                                         [{:field-id      "b0839467-a6e8-4294-b5cc-830756bbda8a"
                                                           :deadline      (.plusDays (DateTime/now) 1)
                                                           :last-modified (DateTime/now)}])]
        (with-get-response "12345" resp
          (should= 200 (:status resp))
          (should-not (-> (get-in resp [:body :form])
                          util/form-fields-by-id
                          (get-in [:b0839467-a6e8-4294-b5cc-830756bbda8a
                                   :cannot-edit]))))))

    (it "should get application with hakuaika ended and field deadline passed"
      (with-redefs [hakuaika/hakukohteen-hakuaika      hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    field-deadline/get-field-deadlines (fn [_]
                                                         [{:field-id      "b0839467-a6e8-4294-b5cc-830756bbda8a"
                                                           :deadline      (.minusDays (DateTime/now) 1)
                                                           :last-modified (DateTime/now)}])]
        (with-get-response "12345" resp
          (should= 200 (:status resp))
          (should (-> (get-in resp [:body :form])
                      util/form-fields-by-id
                      (get-in [:b0839467-a6e8-4294-b5cc-830756bbda8a
                               :cannot-edit]))))))

    (it "should hide answer for question marked as sensitive-answer"
      (with-get-response "12345" resp
        (should= 200 (:status resp))
        (should (-> (get-in resp [:body :application])
                  (get-answer "87834771-34da-40a4-a9f6-sensitive")
                  nil?))))

    (it "should get application's kk payment data"
        (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing]
          (with-get-response "12345" resp
            (let [application-id (get-in resp [:body :application :id])]
              (store/add-person-oid application-id "1.2.3.4.5.6")
              (payment/set-application-fee-required "1.2.3.4.5.6" "kausi_s" 2025 nil nil)
              (with-get-response "12345" resp
                (should= 200 (:status resp))
                (should= {:person-oid "1.2.3.4.5.6", :start-term "kausi_s", :start-year 2025, :state "awaiting-payment"}
                         (select-keys
                           (get-in resp [:body :kk-payment :status])
                           [:person-oid :start-term :start-year :state]))))))))

          (describe "PUT application"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    application-email/start-email-edit-confirmation-job   (constantly nil)
                    application-service/remove-orphan-attachments         (fn [_ _ _])
                    hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                            (case uri
                                                                              "maatjavaltiot2"
                                                                              #{"246"}
                                                                              "kunta"
                                                                              #{"273"}
                                                                              "sukupuoli"
                                                                              #{"1" "2"}
                                                                              "kieli"
                                                                              #{"FI" "SV" "EN"}))]
        (spec)))

    (before-all
      (reset! form (db/init-db-fixture form-fixtures/person-info-form)))

    (it "should create"
      (with-redefs [crypto/url-part (constantly "0000000010")]
        (with-response :post resp application-fixtures/person-info-form-application
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id]))))))

    (it "should edit application"
      (with-redefs [crypto/url-part (constantly "0000000011")]
        (with-response :put resp (merge application-edited-email {:secret "0000000010"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "edited@foo.com" (get-answer application "email"))))))

    (it "should not allow editing ssn"
      (with-redefs [crypto/url-part (constantly "0000000012")]
        (with-response :put resp (merge application-edited-ssn {:secret "0000000011"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "010101A123N" (get-answer application "ssn"))))))

    (it "should create for hakukohde with hakukohde order check"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing
                    crypto/url-part (constantly "0000000013")]
        (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id])))
          (should= ["1.2.246.562.20.49028196523" "1.2.246.562.20.49028196524"]
                   (->> (get-application-by-id (-> resp :body :id))
                        :content
                        :answers
                        (filter #(= "hakukohteet" (:key %)))
                        first
                        :value)))))

    (it "should change hakukohde order"
      (with-response :put resp (merge application-for-hakukohde-hakukohde-order-edited {:secret "0000000013"})
        (should= 200 (:status resp))
        (should= ["1.2.246.562.20.49028196524" "1.2.246.562.20.49028196523"]
                 (->> (get-application-by-id (-> resp :body :id))
                      :content
                      :answers
                      (filter #(= "hakukohteet" (:key %)))
                      first
                      :value)))))

  (describe "PUT application after hakuaika ended"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    application-email/start-email-edit-confirmation-job   (constantly nil)
                    application-service/remove-orphan-attachments         (fn [_ _ _])
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                            (case uri
                                                                              "maatjavaltiot2"
                                                                              #{"246"}
                                                                              "kunta"
                                                                              #{"273"}
                                                                              "sukupuoli"
                                                                              #{"1" "2"}
                                                                              "kieli"
                                                                              #{"FI" "SV" "EN"}))]
        (spec)))

    (before-all
      (reset! form (db/init-db-fixture form-fixtures/person-info-form)))

    (it "should create"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing
                    crypto/url-part (constantly "0000000020")]
        (with-response :post resp application-fixtures/person-info-form-application-for-hakukohde
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id]))))))

    (it "should allow application edit after hakuaika within 10 days and only changes to attachments and limited person info"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-within-grace-period-hakukierros-ongoing
                    crypto/url-part (constantly "0000000021")]
        (with-response :put resp (merge application-for-hakukohde-edited {:secret "0000000020"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "FI" (get-answer application "language"))
            (should= "edited@foo.com" (get-answer application "email"))
            (should= ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"]
                     (get-answer application "164954b5-7b23-4774-bd44-dee14071316b"))))))

    (it "should allow application edit after grace period and only changes to limited person info"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    crypto/url-part (constantly "0000000022")]
        (with-response :put resp (merge application-for-hakukohde-email-edited {:secret "0000000021"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "FI" (get-answer application "language"))
            (should= "aku@ankkalinna.com" (get-answer application "email"))
            (should= ["57af9386-d80c-4321-ab4a-d53619c14a74_edited"]
                     (get-answer application "164954b5-7b23-4774-bd44-dee14071316b"))))))

    (it "should disallow application edit after grace period to attachments"
      (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    crypto/url-part (constantly "0000000023")]
        (with-response :put resp (merge application-fixtures/person-info-form-application-for-hakukohde {:secret "0000000022"})
          (should= 400 (:status resp)))))

    (it "should disallow application edit after grace period to attachment with extended field deadline that has passed"
      (with-redefs [hakuaika/hakukohteen-hakuaika      hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    field-deadline/get-field-deadlines (fn [_]
                                                         [{:field-id      "164954b5-7b23-4774-bd44-dee14071316b"
                                                           :deadline      (.minusDays (DateTime/now) 1)
                                                           :last-modified (DateTime/now)}])
                    crypto/url-part                    (constantly "0000000023")]
        (with-response :put resp (merge application-fixtures/person-info-form-application-for-hakukohde {:secret "0000000022"})
          (should= 400 (:status resp)))))

    (it "should allow application edit after grace period to attachment with extended field deadline"
      (with-redefs [hakuaika/hakukohteen-hakuaika      hakuaika-ended-grace-period-passed-hakukierros-ongoing
                    field-deadline/get-field-deadlines (fn [_]
                                                         [{:field-id      "164954b5-7b23-4774-bd44-dee14071316b"
                                                           :deadline      (.plusDays (DateTime/now) 1)
                                                           :last-modified (DateTime/now)}])
                    crypto/url-part                    (constantly "0000000023")]
        (with-response :put resp (merge application-fixtures/person-info-form-application-for-hakukohde {:secret "0000000022"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= ["57af9386-d80c-4321-ab4a-d53619c14a74"]
                     (get-answer application "164954b5-7b23-4774-bd44-dee14071316b")))))))

  (describe "PUT application with empty answers"
    (it "should work"
      (reset! form (db/init-db-fixture form-fixtures/person-info-form-with-more-questions))
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    application-email/start-email-edit-confirmation-job   (constantly nil)
                    application-service/remove-orphan-attachments         (fn [_ _ _])
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                            (case uri
                                                                              "maatjavaltiot2"
                                                                              #{"246"}
                                                                              "kunta"
                                                                              #{"273"}
                                                                              "sukupuoli"
                                                                              #{"1" "2"}
                                                                              "kieli"
                                                                              #{"FI" "SV" "EN"}))]
        (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ongoing
                      crypto/url-part (constantly "0000000023")]
          (with-response :post resp application-fixtures/person-info-form-application-with-empty-answers
            (should= 200 (:status resp))))
        (with-redefs [hakuaika/hakukohteen-hakuaika hakuaika-ended-grace-period-passed-hakukierros-ongoing
                      crypto/url-part (constantly "0000000024")]
          (with-response :put resp (merge application-fixtures/person-info-form-application-with-empty-answers {:secret "0000000023"})
            (should= 200 (:status resp)))))))

  (describe "Tests for a more complicated form"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    application-email/start-email-edit-confirmation-job   (constantly nil)
                    application-service/remove-orphan-attachments         (fn [_ _ _])
                    hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                            (case uri
                                                                              "maatjavaltiot2"
                                                                              #{"246"}
                                                                              "kunta"
                                                                              #{"273"}
                                                                              "sukupuoli"
                                                                              #{"1" "2"}
                                                                              "kieli"
                                                                              #{"FI" "SV" "EN"}))]
        (spec)))

    (before-all
      (reset! form (db/init-db-fixture form-fixtures/person-info-form-with-more-questions)))

    (it "should not create"
      (with-response :post resp application-fixtures/person-info-form-application
        (should= 400 (:status resp))
        (should= {:failures {:adjacent-answer-1            nil
                             :repeatable-required          nil
                             :more-questions-attachment-id nil}
                  :code "application-validation-failed-error"}
                 (:body resp))))

    (it "should create"
      (with-redefs [crypto/url-part (constantly "0000000030")]
        (with-response :post resp application-fixtures/person-info-form-application-with-more-answers
          (should= 200 (:status resp))
          (should (have-application-in-db (get-in resp [:body :id]))))))

    (it "should update answers"
      (with-redefs [crypto/url-part (constantly "0000000031")]
        (with-response :put resp (merge application-fixtures/person-info-form-application-with-modified-answers {:secret "0000000030"})
          (should= 200 (:status resp))
          (let [id          (-> resp :body :id)
                application (get-application-by-id id)]
            (should= "Toistuva pakollinen 4" (last (get-answer application "repeatable-required")))
            (should= ["modified-attachment-id"] (get-answer application "more-questions-attachment-id"))
            (should= "Vierekkäinen vastaus 2" (get-answer application "adjacent-answer-2"))
            (should= "toka vaihtoehto" (get-answer application "more-answers-dropdown-id"))))))

    (it "should not update dropdown answer when required followups are not answered"
      (with-response :put resp (-> application-fixtures/person-info-form-application-with-modified-answers
                                   (assoc-in [:answers 18 :value] "eka vaihtoehto")
                                   (merge {:secret "0000000031"}))
        (should= 400 (:status resp))
        (should= {:failures {:dropdown-followup-2 nil}
                  :code "application-validation-failed-error"} (:body resp))))

    (it "should update dropdown answer"
      (with-response :put resp (merge application-fixtures/person-info-form-application-with-more-modified-answers {:secret "0000000031"})
        (should= 200 (:status resp))
        (let [id          (-> resp :body :id)
              application (get-application-by-id id)]
          (should= "eka vaihtoehto" (get-answer application "more-answers-dropdown-id"))
          (should= ["followup-attachment"] (get-answer application "dropdown-followup-1"))
          (should= "toka" (get-answer application "dropdown-followup-2"))))))

  (describe "Form with followup questions inside a question group"
    (around [spec]
      (with-redefs [application-email/start-email-submit-confirmation-job (constantly nil)
                    application-email/start-email-edit-confirmation-job   (constantly nil)
                    application-service/remove-orphan-attachments         (fn [_ _ _])
                    hakuaika/hakukohteen-hakuaika                         hakuaika-ongoing
                    koodisto/all-koodisto-values                          (fn [_ uri _ _]
                                                                            (case uri
                                                                              "maatjavaltiot2"
                                                                              #{"246"}
                                                                              "kunta"
                                                                              #{"273"}
                                                                              "sukupuoli"
                                                                              #{"1" "2"}
                                                                              "kieli"
                                                                              #{"FI" "SV" "EN"}))]
        (spec)))

    (before-all
     (reset! form (db/init-db-fixture form-fixtures/form-with-followup-inside-a-question-group)))

    (it "should not validate if answer to a non-visible repeat of a question"
      (with-response :post resp (update application-fixtures/form-with-followup-inside-a-question-group-application
                                        :answers concat [{:key       "choice"
                                                          :value     [["0"] ["1"]]
                                                          :fieldType "singleChoice"}
                                                         {:key       "text"
                                                          :value     [[""] [""]]
                                                          :fieldType "textField"}])
        (should= 400 (:status resp))
        (should= {:failures {:text {:key "text" :value [[""] [""]] :fieldType "textField" :original-value nil}}
                  :code     "application-validation-failed-error"}
                 (:body resp))))

    (it "should validate if no answer to a non-visible repeat of a question"
      (with-response :post resp (update application-fixtures/form-with-followup-inside-a-question-group-application
                                        :answers concat [{:key       "choice"
                                                          :value     [["0"] ["1"]]
                                                          :fieldType "singleChoice"}
                                                         {:key       "text"
                                                          :value     [[""] nil]
                                                          :fieldType "textField"}])
        (should= 200 (:status resp)))))

    (describe "cas-oppija-tests"
      (around [spec]
              (with-redefs []
                (spec)))
      (before-all
        (reset! form (db/init-db-fixture form-fixtures/form-with-followup-inside-a-question-group))
        (db/init-oppija-session-to-db "ST-6778-thisticketiknow-cas.1234567890ac" {:data {:fields {"email" "masa@kajaani.com"}}}))
      (it "CAS-OPPIJA LOGOUT should return 200 if session successfully found and deleted"
          (logout-request-with-response :post resp
                                        "<samlp:LogoutRequest
                                           xmlns:samlp= \"urn:oasis:names:tc:SAML:2.0:protocol\"
                                           xmlns:saml= \"urn:oasis:names:tc:SAML:2.0:assertion\"
                                           ID= \"some-id\"
                                           Version= \"2.0\"
                                           IssueInstant= \"2019-09-12\" >
                                           <samlp:SessionIndex>
                                             ST-6778-thisticketiknow-cas.1234567890ac
                                           </samlp:SessionIndex>
                                         </samlp:LogoutRequest>"
                                        (should= 200 (:status resp))))
      (it "CAS-OPPIJA LOGOUT should return 404 if session with ticket not found"
          (logout-request-with-response :post resp
                                        "<samlp:LogoutRequest
                                                           xmlns:samlp= \"urn:oasis:names:tc:SAML:2.0:protocol\"
                                                           xmlns:saml= \"urn:oasis:names:tc:SAML:2.0:assertion\"
                                                           ID= \"some-id\"
                                                           Version= \"2.0\"
                                                           IssueInstant= \"2019-09-12\" >
                                                           <samlp:SessionIndex>
                                                             ST-6778-strangeticketthathasneverbeenusedbefore-cas.1234567890ac
                                                           </samlp:SessionIndex>
                                                         </samlp:LogoutRequest>"
                                        (should= 404 (:status resp))))))
