(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [cheshire.core :as json]
    [oph.soresu.common.config :refer [config]]
    [org.httpkit.client :as http]
    [taoensso.timbre :refer [warn]]))

(defn get-hakukohde [hakukohde-oid]
  (let [url      (str (get-in config [:tarjonta-service :hakukohde-base-url]) hakukohde-oid)
        response @(http/get url)
        status   (:status response)
        result   (when (= 200 status)
                   (-> response :body (json/parse-string true) :result))]
    (if result
      result
      (warn "could not retrieve hakukohde details" status hakukohde-oid))))

(defn get-form-key-for-hakukohde [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))