(ns ataru.cache.caches
  (:require [ataru.cache.redis-cache :as redis]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as s])
  (:import java.util.concurrent.TimeUnit))

(def caches
  [(redis/map->UpdatingCache
     {:name   "hakukohde"
      :fetch  tarjonta-client/get-hakukohde
      :ttl    [3 TimeUnit/DAYS]
      :period [15 TimeUnit/MINUTES]})
   (redis/map->BasicCache
     {:name  "haku"
      :fetch tarjonta-client/get-haku
      :ttl   [1 TimeUnit/HOURS]})
   (redis/map->BasicCache
     {:name  "forms-in-use"
      :fetch tarjonta-client/get-forms-in-use
      :ttl   [5 TimeUnit/MINUTES]})
   (redis/map->BasicCache
     {:name  "ohjausparametrit"
      :fetch ohjausparametrit-client/get-ohjausparametrit
      :ttl   [1 TimeUnit/HOURS]})
   (redis/map->UpdatingCache
     {:name   "koulutus"
      :fetch  tarjonta-client/get-koulutus
      :ttl    [3 TimeUnit/DAYS]
      :period [15 TimeUnit/MINUTES]})
   (redis/map->BasicCache
     {:name  "statistics-month"
      :fetch s/get-and-parse-application-stats
      :ttl   [10 TimeUnit/HOURS]})
   (redis/map->BasicCache
     {:name  "statistics-week"
      :fetch s/get-and-parse-application-stats
      :ttl   [1 TimeUnit/HOURS]})
   (redis/map->BasicCache
     {:name  "statistics-day"
      :fetch s/get-and-parse-application-stats
      :ttl   [5 TimeUnit/MINUTES]})])
