(ns ataru.cache.two-layer-cache
  (:require [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.timbre :as log]
            [ataru.cache.cache-service :as cache])
  (:import [java.util.concurrent
            ArrayBlockingQueue
            CompletableFuture
            LinkedBlockingQueue
            ThreadPoolExecutor
            TimeUnit]
           [com.github.benmanes.caffeine.cache
            AsyncCacheLoader
            CacheWriter
            Caffeine
            LoadingCache
            RemovalCause]))

(defn- ->supplier
  [f]
  (reify java.util.function.Supplier
    (get [_] (f))))

(defn- ->function
  [f]
  (reify java.util.function.Function
    (apply [_ x] (f x))))

(defn- ->bi-comsumer
  [f]
  (reify java.util.function.BiConsumer
    (accept [_ x y] (f x y))))

(defn- supplyAsync
  [executor f]
  (CompletableFuture/supplyAsync (->supplier f) executor))

(defn- thenComposeAsync
  [executor completion-stage f]
  (.thenComposeAsync completion-stage (->function f) executor))

(defn- whenComplete
  [completion-stage f]
  (.whenComplete completion-stage (->bi-comsumer f)))

(defn- ->cache-key
  [name key]
  (str "ataru:cache:item:" name ":" key))

(defn- ->lock-key
  [name key]
  (str "ataru:cache:lock:" name ":" key))

(defn- acquire-lock
  [lock-key lock-id timeout-ms]
  (car/set lock-key lock-id :nx :px timeout-ms))

(defn- release-lock
  [lock-key lock-id]
  (car/lua "if redis.call('get', _:key) == _:id then
                return redis.call('del', _:key)
            else
                return 0
            end"
           {:key lock-key}
           {:id lock-id}))

(defn- redis-get
  [redis loader name key]
  (let [cache-key   (->cache-key name key)
        [value ttl] (wcar (:connection-opts redis) :as-pipeline
                          (car/get cache-key)
                          (car/pttl cache-key))]
    (when (some? value)
      (if-let [e (cache/check-schema loader value)]
        (do (log/warn e "Schema check for" key "failed")
            nil)
        [value ttl]))))

