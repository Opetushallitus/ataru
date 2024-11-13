(ns ataru.tarjonta-service.tarjonta-service
  (:require [ataru.organization-service.organization-service :as organization-service]
            [ataru.organization-service.organization-client :refer [oph-organization]]
            [ataru.config.core :refer [config]]
            [ataru.cache.cache-service :as cache]
            [ataru.tarjonta-service.kouta.kouta-client :as kouta-client]
            [ataru.tarjonta-service.tarjonta-protocol :refer [TarjontaService get-haku]]
            [ataru.tarjonta-service.mock-tarjonta-service :refer [->MockTarjontaService]]))

(defn fetch-or-cached-hakukohde-search
  [hakukohde-search-cache haku-oid organization-oid]
  (cache/get-from hakukohde-search-cache
                  (str haku-oid "#" organization-oid)))

(defrecord CachedTarjontaService [organization-service
                                  forms-in-use-cache
                                  koulutus-cache
                                  kouta-hakus-by-form-key-cache
                                  hakukohde-cache
                                  haku-cache
                                  hakukohde-search-cache
                                  kouta-internal-cas-client]
  TarjontaService
  (get-hakukohde [_ hakukohde-oid]
    (cache/get-from hakukohde-cache hakukohde-oid))

  (get-hakukohteet [_ hakukohde-oids]
    (vals (cache/get-many-from hakukohde-cache hakukohde-oids)))

  (get-hakukohde-name [_ hakukohde-oid]
    (:name (cache/get-from hakukohde-cache hakukohde-oid)))

  (hakukohde-search [this haku-oid organization-oid]
    (let [haun-hakukohteet (some->> (fetch-or-cached-hakukohde-search
                                     hakukohde-search-cache
                                     haku-oid
                                     nil)
                                    (.get-hakukohteet this))]
      (cond (or (nil? organization-oid)
                (= oph-organization organization-oid))
            (map #(assoc % :user-organization? true)
                 haun-hakukohteet)
            (organization-service/group-oid? organization-oid)
            (map #(assoc % :user-organization?
                         (boolean
                          (some (fn [oid] (= organization-oid oid))
                                (:ryhmaliitokset %))))
                 haun-hakukohteet)
            :else
            (let [filtered-hakukohde-oids (->> (organization-service/get-all-organizations
                                                organization-service
                                                [{:oid organization-oid}])
                                               (map :oid)
                                               (mapcat #(fetch-or-cached-hakukohde-search
                                                         hakukohde-search-cache
                                                         haku-oid
                                                         %))
                                               set)]
              (map #(assoc % :user-organization? (contains? filtered-hakukohde-oids (:oid %)))
                   haun-hakukohteet)))))

  (get-haku [_ haku-oid]
    (cache/get-from haku-cache haku-oid))

  (hakus-by-form-key [_ form-key]
    (->> (concat
          (some #(when (= form-key (:avain %))
                  (map :oid (:haut %)))
                (cache/get-from forms-in-use-cache oph-organization))
          (cache/get-from kouta-hakus-by-form-key-cache form-key))
         (cache/get-many-from haku-cache)
         vals))

  (get-haku-name [_ haku-oid]
    (:name (cache/get-from haku-cache haku-oid)))

  (get-koulutus [_ koulutus-oid]
    (cache/get-from koulutus-cache koulutus-oid))

  (get-koulutukset [_ koulutus-oids]
    (cache/get-many-from koulutus-cache koulutus-oids))

  (clear-haku-caches [this haku-oid]
    (let [haku (get-haku this haku-oid)
          hakukohde-oids (:hakukohteet haku)]
      (cache/remove-from haku-cache haku-oid)
      (doseq [hakukohde-oid hakukohde-oids]
        (cache/remove-from hakukohde-cache hakukohde-oid))))

  (get-haku-oids [_]
    (kouta-client/get-haku-oids [kouta-internal-cas-client])))

(defn new-tarjonta-service
  []
  (if (-> config :dev :fake-dependencies)
    (->MockTarjontaService)
    (map->CachedTarjontaService {})))
