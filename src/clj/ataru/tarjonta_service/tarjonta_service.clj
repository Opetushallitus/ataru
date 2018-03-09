(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.organization-service.session-organizations :refer [select-organizations-for-rights]]
    [ataru.organization-service.organization-service :as organization-protocol]
    [ataru.organization-service.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]
    [ataru.config.core :refer [config]]
    [ataru.cache.cache-service :as cache]
    [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService VirkailijaTarjontaService get-hakukohde]]
    [ataru.tarjonta-service.mock-tarjonta-service :refer [->MockTarjontaService ->MockVirkailijaTarjontaService]]))

(defn- parse-multi-lang-text
  [text]
  (reduce-kv (fn [m lang s]
               (if (or (nil? s) (clojure.string/blank? s))
                 m
                 (assoc m lang s)))
    {}
    (clojure.set/rename-keys text {:kieli_fi :fi
                                   :kieli_sv :sv
                                   :kieli_en :en})))

(defn- haku-name-and-oid [haku-names-and-oids haku]
  (assoc haku-names-and-oids
         (:oid haku)
         {:haku-oid  (:oid haku)
          :haku-name (parse-multi-lang-text (:nimi haku))}))

(defn- hakus-by-form-key [hakus {:keys [avain haut]}]
  (let [haku-info (reduce haku-name-and-oid {} haut)]
    (if (not-empty haku-info)
      (assoc hakus avain haku-info)
      hakus)))

(defn- forms-in-use
  [cache-service organization-service session]
  (let [direct-organizations    (select-organizations-for-rights session [:form-edit])
        in-oph-organization?    (some #{oph-organization} (map :oid direct-organizations))
        query-organization-oids (if in-oph-organization?
                                  [oph-organization]
                                  (map :oid (organization-protocol/get-all-organizations
                                             organization-service
                                             direct-organizations)))
        hakus                   (map (fn [oid] (cache/cache-get-or-fetch cache-service
                                                                         :forms-in-use
                                                                         oid
                                                                         #(client/get-forms-in-use oid)))
                                     query-organization-oids)]
    (reduce hakus-by-form-key
            {}
            hakus)))

(defn- epoch-millis->zoned-date-time
  [millis]
  (java.time.ZonedDateTime/ofInstant
   (.truncatedTo (java.time.Instant/ofEpochMilli millis)
                 (java.time.temporal.ChronoUnit/SECONDS))
   (java.time.ZoneId/of "Europe/Helsinki")))

(defn- parse-hakuaika
  [hakuaika]
  (cond-> {:start (epoch-millis->zoned-date-time (:alkuPvm hakuaika))}
    (contains? hakuaika :loppuPvm)
    (assoc :end (epoch-millis->zoned-date-time (:loppuPvm hakuaika)))))

(defn parse-haku
  [haku]
  {:oid (:oid haku)
   :name (parse-multi-lang-text (:nimi haku))
   :hakuajat (mapv parse-hakuaika (:hakuaikas haku))})

(defn- parse-hakukohde
  [hakukohde]
  {:oid (:oid hakukohde)
   :haku-oid (:hakuOid hakukohde)
   :name (parse-multi-lang-text (:hakukohteenNimet hakukohde))
   :tarjoaja-name (parse-multi-lang-text (:tarjoajaNimet hakukohde))
   :ryhmaliitokset (some->> (:ryhmaliitokset hakukohde)
                     (map #(:ryhmaOid %)))})

(defn- parse-search-result
  [search-result]
  (doall (mapcat :tulokset (:tulokset search-result))))

(defn get-hakukohde-and-tarjoaja-name [hakukohde]
  {:name          (parse-multi-lang-text (:hakukohteenNimet hakukohde))
   :tarjoaja-name (parse-multi-lang-text (:tarjoajaNimet hakukohde))})

(defrecord CachedTarjontaService [cache-service]
  TarjontaService
  (get-hakukohde [this hakukohde-oid]
    (when-let [hakukohde (cache/cache-get-or-fetch cache-service :hakukohde hakukohde-oid #(client/get-hakukohde hakukohde-oid))]
      (when-not (= (:tila hakukohde) "PERUTTU")
        ;; Serialization breaks boxed booleans, as it doesn't return the
        ;; canonical instance
        (update hakukohde
                :kaytetaanHakukohdekohtaistaHakuaikaa #(.booleanValue %)))))

  (get-hakukohde-name [this hakukohde-oid]
    (when-let [hakukohde (.get-hakukohde this hakukohde-oid)]
      (parse-multi-lang-text (:hakukohteenNimet hakukohde))))

  (hakukohde-search [this haku-oid organization-oid]
    (some->> (client/hakukohde-search haku-oid organization-oid)
             parse-search-result
             (map :oid)
             (keep (partial get-hakukohde this))
             (mapv parse-hakukohde)))

  (get-haku [this haku-oid]
    ;; Serialization breaks boxed booleans, as it doesn't return the
    ;; canonical instance
    (some-> (cache/cache-get-or-fetch cache-service
                                      :haku haku-oid
                                      #(client/get-haku haku-oid))
            (update :canSubmitMultipleApplications #(.booleanValue %))
            (update :usePriority #(.booleanValue %))))

  (get-haku-name [this haku-oid]
    (when-let [haku (.get-haku this haku-oid)]
      (parse-multi-lang-text (:nimi haku))))

  (get-koulutus [this koulutus-oid]
    (cache/cache-get-or-fetch cache-service :koulutus koulutus-oid #(client/get-koulutus koulutus-oid))))

(defrecord VirkailijaTarjontaFormsService [cache-service]
  component/Lifecycle
  VirkailijaTarjontaService

  (start [this] this)
  (stop [this] this)

  (get-forms-in-use [this session]
    (forms-in-use cache-service (:organization-service this) session)))

(defn new-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->MockTarjontaService)
    (->CachedTarjontaService nil)))

(defn new-virkailija-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->MockVirkailijaTarjontaService)
    (->VirkailijaTarjontaFormsService nil)))
