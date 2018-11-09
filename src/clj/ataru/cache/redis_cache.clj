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
      (when (< 0 updated-count)
        (info "Updated" updated-count name "keys")))))

(defn- mark-all-keys-to-update [redis name]
  (loop [[cursor keys] (redis-scan redis name 0)
         key-count     0]
    (when (not-empty keys)
      (wcar (:connection-opts redis)
            (car-mark-to-update name (map #(cache-key->key name %) keys))))
    (if (= "0" cursor)
      (when (< 0 (+ key-count (count keys)))
        (info "Marked" (+ key-count (count keys)) name "keys to update"))
      (recur (redis-scan redis name cursor)
             (+ key-count (count keys))))))

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
