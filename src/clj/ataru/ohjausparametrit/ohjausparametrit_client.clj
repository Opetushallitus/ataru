(ns ataru.ohjausparametrit.ohjausparametrit-client
  (:require [ataru.util.http-util :as hu]
            [ataru.config.url-helper :as url-helper]
            [cheshire.core :as json]
            [clj-time.coerce :as t]))

(defn get-ohjausparametrit [haku-oid]
  (let [{:keys [status body]} (-> :ohjausparametrit-service.parametri
                                  (url-helper/resolve-url haku-oid)
                                  (hu/do-get))]
    (when (= 200 status)
      (json/parse-string body true))))
