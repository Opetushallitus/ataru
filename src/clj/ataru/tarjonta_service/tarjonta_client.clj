(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [cheshire.core :as json]
    [oph.soresu.common.config :refer [config]]
    [org.httpkit.client :as http]
    [taoensso.timbre :refer [warn]]
    [clojure.string :as string]))

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
  [organization-oids]
  (let [url      (get-in config [:tarjonta-service :forms-in-use-url])
        query    (when (< 0 (count organization-oids))
                   (str "?" (string/join "&" (map #(str "oid=" %) organization-oids))))
        response @(http/get (str url query))]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))