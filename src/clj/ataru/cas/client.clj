(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [ataru.util.http-util :as http-util]
            [cheshire.core :as json])
  (:import [fi.vm.sade.utils.cas CasClient CasParams]
           [org.http4s.client.blaze package$]))

(defrecord CasClientState [client params session-cookie-name session-id])

(defn new-cas-client [caller-id]
  (new CasClient
       (resolve-url :cas-client)
       (.defaultClient package$/MODULE$)
       caller-id))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [username   (get-in config [:cas :username])
        password   (get-in config [:cas :password])
        cas-params (CasParams/apply service security-uri-suffix username password)
        cas-client (new-cas-client caller-id)]
    (map->CasClientState {:client              cas-client
                          :params              cas-params
                          :session-cookie-name session-cookie-name
                          :session-id          (atom nil)})))

(defn- request-with-json-body [request body]
  (-> request
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :body (json/generate-string body))))

(defn- create-params [session-cookie-name cas-session-id body]
  (cond-> {:cookies          {session-cookie-name  {:value @cas-session-id :path "/"}}
           :redirect-strategy :none
           :throw-exceptions false}
          (some? body) (request-with-json-body body)))

(defn- cas-http [client method url opts-fn & [body]]
  (let [cas-client          (:client client)
        cas-params          (:params client)
        session-cookie-name (:session-cookie-name client)
        cas-session-id      (:session-id client)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name))))
    (let [resp (http-util/do-request (merge {:url url :method method}
                                            (opts-fn)
                                            (create-params session-cookie-name cas-session-id body)))]
      (if (or (= 401 (:status resp))
              (= 302 (:status resp)))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name)))
          (http-util/do-request (merge {:url url :method method}
                                       (opts-fn)
                                       (create-params session-cookie-name cas-session-id body))))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url (constantly {})))

(defn cas-authenticated-delete [client url]
  (cas-http client :delete url (constantly {})))

(defn cas-authenticated-post [client url body]
  (cas-http client :post url (constantly {}) body))

(defn cas-authenticated-multipart-post [client url opts-fn]
  (cas-http client :post url opts-fn nil))

(defn cas-authenticated-get-as-stream [client url]
  (cas-http client :get url (constantly {:as :stream}) nil))