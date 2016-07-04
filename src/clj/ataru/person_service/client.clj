(ns ataru.person-service.client
  (:require [aleph.http :as http]
            [cheshire.core :as json]
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
          session-id (:cas-session-id client)]
      (when
        (nil? @session-id)
        (reset! session-id (.run (.fetchCasSession cas-client cas-params))))
      (let [url    (str (get-in config [:person-service :url]) "/authentication-service/resources/henkilo")
            params {:query-params {"q" username}
                    :headers {"Cookie" (str "JSESSIONID=" @session-id)}
                    :follow-redirects false}
            resp    @(http/get url params)]
        (if
          (= 302 (:status resp))
          (do
            (reset! session-id (.run (.fetchCasSession cas-client cas-params)))
            (-> @(http/get url params) :body slurp (json/parse-string true)))
          (-> resp :body slurp (json/parse-string true))))))

  (start [this]
    (assoc this :cas-session-id (atom nil)))

  (stop [this]
    (assoc this :cas-session-id nil)))

(defn new-client []
  (->PersonServiceClient))
