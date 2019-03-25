(ns ataru.cache.two-layer-cache
  (:require [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.timbre :as log]
            [ataru.cache.cache-service :as cache])
  (:import [com.github.benmanes.caffeine.cache
            CacheLoader
            CacheWriter
            Caffeine
            LoadingCache
            RemovalCause]))

(defn- ->cache-key
  [name key]
  (str "ataru:cache:item:" name ":" key))

(defn- redis-get
  [redis loader name key]
  (let [value (wcar (:connection-opts redis)
                    (car/get (->cache-key name key)))]
    (when (some? value)
      (if-let [e (cache/check-schema loader value)]
        (do (log/warn e "Schema check for" key "failed")
            nil)
        value))))

(defn- redis-mget
  [redis loader name keys]
  (reduce (fn [acc keys]
            (let [cache-keys (map #(->cache-key name %) keys)
                  result     (wcar (:connection-opts redis)
                                   (apply car/mget cache-keys))]
              (reduce (fn [acc [key value]]
                        (if (some? value)
                          (if-let [e (cache/check-schema loader value)]
                            (do (log/warn e "Schema check for" key "failed")
                                (update acc :misses conj key))
                            (update acc :hits assoc key value))
                          (update acc :misses conj key)))
                      acc
                      (map vector keys result))))
          {:hits   {}
           :misses []}
          (partition 5000 5000 nil keys)))

(defn- redis-set
  [redis name key value expires-after]
  (when (some? value)
    (wcar (:connection-opts redis)
          (car/set (->cache-key name key) value
                   :px (.toMillis (second expires-after)
                                  (first expires-after)))))
  value)

(defn- redis-mset
  [redis name m expires-after]
  (let [px (.toMillis (second expires-after)
                      (first expires-after))]
    (doseq [kvs (partition 5000 5000 nil m)]
      (wcar (:connection-opts redis)
            (doseq [[key value] kvs]
              (car/set (->cache-key name key) value :px px)))))
  m)

(defn- redis-del
  [redis name key]
  (wcar (:connection-opts redis)
        (car/del (->cache-key name key))))

(defn- ->redis-writer
  [redis name expires-after]
  (reify CacheWriter
    (write [this key value]
      (redis-set redis name key value expires-after))
    (delete [this key value cause]
      (cond (= RemovalCause/EXPLICIT cause) (redis-del redis name key)
            (= RemovalCause/REPLACED cause) (redis-set redis name key value expires-after)))))

(defn- ->redis-loader
  [redis loader name expires-after]
  (reify CacheLoader
    (load [this key]
      (let [from-redis (redis-get redis loader name key)]
        (if (some? from-redis)
          from-redis
          (.reload this key nil))))
    (loadAll [this keys]
      (let [from-redis (redis-mget redis loader name keys)]
        (merge (:hits from-redis)
               (when-let [misses (seq (:misses from-redis))]
                 (redis-mset redis name (cache/load-many loader misses) expires-after)))))
    (reload [this key _]
      (redis-set redis name key (cache/load loader key) expires-after))))

(defrecord Cache [redis
                  name
                  loader
                  size
                  expires-after
                  refresh-after
                  ^LoadingCache caffeine]
  component/Lifecycle
  (start [this]
    (if (nil? caffeine)
      (assoc this :caffeine
             (-> (cond-> (Caffeine/newBuilder)
                         (some? size)
                         (.maximumSize size))
                 (.recordStats)
                 (.expireAfterWrite (first expires-after) (second expires-after))
                 (.refreshAfterWrite (first refresh-after) (second refresh-after))
                 (.writer (->redis-writer redis name expires-after))
                 (.build (->redis-loader redis loader name expires-after))))
      this))
  (stop [this]
    (assoc this :caffeine nil))
  cache/Stats
  (stats [_]
    (let [s (.stats caffeine)]
      {:hit-rate             (.hitRate s)
       :eviction-count       (.evictionCount s)
       :average-load-penalty (.averageLoadPenalty s)}))
  cache/Cache
  (get-from [this key]
    (.get caffeine key))
  (get-many-from [this keys]
    (.getAll caffeine keys))
  (remove-from [this key]
    (.invalidate caffeine key))
  (clear-all [this]
    (.invalidateAll caffeine)))
