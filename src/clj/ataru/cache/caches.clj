(ns ataru.cache.caches
  (:require [ataru.cache.cache-service :as cache]
            [ataru.cache.redis-cache :as redis]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as s]
            [com.stuartsierra.component :as component])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [[:hakukohde-cache
    (component/using
     (redis/map->Cache
      {:name            "hakukohde"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-hakukohde)
       :ttl-after-write [3 TimeUnit/DAYS]
       :update-period   [15 TimeUnit/MINUTES]})
     [:redis])]
   [:haku-cache
    (component/using
     (redis/map->Cache
      {:name            "haku"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-haku)
       :ttl-after-write [1 TimeUnit/HOURS]})
     [:redis])]
   [:forms-in-use-cache
    (component/using
     (redis/map->Cache
      {:name            "forms-in-use"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-forms-in-use)
       :ttl-after-write [5 TimeUnit/MINUTES]})
     [:redis])]
   [:ohjausparametrit-cache
    (component/using
     (redis/map->Cache
      {:name            "ohjausparametrit"
       :loader          (cache/->FunctionCacheLoader ohjausparametrit-client/get-ohjausparametrit)
       :ttl-after-write [1 TimeUnit/HOURS]})
     [:redis])]
   [:koulutus-cache
    (component/using
     (redis/map->Cache
      {:name            "koulutus"
       :loader          (cache/->FunctionCacheLoader tarjonta-client/get-koulutus)
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
       :loader          (cache/->FunctionCacheLoader tarjonta-client/hakukohde-search)
       :ttl-after-write [1 TimeUnit/HOURS]})
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
     [:redis])]])
