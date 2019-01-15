(ns ataru.aws.auth
  (:require [com.stuartsierra.component :as component])
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           java.io.Closeable))

(defrecord CredentialsProvider [credentials-provider]
  component/Lifecycle
  (start [this]
    (if (nil? credentials-provider)
      (assoc this :credentials-provider (DefaultAWSCredentialsProviderChain/getInstance))
      this))
  (stop [this]
    (when (and (some? credentials-provider)
               (instance? Closeable credentials-provider))
      (.close credentials-provider))
    (assoc this :credentials-provider nil)))
