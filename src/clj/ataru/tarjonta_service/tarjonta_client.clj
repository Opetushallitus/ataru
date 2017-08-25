(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [cheshire.core :as json]
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
  (do-request (resolve-url :tarjonta-service.hakukohde hakukohde-oid)))

(defn hakukohteet-by-organization
  [organization-oid]
  (do-request (resolve-url :tarjonta-service.hakukohde.search
                           {"defaultTarjoaja" organization-oid
                            "organisationOid" organization-oid})))

(defn hakukohteet-by-organization-group
  [organization-oid]
  (do-request (resolve-url :tarjonta-service.hakukohde.search
                           {"organisaatioRyhmaOid" organization-oid})))

(defn all-haut
  []
  (do-request (resolve-url :tarjonta-service.all-haut)))

(defn get-haku
  [haku-oid]
  (do-request (resolve-url :tarjonta-service.haku haku-oid)))

(defn get-koulutus
  [koulutus-oid]
  (do-request (resolve-url :tarjonta-service.koulutus koulutus-oid)))

(defn get-forms-in-use
  [organization-oids]
  (let [url      (resolve-url :tarjonta-service.forms-in-use)
        query    (when (< 0 (count organization-oids))
                   (str "?" (string/join "&" (map #(str "oid=" %) organization-oids))))
        response @(http/get (str url query))]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))
