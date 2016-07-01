(ns ataru.person-service.client
  (:require [clj-util.cas :as cas]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defprotocol PersonService
  (resolve-person-oids [client username]))

(defrecord PersonServiceClient []
  PersonService

  (resolve-person-oids [client username]
    {:totalCount 2
     :results [{:oidHenkilo "1.2.246.562.24.00000000001"}
               {:oidHenkilo "1.2.246.562.24.00000000002"}]}))

(defn new-client []
  (->PersonServiceClient))
