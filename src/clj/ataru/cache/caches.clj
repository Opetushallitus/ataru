(ns ataru.cache.caches
  (:require [ataru.applications.application-store :as application-store]
            [ataru.cache.cache-service :as cache]
            [ataru.cache.in-memory-cache :as in-memory]
            [ataru.cache.two-layer-cache :as two-layer]
            [ataru.cache.redis-cache :as redis]
            [ataru.lokalisointi-service.lokalisointi-service :as lokalisointi-service]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.organization-service.organization-client :as organization-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as s]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [com.stuartsierra.component :as component])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [[:get-haut-cache
    (in-memory/map->InMemoryCache
     {:loader        (cache/->FunctionCacheLoader
                      (fn [key]
                        (case key
                          :haut             (application-store/get-haut)
                          :direct-form-haut (application-store/get-direct-form-haut))))
      :expires-after [3 TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]
   [:all-organization-groups-cache
    (in-memory/map->InMemoryCache
     {:loader        (cache/->FunctionCacheLoader
                      (fn [_] (organization-client/get-groups)))
      :expires-after [3 TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]
   [:localizations-cache
    (in-memory/map->InMemoryCache
     {:loader        (cache/->FunctionCacheLoader lokalisointi-service/get-localizations)
      :expires-after [3 TimeUnit/DAYS]
      :refresh-after [5 TimeUnit/MINUTES]})]
   [:hakukohde-cache
    (component/using
     (two-layer/map->Cache
      {:name                   "hakukohde"
       :size                   6000
       :loader                 (cache/->FunctionCacheLoader tarjonta-client/get-hakukohde
                                                            tarjonta-client/hakukohde-checker)
       :expires-after          [3 TimeUnit/DAYS]
       :refresh-off-heap-after [15 TimeUnit/MINUTES]
       :refresh-on-heap-after  [7 TimeUnit/MINUTES]})
     [:redis])]
   [:haku-cache
    (component/using
     (redis/map->Cache
      {:name            "haku"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-haku)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:forms-in-use-cache
    (component/using
     (redis/map->Cache
      {:name            "forms-in-use"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-forms-in-use)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:ohjausparametrit-cache
    (component/using
     (redis/map->Cache
      {:name            "ohjausparametrit"
       :loader          (cache/->FunctionCacheLoader ohjausparametrit-client/get-ohjausparametrit)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:koulutus-cache
    (component/using
     (two-layer/map->Cache
      {:name                   "koulutus"
       :size                   6000
       :loader                 (cache/->FunctionCacheLoader tarjonta-client/get-koulutus
                                                            tarjonta-client/koulutus-checker)
       :expires-after          [3 TimeUnit/DAYS]
       :refresh-off-heap-after [15 TimeUnit/MINUTES]
       :refresh-on-heap-after  [7 TimeUnit/MINUTES]})
     [:redis])]
   [:henkilo-cache
    (component/using
     (two-layer/map->Cache
      {:name                   "henkilo"
       :size                   200000
       :expires-after          [3 TimeUnit/DAYS]
       :refresh-off-heap-after [1 TimeUnit/DAYS]
       :refresh-on-heap-after  [10 TimeUnit/SECONDS]})
     {:redis  :redis
      :loader :henkilo-cache-loader})]
   [:hakukohde-search-cache
    (component/using
     (redis/map->Cache
      {:name            "hakukohde-search"
       :loader          (cache/->FunctionCacheLoader tarjonta-service/hakukohde-search-cache-loader-fn)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:statistics-month-cache
    (component/using
     (redis/map->Cache
      {:name            "statistics-month"
       :loader          (cache/->FunctionCacheLoader s/get-and-parse-application-stats)
       :ttl-after-write [10 TimeUnit/HOURS]})
     [:redis])]
   [:statistics-week-cache
    (component/using
     (redis/map->Cache
      {:name            "statistics-week"
       :loader          (cache/->FunctionCacheLoader s/get-and-parse-application-stats)
       :ttl-after-write [1 TimeUnit/HOURS]})
     [:redis])]
   [:statistics-day-cache
    (component/using
     (redis/map->Cache
      {:name            "statistics-day"
       :loader          (cache/->FunctionCacheLoader s/get-and-parse-application-stats)
       :ttl-after-write [5 TimeUnit/MINUTES]})
     [:redis])]
   [:koodisto-cache
    (component/using
     (redis/map->Cache
      {:name            "koodisto"
       :loader          (cache/->FunctionCacheLoader koodisto-cache/get-koodi-options)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:form-by-haku-oid-and-id-cache
    (component/using
     (redis/map->Cache
      {:name               "form-by-haku-oid-and-id"
       :ttl-after-read     [1 TimeUnit/HOURS]
       :ttl-after-write    [1 TimeUnit/HOURS]
       :update-period      [1 TimeUnit/MINUTES]
       :update-after-read? true})
     {:redis  :redis
      :loader :form-by-haku-oid-and-id-cache-loader})]
   [:form-by-haku-oid-str-cache
    (component/using
     (redis/map->Cache
      {:name               "form-by-haku-oid-str"
       :ttl-after-read     [3 TimeUnit/DAYS]
       :ttl-after-write    [3 TimeUnit/DAYS]
       :update-period      [1 TimeUnit/MINUTES]
       :update-after-read? true})
     {:redis  :redis
      :loader :form-by-haku-oid-str-cache-loader})]])
