(ns ataru.temp-file-storage.s3-client
  (:require [com.stuartsierra.component :as component]
            [ataru.config.core :refer [config]])
  (:import software.amazon.awssdk.services.s3.S3Client
           software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
           software.amazon.awssdk.regions.Region))

(defrecord AmazonS3Client [credentials-provider s3-client]
  component/Lifecycle

  (start [this]
    (if (nil? s3-client)
      (assoc this :s3-client (-> (S3Client/builder)
                                 (.region (Region/of (get-in config [:aws :region] "eu-west-1")))
                                 (.credentialsProvider ^AwsCredentialsProvider (:credentials-provider credentials-provider))
                                 (.build)))
      this))

  (stop [this]
    (when-let [^S3Client client (:s3-client this)]
      (.close client))
    (assoc this :s3-client nil)))

(defn new-client []
  (map->AmazonS3Client {}))
