(ns ataru.temp-file-storage.s3-client
  (:require [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [com.amazonaws.auth InstanceProfileCredentialsProvider DefaultAWSCredentialsProviderChain]
           (java.io Closeable)))

(defrecord S3Client []
  component/Lifecycle

  (start [this]
    (let [credentials-provider (DefaultAWSCredentialsProviderChain/getInstance)
          client               (-> (AmazonS3Client/builder)
                                   (.withRegion (Regions/fromName (get-in config [:aws :region] "eu-west-1")))
                                   (.withCredentials credentials-provider)
                                   (.build))]
      (-> this
          (assoc :s3-client client)
          (assoc :credentials-provider credentials-provider))))

  (stop [this]
    (when-let [client (:s3-client this)]
      (.shutdown client))
    (when-let [credentials-provider (:credentials-provider this)]
      (when (instance? Closeable credentials-provider)
        (.close credentials-provider)))
    (-> this
        (assoc :s3-client nil)
        (assoc :credentials-provider nil))))

(defn new-client []
  (map->S3Client {}))
