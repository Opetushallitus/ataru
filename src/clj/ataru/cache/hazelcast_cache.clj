(ns ataru.cache.hazelcast-cache
  (:require [taoensso.timbre :refer [warn]]
            [com.stuartsierra.component :as component]
            [ataru.cache.cache-service :refer [Cache get-from put-to]]
            [ataru.cache.hazelcast :refer [Configurator]])
  (:import com.hazelcast.config.MapConfig))

(defn- with-lock [m key f]
  (.lock m key)
  (try (f) (finally (.unlock m key))))

(defrecord BasicCache [hazelcast map name max-size ttl]
  component/Lifecycle
  (start [this]
    (if (nil? map)
      (assoc this :map (.getMap (:instance hazelcast) name))
      this))
  (stop [this]
    (assoc this :map nil))

  Configurator
  (configure [_ configuration]
    (let [mc (MapConfig.)]
      (.setName mc name)
      (.setSize (.getMaxSizeConfig mc) max-size)
      (.setTimeToLiveSeconds mc ttl)
      (.addMapConfig configuration mc)))

  Cache
  (get-from [_ key]
    (.get map key))
  (put-to [_ key value]
    (.put map key value))
  (get-from [this key get-fn]
    (if-let [value (get-from this key)]
      value
      (with-lock map key
        (fn []
          (if-let [value (get-from this key)]
            value
            (if-let [new-value (get-fn)]
              (do (put-to this key new-value)
                  new-value)
              (warn "Could not fetch value for cache" name key)))))))
  (remove-from [_ key]
    (.evict map key))
  (clear-all [_]
    (.evictAll map)))
