(ns ataru.temp-file-storage.s3-client
  (:require [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]
           [com.amazonaws.auth InstanceProfileCredentialsProvider]
           [com.amazonaws.auth.profile ProfileCredentialsProvider]))

(defn- credentials-provider []
  (if-let [profile-name (get-in config [:temp-files :s3 :credentials-profile])]
    [(new ProfileCredentialsProvider profile-name)
     false]
    [(new InstanceProfileCredentialsProvider true)
     true]))

(defn- region []
  (Regions/fromName (get-in config [:temp-files :s3 :region])))

(defrecord S3Client []
  component/Lifecycle

  (start [this]
    (let [[credentials-provider closable] (credentials-provider)
          client (-> (AmazonS3Client/builder)
                     (.withRegion (region))
                     (.withCredentials credentials-provider)
                     (.build))]
      (-> this
          (assoc :s3-client client)
          (assoc :credentials-provider [credentials-provider closable]))))

  (stop [this]
    (when-let [client (:s3-client this)]
      (.shutdown client))
    (when-let [[credentials-provider closable] (:credentials-provider this)]
      (when closable
        (.close credentials-provider)))
    (-> this
        (assoc :s3-client nil)
        (assoc :credentials-provider nil))))

(defn new-client []
  (map->S3Client {}))
