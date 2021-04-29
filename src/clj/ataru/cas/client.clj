(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [cheshire.core :as json]
            [taoensso.timbre :as log]
            [clojure.core.match :refer [match]]
            [clojure.string :as s])
  (:import [fi.vm.sade.javautils.nio.cas CasClient CasConfig]
           [org.asynchttpclient RequestBuilder]
           ))

(defrecord CasClientState [client params session-cookie-name session-id])

(defn create-cas-config [service security-uri-suffix session-cookie-name caller-id]
  (let [
        username (get-in config [:cas :username])
        password (get-in config [:cas :password])
        cas-url (resolve-url :cas-client)
        service-url (s/str (resolve-url :url-virkailija) service)
        csrf (clojure.string/replace service "/" "")]
    (match session-cookie-name
           "JSESSIONID" (CasConfig/SpringSessionCasConfig
                          username
                          password
                          cas-url
                          service-url
                          csrf
                          caller-id)
           "ring-session" (CasConfig/RingSessionCasConfig
                            username
                            password
                            cas-url
                            service-url
                            csrf
                            caller-id)
           :else (new CasConfig
                   username
                   password
                   cas-url
                   service-url
                   csrf
                   caller-id
                   session-cookie-name
                   security-uri-suffix))))

(defn new-cas-client [service security-uri-suffix session-cookie-name caller-id]
  (new CasClient
       (create-cas-config service security-uri-suffix session-cookie-name caller-id)))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [cas-client (new-cas-client service security-uri-suffix session-cookie-name caller-id)]
    (map->CasClientState {:client              cas-client
                          ;:cas-config              cas-params
                          :session-cookie-name session-cookie-name
                          :session-id          (atom nil)})))

;(defn- request-with-json-body [request body]
;  (-> request
;      (assoc-in [:headers "Content-Type"] "application/json")
;      (assoc :body (json/generate-string body))))

;(defn- create-params [session-cookie-name cas-session-id body]
;  (cond-> {:cookies           {session-cookie-name {:value @cas-session-id :path "/"}}
;           :redirect-strategy :none
;           :throw-exceptions  false}
;          (some? body) (request-with-json-body body)))

(defn- cas-http [client method url opts-fn & [body]]
  (log/info "CREATE REQUEST...")
  (log/info "fix this opts-fn" opts-fn)
  (let [cas-client (:client client)
        ;cas-params (:params client)
        ;TODO HANDLE opts-fn???
        request (match method
                  :get (-> (RequestBuilder.)
                           (.setUrl url)
                           (.setMethod "GET")
                           (.build)
                           )
                  :post (-> (RequestBuilder.)
                            (.setUrl url)
                            (.setMethod "POST")
                            (.setHeader "Content-Type" "application/json")
                            (.setBody (json/generate-string body))
                            (.build))
                  :delete (-> (RequestBuilder.)
                              (.setUrl url)
                              (.setMethod "DELETE")
                              (.build)))
        ]
    (log/info "REQUEST" request)
    ;(when (nil? @cas-session-id)
      ;(reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name))))
    (let [resp (.executeBlocking cas-client request)
          status (.getStatusCode resp)
          body (.getResponseBody resp)
          response {:status status :body body}


          ;response.getStatusCode();
          ;response.getResponseBody();
          ;(http-util/do-request (merge {:url url :method method}
          ;                                  (opts-fn)
          ;                                  (create-params session-cookie-name cas-session-id body)))
          ]
      ;(if (or (= 401 (:status resp))
      ;        (= 302 (:status resp)))
        ;(do
        ;  (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name)))
        ;  (http-util/do-request (merge {:url url :method method}
        ;                               (opts-fn)
        ;                               (create-params session-cookie-name cas-session-id body))))
        ;resp)
      (log/info "RESPONSE STATUS: " status)
      ;(log/info "RESPONSE BODY: " body)
      response)
      ;)

    ))

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