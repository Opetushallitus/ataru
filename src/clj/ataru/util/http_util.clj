(ns ataru.util.http-util
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as log]))

(defn do-request
  [{:keys [url method] :as opts}]
  (let [opts        (update opts :headers merge {"Caller-Id" "ataru"})
        method-name (clojure.string/upper-case (name method))
        start       (System/currentTimeMillis)
        response    @(http/request opts)
        time        (- (System/currentTimeMillis) start)
        status      (:status response 500)]
    (when (or (<= 400 status) (< 1000 time))
      (log/warn "HTTP" method-name url status (str time "ms")))
    response))

(defn do-get
  [url]
  (do-request {:url url :method :get}))

(defn do-post
  [url opts]
  (do-request (assoc opts :url url :method :post)))

(defn do-delete
  [url]
  (do-request {:url url :method :delete}))
