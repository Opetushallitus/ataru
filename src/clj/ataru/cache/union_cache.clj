(ns ataru.cache.union-cache
  (:require [ataru.cache.cache-service :as cache]))

(defrecord CacheLoader [high-priority-loader low-priority-loader]
  cache/CacheLoader
  (load [_ key]
    (or (cache/load high-priority-loader key)
        (cache/load low-priority-loader key)))
  (load-many [_ keys]
    (let [high-results (cache/load-many high-priority-loader keys)]
      (merge (->> keys
                  (remove #(contains? high-results %))
                  (cache/load-many low-priority-loader))
             high-results)))
  (load-many-size [_]
    (min (cache/load-many-size high-priority-loader)
         (cache/load-many-size low-priority-loader)))
  (check-schema [_ value]
    (or (cache/check-schema high-priority-loader value)
        (cache/check-schema low-priority-loader value))))
