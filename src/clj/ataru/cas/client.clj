(ns ataru.cas.client
  (:require [ataru.config.url-helper :refer [resolve-url]]
            [ataru.config.core :refer [config]]
            [taoensso.timbre :as log])
  (:import [fi.vm.sade.javautils.nio.cas CasConfig$CasConfigBuilder CasClientBuilder]
           [org.asynchttpclient RequestBuilder]))


; used only in some tests
(defn new-cas-client [caller-id]
  (let [fake-config (-> (CasConfig$CasConfigBuilder. "fake-username" "fake-password" (resolve-url :cas-client) "http://fake-service-url" caller-id caller-id "fake-security-uri-suffix")
                        (.setJsessionName "fake-session-cookie-name")
                        .build)]
    (CasClientBuilder/build fake-config)))

(defn new-client [service security-uri-suffix session-cookie-name caller-id]
  {:pre [(some? (:cas config))]}
  (let [username   (get-in config [:cas :username])
        password   (get-in config [:cas :password])
        casUrl     (resolve-url :cas-client)
        serviceUrl (str (resolve-url :url-virkailija) service)
        cas-config (-> (CasConfig$CasConfigBuilder. username password casUrl serviceUrl caller-id caller-id security-uri-suffix)
                       (.setJsessionName session-cookie-name)
                       .build)
        cas-client (CasClientBuilder/build cas-config)]
    (log/info "Created CAS client for service" service "with security URI suffix" security-uri-suffix)
    cas-client))

(defn cas-authenticated-get [client url]
  (log/debug "Performing CAS authenticated GET to URL:" url)
  (try
    (when (nil? client)
      (throw (IllegalArgumentException. "Client cannot be null")))
    (when (nil? url)
      (throw (IllegalArgumentException. "URL cannot be null")))
    (let [request (-> (RequestBuilder.)
                      (.setMethod "GET")
                      (.setUrl url)
                      .build)]
      ;(.executeAndRetryWithCleanSessionOnStatusCodes client request #{401 302}))
      (.execute client request))
    (catch Exception e
      (do
        (log/error "Error during CAS authenticated GET: " (.getMessage e))
        (throw e)))))

(defn apply-extra-opts [base-request extra-opts]
  (reduce (fn [req [k v]]
            (case k
              :headers (reduce (fn [r [header-key header-value]]
                                 (.addHeader r header-key header-value))
                               req v)
              ;; future opts can be added here
              req))
          base-request
          extra-opts))

(defn cas-authenticated-delete [client url & [opts-fn]]
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "DELETE")
                         (.setUrl url)
                         (.addHeader "Content-type" "application/json")
                         (.addHeader "Accept" "application/json"))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (apply-extra-opts base-request extra-opts)]
    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))

(defn cas-authenticated-post [client url body & [opts-fn]]
  (log/debug "Performing CAS authenticated POST to URL:" url)
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "POST")
                         (.setUrl url)
                         (.addHeader "Content-type" "application/json")
                         (.addHeader "Accept" "application/json")
                         (.setBody body))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (apply-extra-opts base-request extra-opts)]
    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))

;(defn cas-authenticated-delete [client url & [opts-fn]]
;  (let [request (-> (RequestBuilder.)
;                    (.setMethod "DELETE")
;                    (.setUrl url)
;                    (.addHeader "Content-type" "application/json")
;                    (.addHeader "Accept" "application/json")
;                    .build)]
;    (.executeAndRetryWithCleanSessionOnStatusCodes client request #{401 302})))
;
;(defn cas-authenticated-post [client url body & [opts-fn]]
;  (log/debug "Performing CAS authenticated POST to URL:" url)
;  (let [base-request (-> (RequestBuilder.)
;                         (.setMethod "POST")
;                         (.setUrl url)
;                         (.addHeader "Content-type" "application/json")
;                         (.addHeader "Accept" "application/json")
;                         (.setBody body))
;        extra-opts   (if opts-fn (opts-fn) {})
;        request      (reduce (fn [req [k v]]
;                               (case k
;                                 :headers (reduce (fn [r [header-key header-value]]
;                                                    (.addHeader r header-key header-value))
;                                                  req v)
;                                 req))
;                             base-request
;                             extra-opts)]
;    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))

(defn cas-authenticated-multipart-post [client url body & [opts-fn]]
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "POST")
                         (.setUrl url)
                         (.addHeader "Content-type" "multipart/form-data")
                         (.addHeader "Accept" "application/json")
                         (.setBody body))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (reduce (fn [req [k v]]
                               (case k
                                 :headers (reduce (fn [r [header-key header-value]]
                                                    (.addHeader r header-key header-value))
                                                  req v)
                                 req))
                             base-request
                             extra-opts)]
    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))

(defn cas-authenticated-get-as-stream [cas-client url]
  (let [request (-> (RequestBuilder.)
                    (.setMethod "GET")
                    (.setUrl url)
                    (.addHeader "Accept" "application/octet-stream")
                    .build)]
;    (.executeAndRetryWithCleanSessionOnStatusCodes cas-client request #{401 302})))
    (.execute cas-client request)))

(defn cas-authenticated-patch [client url body & [opts-fn]]
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "PATCH")
                         (.setUrl url)
                         (.addHeader "Content-type" "application/json")
                         (.addHeader "Accept" "application/json")
                         (.setBody body))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (reduce (fn [req [k v]]
                               (case k
                                 :headers (reduce (fn [r [header-key header-value]]
                                                    (.addHeader r header-key header-value))
                                                  req v)
                                 req))
                             base-request
                             extra-opts)]
    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))

(defn cas-authenticated-put [client url body & [opts-fn]]
  (let [base-request (-> (RequestBuilder.)
                         (.setMethod "PUT")
                         (.setUrl url)
                         (.addHeader "Content-type" "application/json")
                         (.addHeader "Accept" "application/json")
                         (.setBody body))
        extra-opts   (if opts-fn (opts-fn) {})
        request      (reduce (fn [req [k v]]
                               (case k
                                 :headers (reduce (fn [r [header-key header-value]]
                                                    (.addHeader r header-key header-value))
                                                  req v)
                                 req))
                             base-request
                             extra-opts)]
    (.executeAndRetryWithCleanSessionOnStatusCodes client (.build request) #{401 302})))
