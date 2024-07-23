(ns ataru.util.http-util
  (:require [clj-http.client :as http-client]
            [clojure.string]
            [taoensso.timbre :as log]))

(def csrf-value "ataru")

;todo maybe only add csrf cookie to PUT POST DELETE?
(defn enrich-with-mandatory-headers-and-common-settings [opts]
  (-> opts
      (update :connection-timeout (fnil identity 300000))
      (update :socket-timeout (fnil identity 300000))
      (assoc  :throw-exceptions false)
      (update :headers merge
              {"Caller-Id" "1.2.246.562.10.00000000001.ataru.backend"}
              {"CSRF" csrf-value})
      (update :cookies merge {"CSRF" {:value csrf-value :path "/"}})))

(defn do-request
  [{:keys [url method] :as opts}]
  (let [method-name (clojure.string/upper-case (name method))
        opts        (enrich-with-mandatory-headers-and-common-settings opts)
        start       (System/currentTimeMillis)
        response    (http-client/request opts)
        time        (- (System/currentTimeMillis) start)
        status      (:status response 500)]
    (when (or (<= 400 status) (< 1000 time))
      (log/warn "HTTP" method-name url status (str time "ms")))
    response))

(defn do-get
  [url]
  (do-request {:url url :method :get}))

(defn do-get-stream
  [url]
  (do-request {:url url :method :get :as :stream}))

(defn do-post
  [url opts]
  (do-request (assoc opts :url url :method :post)))

(defn do-delete
  [url]
  (do-request {:url url :method :delete}))
