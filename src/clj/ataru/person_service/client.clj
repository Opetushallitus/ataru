(ns ataru.person-service.client
  (:require [com.stuartsierra.component :as component]))

(defrecord PersonServiceClient []
  component/Lifecycle

  (start [this]
    this)

  (stop [this]
    this))

(defn new-client []
  (->PersonServiceClient))
