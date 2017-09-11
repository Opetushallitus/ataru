(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.virkailija.user.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]
    [ataru.config.core :refer [config]]
    [ataru.cache.cache-service :as cache]
    [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService get-hakukohde]]
    [ataru.tarjonta-service.mock-tarjonta-service :refer [->MockTarjontaService]]))

(defn forms-in-use
  [organization-service username]
  (let [direct-organizations     (.get-direct-organizations-for-rights organization-service username [:form-edit])
        all-organization-oids    (map :oid (.get-all-organizations organization-service (:form-edit direct-organizations)))
        in-oph-organization?     (some #{oph-organization} all-organization-oids)]
    (reduce (fn [acc1 {:keys [avain haut]}]
              (assoc acc1 avain
                          (reduce (fn [acc2 haku]
                                    (assoc acc2 (:oid haku)
                                                {:haku-oid  (:oid haku)
                                                 :haku-name (get-in haku [:nimi :kieli_fi])}))
                                  {} haut)))
            {}
            (client/get-forms-in-use (if in-oph-organization? nil all-organization-oids)))))

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
   :tarjoaja-name (parse-multi-lang-text (:tarjoajaNimet hakukohde))})

(defn- parse-search-result
  [search-result]
  (doall (mapcat :tulokset (:tulokset search-result))))

(defrecord CachedTarjontaService [cache-service]
  TarjontaService
  (get-hakukohde [this hakukohde-oid]
    (cache/cache-get-or-fetch cache-service :hakukohde hakukohde-oid #(client/get-hakukohde hakukohde-oid)))

  (get-hakukohde-name [this hakukohde-oid]
    (-> this
        (.get-hakukohde hakukohde-oid)
        :hakukohteenNimet
        :kieli_fi))

  (hakukohde-search [this haku-oid organization-oid]
    (some->> (client/hakukohde-search haku-oid organization-oid)
             parse-search-result
             (map :oid)
             (map (partial get-hakukohde this))
             (mapv parse-hakukohde)))

  (get-haku [this haku-oid]
    (cache/cache-get-or-fetch cache-service :haku haku-oid #(client/get-haku haku-oid)))

  (get-haku-name [this haku-oid]
    (-> this
        (.get-haku haku-oid)
        :nimi
        :kieli_fi))

  (get-koulutus [this koulutus-oid]
    (cache/cache-get-or-fetch cache-service :koulutus koulutus-oid #(client/get-koulutus koulutus-oid))))

(defprotocol VirkailijaTarjontaService
  (get-forms-in-use [this username]))

(defrecord VirkailijaTarjontaFormsService []
  component/Lifecycle
  VirkailijaTarjontaService

  (start [this] this)
  (stop [this] this)

  (get-forms-in-use [this username]
    (forms-in-use (:organization-service this) username)))

(defn new-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->MockTarjontaService)
    (->CachedTarjontaService nil)))

(defn new-virkailija-tarjonta-service
  []
  (->VirkailijaTarjontaFormsService))
