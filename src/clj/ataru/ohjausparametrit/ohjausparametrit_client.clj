(ns ataru.ohjausparametrit.ohjausparametrit-client
  (:require [ataru.util.http-util :as hu]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]))

(defn get-ohjausparametrit [haku-oid]
  (let [url                   (url-helper/resolve-url :ohjausparametrit-service.parametri haku-oid)
        {:keys [status body]} (hu/do-get url)]
    (case status
      200 (json/parse-string body true)
      404 nil
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))))
