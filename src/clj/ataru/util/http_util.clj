(ns ataru.util.http-util
  (:require [cheshire.core :as json]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log]))

(defn do-request
  [{:keys [url] :as opts}]
  (log/info "HTTP GET:" url)
  (let [opts     (update opts :headers merge {"clientSubSystemCode" "ataru" "Caller-Id" "ataru"})
        response @(http/request opts)
        status   (:status response)]
    (if (= 200 status)
      response
      (log/warn "HTTP GET FAILED:" url status))))

(defn do-get
  [url]
  (do-request {:url url :method :get}))

(defn do-post
  [url opts]
  (do-request (assoc opts :url url :method :post)))

(defn do-delete
  [url]
  (do-request {:url url :method :delete}))
