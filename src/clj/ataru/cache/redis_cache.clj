(ns ataru.cache.redis-cache
  (:require [taoensso.timbre :refer [info warn error]]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.carmine.locks :as carlocks]
            [ataru.cache.cache-service :as cache])
  (:import [java.util.concurrent
            ScheduledThreadPoolExecutor
            TimeUnit]))

(defn- update-index [redis cache-name]
  (if-let [index (wcar (:connection-opts redis)
                       (car/get (str cache-name "-update-index")))]
    (Long/parseLong index)
    0))

(defn- to-update-count [redis cache-name]
  (wcar (:connection-opts redis)
        (car/scard (str cache-name "-to-update"))))

(defn- no-keys-to-update? [redis cache-name]
  (zero? (to-update-count redis cache-name)))

(defn- inc-update-index [redis cache-name]
  (wcar (:connection-opts redis)
        (car/incr (str cache-name "-update-index"))))

(defn- mark-all-keys-for-update [redis cache-name]
  (loop [[cursor keys] (wcar (:connection-opts redis)
                             (car/scan 0 :match (str cache-name "_*")))]
    (wcar (:connection-opts redis)
          (mapv #(car/sadd (str cache-name "-to-update") %) keys))
    (when (not= "0" cursor)
      (recur (wcar (:connection-opts redis)
                   (car/scan cursor :match (str cache-name "_*")))))))

(defn- mark-keys-for-update [redis cache-name my-update-index]
  (carlocks/with-lock (:connection-opts redis) (str cache-name "-update-lock") 1000 500
    (let [current-update-index (update-index redis cache-name)]
      (if (and (= @my-update-index current-update-index)
               (no-keys-to-update? redis cache-name))
        (do (info "marking" cache-name "keys for update")
            (inc-update-index redis cache-name)
            (swap! my-update-index inc)
            (mark-all-keys-for-update redis cache-name)
            (info "marked" (to-update-count redis cache-name) cache-name "keys for update"))
        (reset! my-update-index current-update-index)))))

(defn- find-to-update [redis count cache-name]
  (wcar (:connection-opts redis)
        (car/spop (str cache-name "-to-update") count)))

(defn- strip-name [cache-name key]
  (clojure.string/replace key (str cache-name "_") ""))

(defn- update-marked-keys [redis cache-name fetch]
  (info "updating" cache-name "keys")
  (loop [keys (find-to-update redis 10 cache-name)]
    (when (not-empty keys)
      (let [key-values (->> keys
                            (mapv (fn [key] [key (fetch (strip-name cache-name key))]))
                            (remove (comp nil? second)))
            ttls (wcar (:connection-opts redis) :as-pipeline
                       (mapv (comp car/pttl first) key-values))]
        (wcar (:connection-opts redis)
              (mapv (fn [[key value] ttl] (car/set key value :px ttl))
                    key-values
                    ttls))
        (recur (find-to-update redis 10 cache-name)))))
  (info cache-name "keys updated"))

(defrecord BasicCache [redis
                       name
                       fetch
                       ttl]
  cache/Cache
  (get-from [this key]
    (if-let [value (wcar (:connection-opts redis) (car/get (str name "_" key)))]
      value
      (:result
       (carlocks/with-lock (:connection-opts redis) (str name "-fetch-lock") 10000 5000
         (if-let [value (wcar (:connection-opts redis) (car/get (str name "_" key)))]
           value
           (when-let [new-value (fetch key)]
             (cache/put-to this key new-value)
             new-value))))))
  (get-many-from [this keys]
    (if (empty? keys)
      []
      (mapcat (fn [keys]
                (map (fn [key value] (if (some? value) value (cache/get-from this key)))
                     keys
                     (wcar (:connection-opts redis)
                           (apply car/mget (map #(str name "_" %) keys)))))
              (partition 5000 5000 nil keys))))
  (put-to [_ key value]
    (let [[ttl timeunit] ttl]
      (wcar (:connection-opts redis)
            (car/set (str name "_" key) value :px (.toMillis timeunit ttl)))))
  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (str name "_" key))))
  (clear-all [_]
    (loop [[cursor keys] (wcar (:connection-opts redis)
                               (car/scan 0 :match (str name "_*")))]
      (wcar (:connection-opts redis)
            (mapv car/del keys))
      (when (not= "0" cursor)
        (recur (wcar (:connection-opts redis)
                     (car/scan cursor :match (str name "_*"))))))))

(defrecord UpdatingCache [redis
                          scheduler
                          name
                          fetch
                          period
                          ttl
                          cache]
  component/Lifecycle
  (start [this]
    (if (nil? scheduler)
      (let [cache (map->BasicCache {:redis redis
                                    :name name
                                    :fetch fetch
                                    :ttl ttl})
            scheduler (ScheduledThreadPoolExecutor. 1)
            my-update-index (atom 0)
            [period timeunit] period]
        (.scheduleAtFixedRate
         scheduler
         (fn []
           (try
             (mark-keys-for-update redis name my-update-index)
             (update-marked-keys redis name fetch)
             (catch Exception e (error e "updating" name "keys failed"))))
         period period timeunit)
        (-> this
            (assoc :scheduler scheduler)
            (assoc :cache cache)))
      this))
  (stop [this]
    (when (some? scheduler)
      (.shutdown scheduler))
    (assoc this :scheduler nil))

  cache/Cache
  (get-from [_ key] (cache/get-from cache key))
  (get-many-from [_ keys] (cache/get-many-from cache keys))
  (put-to [_ key value] (cache/put-to cache key value))
  (remove-from [_ key] (cache/remove-from cache key))
  (clear-all [_] (cache/clear-all cache)))

(defrecord MappedCache
  [redis name ttl]
  cache/MappedCache
  (get-from-or-fetch [this fetch-fn key]
    (when key
      (if-let [value (wcar (:connection-opts redis) (car/get (str name "_" key)))]
        value
        (:result
          (carlocks/with-lock (:connection-opts redis) (str name "-fetch-lock") 10000 5000
                              (if-let [value (wcar (:connection-opts redis) (car/get (str name "_" key)))]
                                value
                                (when-let [new-value (fetch-fn key)]
                                  (cache/put-to this key new-value)
                                  new-value)))))))

  (put-many-to [_ key-values]
    (when (not-empty key-values)
      (let [key-value-flattened (->> key-values
                                     (map (fn [[k v]] [(str name "_" k) v]))
                                     (filter (fn [[_ v]] (some? v))))
            [ttl timeunit] ttl
            ttl-ms              (.toMillis timeunit ttl)]
        (wcar (:connection-opts redis)
              (mapv (fn [[k v]] (car/set k v :px ttl-ms)) key-value-flattened)))))

  cache/Cache
  (get-from [_ key]
    (throw (RuntimeException. "Not implemented")))
  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (str name "_" key))))
  (get-many-from [_ keys]
    (if (empty? keys)
      []
      (into {}
            (mapcat (fn [keys]
                      (map (fn [key value] (when (some? value) [key value]))
                           keys
                           (wcar (:connection-opts redis)
                                 (apply car/mget (map #(str name "_" %) keys)))))
                    (partition 5000 5000 nil keys)))))
  (put-to [_ key value]
    (let [[ttl timeunit] ttl]
      (wcar (:connection-opts redis)
            (car/set (str name "_" key) value :px (.toMillis timeunit ttl)))))
  (clear-all [_]
    (loop [[cursor keys] (wcar (:connection-opts redis)
                               (car/scan 0 :match (str name "_*")))]
      (wcar (:connection-opts redis)
            (mapv car/del keys))
      (when (not= "0" cursor)
        (recur (wcar (:connection-opts redis)
                     (car/scan cursor :match (str name "_*"))))))))

(defn- ->cache-key
  [name key]
  (str "ataru:cache:item:" name ":" key))

(defn- cache-key->key
  [name cache-key]
  (clojure.string/replace-first
   cache-key
   (str "ataru:cache:item:" name ":")
   ""))

(defn- ->lock-key
  [name lock-name]
  (str "ataru:cache:lock:" name ":" lock-name))

(defn- ->to-update-key
  [name]
  (str "ataru:cache:to-update:" name))

(defn- ->mark-all-keys-to-update-queue-name
  [name]
  (str "mark-all-keys-to-update-queue:" name))

(defn- car-set
  [name key value ttl]
  (let [[ttl timeunit] ttl]
    (car/set (->cache-key name key) value :px (.toMillis timeunit ttl))))

(defn- car-mset
  [name m ttl]
  (doseq [[key value] m]
    (car-set name key value ttl)))

(defn- car-mark-to-update
  [name keys]
  (apply car/sadd (->to-update-key name) keys))

(defn- redis-set
  [redis name key value ttl]
  (when (some? value)
    (wcar (:connection-opts redis)
          (car-set name key value ttl)))
  value)

(defn- redis-get
  [redis name key ttl-after-read update-after-read?]
  (let [value (wcar (:connection-opts redis)
                    (car/get (->cache-key name key)))]
    (when (some? value)
      (wcar (:connection-opts redis)
            (when (some? ttl-after-read)
              (car-set name key value ttl-after-read))
            (when update-after-read?
              (car-mark-to-update name [key]))))
    value))

(defn- redis-scan
  [redis name cursor]
  (wcar (:connection-opts redis)
        (car/scan cursor :match (->cache-key name "*"))))

(defn- redis-mset
  [redis name m ttl]
  (doseq [chunk (partition 5000 5000 nil m)]
    (wcar (:connection-opts redis)
          (car-mset name chunk ttl)))
  m)

(defn- redis-mget
  [redis name keys ttl-after-read update-after-read?]
  (reduce (fn [acc keys]
            (let [from-cache (map vector
                                  keys
                                  (wcar (:connection-opts redis)
                                        (apply car/mget (map #(->cache-key name %) keys))))
                  hits       (filter #(some? (second %)) from-cache)]
              (when (not-empty hits)
                (wcar (:connection-opts redis)
                      (when (some? ttl-after-read)
                        (car-mset name hits ttl-after-read))
                      (when update-after-read?
                        (car-mark-to-update name (map first hits)))))
              (-> acc
                  (update :hits into hits)
                  (update :misses concat (keep #(when (nil? (second %)) (first %)) from-cache)))))
          {:hits   {}
           :misses []}
          (partition 5000 5000 nil keys)))

(defn- redis-with-lock
  [redis name lock-name timeout-ms wait-ms thunk]
  (let [l-name (->lock-key name lock-name)
        result (carlocks/with-lock
                 (:connection-opts redis)
                 l-name
                 timeout-ms wait-ms
                 (thunk))]
    (if (some? result)
      (:result result)
      (do (warn (str "Failed to acquire lock " l-name " in " wait-ms " ms"))
          (thunk)))))

(defn- update-to-update-keys [redis loader name]
  (info "Updating" name "keys")
  (loop [updated-count 0]
    (if-let [keys (seq (wcar (:connection-opts redis)
                             (car/spop (->to-update-key name) 100)))]
      (let [ttls      (wcar (:connection-opts redis) :as-pipeline
                            (doseq [key keys]
                              (car/pttl (->cache-key name key))))
            to-update (filter #(< 0 (second %)) (map vector keys ttls))
            values    (cache/load-many loader (map first to-update))]
        (wcar (:connection-opts redis)
              (doseq [[key ttl] to-update
                      :when     (contains? values key)]
                (car/set (->cache-key name key) (get values key) :px ttl)))
        (recur (+ updated-count (count values))))
      (info "Updated" updated-count name "keys"))))

(defn- mark-all-keys-to-update [redis name]
  (loop [[cursor keys] (redis-scan redis name 0)]
    (when (not-empty keys)
      (wcar (:connection-opts redis)
            (car-mark-to-update name (map #(cache-key->key name %) keys))))
    (when (not= "0" cursor)
      (recur (redis-scan redis name cursor)))))

(defn- update-to-update-keys-runnable [redis loader name update-period]
  (fn []
    (try
      (when (some? update-period)
        (wcar (:connection-opts redis)
              (car-mq/enqueue (->mark-all-keys-to-update-queue-name name) name name)))
      (update-to-update-keys redis loader name)
      (catch Exception e
        (error e "Error while updating" name "keys")))))

(defn- mark-all-keys-to-update-handler [redis name [update-period time-unit]]
  (fn [{:keys [attempt]}]
    (try
      (info "Marking all" name "keys to update")
      (mark-all-keys-to-update redis name)
      {:status     :success
       :backoff-ms (.toMillis time-unit update-period)}
      (catch Exception e
        (if (< attempt 2)
          (do (warn "Error while marking all" name "keys to update, retrying")
              {:status :retry})
          (do (error e "Error while marking all" name "keys to update")
              {:status :success}))))))

(defrecord Cache [redis
                  loader
                  name
                  ttl-after-read
                  ttl-after-write
                  update-after-read?
                  update-period
                  scheduler
                  mq-worker]
  component/Lifecycle

  (start [this]
    (if (nil? scheduler)
      (let [scheduler (ScheduledThreadPoolExecutor. 1)
            mq-worker (when (some? update-period)
                          (car-mq/worker
                           (:connection-opts redis)
                           (->mark-all-keys-to-update-queue-name name)
                           {:handler        (mark-all-keys-to-update-handler redis name update-period)
                            :lock-ms        (.toMillis TimeUnit/MINUTES 2)
                            :eoq-backoff-ms (.toMillis TimeUnit/SECONDS 10)
                            :throttle-ms    (.toMillis TimeUnit/SECONDS 10)}))]
        (.scheduleAtFixedRate
         scheduler
         (update-to-update-keys-runnable redis loader name update-period)
         10 10 TimeUnit/SECONDS)
        (assoc this
               :scheduler scheduler
               :mq-worker mq-worker))
      this))
  (stop [this]
    (when (some? scheduler)
      (.shutdown scheduler))
    (when (some? mq-worker)
      (car-mq/stop mq-worker))
    (assoc this
           :scheduler nil
           :mq-worker nil))

  cache/Cache

  (get-from [this key]
    (try
      (let [from-cache (redis-get redis name key ttl-after-read update-after-read?)]
        (if (some? from-cache)
          from-cache
          (redis-with-lock
           redis name (str "single:" key)
           (.toMillis TimeUnit/MINUTES 2) (.toMillis TimeUnit/MINUTES 2)
           (fn []
             (let [from-cache (redis-get redis name key ttl-after-read update-after-read?)]
               (if (some? from-cache)
                 from-cache
                 (redis-set redis name key (cache/load loader key) ttl-after-write)))))))
      (catch Exception e
        (error e "Error while get from Redis")
        (cache/load loader key))))

  (get-many-from [this keys]
    (try
      (let [{:keys [hits misses]} (redis-mget redis name keys ttl-after-read update-after-read?)]
        (if (empty? misses)
          hits
          (merge hits
                 (redis-with-lock
                  redis name "many"
                  (.toMillis TimeUnit/MINUTES 2) (.toMillis TimeUnit/MINUTES 2)
                  (fn []
                    (let [{:keys [hits misses]} (redis-mget redis name keys ttl-after-read update-after-read?)]
                      (if (empty? misses)
                        hits
                        (merge hits (redis-mset redis name (cache/load-many loader misses) ttl-after-write)))))))))
      (catch Exception e
        (error e "Error while get-many from Redis")
        (cache/load-many loader keys))))

  (put-to [_ key value]
    (redis-set redis name key value ttl-after-write))

  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (->cache-key name key))))

  (clear-all [_]
    (loop [[cursor keys] (redis-scan redis name 0)]
      (wcar (:connection-opts redis)
            (doseq [key keys] (car/del key)))
      (when (not= "0" cursor)
        (recur (redis-scan redis name cursor))))))
