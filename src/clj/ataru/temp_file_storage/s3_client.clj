(ns ataru.temp-file-storage.s3-client
  (:require [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]])
  (:import [com.amazonaws.services.s3 AmazonS3Client]
           [com.amazonaws.regions Regions]))

(defrecord S3Client [credentials-provider s3-client]
  component/Lifecycle

  (start [this]
    (if (nil? s3-client)
      (assoc this :s3-client (-> (AmazonS3Client/builder)
                                 (.withRegion (Regions/fromName (get-in config [:aws :region] "eu-west-1")))
                                 (.withCredentials
                                  (:credentials-provider credentials-provider))
                                 (.build)))
      this))

  (stop [this]
    (when-let [client (:s3-client this)]
      (.shutdown client))
    (assoc this :s3-client nil)))

(defn new-client []
  (map->S3Client {}))
