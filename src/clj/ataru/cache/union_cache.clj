(ns ataru.cache.union-cache
  (:require [ataru.cache.cache-service :as cache]))

(defrecord CacheLoader [high-priority-loader low-priority-loader]
  cache/CacheLoader
  (load [this key]
    (or (cache/load high-priority-loader key)
        (cache/load low-priority-loader key)))
  (load-many [this keys]
    (merge (cache/load-many low-priority-loader keys)
           (cache/load-many high-priority-loader keys)))
  (load-many-size [this]
    (min (cache/load-many-size high-priority-loader)
         (cache/load-many-size low-priority-loader)))
  (check-schema [this value]
    (or (cache/check-schema high-priority-loader value)
        (cache/check-schema low-priority-loader value))))
