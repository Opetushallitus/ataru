(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [cheshire.core :as json]
    [oph.soresu.common.config :refer [config]]
    [org.httpkit.client :as http]
    [taoensso.timbre :refer [warn info]]
    [clojure.string :as string]))

(defn- do-request
  [url]
  (info "Fetching from tarjonta:" url)
  (let [response @(http/get url)
        status   (:status response)
        result   (when (= 200 status)
                   (-> response :body (json/parse-string true) :result))]
    (if result
      result
      (warn "Tarjonta API request failed" url status))))

(defn get-hakukohde
  [hakukohde-oid]
  (do-request (str
                (get-in config [:tarjonta-service :hakukohde-base-url])
                hakukohde-oid)))

(defn get-haku
  [haku-oid]
  (do-request (str
                (get-in config [:tarjonta-service :haku-base-url])
                haku-oid)))

(defn get-koulutus
  [koulutus-oid]
  (do-request (str
                (get-in config [:tarjonta-service :koulutus-base-url])
                koulutus-oid)))

(defn get-forms-in-use
  [organization-oids]
  (let [url      (get-in config [:tarjonta-service :forms-in-use-url])
        query    (when (< 0 (count organization-oids))
                   (str "?" (string/join "&" (map #(str "oid=" %) organization-oids))))
        response @(http/get (str url query))]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))