(ns ataru.cas.client
  (:require
   [aleph.http :as http]
   [clj-util.cas :as cas]
   [oph.soresu.common.config :refer [config]]))

(defn new-client [cas-uri]
  {:pre [(some? (:cas config))]}
  (let [username           (get-in config [:cas :username])
        password           (get-in config [:cas :password])
        cas-url            (get-in config [:authentication :cas-client-url])
        cas-params         (cas/cas-params cas-uri username password)
        cas-client         (cas/cas-client cas-url)]
    {:client cas-client
     :params cas-params
     :session-id (atom nil)}))

(defn- cas-http [client method url]
  (let [cas-client     (:client client)
        cas-params     (:params client)
        cas-session-id (:session-id client)
        http-fn        (case method
                         :get http/get)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params))))
    (let [params {:headers          {"Cookie" (str "JSESSIONID=" @cas-session-id)}
                  :follow-redirects false}
          resp   @(http-fn url params)]
      (if (= 302 (:status resp))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params)))
          @(http-fn url params))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url))
