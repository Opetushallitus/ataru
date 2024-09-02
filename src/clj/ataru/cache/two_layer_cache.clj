(ns ataru.cache.two-layer-cache
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [ataru.cache.cache-service :as cache])
  (:import java.util.concurrent.TimeUnit
           [com.github.benmanes.caffeine.cache
            CacheLoader
            Caffeine
            LoadingCache]))

(defn- ->redis-loader
  [redis-cache]
  (reify CacheLoader
    (load [_ key]
      (cache/get-from redis-cache key))

    (loadAll [_ keys]
      (cache/get-many-from redis-cache keys))

    (reload [_ key old-value]
      (try
        (cache/get-from redis-cache key)
        (catch Exception e
          (log/error e "Failed to refresh key" key "of cache" name)
          old-value)))))

(defrecord Cache [redis-cache
                  name
                  size
                  expire-after-access
                  ^LoadingCache caffeine]
  component/Lifecycle
  (start [this]
    (if (nil? caffeine)
      (assoc this :caffeine (-> (cond-> (Caffeine/newBuilder)
                                        (some? size)
                                        (.maximumSize size))
                                (.recordStats)
                                (.expireAfterAccess (first expire-after-access) (second expire-after-access))
                                (.build (->redis-loader redis-cache))))
      this))
  (stop [this]
    (assoc this :caffeine nil))
  cache/Stats
  (stats [_]
    (let [caffeine-stats (.stats caffeine)]
      {:average-load-penalty (.toMillis TimeUnit/NANOSECONDS
                                        (.averageLoadPenalty caffeine-stats))
       :hit-rate             (.hitRate caffeine-stats)}))
  cache/Cache
  (get-from [_ key]
    (.get caffeine key))
  (get-many-from [_ keys]
    (.getAll caffeine keys))
  (remove-from [_ key]
    (cache/remove-from redis-cache key)
    (.invalidate caffeine key))
  (clear-all [_]
    (cache/clear-all redis-cache)
    (.invalidateAll caffeine)))
