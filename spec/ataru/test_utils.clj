(ns ataru.test-utils
  (:require [ataru.applications.application-store :as application-store]
            [ataru.applications.excel-export :as excel-export]
            [ataru.cache.cache-service :as cache-service]
            [ataru.db.db :as db]
            [ataru.fixtures.excel-fixtures :as fixtures]
            [ataru.maksut.maksut-protocol :refer [MaksutServiceProtocol]]
            [ataru.ohjausparametrit.ohjausparametrit-protocol :refer [OhjausparametritService]]
            [ataru.organization-service.organization-service :as organization-service]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.tarjonta-service.mock-tarjonta-service :as mock-tarjonta-service]
            [ataru.valinta-tulos-service.valintatulosservice-protocol :refer [ValintaTulosService]]
            [ataru.koski.koski-service :refer [KoskiTutkintoService]]
            [ataru.virkailija.authentication.virkailija-edit :as virkailija-edit]
            [ataru.time.coerce :as coerce]
            [ataru.time :as time]
            [ataru.time.format :as format]
            [cheshire.core :as json]
            [clojure.string :as clj-string]
            [ring.mock.request :as mock]
            [speclj.core :refer [should-contain should-not-be-nil
                                 should-not-contain should=]]
            [yesql.core :as sql])

  (:import [fi.vm.sade.auditlog DummyAuditLog]
           [java.io File FileOutputStream]
           [java.time Instant]
           [java.util UUID]
           [org.apache.poi.ss.usermodel WorkbookFactory]))

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

