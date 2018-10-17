(ns ataru.cache.caches
  (:require [ataru.cache.redis-cache :as redis]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.person-service.person-client :as person-client]
            [ataru.statistics.statistics-service :as s]
            [com.stuartsierra.component :as component])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [[:hakukohde-cache
    (component/using
     (redis/map->UpdatingCache
      {:name   "hakukohde"
       :fetch  tarjonta-client/get-hakukohde
       :ttl    [3 TimeUnit/DAYS]
       :period [15 TimeUnit/MINUTES]})
     [:redis])]
   [:haku-cache
    (component/using
     (redis/map->BasicCache
      {:name  "haku"
       :fetch tarjonta-client/get-haku
       :ttl   [1 TimeUnit/HOURS]})
     [:redis])]
   [:forms-in-use-cache
    (component/using
     (redis/map->BasicCache
      {:name  "forms-in-use"
       :fetch tarjonta-client/get-forms-in-use
       :ttl   [5 TimeUnit/MINUTES]})
     [:redis])]
   [:ohjausparametrit-cache
    (component/using
     (redis/map->BasicCache
      {:name  "ohjausparametrit"
       :fetch ohjausparametrit-client/get-ohjausparametrit
       :ttl   [1 TimeUnit/HOURS]})
     [:redis])]
   [:koulutus-cache
    (component/using
     (redis/map->UpdatingCache
      {:name   "koulutus"
       :fetch  tarjonta-client/get-koulutus
       :ttl    [3 TimeUnit/DAYS]
       :period [15 TimeUnit/MINUTES]})
     [:redis])]
   [:henkilo-cache-loader
    (component/using
     (person-client/map->PersonCacheLoader {})
     [:oppijanumerorekisteri-cas-client])]
   [:henkilo-cache
    (component/using
     (redis/map->Cache
      {:name            "henkilo"
       :ttl-after-write [1 TimeUnit/HOURS]})
     {:redis  :redis
      :loader :henkilo-cache-loader})]
   [:hakukohde-search-cache
    (component/using
     (redis/map->BasicCache
      {:name  "hakukohde-search"
       :fetch tarjonta-client/hakukohde-search
       :ttl   [1 TimeUnit/HOURS]})
     [:redis])]
   [:statistics-month-cache
    (component/using
     (redis/map->BasicCache
      {:name  "statistics-month"
       :fetch s/get-and-parse-application-stats
       :ttl   [10 TimeUnit/HOURS]})
     [:redis])]
   [:statistics-week-cache
    (component/using
     (redis/map->BasicCache
      {:name  "statistics-week"
       :fetch s/get-and-parse-application-stats
       :ttl   [1 TimeUnit/HOURS]})
     [:redis])]
   [:statistics-day-cache
    (component/using
     (redis/map->BasicCache
      {:name  "statistics-day"
       :fetch s/get-and-parse-application-stats
       :ttl   [5 TimeUnit/MINUTES]})
     [:redis])]])
