(ns ataru.cache.redis-cache
  (:require [taoensso.timbre :refer [info warn error]]
            [com.stuartsierra.component :as component]
            [taoensso.carmine :as car :refer [wcar]]
            [taoensso.carmine.locks :as carlocks]
            [ataru.cache.cache-service :as cache])
  (:import java.util.concurrent.ScheduledThreadPoolExecutor))

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
      (map (fn [key value] (if (some? value) value (cache/get-from this key)))
           keys
           (wcar (:connection-opts redis)
                 (apply car/mget (map #(str name "_" %) keys))))))
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
  [redis cache-name ttl]
  cache/MappedCache
  (get-from-or-fetch [this fetch-fn key]
    (when key
      (if-let [value (wcar (:connection-opts redis) (car/get (str cache-name "_" key)))]
        value
        (:result
          (carlocks/with-lock (:connection-opts redis) (str cache-name "-fetch-lock") 10000 5000
                              (if-let [value (wcar (:connection-opts redis) (car/get (str cache-name "_" key)))]
                                value
                                (when-let [new-value (fetch-fn key)]
                                  (cache/put-to this key new-value)
                                  new-value)))))))
  (put-many-to [_ key-values]
    (when (not-empty key-values)
      (wcar (:connection-opts redis)
            (apply car/mset (flatten (map (fn [[k v]] [(str cache-name "_" (name k)) v]) key-values))))))

  cache/Cache
  (get-from [_ key]
    (throw (RuntimeException. "Not implemented")))
  (remove-from [_ key]
    (wcar (:connection-opts redis)
          (car/del (str cache-name "_" key))))
  (get-many-from [this keys]
    (if (empty? keys)
      []
      (into {}
            (map (fn [key value] (when (some? value) [key value]))
                 keys
                 (wcar (:connection-opts redis)
                       (apply car/mget (map #(str cache-name "_" %) keys)))))))
  (put-to [_ key value]
    (let [[ttl timeunit] ttl]
      (wcar (:connection-opts redis)
            (car/set (str cache-name "_" key) value :px (.toMillis timeunit ttl)))))
  (clear-all [_]
    (loop [[cursor keys] (wcar (:connection-opts redis)
                               (car/scan 0 :match (str cache-name "_*")))]
      (wcar (:connection-opts redis)
            (mapv car/del keys))
      (when (not= "0" cursor)
        (recur (wcar (:connection-opts redis)
                     (car/scan cursor :match (str cache-name "_*"))))))))