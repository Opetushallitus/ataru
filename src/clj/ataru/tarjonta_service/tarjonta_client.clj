(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [cheshire.core :as json]
    [oph.soresu.common.config :refer [config]]
    [org.httpkit.client :as http]
    [taoensso.timbre :refer [warn]]))

(defn get-hakukohde
  [hakukohde-oid]
  (let [url      (str (get-in config [:tarjonta-service :hakukohde-base-url]) hakukohde-oid)
        response @(http/get url)
        status   (:status response)
        result   (when (= 200 status)
                   (-> response :body (json/parse-string true) :result))]
    (if result
      result
      (warn "could not retrieve hakukohde details" url status))))

(defn get-forms-in-use
  []
  (let [url      (get-in config [:tarjonta-service :forms-in-use-url])
        response @(http/get url)]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))