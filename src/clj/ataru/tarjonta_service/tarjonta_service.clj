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
  (update hakus avain merge (reduce haku-name-and-oid {} haut)))

(defn- forms-in-use
  [cache-service organization-service session]
  (->> (select-organizations-for-rights organization-service
                                        session
                                        [:form-edit])
       (map :oid)
       ((fn [oids] (if (and (empty? oids)
                            (get-in session [:identity :superuser]))
                     [oph-organization]
                     oids)))
       (mapcat (partial cache/cache-get cache-service :forms-in-use))
       (reduce hakus-by-form-key {})))

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

(defn yhteishaku? [haku]
  (= (:hakutapaUri haku) "hakutapa_01#1"))

(defn parse-haku
  [haku]
  {:oid                    (:oid haku)
   :name                   (parse-multi-lang-text (:nimi haku))
   :prioritize-hakukohteet (:usePriority haku)
   :yhteishaku             (yhteishaku? haku)
   :hakuajat               (mapv parse-hakuaika (:hakuaikas haku))})

(defn- parse-hakukohde
  [hakukohde]
  {:oid (:oid hakukohde)
   :haku-oid (:hakuOid hakukohde)
   :name (parse-multi-lang-text (:hakukohteenNimet hakukohde))
   :tarjoaja-name (parse-multi-lang-text (:tarjoajaNimet hakukohde))
   :tarjoaja-oids (:tarjoajaOids hakukohde)
   :ryhmaliitokset (some->> (:ryhmaliitokset hakukohde)
                     (map #(:ryhmaOid %)))})

(defn- parse-search-result
  [search-result]
  (doall (mapcat :tulokset (:tulokset search-result))))

(def allowed-hakukohde-tilas #{"VALMIS" "JULKAISTU"})

(defrecord CachedTarjontaService [cache-service]
  TarjontaService
  (get-hakukohde [this hakukohde-oid]
    (when-let [hakukohde (cache/cache-get cache-service :hakukohde hakukohde-oid)]
      (when (contains? allowed-hakukohde-tilas (:tila hakukohde))
        ;; Serialization breaks boxed booleans, as it doesn't return the
        ;; canonical instance
        (update hakukohde
                :kaytetaanHakukohdekohtaistaHakuaikaa #(.booleanValue %)))))

  (get-hakukohteet [this hakukohde-oids]
    (remove #(or (nil? %)
                 (not (contains? allowed-hakukohde-tilas (:tila %))))
            (cache/cache-get-many
             cache-service
             :hakukohde
             hakukohde-oids)))

  (get-hakukohde-name [this hakukohde-oid]
    (when-let [hakukohde (.get-hakukohde this hakukohde-oid)]
      (parse-multi-lang-text (:hakukohteenNimet hakukohde))))

  (hakukohde-search [this haku-oid organization-oid]
    (some->> (client/hakukohde-search haku-oid organization-oid)
             parse-search-result
             (map :oid)
             (.get-hakukohteet this)
             (mapv parse-hakukohde)))

  (get-haku [this haku-oid]
    ;; Serialization breaks boxed booleans, as it doesn't return the
    ;; canonical instance
    (some-> (cache/cache-get cache-service :haku haku-oid)
            (update :canSubmitMultipleApplications #(.booleanValue %))
            (update :usePriority #(.booleanValue %))))

  (get-haku-name [this haku-oid]
    (when-let [haku (.get-haku this haku-oid)]
      (parse-multi-lang-text (:nimi haku))))

  (get-koulutus [this koulutus-oid]
    (cache/cache-get cache-service :koulutus koulutus-oid)))

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
