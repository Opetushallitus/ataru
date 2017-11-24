(ns ataru.util.http-util
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :as json]))

(defn do-request
  [url]
  (log/info "HTTP GET:" url)
  (let [response @(http/get url)
        status   (:status response)
        body     (when (= 200 status)
                   (-> response :body (json/parse-string true)))]
    (if body
      body
      (log/warn "HTTP GET FAILED:" url status))))
