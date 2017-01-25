(ns ataru.cache.cache-service
  (:require [taoensso.timbre :refer [info warn]]
            [com.stuartsierra.component :as component])
  (:import (com.hazelcast.core Hazelcast HazelcastInstance)
           (com.hazelcast.config Config MapConfig ClasspathXmlConfig)))

(def default-map-config {:ttl      600
                         :max-size 500})

(def cached-map-config {:hakukohde {:config {:max-size 1000}}
                        :haku      {:config {:max-size 1000}}})

(defn- build-config
  []
  (let [configuration (ClasspathXmlConfig. "hazelcast-default.xml")]
    (-> configuration
        (.getGroupConfig)
        (.setName "ataru-hz-group"))
    (doseq [[name-kw {:keys [config]}] cached-map-config]
      (let [mc (MapConfig.)]
        (.setName mc (name name-kw))
        (-> mc
            (.getMaxSizeConfig)
            (.setSize (or (:max-size config)
                          (:max-size default-map-config))))
        (.setTimeToLiveSeconds mc (or (:ttl config)
                                      (:ttl default-map-config)))
        (.addMapConfig configuration mc)))))

(defprotocol CacheService
  (cache-get [this cache key]
    "Get cached item or return nil if not found.
    e.g. (cache-get :hakukohde objectid-of-hakukohde")
  (cache-put [this cache key value]
    "Store item in cache
    e.g. (cache-put :hakukohde objectid-of-hakukohde {...}")
  (cache-get-or-fetch [this cache key get-fn]
    "Get cached item or invoke get-fn to store & return
    e.g. (cache-get-or-fetch :hakukohde #(hakukohde-client/get-hakukohde objectid-of-hakukohde)"))

(defn- get-cached-map [hazelcast-instance cache]
  "Only allow access to preconfigured maps"
  (when (cache cached-map-config)
    (.getMap hazelcast-instance (name cache))))

(defrecord HazelcastCacheService []
  component/Lifecycle
  CacheService

  (start [this]
    (info "Initializing Hazelcast caching")
    (Hazelcast/newHazelcastInstance (build-config)))

  (stop [this]
    (info "Shutting down Hazelcast")
    (.shutdown this)
    nil)

  (cache-get [this cache key]
    (.get (get-cached-map this cache) key))

  (cache-put [this cache key value]
    (.put (get-cached-map this cache) key value))

  (cache-get-or-fetch [this cache key fetch-fn]
    (if-let [value (cache-get this cache key)]
      value
      (if-let [new-value (fetch-fn)]
        (do
          (cache-put this cache key new-value)
          new-value)
        (warn "Could not fetch value for cache" cache key)))))

(defn new-cache-service
  []
  (->HazelcastCacheService))
