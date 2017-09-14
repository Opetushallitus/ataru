(ns ataru.cache.caches
  (:require [ataru.cache.hazelcast-cache :refer [map->BasicCache
                                                 map->UpdatingCache]])
  (:import java.util.concurrent.TimeUnit))

(def hazelcast-caches
  [(map->UpdatingCache
    {:name "hakukohde"
     :max-size 10000
     :max-idle [3 TimeUnit/DAYS]
     :period [15 TimeUnit/MINUTES]})
   (map->BasicCache
    {:name "haku" :max-size 10000 :ttl 3600})
   (map->UpdatingCache
    {:name "koulutus"
     :max-size 10000
     :max-idle [3 TimeUnit/DAYS]
     :period [15 TimeUnit/MINUTES]})
   (map->BasicCache
    {:name "statistics-month" :max-size 500 :ttl 36000})
   (map->BasicCache
    {:name "statistics-week" :max-size 500 :ttl 3600})
   (map->BasicCache
    {:name "statistics-day" :max-size 500 :ttl 300})])

(def caches hazelcast-caches)
