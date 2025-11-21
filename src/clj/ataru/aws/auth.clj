(ns ataru.aws.auth
  (:require [com.stuartsierra.component :as component])
  (:import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
           java.io.Closeable))

(defrecord CredentialsProvider [credentials-provider]
  component/Lifecycle
  (start [this]
    (if (nil? credentials-provider)
      (assoc this :credentials-provider (.build (DefaultCredentialsProvider/builder)))
      this))
  (stop [this]
    (when (and (some? credentials-provider)
               (instance? Closeable credentials-provider))
      (.close credentials-provider))
    (assoc this :credentials-provider nil)))
