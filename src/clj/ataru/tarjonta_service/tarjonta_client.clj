(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [taoensso.timbre :refer [warn info]]
    [clojure.string :as string]
    [ataru.util.http-util :as h]))

(defn get-hakukohde
  [hakukohde-oid]
  (h/do-request (resolve-url :tarjonta-service.hakukohde hakukohde-oid)))

(defn hakukohde-search
  [haku-oid organization-oid]
  (h/do-request (resolve-url :tarjonta-service.hakukohde.search
                           {"hakuOid" haku-oid
                            "defaultTarjoaja" organization-oid
                            "organisationOid" organization-oid})))

(defn get-haku
  [haku-oid]
  (h/do-request (resolve-url :tarjonta-service.haku haku-oid)))

(defn get-koulutus
  [koulutus-oid]
  (h/do-request (resolve-url :tarjonta-service.koulutus koulutus-oid)))

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
