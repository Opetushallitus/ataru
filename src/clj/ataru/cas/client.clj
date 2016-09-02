(ns ataru.cas.client
  (:require
   [aleph.http :as http]
   [clj-util.cas :as cas]
   [oph.soresu.common.config :refer [config]]))

(defn new-client []
  (let [person-service-url (str (get-in config [:person-service :url]) "/authentication-service")
        username           (get-in config [:cas :username])
        password           (get-in config [:cas :password])
        cas-url            (get-in config [:authentication :cas-client-url])
        cas-params         (cas/cas-params person-service-url username password)
        cas-client         (cas/cas-client cas-url)]
    {:client cas-client
     :params cas-params
     :session-id (atom nil)}))

(defn cas-authenticated-get [client url]
  (let [cas-client (:client client)
        cas-params (:params client)
        cas-session-id (:session-id client)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params))))
    (let [params {:headers {"Cookie" (str "JSESSIONID=" @cas-session-id)}
                  :follow-redirects false}
            resp  @(http/get url params)]
      (if (= 302 (:status resp))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params)))
          @(http/get url params))
        resp))))
