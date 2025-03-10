(ns ataru.test-utils
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as excel-export]
            [ataru.cache.cache-service :as cache-service]
            [ataru.db.db :as db]
            [ataru.fixtures.db.browser-test-db :refer [insert-test-form]]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.forms.form-store :as form-store]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :as ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]
            [clj-time.format :as format]
            [clojure.string :as clj-string]
            [ring.mock.request :as mock]
            [speclj.core :refer [should-contain should-not-be-nil
                                 should-not-contain should=]]
            [yesql.core :as sql])

  (:import [java.io File FileOutputStream]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]
           (org.joda.time DateTimeUtils)))

(sql/defqueries "sql/virkailija-queries.sql")
(declare yesql-upsert-virkailija<!)

(defn login
  "Generate ring-session=abcdefgh cookie"
  ([virkailija-routes]
   (login virkailija-routes nil))
  ([virkailija-routes ticket]
   (-> (mock/request :get (str "/lomake-editori/auth/cas?ticket=" ticket))
       virkailija-routes
       :headers
       (get "Set-Cookie")
       first
       (clj-string/split #";")
       first)))

(defn should-have-header
  [header expected-val resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-contain header headers)
    (should= expected-val (get headers header))))

(defn should-not-have-header
  [header resp]
  (let [headers (:headers resp)]
    (should-not-be-nil headers)
    (should-not-contain header headers)))

(defn- create-fake-virkailija-update-secret
  [application-key]
  (db/exec :db yesql-upsert-virkailija<! {:oid        "1.2.246.562.24.00000001213"
                                          :first_name "Hemuli"
                                          :last_name  "Hemuli?"})
  (virkailija-edit/create-virkailija-update-secret
   {:identity {:oid        "1.2.246.562.24.00000001213"
               :username   "tsers"
               :first-name "Hemuli"
               :last-name  "Hemuli?"}}
   application-key))

(defn- create-fake-virkailija-create-secret
  []
  (db/exec :db yesql-upsert-virkailija<! {:oid        "1.2.246.562.24.00000001214"
                                          :first_name "Mymmeli"
                                          :last_name  "Mymmeli?"})
  (virkailija-edit/create-virkailija-create-secret
   {:identity {:oid        "1.2.246.562.24.00000001214"
               :username   "ksers"
               :first-name "Mymmeli"
               :last-name  "Mymmeli?"}}))

(defn get-latest-form
  [form-name]
  (if-let [form (->> (form-store/get-all-forms)
                     (filter #(= (-> % :name :fi) form-name))
                     (first))]
    form
    (insert-test-form form-name)))

(defn get-latest-application-id-for-form [form-name]
  (->> (application-store/get-application-heading-list
        {:form (:key (get-latest-form form-name))}
        {:order-by "created-time"
         :order    "desc"})
       first
       :id))

(defn get-latest-application-secret []
  (application-store/get-latest-application-secret))

(defn alter-application-to-hakuaikaloppu-for-secret [secret]
  (let [application (application-store/get-latest-version-of-application-for-edit false {:secret secret})
        hakukohde   (vec (cons "1.2.246.562.20.49028100001" (rest (:hakukohde application))))
        answers     (mapv (fn [answer]
                            (if (= "hakukohteet" (:key answer))
                              (assoc answer :value hakukohde)
                              answer))
                          (:answers application))]
    (application-store/alter-application-hakukohteet-with-secret secret hakukohde answers)))

(defn get-latest-application-by-form-name [form-name]
  (if-let [latest-application-id (get-latest-application-id-for-form form-name)]
    (application-store/get-application latest-application-id)
    (println "No test application found. Run hakija form test first!")))

(defn get-test-vars-params
  "Used in hakija routes to get required test params instead of writing them to test url."
  []
  (let [test-form   (get-latest-form "Testilomake")
        application (get-latest-application-by-form-name "Testilomake")]
    (println (str "using application " (:key application)))
    (cond->
     {:test-form-key                (:key test-form)
      :ssn-form-key                 (:key (get-latest-form "SSN_testilomake"))
      :test-question-group-form-key (:key (get-latest-form "Kysymysryhmä: testilomake"))
      :test-selection-limit-form-key (:key (get-latest-form "Selection Limit"))
      :test-form-application-secret (:secret application)
      :virkailija-create-secret     (create-fake-virkailija-create-secret)}

      (some? application)
      (assoc :virkailija-secret (create-fake-virkailija-update-secret (:key application))))))

(def test-koodisto-cache (reify cache-service/Cache
                           (get-from [_this _key])
                           (get-many-from [_this _keys])
                           (remove-from [_this _key])
                           (clear-all [_this])))


(defrecord MockOhjausparametritServiceWithGetParametri [get-param]
  OhjausparametritService
  (get-parametri [this haku-oid] (get-param this haku-oid)))

(defn- default-get-parametri [_ _] {:jarjestetytHakutoiveet true})

(def liiteri-cas-client nil)
(defn export-test-excel
  [applications & rest]
  (let [[input-params application-reviews application-review-notes] rest]
    (excel-export/export-applications liiteri-cas-client
                                      applications
                                      (or application-reviews
                                          (reduce #(assoc %1 (:key %2) fixtures/application-review)
                                                  {}
                                                  applications))
                                      (or application-review-notes fixtures/application-review-notes)
                                      (:selected-hakukohde input-params)
                                      (:selected-hakukohderyhma input-params)
                                      (:skip-answers? input-params)
                                      (or (:included-ids input-params) #{})
                                      (:ids-only? input-params)
                                      :created-time
                                      :desc
                                      :fi
                                      (delay {})
                                      (tarjonta-service/new-tarjonta-service)
                                      test-koodisto-cache
                                      (organization-service/new-organization-service)
                                      (->MockOhjausparametritServiceWithGetParametri default-get-parametri))))

(defn with-excel-workbook [excel-data run-test]
  (let [file (File/createTempFile (str "excel-" (UUID/randomUUID)) ".xlsx")]
    (try
      (with-open [output (FileOutputStream. (.getPath file))]
        (->> excel-data
             (.write output)))
      (run-test (WorkbookFactory/create file))
      (finally (.delete file)))))

(defonce formatter (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm:ss") (time/time-zone-for-id "Europe/Helsinki")))

; Muunnetaan lokaali timestamp UTC-millisekunneiksi, jotta voidaan väärentää järjestelmän kello olemaan
; UTC-ajassa antamalla lokaali timestamp
(defn local-timestamp-to-utc-millis [timestamp]
  (coerce/to-long (time/to-time-zone (format/parse formatter timestamp) (time/time-zone-for-id "UTC"))))

(defn set-fixed-time [timestamp]
  (let [millis (local-timestamp-to-utc-millis timestamp)]
    (println (str "Setting fixed millis " timestamp ", formatted with Helsinki timezone " (format/parse formatter timestamp) ", result millis " millis))
    (DateTimeUtils/setCurrentMillisFixed millis)))

(defn reset-fixed-time! []
  (DateTimeUtils/setCurrentMillisSystem))
