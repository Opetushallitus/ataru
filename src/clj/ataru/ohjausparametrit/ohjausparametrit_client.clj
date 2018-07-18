(ns ataru.ohjausparametrit.ohjausparametrit-client
  (:require [ataru.util.http-util :as hu]
            [ataru.config.url-helper :as url-helper]
            [clj-time.coerce :as t]))

(defn get-ohjausparametrit [haku-oid]
  (-> :ohjausparametrit-service.parametri
      (url-helper/resolve-url haku-oid)
      (hu/do-get)
      :body))
