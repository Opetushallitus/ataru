(ns ataru.cache.caches
  (:require [ataru.cache.hazelcast-cache :refer [map->BasicCache
                                                 map->UpdatingCache]]
            [ataru.cache.redis-cache :as redis]
            [ataru.tarjonta-service.tarjonta-client :as tarjonta-client]
            [ataru.ohjausparametrit.ohjausparametrit-client :as ohjausparametrit-client]
            [ataru.statistics.statistics-service :as s])
  (:import java.util.concurrent.TimeUnit))

(def hazelcast-caches
  [(map->UpdatingCache
     {:name     "hakukohde"
      :max-size 10000
      :max-idle [3 TimeUnit/DAYS]
      :period   [15 TimeUnit/MINUTES]})
   (map->BasicCache
     {:name     "forms-in-use"
      :max-size 1000 :ttl 300})
   (map->BasicCache
     {:name "haku" :max-size 10000 :ttl 3600})
   (map->BasicCache
     {:name "ohjausparametrit" :max-size 10000 :ttl 3600})
   (map->UpdatingCache
     {:name     "koulutus"
      :max-size 10000
      :max-idle [3 TimeUnit/DAYS]
      :period   [15 TimeUnit/MINUTES]})
   (map->BasicCache
     {:name "statistics-month" :max-size 500 :ttl 36000})
   (map->BasicCache
     {:name "statistics-week" :max-size 500 :ttl 3600})
   (map->BasicCache
     {:name "statistics-day" :max-size 500 :ttl 300})])

(def redis-caches
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
