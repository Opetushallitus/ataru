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
  (-> :tarjonta-service.hakukohde
      (resolve-url hakukohde-oid)
      (h/do-request)
      :result))

(defn hakukohde-search
  [haku-oid organization-oid]
  (-> :tarjonta-service.hakukohde.search
      (resolve-url (cond-> {"hakuOid"         haku-oid
                            "defaultTarjoaja" organization-oid}
                           (some? organization-oid)
                           (assoc "organisationOid" organization-oid)))
      (h/do-request)
      :result))

(defn get-haku
  [haku-oid]
  (-> :tarjonta-service.haku
      (resolve-url haku-oid)
      (h/do-request)
      :result))

(defn get-koulutus
  [koulutus-oid]
  (-> :tarjonta-service.koulutus
      (resolve-url koulutus-oid)
      (h/do-request)
      :result))

(defn get-forms-in-use
  [organization-oid]
  (let [url      (resolve-url :tarjonta-service.forms-in-use)
        response @(http/get (str url "?oid=" organization-oid))]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))
