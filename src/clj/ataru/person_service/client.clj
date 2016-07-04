(ns ataru.person-service.client
  (:require [aleph.http :as http]
            [clj-util.cas :as cas]
            [com.stuartsierra.component :as component]
            [oph.soresu.common.config :refer [config]]))

(defprotocol PersonService
  (resolve-person-oids [client username]))

(defrecord PersonServiceClient []
  component/Lifecycle
  PersonService

  (resolve-person-oids [client username]
    (let [cas-client (get-in client [:cas-client :client])
          cas-params (get-in client [:cas-client :params])
          url        (str (get-in config [:person-service :url]) "/authentication-service/resources/henkilo")
          session-id (.run (.fetchCasSession cas-client cas-params))]
      @(http/get url {:query-params {"q" username}
                      :headers {"Cookie" (str "JSESSIONID=" session-id)}
                      :follow-redirects false})))

  (start [this]
    (assoc this :cas-session-id (atom nil)))

  (stop [this]
    (assoc this :cas-session-id nil)))

(defn new-client []
  (->PersonServiceClient))
