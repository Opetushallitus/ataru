(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.javautils.nio.cas CasConfig$CasConfigBuilder CasClientBuilder CasClient]
           [java.util.concurrent CompletableFuture]
           [org.asynchttpclient RequestBuilder Response]))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [username    (get-in config [:cas :username])
        password    (get-in config [:cas :password])
        cas-url     (resolve-url :cas-client)
        service-url (str (resolve-url :url-virkailija) service)
        cas-config  (-> (CasConfig$CasConfigBuilder. username password cas-url service-url caller-id caller-id security-uri-suffix)
                        (.setJsessionName session-cookie-name)
                        .build)
        cas-client  (CasClientBuilder/build cas-config)]
    (log/info "Created CAS client for service" service "with security URI suffix" security-uri-suffix)
    cas-client))

;; Convert Response into map that calling functions expeect
(defn response->map [^Response resp]
  {:status (.getStatusCode resp)
   :body   (.getResponseBody resp)})

;; Apply extra options to RequestBuilder
(defn apply-extra-opts [^RequestBuilder base-request extra-opts]
  (reduce (fn [req [k v]]
            (case k
              :headers
              (reduce (fn [r [header-key header-value]]
                        (.addHeader r (name header-key) (str header-value)))
                      req v)
              :query-params
              (reduce (fn [r [param-key param-value]]
                        (.addQueryParam r (name param-key) (str param-value)))
                      req v)
              :socket-timeout
              (.setRequestTimeout req v)
              req))
          base-request
          extra-opts))

(defn cas-authenticated-get [^CasClient client url]
  (log/info "Performing CAS authenticated GET to URL:" url)
  (try
    (when (nil? client)
      (throw (IllegalArgumentException. "Client cannot be null")))
    (when (nil? url)
      (throw (IllegalArgumentException. "URL cannot be null")))

    (let [request (-> (RequestBuilder.)
                      (.setMethod "GET")
                      (.setUrl url)
                      .build)
          ^CompletableFuture future (.executeAndRetryWithCleanSessionOnStatusCodes client request #{401 302})
          ^Response resp (.get future)]
      (response->map resp))

    (catch Exception e
      (log/error e "Error during CAS authenticated GET:" url)
      (throw e))))

(defn cas-authenticated-get-as-stream [^CasClient client url]
  (log/info "Performing CAS authenticated GET as stream to URL:" url)
  (let [request (-> (RequestBuilder.)
                    (.setMethod "GET")
                    (.setUrl url)
                    (.addHeader "Accept" "application/octet-stream")
                    .build)
        ^CompletableFuture future (.executeAndRetryWithCleanSessionOnStatusCodes client request #{401 302})
        ^Response resp (.get future)]
    (response->map resp)))

(defn cas-authenticated-delete [^CasClient client url & [opts-fn]]
  (log/debug "Performing CAS DELETE to URL:" url)
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "DELETE")
                         (.setUrl url)
                         (.addHeader "Content-Type" "application/json")
                         (.addHeader "Accept" "application/json"))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (apply-extra-opts base-request extra-opts)
        ^CompletableFuture future (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})
        ^Response resp (.get future)]
    (response->map resp)))

(defn- cas-authenticated-with-body [^CasClient client url method body & [opts-fn]]
  (let [payload (cond
                  (map? body)    (json/generate-string body)
                  (string? body) body
                  (nil? body)    nil
                  :else          (str body))
        base-request (-> (RequestBuilder.)
                         (.setMethod (name method))
                         (.setUrl url)
                         (.addHeader "Content-Type" "application/json")
                         (.addHeader "Accept" "application/json")
                         (cond-> payload (.setBody payload)))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (apply-extra-opts base-request extra-opts)
        ^CompletableFuture future (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})
        ^Response resp (.get future)]
    (response->map resp)))

(defn cas-authenticated-post [^CasClient client url body & [opts-fn]]
  (log/debug (str "Performing CAS authenticated POST to URL: " url
                  " with body type " (type body)
                  " body " body))
  (cas-authenticated-with-body client url :POST body opts-fn))

(defn cas-authenticated-put [^CasClient client url body & [opts-fn]]
  (cas-authenticated-with-body client url :PUT body opts-fn))

(defn cas-authenticated-patch [^CasClient client url body & [opts-fn]]
  (cas-authenticated-with-body client url :PATCH body opts-fn))

(defn cas-authenticated-multipart-post [^CasClient client url & [opts-fn]]
  (log/debug "Performing CAS multipart POST to URL:" url)
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "POST")
                         (.setUrl url)
                         (.addHeader "Content-Type" "multipart/form-data")
                         (.addHeader "Accept" "application/json"))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (apply-extra-opts base-request extra-opts)
        ^CompletableFuture future (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})
        ^Response resp (.get future)]
    (response->map resp)))
