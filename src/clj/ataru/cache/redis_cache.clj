(ns ataru.cache.redis-cache
  (:require [taoensso.timbre :refer [info warn error]]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.message-queue :as car-mq]
            [taoensso.carmine.locks :as carlocks]
            [ataru.cache.cache-service :as cache])
  (:import [java.util.concurrent
            ScheduledThreadPoolExecutor
            TimeUnit]
           java.util.concurrent.locks.ReentrantLock))

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

(defn- ->mark-all-keys-to-update-key
  [name]
  (str "ataru:cache:mark-all-keys-to-update:" name))

(defn- car-set
  [name key value ttl]
  (let [[ttl timeunit] ttl]
    (car/set (->cache-key name key) value :px (.toMillis timeunit ttl))))

(defn- car-mset
  [name m ttl]
  (doseq [[key value] m]
    (car-set name key value ttl)))

(defn- redis-set
  [redis name key value ttl]
  (when (some? value)
    (wcar (:connection-opts redis)
          (car-set name key value ttl)))
  value)

(defn- redis-get
  [redis name key ttl-after-read-ms update-after-read?]
  (let [cache-key (->cache-key name key)
        result    (wcar (:connection-opts redis) :as-pipeline
                        (car/get cache-key)
                        (when (some? ttl-after-read-ms)
                          (car/pexpire cache-key ttl-after-read-ms))
                        (when update-after-read?
                          (car/sadd (->to-update-key name) key)))]
    (first result)))

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
  [redis name keys ttl-after-read-ms update-after-read?]
  (reduce (fn [acc keys]
            (let [cache-keys (map #(->cache-key name %) keys)
                  result     (wcar (:connection-opts redis) :as-pipeline
                                   (apply car/mget cache-keys)
                                   (when (some? ttl-after-read-ms)
                                     (doseq [cache-key cache-keys]
                                       (car/pexpire cache-key ttl-after-read-ms)))
                                   (when update-after-read?
                                     (apply car/sadd (->to-update-key name) keys)))]
              (reduce (fn [acc [key value]]
                        (if (some? value)
                          (update acc :hits assoc key value)
                          (update acc :misses conj key)))
                      acc
                      (map vector keys (first result)))))
          {:hits   {}
           :misses []}
          (partition 5000 5000 nil keys)))

(defn- get-lock
  [locks lock-name]
  (get (if (contains? @locks lock-name)
         @locks
         (swap! locks #(if (contains? % lock-name)
                         %
                         (assoc % lock-name (new ReentrantLock)))))
       lock-name))

(defn- redis-with-lock
  [locks name lock-name wait-ms thunk]
  (let [l-name (->lock-key name lock-name)
        lock   (get-lock locks l-name)]
    (try
      (try
        (when-not (.tryLock lock wait-ms TimeUnit/MILLISECONDS)
          (warn (str "Failed to acquire lock " l-name " in " wait-ms " ms")))
        (catch Exception e
          (error e (str "Error while acquiring lock " l-name))))
      (thunk)
      (finally
        (.unlock lock)))))

(defn- update-to-update-keys [redis loader name]
  (loop [updated-keys #{}]
    (if-let [keys (seq (wcar (:connection-opts redis)
                             (car/spop (->to-update-key name) 10)))]
      (do (when-let [keys (seq (remove updated-keys keys))]
            (let [ttls      (wcar (:connection-opts redis) :as-pipeline
                                  (doseq [key keys]
                                    (car/pttl (->cache-key name key))))
                  to-update (filter #(pos? (second %)) (map vector keys ttls))
                  values    (cache/load-many loader (map first to-update))]
              (wcar (:connection-opts redis)
                    (doseq [[key ttl] to-update
                            :when     (contains? values key)]
                      (car/set (->cache-key name key) (get values key) :px ttl)))))
          (recur (into updated-keys keys)))
      (count updated-keys))))

(defn- mark-all-keys-to-update [redis name update-period-ms]
  (if (some? (wcar (:connection-opts redis)
                   (car/set (->mark-all-keys-to-update-key name) "mark" :px update-period-ms :nx)))
    (loop [[cursor keys] (redis-scan redis name 0)
           key-count     0]
      (when (not-empty keys)
        (wcar (:connection-opts redis)
              (apply car/sadd (->to-update-key name) (map #(cache-key->key name %) keys))))
      (if (= "0" cursor)
        (+ key-count (count keys))
        (recur (redis-scan redis name cursor)
               (+ key-count (count keys)))))
    0))

(defn- ->runnable [msg name duration-limit-ms thunk]
  (fn []
    (try
      (let [start-ms (System/currentTimeMillis)
            c        (thunk)
            duration (- (System/currentTimeMillis) start-ms)]
        (info (str "[" msg "]")
              (str "[" name "]")
              (str "[" c "]")
              (str "[" duration "]")
              (str "[" (or duration-limit-ms duration) "]")))
      (catch Throwable e
        (error e (str "[" msg "]") (str "[" name "]"))))))

(defrecord Cache [redis
                  loader
                  name
                  ttl-after-read
                  ttl-after-read-ms
                  ttl-after-write
                  update-after-read?
                  update-period
                  scheduler
                  mq-worker
                  locks]
  component/Lifecycle

  (start [this]
    (if (nil? scheduler)
      (let [update-period-ms (when-let [[t unit] update-period]
                               (.toMillis unit t))
            scheduler        (when (or update-after-read?
                                       (some? update-period-ms))
                               (ScheduledThreadPoolExecutor. 2))]
        (when (some? scheduler)
          (.scheduleAtFixedRate
           scheduler
           (->runnable "Update cache keys"
                       name
                       update-period-ms
                       (fn [] (update-to-update-keys redis loader name)))
           1 1 TimeUnit/SECONDS))
        (when (some? update-period-ms)
          (.scheduleAtFixedRate
           scheduler
           (->runnable "Mark cache keys to update"
                       name
                       1000
                       (fn [] (mark-all-keys-to-update redis name update-period-ms)))
           1 1 TimeUnit/SECONDS))
        (assoc this
               :ttl-after-read-ms (when-let [[ttl timeunit] ttl-after-read]
                                    (.toMillis timeunit ttl))
               :scheduler scheduler
               :mq-worker mq-worker
               :locks (atom {})))
      this))
  (stop [this]
    (when (some? scheduler)
      (.shutdown scheduler))
    (when (some? mq-worker)
      (car-mq/stop mq-worker))
    (assoc this
           :scheduler nil
           :mq-worker nil
           :locks nil))

  cache/Cache

  (get-from [this key]
    (try
      (let [from-cache (redis-get redis name key ttl-after-read-ms update-after-read?)]
        (if (some? from-cache)
          from-cache
          (redis-with-lock
           locks name (str "single:" key)
           (.toMillis TimeUnit/MINUTES 2)
           (fn []
             (let [from-cache (redis-get redis name key ttl-after-read-ms update-after-read?)]
               (if (some? from-cache)
                 from-cache
                 (redis-set redis name key (cache/load loader key) ttl-after-write)))))))
      (catch Exception e
        (error e "Error while get from Redis")
        (cache/load loader key))))

  (get-many-from [this keys]
    (try
      (let [{:keys [hits misses]} (redis-mget redis name keys ttl-after-read-ms update-after-read?)]
        (if (empty? misses)
          hits
          (merge hits
                 (redis-with-lock
                  locks name "many"
                  (.toMillis TimeUnit/MINUTES 2)
                  (fn []
                    (let [{:keys [hits misses]} (redis-mget redis name keys ttl-after-read-ms update-after-read?)]
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
