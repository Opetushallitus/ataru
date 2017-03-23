(ns ataru.cas.client
  (:require
    [org.httpkit.client :as http]
    [clj-util.cas :as cas]
    [ataru.config.url-helper :refer [resolve-url]]
    [oph.soresu.common.config :refer [config]]
    [cheshire.core :as json]))

(defn new-client [cas-uri]
  {:pre [(some? (:cas config))]}
  (let [username           (get-in config [:cas :username])
        password           (get-in config [:cas :password])
        cas-url            (resolve-url :cas-client)
        cas-params         (cas/cas-params cas-uri username password)
        cas-client         (cas/cas-client cas-url)]
    {:client cas-client
     :params cas-params
     :session-id (atom nil)}))

(defn- request-with-json-body [request body]
  (-> request
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :body (json/generate-string body))))

(defn- create-params [cas-session-id body]
  (cond-> {:headers          {"Cookie" (str "JSESSIONID=" @cas-session-id)}
           :follow-redirects false}
    (some? body)
    (request-with-json-body body)))

(defn- cas-http [client method url & [body]]
  (let [cas-client     (:client client)
        cas-params     (:params client)
        cas-session-id (:session-id client)
        http-fn        (case method
                         :get http/get
                         :post http/post)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params))))
    (let [resp @(http-fn url (create-params cas-session-id body))]
      (if (= 302 (:status resp))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params)))
          @(http-fn url (create-params cas-session-id body)))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url))

(defn cas-authenticated-post [client url body]
  (cas-http client :post url body))