(defn- redis-mget-n-lock
  [redis loader name keys lock-id timeout-ms]
  (let [[values & ttls-n-locks] (wcar (:connection-opts redis) :as-pipeline
                                      (apply car/mget (map #(->cache-key name %) keys))
                                      (doseq [key keys]
                                        (car/pttl (->cache-key name key))
                                        (when (some? lock-id)
                                          (acquire-lock (->lock-key name key) lock-id timeout-ms))))]
    (reduce (fn [acc [key value [ttl lock]]]
              (if (some? value)
                (if-let [e (cache/check-schema loader value)]
                  (do (log/warn e "Schema check for" key "failed")
                      (-> acc
                          (update :misses conj key)
                          (assoc-in [:locked? key] (= "OK" lock))))
                  (-> acc
                      (assoc-in [:hits key] value)
                      (assoc-in [:ttls key] ttl)
                      (assoc-in [:locked? key] (= "OK" lock))))
                (-> acc
                    (update :misses conj key)
                    (assoc-in [:locked? key] (= "OK" lock)))))
            {:hits    {}
             :ttls    {}
             :locked? {}
             :misses  []}
            (map vector keys values (partition 2 2 nil (if (some? lock-id)
                                                         ttls-n-locks
                                                         (interleave ttls-n-locks (repeat nil))))))))

(defn- redis-set
  [redis name key value px]
  (when (some? value)
    (wcar (:connection-opts redis)
          (car/set (->cache-key name key) value :px px)))
  value)

(defn- redis-mset
  [redis name px m]
  (wcar (:connection-opts redis) :as-pipeline
        (doseq [[key value] m]
          (car/set (->cache-key name key) value :px px)))
  m)

(defn- load-keys
  [redis loader name keys-to-load px min-ttl timeout-ms]
  (let [lock-id    (str (java.util.UUID/randomUUID))
        load-size  (cache/load-many-size loader)
        load-count (Math/toIntExact (Math/ceil (/ (count keys-to-load) load-size)))
        deadline   (+ (System/currentTimeMillis) (* timeout-ms load-count))]
    (loop [to-load (set keys-to-load)
           values  {}]
      (cond (empty? to-load)
            values
            (< deadline (System/currentTimeMillis))
            (throw (new RuntimeException
                        (str "Failed to load "
                             (count to-load) "/" (count keys-to-load)
                             " keys of cache " name
                             " in " timeout-ms "ms")))
            :else
            (let [key-batch (take load-size to-load)
                  [to-load
                   values]  (try
                              (let [result         (redis-mget-n-lock redis loader name key-batch lock-id timeout-ms)
                                    fresh?         #(<= min-ttl (get-in result [:ttls %]))
                                    locked?        #(get-in result [:locked? %])
                                    fresh-hits     (filter (comp fresh? first) (:hits result))
                                    loader-to-load (->> (:hits result)
                                                        (map first)
                                                        (remove fresh?)
                                                        (concat (:misses result))
                                                        (filter locked?))]
                                [(clojure.set/difference to-load
                                                         (set (map first fresh-hits))
                                                         (set loader-to-load))
                                 (merge (into values fresh-hits)
                                        (when (not-empty loader-to-load)
                                          (redis-mset redis name px (cache/load-many loader loader-to-load))))])
                              (finally
                                (wcar (:connection-opts redis)
                                      (doseq [key key-batch]
                                        (release-lock (->lock-key name key) lock-id)))))]
              (when (not-empty to-load)
                (Thread/sleep (inc (rand-int 10))))
              (recur to-load values))))))

(defn- batch-load-key
  [batch-queue io-executor redis loader name key px min-ttl timeout-ms]
  (if (nil? batch-queue)
    (supplyAsync io-executor (fn [] (get (load-keys redis loader name [key] px min-ttl timeout-ms) key)))
    (let [p (new CompletableFuture)]
      (when-not (.offer batch-queue [key p])
        (throw (new RuntimeException
                    (str "Offer of key " key
                         " of cache " name
                         " to batch queue failed"))))
      (.execute
       io-executor
       (fn []
         (try
           (let [to-load (->> (repeatedly (cache/load-many-size loader)
                                          (fn [] (.poll batch-queue)))
                              (filter some?))]
             (try
               (let [values (load-keys redis loader name (map first to-load) px min-ttl timeout-ms)]
                 (doseq [[key pp] to-load]
                   (.complete pp (get values key))))
               (catch Exception e
                 (doseq [[_ pp] to-load]
                   (.completeExceptionally pp e)))))
           (catch Exception e
             (log/error e "Failed to batch load key" key "of cache" name)))))
      p)))

(defn- ->redis-loader
  [batch-queue redis-executor io-executor redis loader name expires-after refresh-after load-timeout]
  (let [px              (.toMillis (second expires-after) (first expires-after))
        min-ttl         (- px (.toMillis (second refresh-after) (first refresh-after)))
        load-timeout-ms (.toMillis (second load-timeout) (first load-timeout))]
    (reify AsyncCacheLoader
      (asyncLoad [this key executor]
        (thenComposeAsync
         executor
         (supplyAsync redis-executor (fn [] (redis-get redis loader name key)))
         (fn [[value ttl]]
           (when (and (some? value) (< ttl min-ttl))
             (whenComplete
              (batch-load-key batch-queue io-executor redis loader name key px min-ttl load-timeout-ms)
              (fn [_ t]
                (when (some? t)
                  (log/error t "Failed to refresh key" key "of cache" name)))))
           (if (some? value)
             (CompletableFuture/completedFuture value)
             (batch-load-key batch-queue io-executor redis loader name key px min-ttl load-timeout-ms)))))

      (asyncLoadAll [this keys executor]
        (thenComposeAsync
         executor
         (supplyAsync redis-executor (fn [] (redis-mget-n-lock redis loader name keys nil nil)))
         (fn [from-redis]
           (when-let [keys (seq (keep (fn [[key ttl]] (when (< ttl min-ttl) key)) (:ttls from-redis)))]
             (whenComplete
              (supplyAsync io-executor (fn [] (load-keys redis loader name keys px min-ttl load-timeout-ms)))
              (fn [_ t]
                (when (some? t)
                  (log/error t "Failed to refresh" (count keys) "keys of cache" name)))))
           (if (empty? (:misses from-redis))
             (CompletableFuture/completedFuture (:hits from-redis))
             (thenComposeAsync
              executor
              (supplyAsync io-executor (fn [] (load-keys redis loader name (:misses from-redis) px min-ttl load-timeout-ms)))
              (fn [result]
                (CompletableFuture/completedFuture
                 (merge (:hits from-redis) result))))))))

      (asyncReload [this key old-value executor]
        (try
          (thenComposeAsync
           executor
           (supplyAsync redis-executor (fn [] (redis-get redis loader name key)))
           (fn [[value ttl]]
             (if (or (nil? value) (< ttl min-ttl))
               (batch-load-key batch-queue io-executor redis loader name key px min-ttl load-timeout-ms)
               (CompletableFuture/completedFuture value))))
          (catch Exception e
            (log/error e "Failed to refresh key" key "of cache" name)
            (CompletableFuture/completedFuture old-value)))))))

(defrecord Cache [redis
                  name
                  loader
                  size
                  expires-after
                  refresh-on-heap-after
                  refresh-off-heap-after
                  ^LoadingCache caffeine
                  batch-queue
                  redis-executor-queue
                  io-executor-queue
                  redis-executor
                  io-executor]
  component/Lifecycle
  (start [this]
    (if (nil? caffeine)
      (let [batch-queue          (when (< 1 (cache/load-many-size loader))
                                   (new ArrayBlockingQueue (cache/load-many-size loader)))
            cpu-cores            (.availableProcessors (Runtime/getRuntime))
            load-timeout         [5 TimeUnit/MINUTES]
            redis-executor-queue (new LinkedBlockingQueue)
            io-executor-queue    (new LinkedBlockingQueue)
            redis-executor       (new ThreadPoolExecutor
                                      cpu-cores
                                      (* 4 cpu-cores)
                                      10 TimeUnit/SECONDS
                                      redis-executor-queue)
            io-executor          (new ThreadPoolExecutor
                                      cpu-cores
                                      (* 4 cpu-cores)
                                      10 TimeUnit/SECONDS
                                      io-executor-queue)
            loader               (->redis-loader batch-queue
                                                 redis-executor
                                                 io-executor
                                                 redis
                                                 loader
                                                 name
                                                 expires-after
                                                 refresh-off-heap-after
                                                 load-timeout)]
        (assoc this
               :caffeine (-> (cond-> (Caffeine/newBuilder)
                                     (some? size)
                                     (.maximumSize size))
                             (.recordStats)
                             (.expireAfterAccess (first expires-after) (second expires-after))
                             (.refreshAfterWrite (first refresh-on-heap-after) (second refresh-on-heap-after))
                             (.buildAsync loader)
                             (.synchronous))
               :batch-queue batch-queue
               :redis-executor-queue redis-executor-queue
               :io-executor-queue io-executor-queue
               :redis-executor redis-executor
               :io-executor io-executor))
      this))
  (stop [this]
    (when (some? redis-executor)
      (.shutdown redis-executor))
    (when (some? io-executor)
      (.shutdown io-executor))
    (assoc this
           :caffeine nil
           :batch-queue nil
           :redis-executor-queue nil
           :io-executor-queue nil
           :redis-executor nil
           :io-executor nil))
  cache/Stats
  (stats [_]
    (let [caffeine-stats (.stats caffeine)]
      {:hit-rate                    (.hitRate caffeine-stats)
       :eviction-count              (.evictionCount caffeine-stats)
       :batch-queue-length          (if (some? batch-queue) (.size batch-queue) 0)
       :redis-executor-queue-length (.size redis-executor-queue)
       :io-executor-queue-length    (.size io-executor-queue)}))
  cache/Cache
  (get-from [_ key]
    (.get caffeine key))
  (get-many-from [_ keys]
    (.getAll caffeine keys))
  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (->cache-key name key)))
    (.invalidate caffeine key))
  (clear-all [_]
    (loop [[cursor keys] (wcar (:connection-opts redis)
                               (car/scan 0 :match (->cache-key name "*")))]
      (wcar (:connection-opts redis) :as-pipeline
            (doseq [key keys] (car/del key)))
      (when (not= "0" cursor)
        (recur (wcar (:connection-opts redis)
                     (car/scan cursor :match (->cache-key name "*"))))))
    (.invalidateAll caffeine)))
