(ns ataru.cache.hazelcast
  (:require [taoensso.timbre :refer [info]]
            [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]])
  (:import java.net.InetAddress
           com.hazelcast.core.Hazelcast
           [com.hazelcast.config MapConfig ClasspathXmlConfig]))

(defprotocol Configurator
  (configure [this configuration]))

(def local-cluster-cfg {:clustered? true
                        :hosts      ["127.0.0.1"]})

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
  [{:keys [clustered? hosts cluster-name]} configurators]
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

    (doseq [c configurators]
      (configure c configuration))

    configuration))

(defrecord HazelcastInstance [configurators instance]
  component/Lifecycle
  (start [this]
    (let [cluster-config (build-cluster-config)]
      (info "Starting Hazelcast" cluster-config)
      (assoc this :instance (Hazelcast/newHazelcastInstance (build-config cluster-config configurators)))))

  (stop [this]
    (when (some? instance)
      (info "Stopping Hazelcast")
      (.shutdown instance)
      (assoc this :instance nil))))
