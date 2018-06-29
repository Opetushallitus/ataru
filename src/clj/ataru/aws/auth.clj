(ns ataru.aws.auth
  (:require [ataru.config.core :refer [config]]
            [com.stuartsierra.component :as component])
  (:import [com.amazonaws.regions Regions]
           [com.amazonaws.auth InstanceProfileCredentialsProvider]
           [com.amazonaws.auth.profile ProfileCredentialsProvider]))

(defrecord CredentialsProvider [credentials-provider closable?]
  component/Lifecycle
  (start [this]
    (if (nil? credentials-provider)
      (if-let [profile-name (:credentials-profile (:aws config))]
        (-> this
            (assoc :credentials-provider (new ProfileCredentialsProvider
                                              profile-name))
            (assoc :closable? false))
        (-> this
            (assoc :credentials-provider (new InstanceProfileCredentialsProvider
                                              true))
            (assoc :closable? true)))
      this))
  (stop [this]
    (when (and (some? credentials-provider) closable?)
      (.close credentials-provider))
    (-> this
        (assoc :credentials-provider nil)
        (assoc :closable? nil))))
