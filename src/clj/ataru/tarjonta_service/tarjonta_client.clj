(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.util.http-util :as http-util]
    [cheshire.core :as json]
    [clojure.string :as string]
    [taoensso.timbre :refer [warn info]]))

(defn get-hakukohde
  [hakukohde-oid]
  (-> :tarjonta-service.hakukohde
      (resolve-url hakukohde-oid)
      (http-util/do-get)
      :result))

(defn hakukohde-search
  [haku-oid organization-oid]
  (-> :tarjonta-service.hakukohde.search
      (resolve-url (cond-> {"hakuOid"         haku-oid
                            "defaultTarjoaja" organization-oid}
                           (some? organization-oid)
                           (assoc "organisationOid" organization-oid)))
      (http-util/do-get)
      :result))

(defn get-haku
  [haku-oid]
  (-> :tarjonta-service.haku
      (resolve-url haku-oid)
      (http-util/do-get)
      :result))

(defn get-koulutus
  [koulutus-oid]
  (-> :tarjonta-service.koulutus
      (resolve-url koulutus-oid)
      (http-util/do-get)
      :result))

(defn get-forms-in-use
  [organization-oid]
  (let [url      (resolve-url :tarjonta-service.forms-in-use)
        response (http-util/do-get (str url "?oid=" organization-oid))]
    (-> response :body (json/parse-string true) :result)))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))
