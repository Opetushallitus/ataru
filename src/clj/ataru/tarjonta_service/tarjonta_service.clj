(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.virkailija.user.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]
    [ataru.config.core :refer [config]]
    [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService]]
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

(defn- parse-haku
  [haku]
  {:oid (:oid haku)
   :name (parse-multi-lang-text (:nimi haku))
   :hakuajat (map parse-hakuaika (:hakuaikas haku))})

(defn- parse-hakukohde
  [hakukohde]
  {:oid (:oid hakukohde)
   :haku-oid (:hakuOid hakukohde)
   :name (parse-multi-lang-text (:nimi hakukohde))})

(defn- parse-search-result
  [search-result]
  (mapcat :tulokset (:tulokset search-result)))

(defrecord CachedTarjontaService []
  component/Lifecycle
  TarjontaService

  (start [this] this)
  (stop [this] this)

  (get-hakukohde [this hakukohde-oid]
    (.cache-get-or-fetch (:cache-service this) :hakukohde hakukohde-oid #(client/get-hakukohde hakukohde-oid)))

  (get-hakukohde-name [this hakukohde-oid]
    (-> this
        (.get-hakukohde hakukohde-oid)
        :hakukohteenNimet
        :kieli_fi))

  (hakukohteet-by-organization [this organization-oid]
    (concat (->> (client/hakukohteet-by-organization organization-oid)
                 parse-search-result
                 (map parse-hakukohde))
            (->> (client/hakukohteet-by-organization-group organization-oid)
                 parse-search-result
                 (map parse-hakukohde))))

  (all-haut [_]
    (map parse-haku (client/all-haut)))

  (get-haku [this haku-oid]
    (.cache-get-or-fetch (:cache-service this) :haku haku-oid #(client/get-haku haku-oid)))

  (get-haku-name [this haku-oid]
    (-> this
        (.get-haku haku-oid)
        :nimi
        :kieli_fi))

  (get-koulutus [this koulutus-oid]
    (.cache-get-or-fetch (:cache-service this) :koulutus koulutus-oid #(client/get-koulutus koulutus-oid))))

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
    (->CachedTarjontaService)))

(defn new-virkailija-tarjonta-service
  []
  (->VirkailijaTarjontaFormsService))
