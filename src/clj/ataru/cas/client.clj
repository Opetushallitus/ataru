(ns ataru.cas.client
  (:require [clj-util.cas :as cas]
            [oph.soresu.common.config :refer [config]]))

(defn new-client []
  (let [person-service-url (str (get-in config [:person-service :person-service-url]) "/authentication-service")
        username           (get-in config [:cas :username])
        password           (get-in config [:cas :password])
        cas-url            (get-in config [:authentication :cas-client-url])
        cas-params         (cas/cas-params person-service-url username password)
        cas-client         (cas/cas-client cas-url)]
    {:client cas-client
     :params cas-params}))
