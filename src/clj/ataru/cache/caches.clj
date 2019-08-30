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
   [:hakukohde-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "hakukohde"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-client/get-hakukohde
                                                   tarjonta-client/hakukohde-checker)})
     [:redis])]
   [:hakukohde-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-hakukohde"
       :size                6000
       :expire-after-access [3 TimeUnit/DAYS]
       :refresh-after       [5 TimeUnit/MINUTES]})
     {:redis-cache :hakukohde-redis-cache})]
   [:haku-cache
    (component/using
     (redis/map->Cache
      {:name          "haku"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-client/get-haku)})
     [:redis])]
   [:forms-in-use-cache
    (component/using
     (redis/map->Cache
      {:name          "forms-in-use"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-client/get-forms-in-use)})
     [:redis])]
   [:ohjausparametrit-cache
    (component/using
     (redis/map->Cache
      {:name          "ohjausparametrit"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader ohjausparametrit-client/get-ohjausparametrit)})
     [:redis])]
   [:koulutus-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "koulutus"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-client/get-koulutus
                                                   tarjonta-client/koulutus-checker)})
     [:redis])]
   [:koulutus-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-koulutus"
       :size                6000
       :expire-after-access [3 TimeUnit/DAYS]
       :refresh-after       [7 TimeUnit/MINUTES]})
     {:redis-cache :koulutus-redis-cache})]
   [:henkilo-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "henkilo"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [1 TimeUnit/DAYS]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]})
     {:redis  :redis
      :loader :henkilo-cache-loader})]
   [:henkilo-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-henkilo"
       :size                200000
       :expire-after-access [3 TimeUnit/DAYS]
       :refresh-after       [1 TimeUnit/SECONDS]})
     {:redis-cache :henkilo-redis-cache})]
   [:hakukohde-search-cache
    (component/using
     (redis/map->Cache
      {:name          "hakukohde-search"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader tarjonta-service/hakukohde-search-cache-loader-fn)})
     [:redis])]
   [:statistics-month-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-month"
       :ttl          [10 TimeUnit/HOURS]
       :lock-timeout [10000 TimeUnit/MILLISECONDS]
       :loader       (cache/->FunctionCacheLoader s/get-and-parse-application-stats)})
     [:redis])]
   [:statistics-week-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-week"
       :ttl          [1 TimeUnit/HOURS]
       :lock-timeout [10000 TimeUnit/MILLISECONDS]
       :loader       (cache/->FunctionCacheLoader s/get-and-parse-application-stats)})
     [:redis])]
   [:statistics-day-cache
    (component/using
     (redis/map->Cache
      {:name         "statistics-day"
       :ttl          [5 TimeUnit/MINUTES]
       :lock-timeout [10000 TimeUnit/MILLISECONDS]
       :loader       (cache/->FunctionCacheLoader s/get-and-parse-application-stats)})
     [:redis])]
   [:koodisto-redis-cache
    (component/using
     (redis/map->Cache
      {:name          "koodisto"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [15 TimeUnit/MINUTES]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]
       :loader        (cache/->FunctionCacheLoader koodisto-cache/get-koodi-options
                                                   koodisto-cache/koodisto-checker)})
     [:redis])]
   [:koodisto-cache
    (component/using
     (two-layer/map->Cache
      {:name                "in-memory-koodisto"
       :expire-after-access [3 TimeUnit/DAYS]
       :refresh-after       [7 TimeUnit/MINUTES]})
     {:redis-cache :koodisto-redis-cache})]
   [:form-by-haku-oid-and-id-cache
    (component/using
     (redis/map->Cache
      {:name          "form-by-haku-oid-and-id"
       :ttl           [1 TimeUnit/HOURS]
       :refresh-after [5 TimeUnit/SECONDS]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]})
     {:redis  :redis
      :loader :form-by-haku-oid-and-id-cache-loader})]
   [:form-by-haku-oid-str-cache
    (component/using
     (redis/map->Cache
      {:name          "form-by-haku-oid-str"
       :ttl           [3 TimeUnit/DAYS]
       :refresh-after [5 TimeUnit/SECONDS]
       :lock-timeout  [10000 TimeUnit/MILLISECONDS]})
     {:redis  :redis
      :loader :form-by-haku-oid-str-cache-loader})]])
