(ns ataru.tarjonta-service.tarjonta-client
  (:require
   [ataru.config.url-helper :refer [resolve-url]]
   [ataru.schema.form-schema :as schema]
   [ataru.util.http-util :as http-util]
   [cheshire.core :as json]
   [clojure.string :as string]
   [schema.core :as s]
   [taoensso.timbre :refer [warn info]]))

(def koulutus-checker (s/checker schema/Koulutus))

(defn- localized-names
  ([names]
   (localized-names names identity))
  ([names key-fn]
   (into {}
         (for [[input-k output-k] [[:kieli_fi :fi]
                                   [:kieli_sv :sv]
                                   [:kieli_en :en]]
               :let               [val (-> names input-k key-fn)]
               :when              (not (clojure.string/blank? val))]
           [output-k val]))))

(defn- parse-koulutuskoodi
  [koulutus]
  (-> koulutus
      :koulutuskoodi
      :meta
      (localized-names :nimi)))

(defn- parse-tutkintonimikes
  [koulutus]
  (mapv (fn [[_ name]]
          (-> name
              :meta
              (localized-names :nimi)))
        (-> koulutus :tutkintonimikes :meta)))

(defn- parse-koulutusohjelma
  [koulutus]
  (-> koulutus
      :koulutusohjelma
      :tekstis
      localized-names))

(defn- parse-koulutus
  [response]
  (cond-> {:oid                  (:oid response)
           :koulutuskoodi-name   (parse-koulutuskoodi response)
           :tutkintonimike-names (parse-tutkintonimikes response)
           :koulutusohjelma-name (parse-koulutusohjelma response)}
          (not (clojure.string/blank? (:tarkenne response)))
          (assoc :tarkenne (:tarkenne response))))

(defn- get-result
  [url]
  (let [{:keys [status body]} (http-util/do-get url)]
    (when (not= 200 status)
      (throw (new RuntimeException (str "Could not get " url ", "
                                        "status: " status ", "
                                        "body: " body))))
    (let [{:keys [status result]} (json/parse-string body true)]
      (when (not= "OK" status)
        (throw (new RuntimeException (str "Could not get " url ", "
                                          "tarjonta status: " status))))
      result)))

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
  [haku-oid organization-oid]
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

(s/defn ^:always-validate get-koulutus :- schema/Koulutus
  [koulutus-oid :- s/Str]
  (-> :tarjonta-service.koulutus
      (resolve-url koulutus-oid)
      get-result
      parse-koulutus))

(defn get-forms-in-use
  [organization-oid]
  (-> :tarjonta-service.forms-in-use
      (resolve-url {"oid" organization-oid})
      try-get-result))

(defn get-form-key-for-hakukohde
  [hakukohde-oid]
  (when-let [hakukohde (get-hakukohde hakukohde-oid)]
    (:ataruLomakeAvain hakukohde)))
