(ns ataru.person-service.client
  (:require [clj-util.cas :as cas]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defprotocol PersonService
  (resolve-person-oids [client username]))

(defrecord PersonServiceClient []
  PersonService

  (resolve-person-oids [client username]
    (let [cas-client (get-in client [:cas-client :client])
          cas-params (get-in client [:cas-client :params])]
      (.run (.fetchCasSession cas-client cas-params)))))

(defn new-client []
  (->PersonServiceClient))
