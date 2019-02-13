(ns ataru.cache.caches
  (:require [ataru.cache.cache-service :as cache]
            [ataru.cache.redis-cache :as redis]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.tarjonta-service.tarjonta-service :as tarjonta-service]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as s]
            [ataru.koodisto.koodisto-db-cache :as koodisto-cache]
            [com.stuartsierra.component :as component])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [[:hakukohde-cache
    (component/using
     (redis/map->Cache
      {:name            "hakukohde"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-hakukohde)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
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
     (redis/map->Cache
      {:name            "koulutus"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-koulutus)
       :ttl-after-read  [3 TimeUnit/DAYS]
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:henkilo-cache
    (component/using
     (redis/map->Cache
      {:name            "henkilo"
       :ttl-after-write [1 TimeUnit/HOURS]})
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
