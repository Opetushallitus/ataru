(ns ataru.cache.cache-service
  (:require [taoensso.timbre :refer [info warn]]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]])
  (:import (com.hazelcast.core Hazelcast HazelcastInstance)
           (com.hazelcast.config Config MapConfig ClasspathXmlConfig)
           (java.net InetAddress)))

(def default-map-config {:ttl      600
                         :max-size 500})

(def cached-map-config {:hakukohde {:config {:max-size 1000 :ttl 3600}}
                        :haku      {:config {:max-size 1000 :ttl 3600}}
                        :koulutus  {:config {:max-size 1000 :ttl 3600}}})

(def local-cluster-cfg {:clustered? true
                        :hosts ["127.0.0.1"]})

(def cluster-config {:default {:clustered? false}
                     :dev     local-cluster-cfg
                     :luokka  local-cluster-cfg
                     :qa      local-cluster-cfg
                     :prod    {:clustered? true :hosts ["10.27.54.12" "10.27.54.23" "10.27.54.24" "10.27.54.25"]}})

(defn- build-cluster-config
  []
  (let [environment-name    (-> config :public-config :environment-name)
        cluster-name-suffix (case environment-name
                              nil nil
                              "test" nil
                              "dev" (str "dev-" (.getCanonicalHostName (InetAddress/getLocalHost)))
                              ; else: "luokka", "qa", "prod"
                              environment-name)]
    (merge ((keyword environment-name) cluster-config)
           (when cluster-name-suffix
             {:cluster-name (str "ataru-hz-" cluster-name-suffix)}))))

(defn- build-config
  [{:keys [clustered? hosts cluster-name]}]
  (let [configuration (ClasspathXmlConfig. "hazelcast-default.xml")]
    (.setEnabled (-> configuration .getNetworkConfig .getJoin .getMulticastConfig) false)
    (when (and clustered? cluster-name)
      (-> configuration
          (.getGroupConfig)
          (.setName cluster-name))
      (let [network-cfg (.getNetworkConfig configuration)
            join-cfg    (.getJoin network-cfg)
            tcp-config  (.getTcpIpConfig join-cfg)
            interfaces  (.getInterfaces network-cfg)]
        (.setEnabled (.getMulticastConfig join-cfg) false)
        (doseq [host hosts]
          (.addMember tcp-config host)
          (.addInterface interfaces host))
        (.setEnabled tcp-config true)
        (.setEnabled interfaces true)))

    (doseq [[name-kw {:keys [config]}] cached-map-config]
      (let [mc (MapConfig.)]
        (.setName mc (name name-kw))
        (-> mc
            (.getMaxSizeConfig)
            (.setSize (or (:max-size config)
                          (:max-size default-map-config))))
        (.setTimeToLiveSeconds mc (or (:ttl config)
                                      (:ttl default-map-config)))
        (.addMapConfig configuration mc)))

    configuration))

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

(defn- get-cached-map [component cache]
  "Only allow access to preconfigured maps"
  (when (cache cached-map-config)
    (.getMap (:hazelcast-instance component) (name cache))))

(defrecord HazelcastCacheService [hazelcast-instance]
  component/Lifecycle
  CacheService

  (start [component]
    (let [cluster-config (build-cluster-config)]
      (info "Initializing Hazelcast caching, cluster" (-> config :public-config :environment-name) cluster-config)
      (assoc component :hazelcast-instance (Hazelcast/newHazelcastInstance (build-config cluster-config)))))

  (stop [component]
    (info "Shutting down Hazelcast")
    (.shutdown hazelcast-instance)
    (assoc component :hazelcast-instance nil))

  (cache-get [component cache key]
    (.get (get-cached-map component cache) key))

  (cache-put [component cache key value]
    "Returns old value"
    (.put (get-cached-map component cache) key value))

  (cache-get-or-fetch [component cache key fetch-fn]
    (if-let [value (cache-get component cache key)]
      value
      (if-let [new-value (fetch-fn)]
        (do
          (cache-put component cache key new-value)
          new-value)
        (warn "Could not fetch value for cache" cache key)))))

(defn new-cache-service
  []
  (map->HazelcastCacheService {}))
