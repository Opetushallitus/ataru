(ns ataru.cache.union-cache
  (:require [ataru.cache.cache-service :as cache]
            [com.stuartsierra.component :as component]))

(defrecord Cache [cache-1 cache-2]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)

  cache/Stats
  (stats [_]
    {:cache-1 (cache/stats cache-1)
     :cache-2 (cache/stats cache-2)})

  cache/Cache
  (get-from [_ key]
    (or
      (cache/get-from cache-1 key)
      (cache/get-from cache-2 key)))
  (get-many-from [_ keys]
    (let [result-1 (cache/get-many-from cache-1 keys)
          result-2 (cache/get-many-from cache-2 keys)]
      (merge result-2 result-1)))
  (remove-from [_ key]
    (cache/remove-from cache-1 key)
    (cache/remove-from cache-2 key)
    nil)
  (clear-all [_]
    (cache/clear-all cache-1)
    (cache/clear-all cache-2)
    nil))
