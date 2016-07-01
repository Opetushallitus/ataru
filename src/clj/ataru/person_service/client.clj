(ns ataru.person-service.client
  (:require [clj-util.cas :as cas]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defrecord PersonServiceClient []
  component/Lifecycle

  (start [this]
    (let [person-service-url (str (get-in config [:person-service :person-service-url]) "/authentication-service")
          username           (get-in config [:cas :username])
          password           (get-in config [:cas :password])
          cas-url            (get-in config [:authentication :cas-client-url])
          cas-params         (cas/cas-params person-service-url username password)
          cas-client         (cas/cas-client cas-url)]
      (-> this
          (assoc :cas-client cas-client)
          (assoc :cas-params cas-params))))

  (stop [this]
    (-> this
        (assoc :cas-client nil)
        (assoc :cas-params nil))))

(defn new-client []
  (->PersonServiceClient))
