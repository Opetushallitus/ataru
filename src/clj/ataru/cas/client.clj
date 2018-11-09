(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json]
            [clj-util.cas :as cas]))

(defrecord CasClient [client params session-id])

(defn new-client [cas-uri]
  {:pre [(some? (:cas config))]}
  (let [username   (get-in config [:cas :username])
        password   (get-in config [:cas :password])
        cas-url    (resolve-url :cas-client)
        cas-params (cas/cas-params cas-uri username password)
        cas-client (cas/cas-client cas-url)]
    (map->CasClient {:client     cas-client
                     :params     cas-params
                     :session-id (atom nil)})))

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
        cas-session-id (:session-id client)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params))))
    (let [resp (http-util/do-request (merge {:url url :method method}
                                            (create-params cas-session-id body)))]
      (if (= 302 (:status resp))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params)))
          (http-util/do-request (merge {:url url :method method}
                                       (create-params cas-session-id body))))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url))

(defn cas-authenticated-post [client url body]
  (cas-http client :post url body))
