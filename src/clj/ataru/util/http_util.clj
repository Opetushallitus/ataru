(ns ataru.util.http-util
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as log]))


(defn add-cookie [cookie-to-add opts]
  (let [existing-cookies (get (get opts :headers) "Cookie")
        cookies  (if existing-cookies (str existing-cookies "; " cookie-to-add) cookie-to-add)]
    (update opts :headers merge {"Cookie" cookies})))

(def csrf-value "ataru")

;todo maybe only add csrf cookie to PUT POST DELETE?
(defn enrich-with-mandatory-headers [opts]
  (add-cookie (str "CSRF="csrf-value)
              (update opts :headers merge
                      {"Caller-Id" "1.2.246.562.10.00000000001.ataru.backend"}
                      {"CSRF" csrf-value})))

(defn do-request
  [{:keys [url method] :as opts}]
  (let [method-name (clojure.string/upper-case (name method))
        opts        (enrich-with-mandatory-headers opts)
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
