(ns ataru.tarjonta-service.tarjonta-client
  (:require
    [ataru.config.url-helper :refer [resolve-url]]
    [ataru.util.http-util :as http-util]
    [cheshire.core :as json]
    [clojure.string :as string]
    [taoensso.timbre :refer [warn info]]))

(defn- try-get-result
  [url]
  (let [{:keys [status body]} (http-util/do-get url)]
    (when (= 200 status)
      (:result (json/parse-string body true)))))

(defn get-hakukohde
  [hakukohde-oid]
  (-> :tarjonta-service.hakukohde
      (resolve-url hakukohde-oid)
      try-get-result))

(defn hakukohde-search
  [{:keys [haku-oid organization-oid]}]
  (-> :tarjonta-service.hakukohde.search
      (resolve-url (cond-> {"hakuOid"         haku-oid
                            "defaultTarjoaja" organization-oid}
                           (some? organization-oid)
                           (assoc "organisationOid" organization-oid)))
      try-get-result))

(defn get-haku
  [haku-oid]
  (-> :tarjonta-service.haku
      (resolve-url haku-oid)
      try-get-result))

(defn get-koulutus
  [koulutus-oid]
  (-> :tarjonta-service.koulutus
      (resolve-url koulutus-oid)
      try-get-result))

(defn get-forms-in-use
  [organization-oid]
  (-> :tarjonta-service.forms-in-use
      (resolve-url {"oid" organization-oid})
      try-get-result))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))
