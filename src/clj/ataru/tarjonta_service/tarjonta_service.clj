(ns ataru.tarjonta-service.tarjonta-service
  (:require
    [ataru.tarjonta-service.tarjonta-client :as client]
    [ataru.organization-service.organization-client :refer [oph-organization]]
    [com.stuartsierra.component :as component]
    [ataru.config.core :refer [config]]
    [ataru.cache.cache-service :as cache]
    [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService get-haku get-hakukohde]]
    [ataru.tarjonta-service.mock-tarjonta-service :refer [->MockTarjontaService]]))

(defn- parse-search-result
  [search-result]
  (mapcat :tulokset (:tulokset search-result)))

(def allowed-hakukohde-tilas #{:valmis :julkaistu})

(defn fetch-or-cached-hakukohde-search
  [hakukohde-search-cache haku-oid organization-oid]
  (parse-search-result (cache/get-from
                        hakukohde-search-cache
                        (str haku-oid "#" organization-oid))))

(defn hakukohde-search-cache-loader-fn
  [key]
  (let [[haku-oid organization-oid] (clojure.string/split key #"#")]
    (client/hakukohde-search haku-oid organization-oid)))

(defrecord CachedTarjontaService [forms-in-use-cache
                                  koulutus-cache
                                  kouta-hakus-by-form-key-cache
                                  hakukohde-cache
                                  haku-cache
                                  hakukohde-search-cache]
  TarjontaService
  (get-hakukohde [this hakukohde-oid]
    (when-let [hakukohde (cache/get-from hakukohde-cache hakukohde-oid)]
      (when (contains? allowed-hakukohde-tilas (:tila hakukohde))
        hakukohde)))

  (get-hakukohteet [this hakukohde-oids]
    (filter #(contains? allowed-hakukohde-tilas (:tila %))
            (vals (cache/get-many-from hakukohde-cache hakukohde-oids))))

  (get-hakukohde-name [this hakukohde-oid]
    (:name (cache/get-from hakukohde-cache hakukohde-oid)))

  (hakukohde-search [this haku-oid organization-oid]
    (let [filtered-hakukohde-oids (->> (fetch-or-cached-hakukohde-search
                                        hakukohde-search-cache
                                        haku-oid
                                        organization-oid)
                                       (map :oid)
                                       set)]
      (->> (fetch-or-cached-hakukohde-search hakukohde-search-cache haku-oid nil)
           (map :oid)
           (.get-hakukohteet this)
           (map #(assoc % :user-organization? (contains? filtered-hakukohde-oids (:oid %)))))))

  (get-haku [this haku-oid]
    (cache/get-from haku-cache haku-oid))

  (hakus-by-form-key [this form-key]
    (->> (concat
          (some #(when (= form-key (:avain %))
                   (map :oid (:haut %)))
                (cache/get-from forms-in-use-cache oph-organization))
          (cache/get-from kouta-hakus-by-form-key-cache form-key))
         (cache/get-many-from haku-cache)
         vals))

  (get-haku-name [this haku-oid]
    (:name (cache/get-from haku-cache haku-oid)))

  (get-koulutus [this koulutus-oid]
    (cache/get-from koulutus-cache koulutus-oid))

  (get-koulutukset [this koulutus-oids]
    (cache/get-many-from koulutus-cache koulutus-oids)))

(defn new-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->MockTarjontaService)
    (map->CachedTarjontaService {})))