(defn new-capturing-audit-logger
  "Palauttaa [entries logger], missä entries on atomi ja logger kelpaa :audit-logger-riippuvuudeksi.
   Jokainen merkintä on muotoa
   {:user {...} :operation \"lisäys\" :target {...} :changes [...]}.

   Periytetään DummyAuditLogista eikä rakenneta Auditia suoraan, jotta testiajoon ei synny
   HeartbeatDaemon-säiettä eikä tiedostokirjoitusta."
  []
  (let [entries (atom [])
        ->clj   (fn [json-el] (json/parse-string (str json-el) true))]
    [entries
     (proxy [DummyAuditLog] []
       (log [user operation target changes]
         (swap! entries conj
                {:user      (->clj (.asJson user))
                 :operation (.name operation)
                 :target    (->clj (.asJson target))
                 :changes   (->clj (.asJsonArray changes))})))]))

(defrecord FakeValintaTulosService [calls response]
  ValintaTulosService
  (hakukohteen-ehdolliset [_ _] #{})
  (valinnan-tulos-hakemukselle [_ _ _] @response)
  (valinnantulos-hakemukselle-tilahistorialla [_ _] @response)
  (valinnantulos-monelle-tilahistorialla [_ _] @response)
  (change-kevyt-valinta-property [_ valintatapajono-oid body _]
    (swap! calls conj {:op :change-kevyt-valinta-property
                       :valintatapajono-oid valintatapajono-oid
                       :body body})
    @response)
  (hyvaksynnan-ehto-hakukohteessa-hakemus [_ _ _] @response)
  (add-hyvaksynnan-ehto-hakukohteessa-hakemus [_ ehto hakukohde-oid application-key _]
    (swap! calls conj {:op :add-hyvaksynnan-ehto
                       :ehto ehto
                       :hakukohde-oid hakukohde-oid
                       :application-key application-key})
    @response)
  (delete-hyvaksynnan-ehto-hakukohteessa-hakemus [_ hakukohde-oid application-key _]
    (swap! calls conj {:op :delete-hyvaksynnan-ehto
                       :hakukohde-oid hakukohde-oid
                       :application-key application-key})
    @response)
  (hyvaksynnan-ehto-valintatapajonoissa-hakemus [_ _ _] @response)
  (hyvaksynnan-ehto-hakemukselle [_ _] @response)
  (hyvaksynnan-ehto-hakukohteessa-muutoshistoria [_ _ _] @response))

(defrecord FakeMaksutService [calls laskut invoice]
  MaksutServiceProtocol
  (create-kk-application-payment-lasku [_ lasku]
    (swap! calls conj {:op :create-kk-application-payment-lasku :lasku lasku})
    @invoice)
  (create-kasittely-lasku [_ lasku]
    (swap! calls conj {:op :create-kasittely-lasku :lasku lasku})
    @invoice)
  (create-paatos-lasku [_ lasku]
    (swap! calls conj {:op :create-paatos-lasku :lasku lasku})
    @invoice)
  (list-lasku-statuses [_ _] [])
  (list-laskut-by-application-key [_ _] @laskut)
  (download-receipt [_ _] {:status 200 :body ""})
  (invalidate-laskut [_ _] nil)
  (force-invalidate-laskut [_ _] nil)
  (delete-laskut [_ _] nil)
  (update-laskut-due-date [_ _ _] nil))

;; Fake-palveluiden oletusarvot. Nimettyinä, jotta testit voivat palauttaa atomit näihin
;; before-lohkossa sen sijaan että jokainen muuttaja huolehtisi palautuksesta itse.
(def fake-lasku {:order_id "ORDER-1" :status :active :secret "lasku-secret-1"})

(def fake-invoice {:order_id "ORDER-1"
                   :secret   "lasku-secret-1"
                   :amount   "100"
                   :vat      "24"
                   :due_date "2026-12-31"
                   :status   :active})

(def fake-vts-response {:status 200 :headers {} :body "{}"})

(defn new-fake-maksut-service
  "Palauttaa {:calls :laskut :invoice :service}. :calls kerää luontikutsut, :laskut on
   list-laskut-by-application-key:n vastaus ja :invoice create-*-lasku:n palauttama lasku."
  []
  (let [calls   (atom [])
        laskut  (atom [fake-lasku])
        invoice (atom fake-invoice)]
    {:calls   calls
     :laskut  laskut
     :invoice invoice
     :service (->FakeMaksutService calls laskut invoice)}))

(defn new-fake-valinta-tulos-service
  "Palauttaa {:calls :response :service}. :calls kerää tehdyt muutoskutsut, :response on VTS:n
   vastaus, jonka reitit palauttavat sellaisenaan."
  []
  (let [calls    (atom [])
        response (atom fake-vts-response)]
    {:calls    calls
     :response response
     :service  (->FakeValintaTulosService calls response)}))

(defn new-counting-cache
  "Palauttaa {:calls :cache}. :calls kerää tyhjennykset, jotta testi voi varmistaa myös sen
   ettei välimuistia kosketettu — luvattoman kutsun olennaisin väite."
  []
  (let [calls (atom [])]
    {:calls calls
     :cache (reify cache-service/Cache
              (get-from [_ _])
              (get-many-from [_ _])
              (remove-from [_ key] (swap! calls conj [:remove-from key]))
              (clear-all [_] (swap! calls conj [:clear-all])))}))

(defn audit-entries-for
  "Suodattaa merkinnät operaation ja valinnaisen target-kentän perusteella.
   Operaatiot ovat audit_log.clj:n suomenkielisiä nimiä, esim. \"lisäys\", \"poisto\"."
  ([entries operation]
   (filter #(= operation (:operation %)) @entries))
  ([entries operation target-key target-value]
   (filter #(= target-value (get-in % [:target target-key]))
           (audit-entries-for entries operation))))

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

(defn create-fake-virkailija-rewrite-secret
  [application-key]
  (db/exec :db yesql-upsert-virkailija<! {:oid        "1.2.246.562.24.00000001213"
                                          :first_name "Hemuli"
                                          :last_name  "Hemuli?"})
  (virkailija-edit/create-virkailija-rewrite-secret
   {:identity {:oid        "1.2.246.562.24.00000001213"
               :username   "tsers"
               :first-name "Hemuli"
               :last-name  "Hemuli?"}}
   application-key))

(defn get-latest-application-secret []
  (application-store/get-latest-application-secret))

(defn register-test-haku! [haku]
  (mock-tarjonta-service/register-test-haku! haku))

(defn unregister-test-haku! [haku-oid]
  (mock-tarjonta-service/unregister-test-haku! haku-oid))

(defn register-test-hakukohde! [hakukohde-muutos]
  (mock-tarjonta-service/register-test-hakukohde! hakukohde-muutos))

(defn unregister-test-hakukohde! [hakukohde-oid]
  (mock-tarjonta-service/unregister-test-hakukohde! hakukohde-oid))

(defn alter-application-to-hakuaikaloppu-for-secret [secret]
  (let [application (application-store/get-latest-version-of-application-for-edit false {:secret secret})
        hakukohde   (vec (cons "1.2.246.562.20.49028100001" (rest (:hakukohde application))))
        answers     (mapv (fn [answer]
                            (if (= "hakukohteet" (:key answer))
                              (assoc answer :value hakukohde)
                              answer))
                          (:answers application))]
    (application-store/alter-application-hakukohteet-with-secret secret hakukohde answers)))

(def test-koodisto-cache (reify cache-service/Cache
                           (get-from [_this _key])
                           (get-many-from [_this _keys])
                           (remove-from [_this _key])
                           (clear-all [_this])))


(defrecord MockOhjausparametritServiceWithGetParametri [get-param]
  OhjausparametritService
  (get-parametri [this haku-oid] (get-param this haku-oid)))

(defrecord MockKoskiTutkintoService [koski-cas-client]
  KoskiTutkintoService
  (get-tutkinnot-for-oppija [_ _ _] {}))

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
                                      (->MockOhjausparametritServiceWithGetParametri default-get-parametri)
                                      MockKoskiTutkintoService)))

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
    (time/set-fixed-now! (Instant/ofEpochMilli millis))))

(defn reset-fixed-time! []
  (time/reset-now!))
