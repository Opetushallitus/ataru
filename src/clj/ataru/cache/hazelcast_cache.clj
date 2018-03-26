(ns ataru.cache.hazelcast-cache
  (:require [taoensso.timbre :refer [warn]]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :refer [Cache get-from put-to]]
            [ataru.cache.hazelcast :refer [Configurator]])
  (:import java.util.concurrent.ScheduledThreadPoolExecutor
           com.hazelcast.config.MapConfig
           com.hazelcast.map.listener.EntryEvictedListener))

(defn- with-lock [m key f]
  (.lock m key)
  (try (f) (finally (.unlock m key))))

(defrecord BasicCache [hazelcast hmap name max-size ttl]
  component/Lifecycle
  (start [this]
    (if (nil? hmap)
      (assoc this :hmap (.getMap (:instance hazelcast) name))
      this))
  (stop [this]
    (assoc this :hmap nil))

  Configurator
  (configure [_ configuration]
    (let [mc (MapConfig.)]
      (.setName mc name)
      (.setSize (.getMaxSizeConfig mc) max-size)
      (.setTimeToLiveSeconds mc ttl)
      (.addMapConfig configuration mc)))

  Cache
  (get-from [_ key]
    (.get hmap key))
  (get-many-from [_ keys]
    (map #(.get hmap %) keys))
  (put-to [_ key value]
    (.put hmap key value))
  (get-from [this key get-fn]
    (if-let [value (get-from this key)]
      value
      (with-lock hmap key
        (fn []
          (if-let [value (get-from this key)]
            value
            (if-let [new-value (get-fn)]
              (do (put-to this key new-value)
                  new-value)
              (warn "Could not fetch value for cache" name key)))))))
  (get-many-from [this keys get-fn]
    (map #(get-from this % get-fn) keys))
  (remove-from [_ key]
    (.evict hmap key))
  (clear-all [_]
    (.evictAll hmap)))

(deftype EvictionSynchonizer [other]
  EntryEvictedListener
  (entryEvicted [_ event]
    (.evict other (.getKey event))))

(defrecord UpdatingCache [hazelcast
                          updated-map
                          stable-map
                          scheduler
                          get-fns
                          name
                          max-size
                          ttl
                          max-idle
                          period]
  component/Lifecycle
  (start [this]
    (if (nil? scheduler)
      (let [updated-map (.getMap (:instance hazelcast) (str "updated-" name))
            stable-map (.getMap (:instance hazelcast) (str "stable-" name))
            scheduler (ScheduledThreadPoolExecutor. 1)
            get-fns (atom {})
            [period timeunit] period]
        (.addEntryListener updated-map (EvictionSynchonizer. stable-map) false)
        (.addEntryListener stable-map (EvictionSynchonizer. updated-map) false)
        (.scheduleAtFixedRate
         scheduler
         (fn [] (doseq [key (.keySet stable-map)]
                  (when-let [get-fn (get @get-fns key)]
                    (if-let [new-value (get-fn)]
                      (.replace updated-map key new-value)
                      (warn "Could not update value for cache" name key)))))
         period period timeunit)
        (-> this
            (assoc :updated-map updated-map)
            (assoc :stable-map stable-map)
            (assoc :scheduler scheduler)
            (assoc :get-fns get-fns)))
      this))
  (stop [this]
    (when (some? scheduler)
      (.shutdown scheduler))
    (-> this
        (assoc :updated-map nil)
        (assoc :stable-map nil)
        (assoc :scheduler nil)
        (assoc :get-fns nil)))

  Configurator
  (configure [_ configuration]
    (let [umc (MapConfig.)
          smc (MapConfig.)
          [ttl-duration ttl-timeunit] ttl
          [max-idle-duration max-idle-timeunit] max-idle]
      (.setName umc (str "updated-" name))
      (.setName smc (str "stable-" name))
      (when (some? max-size)
        (.setSize (.getMaxSizeConfig umc) max-size))
      (when (some? ttl)
        (.setTimeToLiveSeconds smc (.toSeconds ttl-timeunit ttl-duration)))
      (when (some? max-idle)
        (.setMaxIdleSeconds smc (.toSeconds max-idle-timeunit max-idle-duration)))
      (.addMapConfig configuration umc)
      (.addMapConfig configuration smc)))

  Cache
  (get-from [_ key]
    (when (.containsKey stable-map key)
      (.get updated-map key)))
  (get-many-from [this keys]
    (map #(get-from this %) keys))
  (put-to [_ key value]
    (.put updated-map key value)
    (.put stable-map key key))
  (get-from [this key get-fn]
    (if-let [value (get-from this key)]
      value
      (with-lock stable-map key
        (fn []
          (if-let [value (get-from this key)]
            value
            (if-let [new-value (get-fn)]
              (do (swap! get-fns assoc key get-fn)
                  (put-to this key new-value)
                  new-value)
              (warn "Could not fetch value for cache" name key)))))))
  (get-many-from [this keys get-fn]
    (map #(get-from this % get-fn) keys))
  (remove-from [_ key]
    (.evict stable-map key))
  (clear-all [_]
    (.evictAll stable-map)))
