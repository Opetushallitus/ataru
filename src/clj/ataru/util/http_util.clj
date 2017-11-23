(ns ataru.util.http-util
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :as json]))

(defn do-request
  [url]
  (log/info "HTTP GET:" url)
  (let [response @(http/get url)
        status   (:status response)
        result   (when (= 200 status)
                   (-> response :body (json/parse-string true) :result))]
    (if result
      result
      (log/warn "HTTP GET FAILED:" url status))))
